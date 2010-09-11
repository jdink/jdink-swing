package de.siteof.jdink.view.swing;

import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.view.swing.debug.JDinkSwingShowInfoAction;

public class JDinkSwingDebugControlPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
//	private final JDinkContext context;
	
	
	public JDinkSwingDebugControlPanel(JDinkContext context) {
		super(new FlowLayout(FlowLayout.LEFT));
		this.add(new JToggleButton(new JDinkSwingShowInfoAction(context)));
	}

}
