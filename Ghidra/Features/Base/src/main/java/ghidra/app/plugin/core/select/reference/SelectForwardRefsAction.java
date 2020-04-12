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
package ghidra.app.plugin.core.select.reference;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import docking.action.KeyBindingData;
import docking.action.MenuData;
import ghidra.app.context.NavigatableActionContext;
import ghidra.app.context.NavigatableContextAction;
import ghidra.app.nav.NavigationUtils;
import ghidra.app.util.HelpTopics;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.Reference;
import ghidra.program.util.ProgramSelection;
import ghidra.util.HelpLocation;

public class SelectForwardRefsAction extends NavigatableContextAction {

	private final PluginTool tool;

	SelectForwardRefsAction(PluginTool tool, String owner) {
		super("Forward Refs", owner);
		this.tool = tool;

		String group = "references";
		setMenuBarData(new MenuData(new String[] { "Select", "Forward Refs" }, null, group));

		setKeyBindingData(new KeyBindingData(KeyEvent.VK_PERIOD, InputEvent.CTRL_MASK));
		setHelpLocation(new HelpLocation(HelpTopics.SELECTION, "Forward"));
//		setKeyBindingData( new KeyBindingData(KeyEvent.VK_SEMICOLON, InputEvent.CTRL_MASK ) );
//		setHelpLocation(new HelpLocation(HelpTopics.SELECTION, "Backward"));
	}

	@Override
	protected boolean isEnabledForContext(NavigatableActionContext context) {
		return context.getAddress() != null || context.hasSelection();
	}

	/**
	 * Method called when the action is invoked.
	 * @param context details regarding the invocation of this action
	 */
	@Override
	public void actionPerformed(NavigatableActionContext context) {

		AddressSetView addressSet =
			context.hasSelection() ? context.getSelection() : new AddressSet(context.getAddress());

		ProgramSelection selection = getSelection(context.getProgram(), addressSet);
		NavigationUtils.setSelection(tool, context.getNavigatable(), selection);
	}

	private ProgramSelection getSelection(Program program, AddressSetView addressSetView) {
		AddressSet addressSet = new AddressSet();

		CodeUnitIterator iter = program.getListing().getCodeUnits(addressSetView, true);

		while (iter.hasNext()) {
			CodeUnit cu = iter.next();
			Reference[] memRef = cu.getReferencesFrom();
			for (Reference element : memRef) {
				Address addr = element.getToAddress();
				if (addr.isMemoryAddress()) {
					addressSet.addRange(addr, addr);
				}

			}
		}
		return new ProgramSelection(addressSet);
	}
}
