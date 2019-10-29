/**
 * 
 */
package com.shtick.apps.budget.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author scox
 *
 */
public class LabeledComponent extends JPanel {
	/**
	 * 
	 * @param label
	 * @param component
	 */
	public LabeledComponent(String label, Component component) {
		super(new BorderLayout());
		add(new JLabel(label),BorderLayout.BEFORE_FIRST_LINE);
		add(component,BorderLayout.CENTER);
	}
}
