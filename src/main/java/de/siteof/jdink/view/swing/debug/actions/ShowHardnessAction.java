package de.siteof.jdink.view.swing.debug.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.view.JDinkDebugView;
import de.siteof.jdink.view.JDinkView;

public class ShowHardnessAction extends AbstractAction {
	
	private static final long serialVersionUID = 1L;
	
	private final JDinkContext context;		
	
	public ShowHardnessAction(JDinkContext context) {
		super("hardness");
		this.context = context;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JDinkView view = context.getView();
		if (view instanceof JDinkDebugView) {
			JDinkDebugView debugView = (JDinkDebugView) view;
			debugView.setHardnessVisible(!debugView.isHardnessVisible());
		}
	}
	
}