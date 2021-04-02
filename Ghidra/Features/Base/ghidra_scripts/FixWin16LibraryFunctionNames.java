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
// Script ensures that the PASCAL calling convention replaces STDCALL
// on function parameters and changes the stack reference for left-to-
// right stacking.  On the way, it also ensures that all Thunks are
// also converted.  This applies to Windows 16-bit apps.
//
//@category Repair
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import generic.jar.ResourceFile;

import java.util.Properties;

import ghidra.app.script.GhidraScript;
import ghidra.framework.Application;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Parameter;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;

public class FixWin16LibraryFunctionNames extends GhidraScript {

	private static HashMap<String, String> fnNameMap = new HashMap<>();

	private int cntFilenamesTotal;
	private int cntFilenamesChanged;

	private List<String> warningMessages = new ArrayList<String>();

	@Override
	public void run() throws Exception {

		// reset for this run
		cntFilenamesTotal = 0;
		cntFilenamesChanged = 0;

		warningMessages.clear();

		if (currentProgram != null) {
			FunctionManager fnMgr = currentProgram.getFunctionManager();
			if (fnMgr == null) {
				return;
			}

			doRun(fnMgr.getFunctions(true));
			doRun(fnMgr.getExternalFunctions());

			// build popup information
			StringBuffer buf = new StringBuffer("Updated " + cntFilenamesChanged
					+ " out of " + cntFilenamesTotal + " functions found.");
			Iterator<String> messages = warningMessages.iterator();
			while (messages.hasNext()) {
				buf.append("\n\n").append(messages.next());
			}

			popup(buf.toString());

		}
	}

	/**
	 * @param functions
	 */
	private void doRun(FunctionIterator functions) {
		while (functions.hasNext()) {
			if ((getMonitor() != null) && getMonitor().isCancelled()) {
				return;
			}

			doRun(functions.next());
		}
	}

	/**
	 * Do for individually identified function
	 * 
	 * @param func this function
	 * @throws InvalidInputException
	 */
	protected void doRun(Function func) {

//		println("Before: " + func.getName() + ": " + func.getCallingConventionName()
//				+ " and isExternal()=" + func.isExternal() + ", isThunk()=" + func.isThunk()
//				+ getDescription(func));

		String updatedName = null;
		String currentName = null;
		try {
			currentName = func.getName();
			updatedName = translateFunctionName(currentName);
			if (!updatedName.contentEquals(currentName)) {
				++cntFilenamesTotal;
				func.setName(updatedName, func.getSignatureSource());
				++cntFilenamesChanged;
				println("Renamed function " + currentName + " to " + updatedName);
			}
		} catch (DuplicateNameException | InvalidInputException e) {
			warningMessages.add("Could not rename function " + currentName + " to " +
					updatedName + ".  Error " + e.getStackTrace());
		}

//		println(" After: " + func.getName() + ":" + getDescription(func));
	}

	/**
	 * @param f Function object
	 * @return string to print out
	 */
//	private static String getDescription(Function f) {
//		StringBuilder s = new StringBuilder();
//		for (int i = 0; i < f.getParameters().length; i++) {
//			Parameter p = f.getParameter(i);
//			s.append(" ").append(p.getName()).append("[")
//					.append(p.getLastStorageVarnode().toString()).append("]");
//		}
//		return s.toString();
//	}

	/**
	 * @param origFnName
	 * @return the updated function name
	 */
	private String translateFunctionName(String origFnName) {
		if (fnNameMap.isEmpty()) {
			Properties prop = new Properties();
	        try
	        {
	    		ResourceFile file = Application.findDataFileInAnyModule("FunctionNames.properties");
//	    		FileInputStream inputStream = new FileInputStream("D:/ReverseEngineering/FunctionNames.properties");
	            prop.load(file.getInputStream());
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	            warningMessages.add("Some issue finding or loading file....!!! " + e.getMessage());
	            return origFnName;

	        }
	        for (final Entry<Object, Object> entry : prop.entrySet()) {
	        	if (fnNameMap.containsKey(entry.getKey().toString())) {
	        		warningMessages.add("Multiple translations exist for " + entry.getKey().toString());
	        	}
	        	fnNameMap.put(entry.getKey().toString(), entry.getValue().toString());
	        }
		}
		
		return fnNameMap.getOrDefault(origFnName, origFnName);
	}

}
