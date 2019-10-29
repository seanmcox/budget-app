/**
 * 
 */
package com.shtick.apps.budget.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.shtick.apps.budget.GUIDriver;
import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;
import com.shtick.apps.budget.structure.model.CurrencyID;
import com.shtick.apps.budget.structure.model.User;
import com.shtick.apps.budget.structure.model.UserID;

/**
 * 
 * @author Sean Cox
 *
 */
public class CreateCategoryDialog extends JDialog {
    private JButton createButton;
    private JButton cancelButton;
    private JComboBox<Option<CategoryID>> parentChooser;
    private JComboBox<Option<CurrencyID>> currencyChooser;
    private JTextField nameField;
    private CategoryID createdCategory = null;
    private CategoryID parentCategoryID = null;
    private CurrencyID currencyID = null;

    /**
     * @param owner
     * @param selectedCategory The currently selected category. It will be used as the parent category unless the user decides to make a new top level category.
     * @throws HeadlessException
     * @throws IOException 
     */
    public CreateCategoryDialog(Frame owner, Category selectedCategory) throws HeadlessException, IOException{
		super(owner,"Create Category",true);
	
		LabeledComponent parentChooserPanel = null;
		if(selectedCategory!=null) {
			Option<?>[] options = new Option<?>[] {
				new Option<CategoryID>(null, "new top level category"),
				new Option<CategoryID>(selectedCategory.getId(), "child of "+selectedCategory.getName())
			};
			parentCategoryID = selectedCategory.getId();
			parentChooser = new JComboBox<Option<CategoryID>>((Option<CategoryID>[])options);
			parentChooser.setSelectedIndex(1);
			parentChooser.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					parentCategoryID = ((Option<CategoryID>)parentChooser.getSelectedItem()).getItem();
				}
			});
			parentChooserPanel = new LabeledComponent("Create as", parentChooser);
		}
		// TODO Get currencies.
		// TODO Get upset if there are no currencies defined.
		currencyChooser = new JComboBox<Option<CurrencyID>>();
		currencyChooser.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				currencyID = ((Option<CurrencyID>)currencyChooser.getSelectedItem()).getItem();
			}
		});
		LabeledComponent currencyChooserPanel = new LabeledComponent("Currency", currencyChooser);
		nameField = new JTextField();
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateButtonEnabling();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateButtonEnabling();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateButtonEnabling();
			}
		});
		LabeledComponent nameFieldPanel = new LabeledComponent("Category Name", nameField);
		createButton=new JButton("Create");
		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					createdCategory = GUIDriver.getGUIDriver().addCategory(new Category(null, parentCategoryID, nameField.getText(), currencyID, 0, null, null));
					setVisible(false);
				}
				catch(IOException t) {
					JOptionPane.showMessageDialog(CreateCategoryDialog.this, t.getMessage(), "I/O Exception", JOptionPane.ERROR_MESSAGE);
					t.printStackTrace();
				}
			}
		});
		cancelButton=new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		updateButtonEnabling();
		
		JPanel categoryDefinitionPanel = new JPanel(new GridLayout((parentChooserPanel!=null)?3:2, 1));
		categoryDefinitionPanel.add(nameFieldPanel);
		if(parentChooserPanel!=null)
			categoryDefinitionPanel.add(parentChooserPanel);
		categoryDefinitionPanel.add(currencyChooserPanel);
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttonPanel.add(createButton);
		buttonPanel.add(cancelButton);

		Container contentPane=getContentPane();
		contentPane.add(categoryDefinitionPanel, BorderLayout.CENTER);
		contentPane.add(buttonPanel, BorderLayout.SOUTH);
		this.setSize(350,150);
    }

    /**
     * Show the dialog.
     */
    public void showDialog(){
		setVisible(true);
        dispose(); // Free up native resources and any references to this dialog box.
    }
    
    /**
     * 
     * @return The CategoryID of the newly created category, or null if there was some failure while creating the category.
     */
    public CategoryID getCreatedCategoryID() {
    	return createdCategory;
    }
        
    private void updateButtonEnabling() {
    	createButton.setEnabled(nameField.getText().length()>0);
    }
    
    private class Option<I> {
    	private I item;
    	private String label;
    	
		public Option(I item, String label) {
			super();
			this.item = item;
			this.label = label;
		}

		/**
		 * @return the item
		 */
		public I getItem() {
			return item;
		}

		/**
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return label;
		}
    }
}
