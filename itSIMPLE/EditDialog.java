/*** 
* itSIMPLE: Integrated Tool Software Interface for Modeling PLanning Environments
* 
* Copyright (C) 2007-2009 Universidade de Sao Paulo
* 
*
* This file is part of itSIMPLE.
*
* itSIMPLE is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version. Other licenses might be available
* upon written agreement.
* 
* itSIMPLE is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with itSIMPLE.  If not, see <http://www.gnu.org/licenses/>.
* 
* Authors:	Tiago S. Vaquero, 
*			Victor Romero.
**/

package itSIMPLE;

import languages.xml.XMLUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SpringLayout;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeModel;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;

import org.jdom.Element;

import sourceEditor.ItHilightedDocument;


public class EditDialog extends JDialog implements KeyListener, ItemListener,TableModelListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2611427807326279073L;
	
	private JTabbedPane mainPane = null;
	
	// Base
	private ItPanel basePanel = null;
	private ItComboBox attributeType = null;
	private JTextField initialValueField = null;
		
	private JTextField nameTextField = null;
	private JTextPane descriptionTextPane = null;
	
	// Operator Parameters
	private ItPanel parametersPanel = null;
	private DefaultTableModel parametersTableModel = null;
	private JTable parametersTable = null;
	private ItComboBox parameterType = null;
	private ArrayList<Element> currentParameters = new ArrayList<Element>();
	
	//Duration
	private ItPanel durationPanel  = null;
	private ItComboBox timedComboBox = null;
	private JTextField durationField = null;
	
	//Attribute Feature - Additional
	private ItPanel attributeFeaturesPanel  = null;
	private ItComboBox changeabilityComboBox = null;
	private ItComboBox attributeMultiplicity = null;
	
	// Parameters Values Panel
	private ItPanel parametersValuesPanel = null;
	private DefaultTableModel parametersValuesTableModel = null;
	private JTable parametersValuesTable = null;
	private ArrayList<Element> currentColumn = new ArrayList<Element>();
	
	// Constraints panel
	private JPanel constraintsPanel = null;
	private JTextPane constraintsTextPane = null;	
	
	private Element data, additional;
	private Object senderObject;
	private PropertiesTabbedPane propertiesPane;
	private final Element commonData = ItSIMPLE.getCommonData();

	public EditDialog(final Element data, Element additional, final Object senderObject, final PropertiesTabbedPane propertiesPane) throws HeadlessException {
		super(ItSIMPLE.getItSIMPLEFrame());
		setModal(true);
		this.data = data;
		this.additional = additional;
		this.senderObject = senderObject;
		this.propertiesPane = propertiesPane;
		setSize(300,450);
		setLocation(200,200);
		add(getMainPane());
                                //The OK button at the bottom
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
                            //Before closing ckeck the inserted name
                            
                            // check the presence of "-"
                            if (nameTextField != null){
                                String name = nameTextField.getText();
				if(name.indexOf("-") > -1){
					JOptionPane.showMessageDialog(ItSIMPLE.getItSIMPLEFrame(),
							"<html><center>The character \"-\" " +
							"can not be used.</center></html>",
							"Not Allowed Character",
							JOptionPane.WARNING_MESSAGE);

					nameTextField.setText(data.getChildText("name"));
				}
                                else{
					if (senderObject instanceof JTable){
						JTable table = (JTable)senderObject;
						DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
						tableModel.setValueAt(nameTextField.getText(), table.getSelectedRow(), 0);
					}
					else if(senderObject instanceof JList){
						JList list = (JList)senderObject;
						DefaultListModel model = (DefaultListModel)list.getModel();
						model.set(list.getSelectedIndex(), nameTextField.getText());
						data.getChild("name").setText(nameTextField.getText());
						propertiesPane.repaintSelectedElement();

						ItTreeNode operatorNode = ItSIMPLE.getInstance().getItTree().findNodeFromData(data.getDocument().getRootElement(), data);
						operatorNode.setUserObject(nameTextField.getText());
                                                DefaultTreeModel treeModel = (DefaultTreeModel) ItSIMPLE.getInstance().getItTree().getModel();
                                                treeModel.nodeChanged(operatorNode);

						// repaint open diagrams
						ItTabbedPane tabbed = ItSIMPLE.getInstance().getItGraphTabbedPane();
						tabbed.repaintOpenDiagrams("stateMachineDiagram");
					}
                                        dispose();                                
                            }
                            }else{//in case there is no name involve (e.g. attribute list)
                                dispose();
                            }
				
			}
		});
		buttonPanel.add(okButton);
                add(buttonPanel, BorderLayout.SOUTH);
	}

	
	
	private JTabbedPane getMainPane(){
		if (mainPane == null){			
			
			mainPane = new JTabbedPane();
			
			if (additional != null){
				mainPane.addTab("List of values", getParametersValuesPanel());
			}
			else{
				mainPane.addTab("Base", getBasePanel());
				
				if (data.getName().equals("attribute")){
					mainPane.addTab("Parameters", getParametersPanel());
					mainPane.addTab("Constraints", getConstraintsPanel());
					mainPane.addTab("Additional", getAttributeFeaturesPanel());
				}	
				else if (data.getName().equals("operator")){
					mainPane.addTab("Parameters", getParametersPanel());
					mainPane.addTab("Duration", getDurationPanel());
					mainPane.addTab("Constraints", getConstraintsPanel());
				}
			}
			
		}
		
		return mainPane;
	}

	
	private ItPanel getBasePanel(){
		if (basePanel == null){
			basePanel = new ItPanel(new BorderLayout());
			
			ItPanel nameBasePanel = new ItPanel(new BorderLayout());
			ItPanel attributeBasePanel = new ItPanel(new BorderLayout());
			ItPanel initialValuePanel = new ItPanel(new BorderLayout());			
			
			ItPanel bottomBasePanel = new ItPanel(new BorderLayout());			
			ItPanel topBasePanel = new ItPanel(new BorderLayout());
			
			JLabel nameLabel = new JLabel("Name ");			
			nameTextField = new JTextField(15);
			nameTextField.addKeyListener(this);
			//nameTextField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			
			// attribute type and dialog title
			if (data.getName().equals("attribute")){
				setTitle("Attribute Properties");
				
				JLabel attributeLabel = new JLabel("Type ");
				
				attributeType = new ItComboBox();				
				attributeType.addItem("",null);
				List<?> result = null;
				try {
					XPath path = new JDOMXPath("project/elements/classes/*");
					result = path.selectNodes(data.getDocument());
				} catch (JaxenException e2) {			
					e2.printStackTrace();
				}
				
				for (int i = 0; i < result.size(); i++){
					Element Class = (Element)result.get(i);
					attributeType.addItem(Class.getChildText("name"),Class);
				}
				
				Element typeClass = null;
				try {
					XPath path = new JDOMXPath("project/elements/classes/*[@id='"+data.getChildText("type")+"']");
					typeClass = (Element)path.selectSingleNode(data.getDocument());
				} catch (JaxenException e2) {			
					e2.printStackTrace();
				}
				
				if (typeClass != null){
					attributeType.setSelectedItem(typeClass.getChildText("name"));
				}
				attributeType.addItemListener(this);
				
				JLabel initialValueLabel = new JLabel("Initial ");
				initialValueField = new JTextField(15);
				initialValueField.addKeyListener(this);
				//initialValueField.setCursor(Cursor.getDefaultCursor());
				
				initialValuePanel.add(initialValueLabel, BorderLayout.WEST);
				initialValuePanel.add(initialValueField, BorderLayout.CENTER);
				
				attributeBasePanel.add(attributeLabel, BorderLayout.WEST);
				attributeBasePanel.add(attributeType, BorderLayout.CENTER);
				
				topBasePanel.add(attributeBasePanel, BorderLayout.NORTH);
				topBasePanel.add(initialValuePanel, BorderLayout.CENTER);
			}
			else if(data.getName().equals("operator")){
				setTitle("Operator Properties");
			}
			
			JLabel descriptionLabel = new JLabel("Description");
			descriptionTextPane = new JTextPane();			
			descriptionTextPane.addKeyListener(this);
			JScrollPane scrollText = new JScrollPane();
			scrollText.setViewportView(descriptionTextPane);
			
			nameBasePanel.add(nameLabel, BorderLayout.WEST);
			nameBasePanel.add(nameTextField, BorderLayout.CENTER);
			topBasePanel.add(nameBasePanel, BorderLayout.NORTH);
			
			bottomBasePanel.add(descriptionLabel, BorderLayout.NORTH);
			bottomBasePanel.add(scrollText, BorderLayout.CENTER);
			
			basePanel.add(topBasePanel, BorderLayout.NORTH);
			basePanel.add(bottomBasePanel, BorderLayout.CENTER);
			
			// set the fieds values
			nameTextField.setText(data.getChildText("name"));
			descriptionTextPane.setText(data.getChildText("description"));		
			
			
		}	
		
		return basePanel;
	}
	
	private ItPanel getParametersPanel(){
		if (parametersPanel == null){
			parametersPanel = new ItPanel(new BorderLayout());
			
			// Table      
			parametersTableModel = new DefaultTableModel();
			parametersTableModel.addTableModelListener(this);			
			parametersTable = new JTable(parametersTableModel);
			
			// set size and add scrollPane
			parametersTable.setRowHeight(20);
			JScrollPane scrollText = new JScrollPane();
			scrollText.setViewportView(parametersTable);
			
			//Name column
			parametersTableModel.addColumn("Name");
			
			// Type column
			parametersTableModel.addColumn("Type");			
			
			parameterType = new ItComboBox();
			parameterType.addItem("",null);
			List<?> result = null;
			try {
				XPath path = new JDOMXPath("project/elements/classes/*");
				result = path.selectNodes(data.getDocument());
			} catch (JaxenException e2) {			
				e2.printStackTrace();
			}
			
			for (int i = 0; i < result.size(); i++){
				Element Class = (Element)result.get(i);
				parameterType.addItem(Class.getChildText("name"),Class);
			}
			
			Element typeClass = null;
			try {
				XPath path = new JDOMXPath("project/elements/classes/*[@id='"+data.getChildText("type")+"']");
				typeClass = (Element)path.selectSingleNode(data.getDocument());
			} catch (JaxenException e2) {			
				e2.printStackTrace();
			}			
			if (typeClass != null){
				parameterType.setSelectedItem(typeClass.getChildText("name"));
			}
			
			TableColumn type = parametersTable.getColumnModel().getColumn(1);		
			type.setCellEditor(new DefaultCellEditor(parameterType));
			
			
			
			
			//	2.2 build attributes
			currentParameters.clear();
			Iterator<?> parameters = data.getChild("parameters").getChildren("parameter").iterator();
			while (parameters.hasNext()){
				Element parameter = (Element)parameters.next();
				currentParameters.add(parameter);	
				showParameter(parameter);
			}
			
			JToolBar toolBar = new JToolBar();
			toolBar.add(newParameter);
			toolBar.add(deleteParameter);
			
			parametersPanel.add(scrollText, BorderLayout.CENTER);
			parametersPanel.add(toolBar, BorderLayout.SOUTH);
			
		}
		
		
		return parametersPanel;
	}
	
	private void showParameter(Element parameter) {

		//DefaultTableModel tableModel = new DefaultTableModel();
		//tableModel = (DefaultTableModel)parametersTable.getModel();
		
		Vector<String> attRow = new Vector<String>();		
		attRow.add(parameter.getChildText("name"));
		
		Element typeClass = null;
		try {
			XPath path = new JDOMXPath("project/elements/classes/*[@id='"+parameter.getChildText("type")+"']");
			typeClass = (Element)path.selectSingleNode(parameter.getDocument());
		} catch (JaxenException e2) {			
			e2.printStackTrace();
		}
		if (typeClass != null){
			attRow.add(typeClass.getChildText("name"));		
		}
		else{
			attRow.add("");	
		}		
		parametersTableModel.addRow(attRow);			
	}

	
	private JPanel getDurationPanel(){
		
		SpringLayout layout = new SpringLayout();
		if (durationPanel == null){
			durationPanel = new ItPanel(new BorderLayout());
			
			JPanel topPanel = new JPanel(layout);
			durationPanel.add(topPanel, BorderLayout.NORTH);
			

			durationField = new JTextField();
			durationField = new JTextField(15);
			durationField.addKeyListener(this);	
			
			timedComboBox = new ItComboBox();			
			ComboBoxRenderer renderer = new ComboBoxRenderer();
			timedComboBox.setRenderer(renderer);
			timedComboBox.addItem("true");
			timedComboBox.addItem("false");
			timedComboBox.addItemListener(this);
			
			
			JLabel timedLabel = new JLabel("Timed ");
			JLabel durationLabel = new JLabel("Duration ");			
			
			topPanel.add(timedLabel);
			topPanel.add(timedComboBox);			
			topPanel.add(durationLabel);
			topPanel.add(durationField);
			SpringUtilities.makeCompactGrid(topPanel,2,2,5,5,5,5);
			
			//Set Duration Values
			durationField.setText(data.getChild("timeConstraints").getChildText("duration"));
			timedComboBox.setSelectedItem(data.getChild("timeConstraints").getAttributeValue("timed"));			
			durationField.setEnabled(Boolean.parseBoolean((String)timedComboBox.getSelectedItem()));
			
		
			
		}
		return durationPanel;
	}
	
	private ItPanel getParametersValuesPanel(){
		if (parametersValuesPanel == null){
			parametersValuesPanel = new ItPanel(new BorderLayout());
			
			// Table      
			parametersValuesTableModel = new DefaultTableModel();
			parametersValuesTableModel.addTableModelListener(this);			
			parametersValuesTable = new JTable(parametersValuesTableModel);
			
			// set size and add scrollPane
			parametersValuesTable.setRowHeight(20);
			JScrollPane scrollText = new JScrollPane();
			scrollText.setViewportView(parametersValuesTable);
			
			// Build Parameters Columns
			Iterator<?> parameters = additional.getChild("parameters").getChildren().iterator();
			while(parameters.hasNext()){
				Element parameter = (Element)parameters.next();
				currentColumn.add(parameter);
				parametersValuesTableModel.addColumn(parameter.getChildText("name"));			
			}
			
			// Value column
			parametersValuesTableModel.addColumn("Value");	
			
			
			//1. set the columns editors
			parameters = additional.getChild("parameters").getChildren().iterator();
			for(int i = 0; parameters.hasNext(); i++){
				Element parameter = (Element)parameters.next();				
				
				Element typeClass = null;
				try {
					XPath path = new JDOMXPath("project/elements/classes/*[@id='" + parameter.getChildText("type")+"']");
					typeClass = (Element)path.selectSingleNode(parameter.getDocument());					
				} catch (JaxenException e2) {			
					e2.printStackTrace();
				}
				
				if (typeClass != null){
					TableColumn column = parametersValuesTable.getColumnModel().getColumn(i);
					String parameterType = typeClass.getChildText("name");
					
					if (parameterType.equals("Boolean")){
						ItComboBox value = new ItComboBox();
						value.addItem("");
						value.addItem("true");
						value.addItem("false");						
						column.setCellEditor(new DefaultCellEditor(value));
					}
					else if (parameterType.equals("Int")) {
							JTextFieldFilter filter = new JTextFieldFilter(JTextFieldFilter.NUMERIC);
							filter.setNegativeAccepted(true);
							JTextField text = new JTextField();
							text.setDocument(filter);				
							column.setCellEditor(new DefaultCellEditor(text));
					}
					else if (parameterType.equals("Float")) {
							JTextFieldFilter filter = new JTextFieldFilter(JTextFieldFilter.FLOAT);
							filter.setNegativeAccepted(true);
							JTextField text = new JTextField();
							text.setDocument(filter);
							column.setCellEditor(new DefaultCellEditor(text));
					}
					else if(parameterType.equals("String")){
							column.setCellEditor(new DefaultCellEditor(new JTextField()));
					}
					else {
			
                        // if it's not a Boolean, Int, Float nor String then its class is in this project
						ItComboBox value = new ItComboBox();	
						value.addItem("");
						Element domain;
						if(data.getParentElement().getParentElement().getParentElement().getParentElement().getName().equals("repositoryDiagram")){
							//				attributes			object			objects			repositoryDiagram	repositoryDiagrams		domain
							domain = data.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
						}else{
							//				attributes			object				objects			objectDiagram		objectDiagrams		problem			planningProblems	domain
							domain = data.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
						}
						
						List<?> result = null;

                        //typeClass can be either a class or a enumeration
                        //if it is a class
						if (typeClass.getName().equals("class")){
                            //Get all descendent classes of typeClass
                            List<?> descendents = XMLUtilities.getClassDescendents(typeClass);

                            //create the queries for xpath
                            String descendentsQuery = "";

                            for (Iterator<?> iter = descendents.iterator(); iter.hasNext();) {
                                Element descendent = (Element) iter.next();
                                String each = "";
                                each = "class='" + descendent.getAttributeValue("id") + "'";
                                if (iter.hasNext()){
                                    each = each + " or ";
                                }
                                descendentsQuery = descendentsQuery + each;
                            }
                            if (descendentsQuery.equals(""))
                                descendentsQuery = "class='" + typeClass.getAttributeValue("id") + "'";			
                            else
                                descendentsQuery = descendentsQuery + " or class='" + typeClass.getAttributeValue("id") + "'";


                            try {					
                                XPath path = new JDOMXPath("elements/objects/object["
                                        +descendentsQuery+"]");
                                result = path.selectNodes(domain);

                            } catch (JaxenException e2) {			
                                e2.printStackTrace();
                            }
                            
                        }
                        //if it is a enumeration
                        else if(typeClass.getName().equals("enumeration")){
                            try {					
                                XPath path = new JDOMXPath("project/elements/classes/enumeration[@id='"+typeClass.getAttributeValue("id")+"']/literals/literal");
                                result = path.selectNodes(domain.getDocument());

                            } catch (JaxenException e2) {			
                                e2.printStackTrace();
                            }
                            
                        }

                        //add values to the list (either objects or literals
						if (result != null){
							Iterator<?> objects = result.iterator();
							while(objects.hasNext()){
								Element object = (Element)objects.next();
								value.addItem(object.getChildText("name"));
							}
						}
						
						column.setCellEditor(new DefaultCellEditor(value));
					}
				}
				
			}			
			
			
			//2. set value editor column 
			Element typeClass = null;
			try {
				XPath path = new JDOMXPath("project/elements/classes/*[@id='" + additional.getChildText("type")+"']");
				typeClass = (Element)path.selectSingleNode(additional.getDocument());					
			} catch (JaxenException e2) {			
				e2.printStackTrace();
			}
			
			if (typeClass != null){
				TableColumn column = parametersValuesTable.getColumnModel().getColumn(parametersValuesTableModel.getColumnCount()-1);
				String parameterType = typeClass.getChildText("name");
				
				if (parameterType.equals("Boolean")){
					ItComboBox value = new ItComboBox();
					value.addItem("");
					value.addItem("true");
					value.addItem("false");						
					column.setCellEditor(new DefaultCellEditor(value));
				}
				else if (parameterType.equals("Int")) {
						JTextFieldFilter filter = new JTextFieldFilter(JTextFieldFilter.NUMERIC);
						filter.setNegativeAccepted(true);
						JTextField text = new JTextField();
						text.setDocument(filter);				
						column.setCellEditor(new DefaultCellEditor(text));
				}
				else if (parameterType.equals("Float")) {
						JTextFieldFilter filter = new JTextFieldFilter(JTextFieldFilter.FLOAT);
						filter.setNegativeAccepted(true);
						JTextField text = new JTextField();
						text.setDocument(filter);
						column.setCellEditor(new DefaultCellEditor(text));
				}
				else if(parameterType.equals("String")){
						column.setCellEditor(new DefaultCellEditor(new JTextField()));
				}
				else {
                                        // if it's not a Boolean, Int, Float or String then its class is in this project
					ItComboBox value = new ItComboBox();	
					value.addItem("");
					//			attributes		object		objects			objectDiagram		objectDiagrams		problem							
					Element domain = data.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
                                        //in case this is a snapshot we need to reach the domain going up
                                        if (domain.getName().equals("problem")){
                                            domain = domain.getParentElement().getParentElement();
                                        }

					List<?> result = null;			
					try {					
						XPath path = new JDOMXPath("elements/objects/object[class='"
								+typeClass.getAttributeValue("id")+"']");
						result = path.selectNodes(domain);
						
					} catch (JaxenException e2) {			
						e2.printStackTrace();
					}

					if (result != null){
						Iterator<?> objects = result.iterator();
						while(objects.hasNext()){
							Element object = (Element)objects.next();
							value.addItem(object.getChildText("name"));
						}
						
					
					}
					
					column.setCellEditor(new DefaultCellEditor(value));
				}
			}
							
			
			/*parameterType = new ItComboBox();
			parameterType.addItem("",null);
			List result = null;
			try {
				XPath path = new JDOMXPath("project/elements/classes/class");
				result = path.selectNodes(data.getDocument());
			} catch (JaxenException e2) {			
				e2.printStackTrace();
			}
			
			for (int i = 0; i < result.size(); i++){
				Element Class = (Element)result.get(i);
				parameterType.addItem(Class.getChildText("name"),Class);
			}
			
			Element typeClass = null;
			try {
				XPath path = new JDOMXPath("project/elements/classes/class[@id='"+data.getChildText("type")+"']");
				typeClass = (Element)path.selectSingleNode(data.getDocument());
			} catch (JaxenException e2) {			
				e2.printStackTrace();
			}			
			if (typeClass != null){
				parameterType.setSelectedItem(typeClass.getChildText("name"));
			}
			
			TableColumn type = parametersTable.getColumnModel().getColumn(1);		
			type.setCellEditor(new DefaultCellEditor(parameterType));*/
			
			
			
			
			//	2.2 build attributes values
			currentParameters.clear();
			parameters = data.getChild("value").getChildren("parameterizedValue").iterator();
			while (parameters.hasNext()){
				Element parameterizedValue = (Element)parameters.next();
				currentParameters.add(parameterizedValue);			
				showParameterizedValue(parameterizedValue);
			}
			
			JToolBar toolBar = new JToolBar();
			toolBar.add(newParameterizedValue);
			toolBar.add(deleteParameterizedValue);
			
			parametersValuesPanel.add(scrollText, BorderLayout.CENTER);
			parametersValuesPanel.add(toolBar, BorderLayout.SOUTH);
			
		}
		
		
		return parametersValuesPanel;
	}
	
	private void showParameterizedValue(Element parameterizedValue){
		Vector<String> attRow = new Vector<String>();
		
		Iterator<?> parameters = parameterizedValue.getChild("parameters").getChildren("parameter").iterator();
		while(parameters.hasNext()){
			Element parameter = (Element)parameters.next();
			attRow.add(parameter.getChildText("value"));
			
		}		
		
		attRow.add(parameterizedValue.getChildText("value"));		
		parametersValuesTableModel.addRow(attRow);			
	}
	
	private JPanel getAttributeFeaturesPanel(){
		
		SpringLayout layout = new SpringLayout();
		if (attributeFeaturesPanel == null){
			attributeFeaturesPanel = new ItPanel(new BorderLayout());
			
			JPanel topPanel = new JPanel(layout);
			attributeFeaturesPanel.add(topPanel, BorderLayout.NORTH);
				
			
			changeabilityComboBox = new ItComboBox();			
			//ComboBoxRenderer renderer = new ComboBoxRenderer();
			//changeabilityComboBox.setRenderer(renderer);
			changeabilityComboBox.addItem("");
			changeabilityComboBox.addItem("changeable");
			changeabilityComboBox.addItem("addOnly");
			changeabilityComboBox.addItem("frozen");
			changeabilityComboBox.addItemListener(this);
			
			attributeMultiplicity = new ItComboBox();			
			//attributeMultiplicity.setRenderer(renderer);
			attributeMultiplicity.setEditable(true);
			attributeMultiplicity.addItem("");
			attributeMultiplicity.addItem("1");
			attributeMultiplicity.addItem("0..1");
			attributeMultiplicity.addItem("0..*");
			attributeMultiplicity.addItem("*");	
			attributeMultiplicity.addItem("1..*");
			attributeMultiplicity.addItemListener(this);
			//attributeMultiplicity.addKeyListener(this);
			
			JLabel constantLabel = new JLabel("Changeability ");
			JLabel multiplicityLabel = new JLabel("Multiplicity ");	
			
			
			topPanel.add(constantLabel);
			topPanel.add(changeabilityComboBox);			
			topPanel.add(multiplicityLabel);
			topPanel.add(attributeMultiplicity);
			SpringUtilities.makeCompactGrid(topPanel,2,2,5,5,5,5);
			
			//Set Addictional Values
			changeabilityComboBox.setSelectedItem(data.getChildText("changeability"));
			attributeMultiplicity.setSelectedItem(data.getChildText("multiplicity"));
		
			
		}
		return attributeFeaturesPanel;
	}
	
	private JPanel getConstraintsPanel(){
		constraintsPanel = new JPanel(new BorderLayout());		

		ItPanel topPanel = new ItPanel(new BorderLayout());		
		
		ItHilightedDocument document = new ItHilightedDocument();
		document.setHighlightStyle(ItHilightedDocument.OCL_STYLE);
		constraintsTextPane = new JTextPane(document);		
		constraintsTextPane.addKeyListener(this);
		JScrollPane scroll = new JScrollPane(constraintsTextPane);		
		JLabel label = new JLabel("Constraints");
		
				
		topPanel.add(label, BorderLayout.CENTER);
	
	
		constraintsPanel.add(topPanel, BorderLayout.NORTH);
		constraintsPanel.add(scroll, BorderLayout.CENTER);		
		
		//set the text
		constraintsTextPane.setText(data.getChildText("constraints"));
		
		return constraintsPanel;
	}
	
	public void keyTyped(KeyEvent e) {
		
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER){
			if (e.getSource() == nameTextField){
				
				// check the presence of "-"
				String name = nameTextField.getText();
				if(name.indexOf("-") > -1){
					JOptionPane.showMessageDialog(ItSIMPLE.getItSIMPLEFrame(),
							"<html><center>The character \"-\" " +
							"can not be used.</center></html>",
							"Not Allowed Character",
							JOptionPane.WARNING_MESSAGE);
					
					nameTextField.setText(data.getChildText("name"));
				}
				else{
					if (senderObject instanceof JTable){				
						JTable table = (JTable)senderObject;
						DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
						tableModel.setValueAt(nameTextField.getText(), table.getSelectedRow(), 0);
					}
					
					else if(senderObject instanceof JList){
						JList list = (JList)senderObject;
						DefaultListModel model = (DefaultListModel)list.getModel();
						model.set(list.getSelectedIndex(), nameTextField.getText());
						data.getChild("name").setText(nameTextField.getText());
						propertiesPane.repaintSelectedElement();
						
						ItTreeNode operatorNode = ItSIMPLE.getInstance().getItTree().findNodeFromData(data.getDocument().getRootElement(), data);
						operatorNode.setUserObject(nameTextField.getText());
		            	DefaultTreeModel treeModel = (DefaultTreeModel) ItSIMPLE.getInstance().getItTree().getModel();
		            	treeModel.nodeChanged(operatorNode);
		            	
						// repaint open diagrams
						ItTabbedPane tabbed = ItSIMPLE.getInstance().getItGraphTabbedPane();						
						tabbed.repaintOpenDiagrams("stateMachineDiagram");
					}
				}
			}
			else if (e.getSource() == initialValueField){
				if (senderObject instanceof JTable){				
					JTable table = (JTable)senderObject;
					DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
					tableModel.setValueAt(initialValueField.getText(), table.getSelectedRow(), 2);
				}
			}
			else if (e.getSource() == durationField){				
				data.getChild("timeConstraints").getChild("duration").setText(durationField.getText());
			}
			else if (e.getSource() == attributeMultiplicity){
				data.getChild("multiplicity").setText(attributeMultiplicity.getSelectedItem().toString());
			}			
		}		
	}
	
	// actions
	private Action newParameter = new AbstractAction("New",new ImageIcon("resources/images/new.png")){
		/**
		 * 
		 */
		private static final long serialVersionUID = -2864445237015276324L;

		public void actionPerformed(ActionEvent e) {		
			if (data != null){
//				 checks whether there is a cell being edited and cancells the edition
				int column = parametersTable.getSelectedColumn();
				if (column > -1 && parametersTable.getColumnModel().getColumn(column).getCellEditor() != null)
					parametersTable.getColumnModel().getColumn(column).getCellEditor().cancelCellEditing();
				
				Element parameter = (Element)commonData.getChild("definedNodes").getChild("elements")
				.getChild("model").getChild("parameter").clone();
				String id = String.valueOf(XMLUtilities.getId(data.getChild("parameters")));
				parameter.getAttribute("id").setValue(id);
				parameter.getChild("name").setText(parameter.getChildText("name")+id);
				data.getChild("parameters").addContent(parameter);
				
				
				// Add the parameter field in all instances of this class
				if (data.getName().equals("attribute")){
					List<?> result = null;
					try {
						XPath path = new JDOMXPath("project/diagrams/planningDomains/domain/planningProblems/problem/objectDiagrams/objectDiagram/objects/object/attributes/attribute[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']/value/parameterizedValue/parameters");
						result = path.selectNodes(data.getDocument());
					} catch (JaxenException e2) {			
						e2.printStackTrace();
					}
					
					for (int i = 0; i < result.size(); i++){
						Element Parameters = (Element)result.get(i);
						Element objectParameter = (Element)commonData.getChild("definedNodes").getChild("elements")
						.getChild("references").getChild("parameter").clone();
						objectParameter.setAttribute("id", id);
						Parameters.addContent(objectParameter);						
					}
				}
				
				currentParameters.add(parameter);	
				showParameter(parameter);			
				propertiesPane.repaintSelectedElement();
				
				// repaint open diagrams
				ItTabbedPane tabbed = ItSIMPLE.getInstance().getItGraphTabbedPane();						
				tabbed.repaintOpenDiagrams("stateMachineDiagram");

                                if (data.getName().equals("operator")){
                                    //check if there is any actionCounter metric of this action to update
                                    //domain metrics
                                    List<Element> result = new ArrayList();
                                    List<Element> domainMetrics = null;
                                    List<Element> problemMetrics = null;
                                    try {
                                            XPath path = new JDOMXPath("project/diagrams/planningDomains/domain/metrics/qualityMetric/actionCounter/chosenOperator[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']");
                                            domainMetrics = path.selectNodes(data.getDocument());
                                    } catch (JaxenException e2) {
                                            e2.printStackTrace();
                                    }
                                    //problem metrics
                                    try {
                                            XPath path = new JDOMXPath("project/diagrams/planningDomains/domain/planningProblems/problem/metrics/qualityMetric/actionCounter/chosenOperator[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']");
                                            problemMetrics = path.selectNodes(data.getDocument());
                                    } catch (JaxenException e2) {
                                            e2.printStackTrace();
                                    }

                                    if (domainMetrics != null){result.addAll(domainMetrics);}
                                    if (problemMetrics != null){result.addAll(problemMetrics);}

                                    //Add a referencial parameter for each actionCounter found
                                    if (result!= null && result.size() > 0){
                                        for (int i = 0; i < result.size(); i++){
                                            Element chosenOperator = (Element)result.get(i);
                                            Element parameterRef = new Element("parameter");
                                            parameterRef.setAttribute("id", parameter.getAttributeValue("id"));
                                            parameterRef.setAttribute("object", "");
                                            chosenOperator.getChild("parameters").addContent(parameterRef);
                                        }
                                    }
                                }

				
			}		
		}
	};	
	
	private Action deleteParameter = new AbstractAction("Delete",new ImageIcon("resources/images/delete.png")){
		/**
		 * 
		 */
		private static final long serialVersionUID = -2864445237015276324L;

		public void actionPerformed(ActionEvent e) {
			if (data != null){
//				 checks whether there is a cell being edited and cancells the edition
				int column = parametersTable.getSelectedColumn();
				CellEditor editor = parametersTable.getColumnModel().getColumn(column).getCellEditor();
				if (column > -1 && editor != null){					;
					editor.cancelCellEditing();
				}
				int row = parametersTable.getSelectedRow();
				if (row > -1){
					DefaultTableModel tableModel = (DefaultTableModel)parametersTable.getModel();
					Element selectedParameter = currentParameters.get(row);
					
					if (data.getName().equals("attribute")){
						if (data.getChild("parameters").getChildren().size() > 1){
							List<?> result = null;
							try {
								XPath path = new JDOMXPath("project/diagrams/planningProblems/problem/objectDiagrams/objectDiagram/objects/object/attributes/attribute[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']/value/parameterizedValue/parameters/parameter[@id='"+ selectedParameter.getAttributeValue("id") +"']");
								result = path.selectNodes(data.getDocument());
							} catch (JaxenException e2) {			
								e2.printStackTrace();
							}
							
							for (int i = 0; i < result.size(); i++){
								Element Parameter = (Element)result.get(i);
								Element parent = Parameter.getParentElement();
								parent.removeContent(Parameter);
							}							
						} else{
							List<?> result = null;
							try {
								XPath path = new JDOMXPath("project/diagrams/planningProblems/problem/objectDiagrams/objectDiagram/objects/object/attributes/attribute[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']/value");
								result = path.selectNodes(data.getDocument());
							} catch (JaxenException e2) {			
								e2.printStackTrace();
							}
							
							for (int i = 0; i < result.size(); i++){
								Element parameterValue = (Element)result.get(i);
								parameterValue.removeContent();								
								parameterValue.setText("");
							}							
						}
						
					}

                                        else if (data.getName().equals("operator")){
                                            //check if there is any actionCounter metric of this action to update
                                            //domain metrics
                                            List<Element> result = new ArrayList();
                                            List<Element> domainMetrics = null;
                                            List<Element> problemMetrics = null;
                                            try {
                                                    XPath path = new JDOMXPath("project/diagrams/planningDomains/domain/metrics/qualityMetric/actionCounter/chosenOperator[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']/parameters/parameter[@id='"+selectedParameter.getAttributeValue("id")+"']");
                                                    domainMetrics = path.selectNodes(data.getDocument());
                                            } catch (JaxenException e2) {
                                                    e2.printStackTrace();
                                            }
                                            //problem metrics
                                            try {
                                                    XPath path = new JDOMXPath("project/diagrams/planningDomains/domain/planningProblems/problem/metrics/qualityMetric/actionCounter/chosenOperator[@class='"+ data.getParentElement().getParentElement().getAttributeValue("id")+ "' and @id='" + data.getAttributeValue("id")+ "']/parameters/parameter[@id='"+selectedParameter.getAttributeValue("id")+"']");
                                                    problemMetrics = path.selectNodes(data.getDocument());
                                            } catch (JaxenException e2) {
                                                    e2.printStackTrace();
                                            }

                                            if (domainMetrics != null){result.addAll(domainMetrics);}
                                            if (problemMetrics != null){result.addAll(problemMetrics);}

                                            //Add a referencial parameter for each actionCounter found
                                            if (result!= null && result.size() > 0){
                                                for (int i = 0; i < result.size(); i++){
                                                    try {
                                                        Element parameterOperator = (Element)result.get(i);
                                                        Element chosenOperatorPars = parameterOperator.getParentElement();
                                                        chosenOperatorPars.removeContent(parameterOperator);
                                                    } catch (Exception ae) {
                                                        System.out.println("delete actionCounter parameter exception");
                                                    }
                                                    
                                                }
                                            }
                                        }


                                        // repaint open diagrams
					ItTabbedPane tabbed = ItSIMPLE.getInstance().getItGraphTabbedPane();
					tabbed.repaintOpenDiagrams("stateMachineDiagram");

					data.getChild("parameters").removeContent(selectedParameter);
					tableModel.removeRow(row);
					currentParameters.remove(row);
					
					propertiesPane.repaintSelectedElement();
					








				}
			}
		}
	};
	
	private Action deleteParameterizedValue = new AbstractAction("Delete",new ImageIcon("resources/images/delete.png")){
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -1390701023181431405L;

		public void actionPerformed(ActionEvent e) {					
			if (data != null){
				// checks whether there is a cell being edited and cancells the edition
				int column = parametersValuesTable.getSelectedColumn();
				if (column > -1)
					parametersValuesTable.getColumnModel().getColumn(column).getCellEditor().cancelCellEditing();
				int row = parametersValuesTable.getSelectedRow();
				if (row > -1){
					DefaultTableModel tableModel = (DefaultTableModel)parametersValuesTable.getModel();
					Element selectedParameter = currentParameters.get(row);
					data.getChild("value").removeContent(selectedParameter);
					tableModel.removeRow(row);
					currentParameters.remove(row);
					
					if(data.getParentElement().getParentElement().getParentElement().getParentElement().getName().equals("repositoryDiagram")){
						// delete the parameterized value in all other references of this object in that domain
						//						attributes			object			objects			repositoryDiagram	repositoryDiagrams		domain
						Element domain = data.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
						Element object = data.getParentElement().getParentElement();
						List<?> result = null;
						try {
							XPath path = new JDOMXPath("planningProblems/problem/objectDiagrams/objectDiagram/objects/object[@id='"+ object.getAttributeValue("id")
									+"']/attributes/attribute[@id='"+ data.getAttributeValue("id")
									+"']/value/parameterizedValue[@id='"+ selectedParameter.getAttributeValue("id") +"']");
							result = path.selectNodes(domain);
						} catch (JaxenException e2) {			
							e2.printStackTrace();
						}
						for (Iterator<?> iterator = result.iterator(); iterator.hasNext();) {
							Element paramValue = (Element) iterator.next();
							Element parent = paramValue.getParentElement();
							parent.removeContent(paramValue);
													
						}
					}
					
					// repaint the element
					propertiesPane.repaintSelectedElement();		
				}
			}
		}
	};
	
	private Action newParameterizedValue = new AbstractAction("New",new ImageIcon("resources/images/new.png")){
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 3140378760767310300L;

		public void actionPerformed(ActionEvent e) {		
			if (data != null){
//				 checks whether there is a cell being edited and cancells the edition
				int column = parametersValuesTable.getSelectedColumn();
				if (column > -1)
					parametersValuesTable.getColumnModel().getColumn(column).getCellEditor().cancelCellEditing();
				Element parameterizedValue = (Element)commonData.getChild("definedNodes").getChild("elements")
					.getChild("references").getChild("parameterizedValue").clone();
				String id = String.valueOf(XMLUtilities.getId(data.getChild("value")));
				parameterizedValue.getAttribute("id").setValue(id);
				
				Iterator<Element> iter = currentColumn.iterator();
				while(iter.hasNext()){
					Element eachParameter = iter.next();
					Element newParameter = (Element)commonData.getChild("definedNodes").getChild("elements")
					.getChild("references").getChild("parameter").clone();
					newParameter.setAttribute("id", eachParameter.getAttributeValue("id"));
					parameterizedValue.getChild("parameters").addContent(newParameter);
				}
				
				if (!additional.getChildText("initialValue").trim().equals("")){
					parameterizedValue.getChild("value").setText(additional.getChildText("initialValue"));
				}
				
				data.getChild("value").addContent(parameterizedValue);
				
				if(data.getParentElement().getParentElement().getParentElement().getParentElement().getName().equals("repositoryDiagram")){
					// send the new parameterized value to all other references of this object in that domain
					//						attributes			object			objects			repositoryDiagram	repositoryDiagrams		domain
					Element domain = data.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
					Element object = data.getParentElement().getParentElement();
					List<?> result = null;
					try {
						XPath path = new JDOMXPath("planningProblems/problem/objectDiagrams/objectDiagram/objects/object[@id='"+ object.getAttributeValue("id")
								+"']/attributes/attribute[@id='"+ data.getAttributeValue("id") +"']");
						result = path.selectNodes(domain);
					} catch (JaxenException e2) {			
						e2.printStackTrace();
					}
					for (Iterator<?> iterator = result.iterator(); iterator.hasNext();) {
						Element attribute = (Element) iterator.next();
						Element refParamValue = (Element)parameterizedValue.clone();
						
						//add the value in the last position
						attribute.getChild("value").addContent(refParamValue);
						if(attribute.getChild("value").getChildren().size() >= Integer.parseInt(id)){
							// if there is already a value with this id, reset all the other values							
							List<?> idResetList = null;
							try {
								XPath path = new JDOMXPath("parameterizedValue[@id>="+ id +"]");
								idResetList = path.selectNodes(attribute.getChild("value"));
							} catch (JaxenException e2) {			
								e2.printStackTrace();
							}
							for (Iterator<?> iterat = idResetList.iterator(); iterat.hasNext();) {
								// increase the other ids
								Element paramValue = (Element) iterat.next();
								if(paramValue != refParamValue){
									// increase the id
									int newId = Integer.parseInt(paramValue.getAttributeValue("id")) + 1;									
									paramValue.setAttribute("id", String.valueOf(newId));
									//remove the node and readd it in the last position
									Element parent = paramValue.getParentElement();
									parent.removeContent(paramValue);
									parent.addContent(paramValue);
								}
							}
						}						
					}
				}
				
				currentParameters.add(parameterizedValue);	
				showParameterizedValue(parameterizedValue);			
				propertiesPane.repaintSelectedElement();
				

				
			}		
		}
	};	

	public void keyReleased(KeyEvent e) {
		if(e.getSource() == descriptionTextPane){
			data.getChild("description").setText(descriptionTextPane.getText());			
		}
		else if(e.getSource() == constraintsTextPane){
			data.getChild("constraints").setText(constraintsTextPane.getText());
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == attributeType && e.getStateChange() == ItemEvent.SELECTED){
			JTable table = (JTable)senderObject;
			DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
			tableModel.setValueAt(attributeType.getSelectedItem(), table.getSelectedRow(), 1);
			
        	Element Class = (Element)attributeType.getDataItem(attributeType.getSelectedIndex());
        	if(Class != null){
        		if(!Class.getAttributeValue("id").equals(data.getChildText("type"))){
        			data.getChild("type").setText(Class.getAttributeValue("id"));
        		}            		
        	}
        	else{
        		data.getChild("type").setText("");
        	}
        	// sets the initial value as null
        	data.getChild("initialValue").setText("");
        	tableModel.setValueAt("",table.getSelectedRow(),2);
		}
		else if (e.getSource() == timedComboBox && e.getStateChange() == ItemEvent.SELECTED){
			String timed = (String)timedComboBox.getSelectedItem();
			data.getChild("timeConstraints").setAttribute("timed",timed);
			durationField.setEnabled(timed.equals("true"));
		}		
		else if (e.getSource() == changeabilityComboBox && e.getStateChange() == ItemEvent.SELECTED){
			data.getChild("changeability").setText((String)changeabilityComboBox.getSelectedItem());
		}
		else if (e.getSource() == attributeMultiplicity && e.getStateChange() == ItemEvent.SELECTED){
			data.getChild("multiplicity").setText((String)attributeMultiplicity.getSelectedItem());
		}
		
		propertiesPane.repaintSelectedElement();
	}
	
	public void tableChanged(TableModelEvent e) {
		
		int row = e.getFirstRow();
                int col = e.getColumn();
                if(row > -1 && col > -1){
                    if (e.getSource() == parametersTableModel){
                        
                        Element selectedParameter = currentParameters.get(row);
                        String strdata = (String)parametersTableModel.getValueAt(row, col);
                        switch(col){
                            case 0:{// name
                                selectedParameter.getChild("name").setText(strdata);
                            }
                            break;
                            case 1:{// type
                                Element Class = (Element)parameterType.getDataItem(parameterType.getSelectedIndex());
                                //System.out.println(Class);
                                if(Class != null){
                                        if(!Class.getAttributeValue("id").equals(selectedParameter.getChildText("type"))){
                                                selectedParameter.getChild("type").setText(Class.getAttributeValue("id"));
                                                //XMLUtilities.printXML(selectedParameter);
                                        }
                                }
                                else{
                                        selectedParameter.getChild("type").setText("");
                                }
                            }
                            break;
                        }

                        // repaint open diagrams
                        ItTabbedPane tabbed = ItSIMPLE.getInstance().getItGraphTabbedPane();
                        tabbed.repaintOpenDiagrams("stateMachineDiagram");
                    }
                    else if(e.getSource() == parametersValuesTableModel){
                            Element objectParameterizedValue = currentParameters.get(row);
                            String strdata = (String)parametersValuesTableModel.getValueAt(row, col);

                            if (col == parametersValuesTableModel.getColumnCount()-1){
                                    //the last column holds the attribute value

                                    //if the value is the same, do nothing
                            if(!objectParameterizedValue.getChildText("value").equals(strdata)){
                                    Element parentObject = data.getParentElement().getParentElement();
                                    if(parentObject.getParentElement().getParentElement().getName().equals("repositoryDiagram")){
                                            //repository diagram
                                            objectParameterizedValue.getChild("value").setText(strdata);
                                            //set this value in all other object diagrams
                                             List<?> result = null;
                                            try {
                                                    XPath path = new JDOMXPath("planningProblems/problem/objectDiagrams/objectDiagram/objects/object[@id='"+
                                                                    parentObject.getAttributeValue("id") +"']");
                                                    result = path.selectNodes(parentObject.getParentElement().getParentElement().getParentElement().getParentElement());//domain
                                            } catch (JaxenException e2) {
                                                    e2.printStackTrace();
                                            }
                                            for (Iterator<?> iter = result.iterator(); iter.hasNext();){
                                                    Element object = (Element) iter.next();
                                                    Element paramValue = null;
                                                    try {
                                                            XPath path = new JDOMXPath("attributes/attribute[@id='"+ objectParameterizedValue.getParentElement().getParentElement().getAttributeValue("id")
                                                                            +"']/value/parameterizedValue[@id='"+ objectParameterizedValue.getAttributeValue("id") +"']");
                                                            paramValue = (Element)path.selectSingleNode(object);
                                                    } catch (JaxenException e2) {
                                                            e2.printStackTrace();
                                                    }
                                                    if(paramValue != null){
                                                            paramValue.getChild("value").setText(strdata);
                                                    }
                                            }

                                    }
                                    else{
                                            // other objetc diagrams
                                            //look for the same attribute in the repository
                                            Element domain = parentObject.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
                                            Element paramValue = null;
                                                            try {
                                                                    XPath path = new JDOMXPath("repositoryDiagrams/repositoryDiagram/objects/object[@id='"+ parentObject.getAttributeValue("id")
                                                                                    +"']/attributes/attribute[@id='"+ data.getAttributeValue("id")
                                                                                    +"']/value/parameterizedValue[@id='"+ objectParameterizedValue.getAttributeValue("id") +"']");
                                                                    paramValue = (Element)path.selectSingleNode(domain);
                                                            } catch (JaxenException e2) {
                                                                    e2.printStackTrace();
                                                            }
                                                            if(paramValue != null){
                                                                    if(paramValue.getChildText("value").trim().equals("") ||
                                                                                    strdata.equals("")){
                                                                            //the value can be changed if there is no value in the repository or the value is null
                                                                            objectParameterizedValue.getChild("value").setText(strdata);
                                            }
                                            else{
                                                    //if the value was set in the repository, it can't be changed
                                                    JOptionPane.showMessageDialog(this,
                                                                                    "<html><center>This value can't be changed since<br>it was defined in the Repository Diagram</center></html>",
                                                                                    "Not Allowed Change",
                                                                                    JOptionPane.WARNING_MESSAGE);
                                                    parametersValuesTableModel.setValueAt(objectParameterizedValue.getChildText("value"), row, col);
                                                    // get the column to cancel the edition
                                                    parametersValuesTable.getColumnModel().getColumn(col).getCellEditor().cancelCellEditing();
                                            }
                                                            }
                                                            else{
                                                                    //this parameterized value was not created in the repository diagram
                                                                    objectParameterizedValue.getChild("value").setText(strdata);
                                                            }
                                    }
                            }

                            }
                            else{
                                    // the other columns holds the attribute parameters
                                    Element classParameter = currentColumn.get(col);
                                    Element parentObject = data.getParentElement().getParentElement();
                                    Element attrParameter = XMLUtilities.getElement(objectParameterizedValue.getChild("parameters"), classParameter.getAttributeValue("id"));
                                    //if the value is the same, do nothing
                                    if (attrParameter != null && !attrParameter.getChildText("value").equals(strdata)){
                                            if(parentObject.getParentElement().getParentElement().getName().equals("repositoryDiagram")){
                                                    attrParameter.getChild("value").setText(strdata);
                                                    //set this value in all other object diagrams
                                                    List<?> result = null;
                                                    try {
                                                            XPath path = new JDOMXPath("planningProblems/problem/objectDiagrams/objectDiagram/objects/object[@id='"+
                                                                            parentObject.getAttributeValue("id") +"']");
                                                            result = path.selectNodes(parentObject.getParentElement().getParentElement().getParentElement().getParentElement());//domain
                                                    } catch (JaxenException e2) {
                                                            e2.printStackTrace();
                                                    }
                                                    for (Iterator<?> iter = result.iterator(); iter.hasNext();) {
                                                            Element object = (Element) iter.next();
                                                            Element parameter = null;
                                                            try {
                                                                    XPath path = new JDOMXPath("attributes/attribute[@id='"+ objectParameterizedValue.getParentElement().getParentElement().getAttributeValue("id")
                                                                                    +"']/value/parameterizedValue[@id='"+ objectParameterizedValue.getAttributeValue("id")
                                                                                    +"']/parameters/parameter[@id='"+ classParameter.getAttributeValue("id") +"']");
                                                                    parameter = (Element)path.selectSingleNode(object);
                                                            } catch (JaxenException e2) {
                                                                    e2.printStackTrace();
                                                            }
                                                            if(parameter != null){
                                                                    parameter.getChild("value").setText(strdata);
                                                            }
                                                    }
                                            }
                                            else{
                                                    //look for the same attribute in the repository
                                                    Element domain = parentObject.getParentElement().getParentElement().getParentElement().getParentElement().getParentElement().getParentElement();
                                                    Element repAttrParameter = null;
                                                    try {
                                                        XPath path = new JDOMXPath("repositoryDiagrams/repositoryDiagram/objects/object[@id='"+ parentObject.getAttributeValue("id")
                                                                            +"']/attributes/attribute[@id='"+ data.getAttributeValue("id")
                                                                            +"']/value/parameterizedValue[@id='"+ objectParameterizedValue.getAttributeValue("id")
                                                                            +"']/parameters/parameter[@id='"+ classParameter.getAttributeValue("id") +"']");
                                                        repAttrParameter = (Element)path.selectSingleNode(domain);
                                                    } catch (JaxenException e2) {
                                                        e2.printStackTrace();
                                                    }
                                                    if(repAttrParameter != null){
                                                            if(repAttrParameter.getChildText("value").trim().equals("") ||
                                                                            strdata.equals("")){
                                                                    //the value can be change if there is no value in the repository or it's null
                                                                    attrParameter.getChild("value").setText(strdata);
                                                            }
                                                            else{
                                                                    //if the value was set in the repository, it can't be changed
                                                                    JOptionPane.showMessageDialog(this,
                                                                                                    "<html><center>This value can't be changed since<br>it was defined in the Repository Diagram</center></html>",
                                                                                                    "Not Allowed Change",
                                                                                                    JOptionPane.WARNING_MESSAGE);
                                                                    parametersValuesTableModel.setValueAt(attrParameter.getChildText("value"), row, col);
                                                                    // get the column to cancel the edition
                                                                    parametersValuesTable.getColumnModel().getColumn(col).getCellEditor().cancelCellEditing();
                                                            }
                                                    }
                                                    else{
                                                            // this parameterized value was not created in the repository diagram
                                                            attrParameter.getChild("value").setText(strdata);
                                                    }
                                                }
                                    }
                            }
                    }

                    propertiesPane.repaintSelectedElement();
            }
	}

}
