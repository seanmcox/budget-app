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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.shtick.apps.budget.GUIDriver;
import com.shtick.apps.budget.structure.model.UserID;

/**
 * 
 * @author Sean Cox
 *
 */
public class LoginDialog extends JDialog {
    private JButton loginButton;
    private JButton cancelButton;
    private JComboBox<String> usernameField;
    private JPasswordField passwordField;
    private boolean loggedIn = false;

    /**
     * @param owner
     * @throws HeadlessException
     * @throws IOException 
     */
    public LoginDialog(Frame owner) throws HeadlessException, IOException{
		super(owner,"Login",true);
	
		usernameField = new JComboBox<>(new Vector<String>(GUIDriver.getGUIDriver().getUsernames()));
		usernameField.setEditable(false);
		usernameField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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
		loginButton=new JButton("Login");
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					UserID userID = GUIDriver.getGUIDriver().login((String)usernameField.getSelectedItem(), new String(passwordField.getPassword()));
					loggedIn = userID!=null;
					if(loggedIn) {
						setVisible(false);
					}
					else {
						JOptionPane.showMessageDialog(LoginDialog.this, "Failed to log you in.", "Login Failed", JOptionPane.ERROR_MESSAGE);
						loginButton.setEnabled(false);
					}
				}
				catch(IOException t) {
					JOptionPane.showMessageDialog(LoginDialog.this, t.getMessage(), "I/O Exception", JOptionPane.ERROR_MESSAGE);
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
		buttonPanel.add(loginButton);
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
    public boolean isLoggedIn() {
    	return loggedIn;
    }
        
    private void updateButtonEnabling() {
    	loginButton.setEnabled((usernameField.getSelectedIndex()>=0)&&(passwordField.getPassword().length>0));
    }
}
