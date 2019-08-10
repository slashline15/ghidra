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
package ghidra.app.plugin.core.format;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ghidra.util.HelpLocation;

public class FloatFormatModel implements UniversalDataFormatModel {

	private final int symbolSize;

	public FloatFormatModel() {
		symbolSize = 13;
	}
	
	/**
	 * Get the number of bytes to make a unit.  float is 4 bytes
	 */
	@Override
	public int getUnitByteSize() {
		return 4;
	}

	@Override
	public String getName() {
		return "Float";
	}

	@Override
	public HelpLocation getHelpLocation() {
		//TODO  Would need a Float section
		return new HelpLocation("ByteViewerPlugin", "formats");
	}

	/**
	 * Get the number of characters required to display a unit
	 */
	@Override
	public int getDataUnitSymbolSize() {
		return symbolSize;
	}

	/**
	 * Get the byte used to generate the character at a given position
	 * TODO  is this possible/reasonable in float?
	 */
	@Override
	public int getByteOffset(ByteBlock block, int position) {
		return 0;
	}

	/**
	 * Get the column position from the byte offset of a unit
	 * TODO  is this possible/reasonable in float?
	 */
	@Override
	public int getColumnPosition(ByteBlock block, int byteOffset) {
		return 0;
	}

	/**
	 * Convert a 4 byte int to a float and return its string
	 */
	@Override
	public String getDataRepresentation(ByteBlock block, BigInteger index) throws ByteBlockAccessException {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(block.getInt(index));
		b.rewind();
		b.order(block.isBigEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
		float f = b.getFloat();
		return Float.toString(f);
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public boolean replaceValue(ByteBlock block, BigInteger index, int pos, char c) throws ByteBlockAccessException {
		return false;
	}

	@Override
	public int getGroupSize() {
		return 1;
	}

	@Override
	public void setGroupSize(int groupSize) {
		throw new UnsupportedOperationException("groups are not supported");
	}

	@Override
	public int getUnitDelimiterSize() {
		return 1;
	}

	@Override
	public boolean validateBytesPerLine(int bytesPerLine) {
		return true;
	}

	@Override
	public void dispose() {
	}

}
