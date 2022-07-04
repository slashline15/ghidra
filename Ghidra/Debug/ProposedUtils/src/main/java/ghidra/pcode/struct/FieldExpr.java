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
package ghidra.pcode.struct;

import ghidra.pcode.struct.StructuredSleigh.LVal;
import ghidra.program.model.data.*;

class FieldExpr extends Expr implements LValInternal {
	private final RValInternal parent;
	private final int offset;

	private FieldExpr(StructuredSleigh ctx, RValInternal parent, int offset, DataType type) {
		super(ctx, type);
		this.parent = parent;
		this.offset = offset;
	}

	private FieldExpr(StructuredSleigh ctx, RValInternal parent, DataTypeComponent component) {
		this(ctx, parent, component.getOffset(), new PointerDataType(component.getDataType()));
	}

	protected FieldExpr(StructuredSleigh ctx, RValInternal parent, String name) {
		this(ctx, parent, ctx.findComponent(parent, name));
	}

	@Override
	public LVal cast(DataType type) {
		return new FieldExpr(ctx, parent, offset, type);
	}

	@Override
	public String toString() {
		return "<Field " + parent + " + 0x" + Long.toString(offset, 16) + ">";
	}

	@Override
	public String generate() {
		return "(" + parent.generate() + " + " + offset + ")";
	}
}
