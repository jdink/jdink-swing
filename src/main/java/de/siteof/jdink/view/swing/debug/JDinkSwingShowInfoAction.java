package de.siteof.jdink.view.swing.debug;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.border.BevelBorder;

import de.siteof.jdink.model.JDinkContext;

public class JDinkSwingShowInfoAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private final JDinkContext context;
	private JFrame frame;
	private JDinkShowInfoPanel showInfoPanel;
	
	public JDinkSwingShowInfoAction(JDinkContext context) {
		super("Info");
		this.context = context;
	}
	
	public void setSelected(boolean selected) {
		this.putValue(Action.SELECTED_KEY, Boolean.valueOf(selected));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (frame != null) {
			if (frame.isVisible()) {
				showInfoPanel.dispose();
				frame.setVisible(false);
				setSelected(false);
			} else {
				showInfoPanel.enable();
				frame.setVisible(true);
				setSelected(true);
			}
		} else {
			showInfoPanel = new JDinkShowInfoPanel(context);
			showInfoPanel.enable();
			showInfoPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
			frame = new JFrame();
			frame.setTitle("Info");
			frame.setContentPane(showInfoPanel);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setSize(400, 500);
			frame.addWindowListener(new WindowAdapter() {
				
				@Override
				public void windowClosing(WindowEvent e) {
					frame.setVisible(false);
				}
				
				@Override
				public void windowClosed(WindowEvent e) {
					JDinkSwingShowInfoAction.this.setSelected(false);
					showInfoPanel.dispose();
				}
			});
			setSelected(true);
			frame.setVisible(true);
		}
	}

}
