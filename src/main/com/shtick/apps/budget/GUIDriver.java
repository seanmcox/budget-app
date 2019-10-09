/**
 * 
 */
package com.shtick.apps.budget;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import com.shtick.apps.budget.structure.MainDriver;
import com.shtick.apps.budget.ui.CreateUserDialog;
import com.shtick.apps.budget.ui.LoginDialog;
import com.shtick.apps.budget.ui.MainFrame;

/**
 * @author scox
 *
 */
public class GUIDriver extends MainDriver {
	private static GUIDriver DRIVER;
	private MainFrame mainFrame;
	
	/**
	 * 
	 */
	public GUIDriver() {
		if(DRIVER!=null)
			throw new RuntimeException("Only one GUIDriver instance can run at a time in a given runtime environment.");
		DRIVER=this;
		
		try {
			List<String> usernames = getUsernames();
			if(usernames.size()==0) {
				// Create user
				CreateUserDialog createUserDialog = new CreateUserDialog(JOptionPane.getRootFrame(),true);
				createUserDialog.showLoginDialog();
				if(!createUserDialog.didCreateUser())
					return;
			}
			else {
				// Log the user in.
				LoginDialog loginDialog = new LoginDialog(JOptionPane.getRootFrame());
				loginDialog.showLoginDialog();
				if(!loginDialog.isLoggedIn())
					return;
			}
						
			// Open the main application window.
			mainFrame = new MainFrame();
			mainFrame.setExtendedState(MainFrame.MAXIMIZED_BOTH);
			mainFrame.setVisible(true);
		}
		catch(IOException t) {
			JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), t.getMessage(), "I/O Exception", JOptionPane.ERROR_MESSAGE);
			t.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @return The current instance of GUIDriver or null if GUIDriver hasn't been initialized.
	 */
	public static GUIDriver getGUIDriver() {
		return DRIVER;
	}
}
