/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License; Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing; software
 * distributed under the License is distributed on an "AS IS" BASIS;
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.util.bin.format.mufom;

import java.io.IOException;

import ghidra.app.util.bin.BinaryReader;
import ghidra.util.Msg;

/*
 * Value Records (ASN)
 *
 * AS-command → “AS” MUFOM-variable “,” expression “,”
 *
 * N-variable → “N” hexnumber
 */
public class MufomASN extends MufomRecord {
	public static final String NAME = "ASN";
	public static final int record_type = MufomType.MUFOM_CMD_AS;
	public static final int record_subtype = MufomType.MUFOM_ID_N;
	public long record_start = -1;
	public long symbol_name_index = -1;
	public long symbol_name_value = -1;

	private void print() {
		String msg = NAME + ": " + symbol_name_index + " " + symbol_name_value;
		if (do_debug) {
			Msg.info(this, msg);
		} else {
			Msg.trace(this, msg);
		}
	}

	public MufomASN(BinaryReader reader) throws IOException {
		record_start = reader.getPointerIndex();
		read_record_type(reader, record_type, record_subtype, NAME);
		symbol_name_index = read_int(reader);
		symbol_name_value = read_int(reader);
		print();
	}
}
