package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.siteof.jdink.model.JDinkContext;

public class JDinkShowInfoPanel extends JPanel implements EnableDisposePanel {

	private static final long serialVersionUID = 1L;
	
	private final JTabbedPane tabbedPane;
	private boolean enabled;
	private EnableDisposePanel currentlySelectedPanel;

	public JDinkShowInfoPanel(JDinkContext context) {
		super(new BorderLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Variables", null,
				new JDinkShowVariablesInfoPanel(context),
				"Shows global variables");
		tabbedPane.addTab("Collisions", null,
				new JDinkShowCollisionInfoPanel(context),
				"Shows collision/bounds information");
		tabbedPane.addTab("Scripts", null,
				new JDinkShowScriptInfoPanel(context),
				"Shows global variables");
		tabbedPane.addTab("Seq", null,
				new JDinkShowSequencesInfoPanel(context),
				"Shows sequences");
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Component selectedComponent = tabbedPane.getSelectedComponent();
				if (currentlySelectedPanel != selectedComponent) {
					if (currentlySelectedPanel != null) {
						currentlySelectedPanel.dispose();
					}
					currentlySelectedPanel = (EnableDisposePanel) selectedComponent;
					currentlySelectedPanel.enable();
				}
			}
		});
		currentlySelectedPanel = (EnableDisposePanel) tabbedPane.getSelectedComponent();
		this.add(tabbedPane, BorderLayout.CENTER);
		this.setMinimumSize(new Dimension(100, 100));
		this.setSize(new Dimension(100, 100));
		enable();
	}
	
	public void dispose() {
		if (currentlySelectedPanel != null) {
			currentlySelectedPanel.dispose();
		}
		enabled = false;
	}
	
	public void enable() {
		if (!enabled) {
			if (currentlySelectedPanel != null) {
				currentlySelectedPanel.enable();
			}
			enabled = true;
		}
	}

}
