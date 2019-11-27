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
package ghidra.app.plugin.core.help;

import java.util.Map;

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.MenuData;
import docking.tool.ToolConstants;
import ghidra.app.CorePluginPackage;
import ghidra.app.context.ProgramActionContext;
import ghidra.app.context.ProgramContextAction;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.util.GenericHelpTopics;
import ghidra.app.util.HelpTopics;
import ghidra.framework.main.FrontEndTool;
import ghidra.framework.main.FrontEndable;
import ghidra.framework.main.datatable.ProjectDataActionContext;
import ghidra.framework.main.datatable.ProjectDataContextAction;
import ghidra.framework.model.DomainFile;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import ghidra.util.HelpLocation;

/**
 * Display a pop-up dialog containing information about the Domain Object
 * that is currently open in the tool.
 */
//@formatter:off
@PluginInfo(
	status = PluginStatus.RELEASED,
	packageName = CorePluginPackage.NAME,
	category = PluginCategoryNames.COMMON,
	shortDescription = "Displays program information",
	description = "This plugin provides an action that displays information about the currently loaded program"
)
//@formatter:on
public class AboutProgramPlugin extends Plugin implements FrontEndable {
	public final static String PLUGIN_NAME = "AboutProgramPlugin";
	public final static String ACTION_NAME = "About program";

	private DockingAction aboutAction;

	public AboutProgramPlugin(PluginTool tool) {
		super(tool);
	}

	@Override
	protected void init() {
		setupActions();
	}

	@Override
	public void dispose() {
		tool.removeAction(aboutAction);
		aboutAction.dispose();
		super.dispose();
	}

	private void setupActions() {
		if (tool instanceof FrontEndTool) {
			aboutAction = new ProjectDataContextAction(ACTION_NAME, PLUGIN_NAME) {

				@Override
				protected void actionPerformed(ProjectDataActionContext context) {
					DomainFile domainFile = context.getSelectedFiles().get(0);
					showAbout(domainFile, domainFile.getMetadata());
				}

				@Override
				protected boolean isAddToPopup(ProjectDataActionContext context) {
					return context.getFileCount() == 1 && context.getFolderCount() == 0;
				}
			};
			aboutAction.setPopupMenuData(new MenuData(new String[] { "About..." }, null, "AAA"));

			aboutAction.setEnabled(true);
		}
		else {
			aboutAction = new ProgramContextAction(ACTION_NAME, PLUGIN_NAME) {
				@Override
				public void actionPerformed(ProgramActionContext context) {
					Program program = context.getProgram();
					showAbout(program.getDomainFile(), program.getMetadata());
				}

				@Override
				public boolean isEnabledForContext(ActionContext context) {
					if (!super.isEnabledForContext(context)) {
						getMenuBarData().setMenuItemName(ACTION_NAME);
						return false;
					}
					return true;
				}

				@Override
				public boolean isEnabledForContext(ProgramActionContext context) {
					Program program = context.getProgram();
					String menuName = "About " + program.getDomainFile().getName();
					getMenuBarData().setMenuItemName(menuName);
					return true;
				}
			};

			aboutAction.setMenuBarData(new MenuData(new String[] { ToolConstants.MENU_HELP,
				ACTION_NAME }, null, "ZZZ"));

			aboutAction.setEnabled(false);
		}

		aboutAction.setHelpLocation(new HelpLocation(HelpTopics.ABOUT, "About_Program"));
		aboutAction.setDescription(getPluginDescription().getDescription());
		tool.addAction(aboutAction);
	}

	private void showAbout(DomainFile domainFile, Map<String, String> metadata) {
		HelpLocation helpLocation = new HelpLocation(GenericHelpTopics.ABOUT, "About_Program");
		AboutDomainObjectUtils.displayInformation(tool, domainFile, metadata,
			"About " + domainFile.getName(), null, helpLocation);
	}

}
