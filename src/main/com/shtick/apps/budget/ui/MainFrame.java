/**
 * 
 */
package com.shtick.apps.budget.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.HeadlessException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.shtick.apps.budget.Info;

/**
 * @author scox
 *
 */
public class MainFrame extends JFrame {
	private JTabbedPane tabbedPane = new JTabbedPane();

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public MainFrame() throws HeadlessException {
		super(Info.NAME+" - "+Info.VERSION);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		contentPane.add(tabbedPane,BorderLayout.CENTER);
		openTab("Categories",new CategoriesSummaryPanel());
		
		this.setDefaultCloseOperation(MainFrame.EXIT_ON_CLOSE);
	}

	/**
	 * 
	 * @param panel
	 */
	public void closeTab(JPanel panel){
		Runnable runnable=new Runnable() {
			@Override
			public void run() {
				for(int i=0;i<tabbedPane.getTabCount();i++) {
					if(tabbedPane.getTabComponentAt(i) == panel) {
						tabbedPane.removeTabAt(i);
						break;
					}
				}
			}
		};
		SwingUtilities.invokeLater(runnable);
	}

	/**
	 * 
	 * @param panel
	 */
	public void closeCurrentTab(JPanel panel){
		Runnable runnable=new Runnable() {
			@Override
			public void run() {
				tabbedPane.removeTabAt(tabbedPane.getSelectedIndex());
			}
		};
		SwingUtilities.invokeLater(runnable);
	}
	
	/**
	 * 
	 * @param title 
	 * @param panel
	 */
	public void openTab(String title, JPanel panel){
		Runnable runnable=new Runnable() {
			@Override
			public void run() {
				tabbedPane.addTab(title, panel);
			}
		};
		SwingUtilities.invokeLater(runnable);
	}
}
