package de.siteof.jdink.view.swing.debug.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import de.siteof.jdink.model.JDinkContext;

public class PauseAction extends AbstractAction {
	
	private static final long serialVersionUID = 1L;
	
	private final JDinkContext context;		
	
	public PauseAction(JDinkContext context) {
		super("||");
		this.context = context;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		context.getController().setPaused(!context.getController().isPaused());
	}
	
}