/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.program.model.correlate;

import static ghidra.util.datastruct.Duo.Side.*;

import java.util.*;
import java.util.Map.Entry;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.block.CodeBlock;
import ghidra.program.model.listing.*;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.util.ListingAddressCorrelation;
import ghidra.util.datastruct.Duo;
import ghidra.util.datastruct.Duo.Side;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

/**
 * Correlator to construct a 1-1 map between the Instructions of two similar Functions. Matching is performed
 * via a greedy algorithm that looks for sequences (n-grams) of Instructions that are similar between the two Functions.
 * Similarity of two sequences is determined by comparing hashes generated by the HashCalculator object.
 * 
 * 1) Potential sequences and their hashes are generated for both functions (see HashStore).
 * 2) Sequences are pulled from the HashStore based on the uniqueness of a potential match and on the size of the sequence.
 * 3) If a unique match is found between sequences, it is extended in either direction as far as possible,
 *    as constrained by HashCalculator and the containing basic-blocks.
 * 4) The matching Instruction pairs are put in the final map and removed from further sequence lists
 *    to allow other potential matches to be considered.
 * 5) Sequences with no corresponding match are also removed from consideration.
 * 6) Sequences are limited to a single basic-block, and the algorithm is basic-block aware.
 *    Once a match establishes a correspondence between a pair of basic blocks, the algorithm uses
 * 7) If a particular sequence has matches that are not unique, the algorithm tries to disambiguate the potential
 *    matches by looking at parent/child relationships of the containing basic-blocks. (see DisambiguateStrategy)
 * 8) Multiple passes are attempted, each time the set of potential sequences is completely regenerated,
 *    varying the range of sequence sizes for which a match is attempted and other hash parameters. This
 *    allows matches discovered by earlier passes to disambiguate sequences in later passes. 
 *
 */
public class HashedFunctionAddressCorrelation implements ListingAddressCorrelation {

	/**
	 * A helper class for sorting through, disambiguating, sequences with identical hashes 
	 *
	 */
	private static class DisambiguatorEntry {
		public Hash hash;			// The disambiguating (secondary) hash
		public int count;			// Number of sequences (n-grams) in the subset matching the secondary hash
		public InstructHash instruct;	// (Starting Instruction of) the n-gram	

		public DisambiguatorEntry(Hash h, InstructHash inst) {
			hash = h;
			instruct = inst;
			count = 1;
		}
	}

	private Duo<Function> functions;
	private TreeMap<Address, Address> srcToDest;		// Final source -> destination address mapping
	private TreeMap<Address, Address> destToSrc;		// Final destination -> source address mapping
	private HashStore srcStore;						// Sorted list of source n-grams from which to draw potential matches
	private HashStore destStore;					// List of destination n-grams
	private HashCalculator hashCalc;				// Object that calculates n-gram hashes
	private TaskMonitor monitor;

	/**
	 * Correlates addresses between the two specified functions.
	 * @param leftFunction the first function
	 * @param rightFunction the second function
	 * @param monitor the task monitor that indicates progress and allows the user to cancel.
	 * @throws CancelledException if the user cancels
	 * @throws MemoryAccessException if either functions memory can't be accessed.
	 */
	public HashedFunctionAddressCorrelation(Function leftFunction, Function rightFunction,
			TaskMonitor monitor) throws CancelledException, MemoryAccessException {
		if (leftFunction == null || rightFunction == null) {
			throw new IllegalArgumentException("Functions can't be null!");
		}
		this.functions = new Duo<>(leftFunction, rightFunction);
		this.monitor = monitor;
		srcToDest = new TreeMap<Address, Address>();
		destToSrc = new TreeMap<Address, Address>();
		srcStore = new HashStore(leftFunction, monitor);
		destStore = new HashStore(rightFunction, monitor);
		hashCalc = new MnemonicHashCalculator();
		calculate();
		buildFinalMaps();
	}

	@Override
	public Program getProgram(Side side) {
		return functions.get(side).getProgram();
	}

	@Override
	public AddressSetView getAddresses(Side side) {
		return functions.get(side).getBody();
	}

	/**
	 * Gets the total number of instructions that are in the first function.
	 * @return the first function's instruction count.
	 */
	public int getTotalInstructionsInFirst() {
		return srcStore.getTotalInstructions();
	}

	/**
	 * Gets the total number of instructions that are in the second function.
	 * @return the second function's instruction count.
	 */
	public int getTotalInstructionsInSecond() {
		return destStore.getTotalInstructions();
	}

	/**
	 * Determines the number of instructions from the first function that match an instruction
	 * in the second function.
	 * @return the number of instructions in the first function that have matches.
	 */
	public int numMatchedInstructionsInFirst() {
		return srcStore.numMatchedInstructions();
	}

	/**
	 * Determines the number of instructions from the second function that match an instruction
	 * in the first function.
	 * @return the number of instructions in the second function that have matches.
	 */
	public int numMatchedInstructionsInSecond() {
		return destStore.numMatchedInstructions();
	}

	/**
	 * Determines the number of instructions from the first function that do not match an 
	 * instruction in the second function.
	 * @return the number of instructions in the first function without matches.
	 */
	public List<Instruction> getUnmatchedInstructionsInFirst() {
		return srcStore.getUnmatchedInstructions();
	}

	/**
	 * Determines the number of instructions from the second function that do not match an 
	 * instruction in the first function.
	 * @return the number of instructions in the second function without matches.
	 */
	public List<Instruction> getUnmatchedInstructionsInSecond() {
		return destStore.getUnmatchedInstructions();
	}

	/**
	 * Finalize a match between two n-grams.  Extend the match is possible, add the matching Instruction pairs to
	 * the final map, and remove the Instructions from further match consideration.
	 * @param srcEntry is the matching source HashEntry
	 * @param srcInstruct is (the starting Instruction of) the source n-gram
	 * @param destEntry is the matching destination HashEntry
	 * @param destInstruct is (the starting Instruction of) the destination n-gram
	 * @throws MemoryAccessException
	 */
	private void declareMatch(HashEntry srcEntry, InstructHash srcInstruct, HashEntry destEntry,
			InstructHash destInstruct) throws MemoryAccessException {
		boolean cancelMatch = false;
		int matchSize = srcEntry.hash.size;
		// Its possible that some instructions of the n-gram have already been matched
		if (!srcInstruct.allUnknown(matchSize)) {	// If any source n-gram instructions are already matched
			srcStore.removeHash(srcEntry);			// Remove this HashEntry
			cancelMatch = true;						// Cancel the match
		}
		if (!destInstruct.allUnknown(matchSize)) {	// If any destination n-gram instructions are already matched
			destStore.removeHash(destEntry);		// Remove this HashEntry
			cancelMatch = true;						// Cancel the match
		}
		if (cancelMatch)
			return;
		ArrayList<Instruction> srcInstructVec = new ArrayList<Instruction>();
		ArrayList<Instruction> destInstructVec = new ArrayList<Instruction>();
		ArrayList<CodeBlock> srcBlockVec = new ArrayList<CodeBlock>();
		ArrayList<CodeBlock> destBlockVec = new ArrayList<CodeBlock>();
		HashStore.NgramMatch srcMatch = new HashStore.NgramMatch();
		HashStore.NgramMatch destMatch = new HashStore.NgramMatch();
		HashStore.extendMatch(matchSize, srcInstruct, srcMatch, destInstruct, destMatch, hashCalc);
		srcStore.matchHash(srcMatch, srcInstructVec, srcBlockVec);
		destStore.matchHash(destMatch, destInstructVec, destBlockVec);
		for (int i = 0; i < srcInstructVec.size(); ++i)
			srcToDest.put(srcInstructVec.get(i).getAddress(), destInstructVec.get(i).getAddress());
	}

	/**
	 * Given multiple n-grams producing the same hash, generate secondary hashes based on the each n-gram and
	 * its containing block, using the DisambiguateStrategy.  Generate a histogram of secondary hashes and return
	 * it to the caller.
	 * @param entry is the HashEntry producing the duplicates
	 * @param strategy is the DisambiguateStrategy to use to produce secondary hashes
	 * @return the map of DisambiguatorEntry objects containing the histogram counts
	 * @throws CancelledException
	 * @throws MemoryAccessException
	 */
	private static TreeMap<Hash, DisambiguatorEntry> constructDisambiguatorTree(HashEntry entry,
			HashStore store, DisambiguateStrategy strategy)
			throws CancelledException, MemoryAccessException {
		TreeMap<Hash, DisambiguatorEntry> entryMap = new TreeMap<Hash, DisambiguatorEntry>();
		int matchSize = entry.hash.size;
		for (InstructHash curInstruct : entry.instList) {
			ArrayList<Hash> hashList = strategy.calcHashes(curInstruct, matchSize, store);
			Iterator<Hash> iter = hashList.iterator();
			while (iter.hasNext()) {
				Hash curHash = iter.next();
				DisambiguatorEntry curEntry = entryMap.get(curHash);
				if (curEntry == null) {
					curEntry = new DisambiguatorEntry(curHash, curInstruct);
					entryMap.put(curHash, curEntry);
				}
				else
					curEntry.count += 1;
			}
		}
		return entryMap;
	}

	/**
	 * Try to disambiguate n-grams with a single hash using a specific secondary hash strategy
	 * @param strategy is the DisambiguateStrategy to use for secondary hashes
	 * @param srcEntry is the collection of n-grams with the same hash on the source side
	 * @param destEntry is the collection of n-grams with the same hash on the destination side
	 * @return the number of disambiguated matches successfully discovered
	 * @throws CancelledException
	 * @throws MemoryAccessException
	 */
	private int disambiguateNgramsWithStrategy(DisambiguateStrategy strategy, HashEntry srcEntry,
			HashEntry destEntry) throws CancelledException, MemoryAccessException {
		TreeMap<Hash, DisambiguatorEntry> srcDisambig =
			constructDisambiguatorTree(srcEntry, srcStore, strategy);
		TreeMap<Hash, DisambiguatorEntry> destDisambig =
			constructDisambiguatorTree(destEntry, destStore, strategy);
		int count = 0;
		Iterator<DisambiguatorEntry> iter = srcDisambig.values().iterator();
		while (iter.hasNext()) {
			DisambiguatorEntry srcDisEntry = iter.next();
			if (srcDisEntry.count != 1)
				continue;
			// Its possible for this InstructHash to have been matched by an earlier DisambiguatorEntry
			if (srcDisEntry.instruct.isMatched)
				continue;
			DisambiguatorEntry destDisEntry = destDisambig.get(srcDisEntry.hash);
			if (destDisEntry == null)
				continue;
			if (destDisEntry.count != 1)
				continue;
			if (destDisEntry.instruct.isMatched)
				continue;
			// If both sides have exactly one matching InstructHash, call it a match
			declareMatch(srcEntry, srcDisEntry.instruct, destEntry, destDisEntry.instruct);
			count += 1;
		}
		return count;
	}

	/**
	 * Attempt to disambiguate n-gram pairs with the same hash using various strategies
	 * @param srcEntry is the collection of n-grams with the same hash on the source side
	 * @param destEntry is the collection of n-grams with the same hash on the destination side
	 * @return true if at least one pair was matched
	 * @throws CancelledException
	 * @throws MemoryAccessException
	 */
	private boolean disambiguateMatchingNgrams(HashEntry srcEntry, HashEntry destEntry)
			throws CancelledException, MemoryAccessException {
		if (srcEntry.hasDuplicateBlocks())
			return false;
		if (destEntry.hasDuplicateBlocks())
			return false;
		if (srcEntry.hash.size != destEntry.hash.size)
			return false;		// This likely never happens, because we know the hash values are equal
		int count = disambiguateNgramsWithStrategy(new DisambiguateByParent(), srcEntry, destEntry);
		if (count != 0)
			return true;
		count = disambiguateNgramsWithStrategy(new DisambiguateByChild(), srcEntry, destEntry);
		if (count != 0)
			return true;
		count = disambiguateNgramsWithStrategy(new DisambiguateByBytes(), srcEntry, destEntry);
		if (count != 0)
			return true;
		count = disambiguateNgramsWithStrategy(new DisambiguateByParentWithOrder(), srcEntry,
			destEntry);
		if (count != 0)
			return true;
		return false;
	}

	/**
	 * Check for matches through one set of n-grams.  If non-unique matches exist, attempt to disambiguate.
	 * This assumes that the srcStore and destStore HashStores have already been populated with the n-gram lists
	 * @throws MemoryAccessException
	 * @throws CancelledException
	 */
	private void findMatches() throws MemoryAccessException, CancelledException {
		while (!srcStore.isEmpty() && !destStore.isEmpty()) {
			HashEntry srcEntry = srcStore.getFirstEntry();
			HashEntry destEntry = destStore.getEntry(srcEntry.hash);
			if (destEntry == null) {
				srcStore.removeHash(srcEntry);	// No match at all
			}
			else if (srcEntry.instList.size() == 1 && destEntry.instList.size() == 1) {
				// Found a unique match
				declareMatch(srcEntry, srcEntry.instList.getFirst(), destEntry,
					destEntry.instList.getFirst());
			}
			else {
				HashEntry destEntry2 = destStore.getFirstEntry();
				HashEntry srcEntry2 = srcStore.getEntry(destEntry2.hash);
				if (srcEntry2 == null) {
					destStore.removeHash(destEntry2);	// No match at all
				}
				else if (srcEntry2.instList.size() == 1 && destEntry2.instList.size() == 1) {
					// Found a unique match
					declareMatch(srcEntry2, srcEntry2.instList.getFirst(), destEntry2,
						destEntry2.instList.getFirst());
				}
				else {
					if (!disambiguateMatchingNgrams(srcEntry, destEntry))
						srcStore.removeHash(srcEntry);
				}
			}
		}
	}

	/**
	 * Run multiple passes with one n-gram/hash generation configuration. A pass consists of generating the sorted
	 * lists of n-grams for both sides, running through the lists looking for matches, and, if matches aren't unique,
	 * running through the disambiguation strategies. Matches that aren't unique and couldn't be disambiguated in one
	 * pass may be matched in later passes.
	 * @param minLength is the minimum length of an n-gram for these passes
	 * @param maxLength is the maximum length of an n-gram for these passes
	 * @param wholeBlock if true, allows blocks that are smaller than the minimum length to be considered as 1 n-gram.
	 * @param matchBlock if true, only generates n-grams for sequences in previously matched blocks
	 * @param maxPasses is the number of passes to run with this configuration
	 * @throws MemoryAccessException
	 * @throws CancelledException
	 */
	private void runPasses(int minLength, int maxLength, boolean wholeBlock, boolean matchBlock,
			int maxPasses) throws MemoryAccessException, CancelledException {
		srcStore.calcHashes(minLength, maxLength, wholeBlock, matchBlock, hashCalc);
		destStore.calcHashes(minLength, maxLength, wholeBlock, matchBlock, hashCalc);
		for (int pass = 0; pass < maxPasses; ++pass) {
			int curMatch = srcStore.numMatchedInstructions();
			if (curMatch == srcStore.getTotalInstructions())
				break;			// quit if there are no unmatched instructions
			srcStore.clearSort();
			destStore.clearSort();

			srcStore.insertHashes();
			destStore.insertHashes();

			findMatches();
			if (curMatch == srcStore.numMatchedInstructions())
				break;		// quit if no new matched instructions
		}
	}

	/**
	 * High-level control of the matching passes. Tries different sequence generation configurations,
	 * terminating early if all instructions are matched.
	 * @throws MemoryAccessException
	 * @throws CancelledException
	 */
	private void calculate() throws MemoryAccessException, CancelledException {
		// Try one pass with range of comparatively long sequences, 5 to 10 instructions, with no specialized constraints
		// With very similar functions, this should match the bulk of the instructions
		srcStore.calcHashes(5, 10, false, false, hashCalc);
		srcStore.insertHashes();
		destStore.calcHashes(5, 10, false, false, hashCalc);
		destStore.insertHashes();

		findMatches();

		if (srcStore.numMatchedInstructions() == srcStore.getTotalInstructions())
			return;
		if (destStore.numMatchedInstructions() == destStore.getTotalInstructions())
			return;

		// Now try multiple passes of 3 and 4 long n-grams hopefully filling in a lot of small holes in our match
		// given a scaffolding of previously matched basic blocks
		runPasses(3, 4, true, true, 10);

		if (srcStore.numMatchedInstructions() == srcStore.getTotalInstructions())
			return;
		if (destStore.numMatchedInstructions() == destStore.getTotalInstructions())
			return;

		// Repeat with big n-grams
		int curMatch = srcStore.numMatchedInstructions();
		runPasses(5, 10, false, false, 3);

		if (srcStore.numMatchedInstructions() == curMatch)
			return;		// No progress
		if (srcStore.numMatchedInstructions() == srcStore.getTotalInstructions())
			return;
		if (destStore.numMatchedInstructions() == destStore.getTotalInstructions())
			return;

		// Repeat with small n-grams
		runPasses(3, 4, true, true, 10);
	}

	/**
	 * {@literal Given the src -> dest map, build the dest -> src map}
	 */
	private void buildFinalMaps() {
		for (Entry<Address, Address> entry : srcToDest.entrySet()) {
			destToSrc.put(entry.getValue(), entry.getKey());		// Build the reverse map of srcToDest
		}
	}

	/**
	 * Gets an iterator of the matching addresses from the first function to the second.
	 * @return the iterator
	 */
	public Iterator<Entry<Address, Address>> getFirstToSecondIterator() {
		return srcToDest.entrySet().iterator();
	}

	@Override
	public Address getAddress(Side side, Address otherSideAddress) {
		if (side == LEFT) {
			return destToSrc.get(otherSideAddress);
		}
		return srcToDest.get(otherSideAddress);
	}

	@Override
	public Function getFunction(Side side) {
		return functions.get(side);
	}
}
