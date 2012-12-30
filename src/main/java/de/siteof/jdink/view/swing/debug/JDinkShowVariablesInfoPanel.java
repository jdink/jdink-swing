package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.script.JDinkScope;
import de.siteof.jdink.script.JDinkVariable;

public class JDinkShowVariablesInfoPanel extends JPanel implements EnableDisposePanel {
	
	private static class VariableInformation {
		private final String name;
		private final Object value;
		
		public VariableInformation(String name, JDinkVariable variable) {
			this.name = name;
			this.value = variable.getValue();
		}

		public String getName() {
			return name;
		}

		public Object getValue() {
			return value;
		}
	}
	
	private static class VariablesTableModel extends AbstractTableModel {
		
		private static final long serialVersionUID = 1L;
		
		private static final String[] columnNames = { "Name", "Value" };
		private List<VariableInformation> rows;
		
		public VariablesTableModel() {
		}

		@Override
		public String toString() {
			return "CollisionTableModel [rows=" + rows + "]";
		}

		public void setRows(List<VariableInformation> rows) {
			this.rows = rows;
			this.fireTableDataChanged();
		}
		
//		public List<VariableInformation> getRows() {
//			return this.rows;
//		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return (rows != null ? rows.size() : 0);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object result = null;
			if ((rows != null) && (rowIndex < rows.size())) {
				VariableInformation info = rows.get(rowIndex);
				switch (columnIndex) {
				case 0:
					result = info.getName();
					break;
				case 1:
					result = info.getValue();
					break;
				}
			}
			return result;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}
		
	}
	
	private static class SetVariableAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;
		
		private final JDinkContext context;
		private final JTextField variableNameField;
		private final JTextField variableValueField;
		private final ActionListener actionListener;
		
		public SetVariableAction(JDinkContext context,
				JTextField variableNameField,
				JTextField variableValueField,
				ActionListener actionListener) {
			super("Set");
			this.context = context;
			this.variableNameField = variableNameField;
			this.variableValueField = variableValueField;
			this.actionListener = actionListener;
		}

		@Override
		public void actionPerformed(final ActionEvent event) {
			final String variableName = variableNameField.getText();
			final String variableValue = variableValueField.getText();
			context.getController().invokeLater(new Runnable() {
				@Override
				public void run() {
					JDinkVariable playerMapVariable = context.getGlobalScope().getVariable(variableName);
					if (playerMapVariable != null) {
						playerMapVariable.setValue(variableValue);
						context.getController().setChanged(true);
						if (actionListener != null) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									actionListener.actionPerformed(event);
								}});
						}
					} else {
						// variable not found
					}
				}				
			});
		}
		
	}

	private static final Log log = LogFactory.getLog(JDinkShowVariablesInfoPanel.class);

	private static final long serialVersionUID = 1L;
	
	private final JDinkContext context;
	private final VariablesTableModel model;
	private final JLabel label;
	private boolean updatesPaused;
	private boolean enabled;

	public JDinkShowVariablesInfoPanel(JDinkContext context) {
		super(new BorderLayout());
		this.context = context;
		this.setMinimumSize(new Dimension(100, 100));
		this.setSize(new Dimension(100, 100));
		
		ActionListener refreshActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}};

		JPanel bottomPanel = new JPanel(new BorderLayout());
		JPanel setVariablePanel = new JPanel(new FlowLayout());
		JTextField variableNameField = new JTextField(10);
		variableNameField.setText("&player_map");
		JTextField variableValueField = new JTextField(10);
		variableValueField.setText("100");
		setVariablePanel.add(variableNameField);
		setVariablePanel.add(variableValueField);
		setVariablePanel.add(new JButton(new SetVariableAction(context, variableNameField, variableValueField,
				refreshActionListener)));
		bottomPanel.add(setVariablePanel, BorderLayout.CENTER);
		bottomPanel.add(label = new JLabel("last updated: "), BorderLayout.SOUTH);
		this.add(bottomPanel, BorderLayout.SOUTH);
		
		final JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(refreshActionListener);
		this.add(refreshButton, BorderLayout.NORTH);
		JPanel panel = new JPanel(new BorderLayout());
		JTable table = new JTable(model = new VariablesTableModel());
		panel.add(table.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		this.add(panel, BorderLayout.CENTER);
//		model.setRows(Arrays.asList(new CollisionInformation(new JDinkHardnessCollisionImpl((byte) 10))));
//		enable();
//		update();
	}
	
	public void dispose() {
		enabled = false;
	}
	
	public void enable() {
		if (!enabled) {
			enabled = true;
			update();
		}
	}
	
	public void update() {
		if (updatesPaused) {
			log.debug("updates paused");
			return;
		}
		context.getController().invokeLater(new Runnable() {			
			@Override
			public void run() {
				JDinkScope scope = context.getGlobalScope();
				List<String> variableNames = new ArrayList<String>(scope.getLocalVariableNames());
				Collections.sort(variableNames);
				final List<VariableInformation> rows = new ArrayList<VariableInformation>(
						variableNames.size());
				for (String name: variableNames) {
					JDinkVariable variable = scope.getLocalVariable(name);
					if (variable != null) {
						rows.add(new VariableInformation(name, variable));
					}
				}
				SwingUtilities.invokeLater(new Runnable() {						
					@Override
					public void run() {
						label.setText("last updated: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
						model.setRows(rows);
					}
				});
			}
		});
	}

}
