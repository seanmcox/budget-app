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
import java.io.IOException;
import java.util.Vector;

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
import com.shtick.apps.budget.structure.model.User;
import com.shtick.apps.budget.structure.model.UserID;

/**
 * 
 * @author Sean Cox
 *
 */
public class CreateUserDialog extends JDialog {
    private JButton createButton;
    private JButton cancelButton;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private boolean createdUser=false;

    /**
     * @param owner
     * @param logIn If true, then the dialog also logs in as the created user after the user is created.
     * @throws HeadlessException
     * @throws IOException 
     */
    public CreateUserDialog(Frame owner, boolean logIn) throws HeadlessException, IOException{
		super(owner,"Create User",true);
	
		usernameField = new JTextField();
		usernameField.getDocument().addDocumentListener(new DocumentListener() {
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
		passwordField = new JPasswordField();
		passwordField.getDocument().addDocumentListener(new DocumentListener() {
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
		createButton=new JButton("Create");
		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					GUIDriver.getGUIDriver().addUser(new User(null, usernameField.getText(), true, null), new String(passwordField.getPassword()));
					createdUser = true;
					if(logIn) {
						UserID userID = GUIDriver.getGUIDriver().login(usernameField.getText(), new String(passwordField.getPassword()));
						if(userID==null)
							throw new RuntimeException("Failed to log in initial user.");
					}
					setVisible(false);
				}
				catch(IOException t) {
					JOptionPane.showMessageDialog(CreateUserDialog.this, t.getMessage(), "I/O Exception", JOptionPane.ERROR_MESSAGE);
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
		
		JPanel loginPanel = new JPanel(new GridLayout(4, 1));
		loginPanel.add(new JLabel("Username"));
		loginPanel.add(usernameField);
		loginPanel.add(new JLabel("Password"));
		loginPanel.add(passwordField);
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		buttonPanel.add(createButton);
		buttonPanel.add(cancelButton);

		Container contentPane=getContentPane();
		contentPane.add(loginPanel, BorderLayout.CENTER);
		contentPane.add(buttonPanel, BorderLayout.SOUTH);
		this.setSize(350,150);
    }

    /**
     * Show the dialog.
     */
    public void showLoginDialog(){
		setVisible(true);
        dispose(); // Free up native resources and any references to this dialog box.
    }
    
    /**
     * 
     * @return true if login was successful and false otherwise;
     */
    public boolean didCreateUser() {
    	return createdUser;
    }
        
    private void updateButtonEnabling() {
    	createButton.setEnabled((usernameField.getText().length()>0)&&(passwordField.getPassword().length>0));
    }
}
