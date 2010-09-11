package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.format.map.JDinkMapEntry;
import de.siteof.jdink.format.map.JDinkMapSpritePlacement;
import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.model.JDinkSprite;
import de.siteof.jdink.view.swing.debug.actions.PauseAction;

public class JDinkShowScriptInfoPanel extends JPanel implements EnableDisposePanel {

	private static class ScriptInformation {
		private final String name;
		private final String script;

		public ScriptInformation(String name, String script) {
			this.name = name;
			this.script = script;
		}

		public String getName() {
			return name;
		}

		public String getScript() {
			return script;
		}
	}

	private static class ScriptTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private static final String[] columnNames = { "Context", "Script" };
		private List<ScriptInformation> rows;

		public ScriptTableModel() {
		}

		@Override
		public String toString() {
			return "CollisionTableModel [rows=" + rows + "]";
		}

		public void setRows(List<ScriptInformation> rows) {
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
				ScriptInformation info = rows.get(rowIndex);
				switch (columnIndex) {
				case 0:
					result = info.getName();
					break;
				case 1:
					result = info.getScript();
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

	private static final Log log = LogFactory.getLog(JDinkShowScriptInfoPanel.class);

	private static final long serialVersionUID = 1L;

	private final JDinkContext context;
	private final ScriptTableModel model;
	private final JLabel label;
	private boolean updatesPaused;
	private boolean enabled;

	public JDinkShowScriptInfoPanel(JDinkContext context) {
		super(new BorderLayout());
		this.context = context;
		this.setMinimumSize(new Dimension(100, 100));
		this.setSize(new Dimension(100, 100));

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(new JToggleButton(new PauseAction(context)));
		bottomPanel.add(label = new JLabel("last updated: "), BorderLayout.SOUTH);
		this.add(bottomPanel, BorderLayout.SOUTH);

		final JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}});
		this.add(refreshButton, BorderLayout.NORTH);
		JPanel panel = new JPanel(new BorderLayout());
		JTable table = new JTable(model = new ScriptTableModel());
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

	@Override
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
				final List<ScriptInformation> rows = new LinkedList<ScriptInformation>();
				int mapNumber = context.getController().getCurrentMapNumber();
				JDinkMapEntry mapEntry = context.getMapEntry(mapNumber);
				if (mapEntry != null) {
					rows.add(new ScriptInformation("map " + mapNumber, mapEntry.getScriptName()));
				}

				List<JDinkSprite> sprites = context.getController().getActiveSprites();
				Map<JDinkMapSpritePlacement, Integer> editorSpritesMap =
					new IdentityHashMap<JDinkMapSpritePlacement, Integer>(sprites.size());
				Map<JDinkMapSpritePlacement, JDinkSprite> m =
					new IdentityHashMap<JDinkMapSpritePlacement, JDinkSprite>(sprites.size());

				JDinkMapSpritePlacement[] spritePlacements;
				if (mapEntry != null) {
					spritePlacements = mapEntry.getSpritePlacements();
				} else {
					spritePlacements = new JDinkMapSpritePlacement[0];
				}
				for (int index = 0; index < spritePlacements.length; index++) {
					JDinkMapSpritePlacement spritePlacement = spritePlacements[index];
					editorSpritesMap.put(spritePlacement, Integer.valueOf(index));
				}

				for (JDinkSprite sprite: sprites) {
					if (sprite.getScriptInstance() != null) {
						String fileName = sprite.getScriptInstance().getScriptFile().getFileName();
						if (fileName != null) {
							int i = fileName.lastIndexOf('/');
							if (i >= 0) {
								fileName = fileName.substring(i + 1);
							}
						}
						JDinkMapSpritePlacement spritePlacement = sprite.getSpritePlacement();
						if (spritePlacement != null) {
							rows.add(new ScriptInformation("sprite " + sprite.getSpriteNumber() +
									" (editor sprite " + editorSpritesMap.get(spritePlacement) + ")",
									fileName));
							m.put(spritePlacement, sprite);
						} else {
							rows.add(new ScriptInformation("sprite " + sprite.getSpriteNumber(),
									fileName));
						}
					}
				}

				for (int index = 0; index < spritePlacements.length; index++) {
					JDinkMapSpritePlacement spritePlacement = spritePlacements[index];
					if (!m.containsKey(spritePlacement)) {
						if ((spritePlacement.getScriptName() != null) &&
								(!spritePlacement.getScriptName().isEmpty())) {
							rows.add(new ScriptInformation("(editor sprite " + index +
									", v=" + spritePlacement.getVision() +
									", t=" + spritePlacement.getType() +
									")",
									spritePlacement.getScriptName()));
						}
					}
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						label.setText("last updated: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
						model.setRows(new ArrayList<ScriptInformation>(rows));
					}
				});
			}
		});
	}

}
