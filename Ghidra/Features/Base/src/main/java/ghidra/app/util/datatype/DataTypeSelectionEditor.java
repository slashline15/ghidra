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
package ghidra.app.util.datatype;

import ghidra.app.plugin.core.datamgr.util.DataTypeChooserDialog;
import ghidra.app.plugin.core.datamgr.util.DataTypeUtils;
import ghidra.app.services.DataTypeManagerService;
import ghidra.framework.plugintool.ServiceProvider;
import ghidra.program.model.data.*;
import ghidra.util.data.DataTypeParser;

import java.awt.Component;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreePath;

import docking.options.editor.ButtonPanelFactory;
import docking.widgets.DropDownSelectionTextField;

/**
 * An editor that is used to show the {@link DropDownSelectionTextField} for the entering of
 * data types by name and offers the user of a completion window.  This editor also provides a
 * browse button that when pressed will show a data type tree so that the user may browse a tree
 * of known data types.
 * <p>
 * The typical usage of this class is in conjunction with the {@link DataTypeChooserDialog}.   The
 * dialog uses this editor as part of its DataType selection process.  Users seeking a dialog
 * that allows users to choose DataTypes are encouraged to use that dialog.  If you wish to add
 * this editor to a widget directly, then see below.
 * <p>
 * <u>Stand Alone Usage</u><br>
 * In order to use this component directly you need to call {@link #getEditorComponent()}.  This
 * will give you a Component for editing.
 * <p>
 * In order to know when changes are made to the component you need to add a DocumentListener
 * via the {@link #addDocumentListener(DocumentListener)} method.  The added listener will be
 * notified as the user enters text into the editor's text field.  Then, to determine when there
 * is as valid DataType in the field you may call {@link #validateUserSelection()}.
 * 
 * 
 */
public class DataTypeSelectionEditor extends AbstractCellEditor {

	private JPanel editorPanel;
	private DropDownSelectionTextField<DataType> selectionField;
	private DataTypeManagerService dataTypeManagerService;
	private int maxSize = -1;
	private DataTypeManager dataTypeManager;
	private DataTypeParser.AllowedDataTypes allowedDataTypes;

	private KeyAdapter keyListener;
	private NavigationDirection navigationDirection;

	// optional path to initially select in the data type chooser tree
	private TreePath initiallySelectedTreePath;

	public DataTypeSelectionEditor(ServiceProvider serviceProvider, int maxSize,
			DataTypeParser.AllowedDataTypes allowedDataTypes) {
		this(serviceProvider.getService(DataTypeManagerService.class), maxSize, allowedDataTypes);
	}

	public DataTypeSelectionEditor(DataTypeManagerService service, int maxSize,
			DataTypeParser.AllowedDataTypes allowedDataTypes) {

		if (service == null) {
			throw new NullPointerException("DataTypeManagerService cannot be null");
		}

		this.dataTypeManagerService = service;
		this.maxSize = maxSize;
		this.allowedDataTypes = allowedDataTypes;

		init();
	}

	/**
	 * Sets the {@link DataTypeManager} to use when the chooser is forced to parse the given
	 * data type text to resolve the data type.  If the users chooses a type, then this value
	 * is not used.  Note that setting this value does not restrict the parser to just the 
	 * given value, but rather the given value is the preferred manager and is thus searched
	 * first. 
	 * 
	 * @param dataTypeManager the preferred data type manager
	 */
	public void setPreferredDataTypeManager(DataTypeManager dataTypeManager) {
		this.dataTypeManager = dataTypeManager;
	}

	/**
	 * @see DropDownSelectionTextField#setConsumeEnterKeyPress(boolean)
	 */
	public void setConsumeEnterKeyPress(boolean consume) {
		selectionField.setConsumeEnterKeyPress(consume);
	}

	private void init() {
		selectionField =
			new DropDownSelectionTextField<DataType>(new DataTypeDropDownSelectionDataModel(
				dataTypeManagerService));
		selectionField.addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingCanceled(ChangeEvent e) {
				fireEditingCanceled();
				navigationDirection = null;
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				fireEditingStopped();
				navigationDirection = null;
			}
		});

		selectionField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

		JButton browseButton = ButtonPanelFactory.createButton(ButtonPanelFactory.BROWSE_TYPE);
		browseButton.setToolTipText("Browse the Data Manager");
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// show the data type manager
				showDataTypeBrowser();
			}
		});

		editorPanel = new JPanel();
		editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.X_AXIS));
		editorPanel.add(selectionField);
		editorPanel.add(Box.createHorizontalStrut(5));
		editorPanel.add(browseButton);

		keyListener = new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_TAB) {
					if (e.isShiftDown()) {
						navigationDirection = NavigationDirection.BACKWARD;
					}
					else {
						navigationDirection = NavigationDirection.FORWARD;
					}

					fireEditingStopped();
					e.consume();
				}
			}
		};
	}

	/**
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	@Override
	public Object getCellEditorValue() {
		return selectionField.getSelectedValue();
	}

	public DataType getCellEditorValueAsDataType() {
		try {
			if (validateUserSelection()) {
				return selectionField.getSelectedValue();
			}
		}
		catch (InvalidDataTypeException e) {
			// just return null
		}
		return null;
	}

	/**
	 * Returns the text value of the editor's text field.
	 * @return the text value of the editor's text field.
	 */
	public String getCellEditorValueAsText() {
		return selectionField.getText();
	}

	/**
	 * Returns the component that allows the user to edit.
	 * @return the component that allows the user to edit.
	 */
	public Component getEditorComponent() {
		return editorPanel;
	}

	public DropDownSelectionTextField<DataType> getDropDownTextField() {
		return selectionField;
	}

	/**
	 * Sets the initially selected node in the data type tree that the user can choose to 
	 * show.
	 * 
	 * @param path The path to set
	 */
	public void setDefaultSelectedTreePath(TreePath path) {
		this.initiallySelectedTreePath = path;
	}

	public void requestFocus() {
		selectionField.requestFocus();
	}

	/**
	 * Highlights the text of the cell editor.
	 */
	void selectCellEditorValue() {
		selectionField.selectAll();
	}

	/**
	 * Sets the value to be edited on this cell editor.
	 * 
	 * @param dataType The data type which is to be edited.
	 */
	public void setCellEditorValue(DataType dataType) {
		selectionField.setSelectedValue(dataType);
		navigationDirection = null;
	}

	public void setCellEditorValueAsText(String text) {
		selectionField.setText(text);
		navigationDirection = null;
	}

	/**
	 * Adds a document listener to the text field editing component of this editor so that users
	 * can be notified when the text contents of the editor change.  You may verify whether the 
	 * text changes represent a valid DataType by calling {@link #validateUserSelection()}.
	 * @param listener the listener to add.
	 * @see #validateUserSelection()
	 */
	public void addDocumentListener(DocumentListener listener) {
		selectionField.getDocument().addDocumentListener(listener);
	}

	/**
	 * Removes a previously added document listener.
	 * @param listener the listener to remove.s
	 */
	public void removeDocumentListener(DocumentListener listener) {
		selectionField.getDocument().removeDocumentListener(listener);
	}

	public void addFocusListener(FocusListener listener) {
		selectionField.addFocusListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		selectionField.removeFocusListener(listener);
	}

	public void setTabCommitsEdit(boolean doesCommit) {
		selectionField.setFocusTraversalKeysEnabled(!doesCommit);

		removeKeyListener(keyListener); // always remove to prevent multiple additions
		if (doesCommit) {
			addKeyListener(keyListener);
		}
	}

	/**
	 * Returns the direction of the user triggered navigation; null if the user did not trigger
	 * navigation out of this component.
	 */
	public NavigationDirection getNavigationDirection() {
		return navigationDirection;
	}

	private void addKeyListener(KeyListener listener) {
		selectionField.addKeyListener(listener);
	}

	private void removeKeyListener(KeyListener listener) {
		selectionField.removeKeyListener(listener);
	}

	/**
	 * Returns true if the current value of the data type editor is a know data type.
	 * @return true if the current value of the data type editor is a know data type.
	 * @throws InvalidDataTypeException If the current text in the editor's text field could not
	 *         be parsed into a valid DataType
	 */
	public boolean validateUserSelection() throws InvalidDataTypeException {

		// if it is not a known type, the prompt user to create new one
		if (!isValidDataType()) {
			return promptUserToCreateDataType();
		}

		return true;
	}

	public boolean containsValidDataType() {
		try {
			return isValidDataType();
		}
		catch (InvalidDataTypeException e) {
			return false;
		}
	}

	private boolean isValidDataType() throws InvalidDataTypeException {
		// look for the case where the user made a selection from the matching window, but 
		// then changed the text field text.
		DataType selectedDataType = selectionField.getSelectedValue();
		if (selectedDataType != null && selectionField.getText().equals(selectedDataType.getName())) {
			DataTypeParser.ensureIsAllowableType(selectedDataType, allowedDataTypes);
			return true;
		}

		return false;
	}

	// looks at the current text and the current data type and will return a non-null value if 
	// the current text starts with the name of the data type
	private DataType getDataTypeRootForCurrentText() {
		DataType dataType = selectionField.getSelectedValue();
		if (dataType != null) {
			String currentText = selectionField.getText();
			DataType selectedBaseDataType = DataTypeUtils.getNamedBaseDataType(dataType);
			if (currentText.startsWith(selectedBaseDataType.getName())) {
				return selectedBaseDataType;
			}
		}
		return null;
	}

	// TODO: implement in the future to allow the user to create data types
	private boolean promptUserToCreateDataType() throws InvalidDataTypeException {

		// we will create new pointer and array types by default
		DataType newDataType = null;
//        try {
		DataTypeParser parser =
			new DataTypeParser(dataTypeManager, null, dataTypeManagerService, allowedDataTypes);
		newDataType = parser.parse(selectionField.getText(), getDataTypeRootForCurrentText());
		if (newDataType != null) {
			if (maxSize >= 0 && newDataType.getLength() > newDataType.getLength()) {
				throw new InvalidDataTypeException("data-type larger than " + maxSize + " bytes");
			}
			selectionField.setSelectedValue(newDataType);
			return true;
		}
//        }
//        // squash these exceptions, as this method returns false if we were unable to create the
//        // given data type
//        catch ( CancelledException ce ) {
//        }

		// prompt user
		/*
		int userChoice = JOptionPane.showOptionDialog( selectionField, 
		    "Data type \"" + selectionField.getText() + "\" does not exist.  Would you " +
		    "like to create it?", "Create New Data Type?", 
		    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null );
		
		 if ( userChoice == JOptionPane.YES_OPTION ) {
		     return createNewDataTypeForUserSelection();
		 }
		*/
		return false;
	}

	private void showDataTypeBrowser() {
		// get the data type browser
		DataType dataType = dataTypeManagerService.getDataType(initiallySelectedTreePath);
		if (dataType != null) {
			setCellEditorValue(dataType);
			selectionField.requestFocus();
		}
	}

}
