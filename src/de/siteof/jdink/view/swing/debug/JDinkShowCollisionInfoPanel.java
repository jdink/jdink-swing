package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.collision.JDinkCollision;
import de.siteof.jdink.collision.JDinkCollisionTestType;
import de.siteof.jdink.collision.JDinkCollisionTester;
import de.siteof.jdink.collision.JDinkHardnessCollision;
import de.siteof.jdink.collision.JDinkSpriteCollision;
import de.siteof.jdink.control.JDinkController;
import de.siteof.jdink.format.map.JDinkMapSpritePlacement;
import de.siteof.jdink.geom.JDinkPoint;
import de.siteof.jdink.geom.JDinkRectangle;
import de.siteof.jdink.geom.JDinkShape;
import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.model.JDinkSequenceFrame;
import de.siteof.jdink.model.JDinkSprite;
import de.siteof.jdink.util.debug.JDinkObjectOutputUtil;
import de.siteof.jdink.view.JDinkDebugView;
import de.siteof.jdink.view.JDinkView;
import de.siteof.jdink.view.swing.JDinkSwingCanvas;
import de.siteof.jdink.view.swing.JDinkSwingPaintListener;
import de.siteof.jdink.view.swing.debug.actions.ShowHardnessAction;

public class JDinkShowCollisionInfoPanel extends JPanel implements EnableDisposePanel {

	private static class CollisionInformation implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String text;
		private final JDinkShape collsionShape;
		private final JDinkShape bounds;
		private final JDinkPoint location;
		private final JDinkMapSpritePlacement spritePlacement;

		public CollisionInformation(JDinkCollision collision) {
			String text = null;
			JDinkShape collisionShape = null;
			JDinkShape bounds = null;
			JDinkPoint location = null;
			JDinkMapSpritePlacement spritePlacement = null;
			if (collision instanceof JDinkSpriteCollision) {
				JDinkSpriteCollision spriteCollision = (JDinkSpriteCollision) collision;
				JDinkSprite sprite = spriteCollision.getSprite();
				spritePlacement = sprite.getSpritePlacement();
				text = "sprite " + sprite.getSpriteNumber() +
						", ct=" + sprite.getCollisionType() +
						", v=" + (spritePlacement != null ? Integer.valueOf(spritePlacement.getVision()) : "?") +
						", t=" + (spritePlacement != null ? Integer.valueOf(spritePlacement.getType()) : "?") +
						", d=" + sprite.getDepthHint() +
						", seq=" + sprite.getSequenceNumber() +
						", f=" + sprite.getFrameNumber();
				JDinkSequenceFrame frame = sprite.getFrame();
				if (frame != null) {
					text += ", img=" + frame.getFileName();
				}
				collisionShape = spriteCollision.getSprite().getCollisionShape();
				bounds = spriteCollision.getSprite().getBounds();
				location = new JDinkPoint(sprite.getX(), sprite.getY());
			} else if (collision instanceof JDinkHardnessCollision) {
				JDinkHardnessCollision hardnessCollision = (JDinkHardnessCollision) collision;
				text = "hardness " + hardnessCollision.getHardness();
				collisionShape = null;
			} else {
				text = "?";
			}
			this.text = text;
			this.collsionShape = collisionShape;
			this.bounds = bounds;
			this.location = location;
			this.spritePlacement = spritePlacement;
		}

		@Override
		public String toString() {
			return "CollisionInformation [bounds=" + bounds
					+ ", collsionShape=" + collsionShape + ", location="
					+ location + ", spritePlacement=" + spritePlacement
					+ ", text=" + text + "]";
		}

		public String getText() {
			return text;
		}

		public JDinkShape getCollsionShape() {
			return collsionShape;
		}

		public JDinkShape getBounds() {
			return bounds;
		}

		public JDinkPoint getLocation() {
			return location;
		}

		@SuppressWarnings("unused")
		public JDinkMapSpritePlacement getSpritePlacement() {
			return spritePlacement;
		}

	}

	private static class CollisionTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private static final String[] columnNames = { "Collision", "Hardness", "Bounds" };
		private List<CollisionInformation> rows;

		public CollisionTableModel() {
		}

		@Override
		public String toString() {
			return "CollisionTableModel [rows=" + rows + "]";
		}

		public void setRows(List<CollisionInformation> rows) {
			this.rows = rows;
			this.fireTableDataChanged();
		}

		public List<CollisionInformation> getRows() {
			return this.rows;
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return (rows != null ? rows.size() : 0);
		}

		private String toString(Object o) {
			String result;
			if (o instanceof JDinkRectangle) {
				JDinkRectangle r = (JDinkRectangle) o;
				result = "R{ x=" + r.getX() + ", y=" + r.getY() + ", w=" + r.getWidth() + ", h=" + r.getHeight() + "}";
			} else {
				result = String.valueOf(o);
			}
			return result;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object result = null;
			JDinkShape shape;
			if ((rows != null) && (rowIndex < rows.size())) {
				CollisionInformation info = rows.get(rowIndex);
				switch (columnIndex) {
				case 0:
					result = info.getText();
					break;
				case 1:
					shape = info.getCollsionShape();
					if (shape != null) {
						result = toString(shape);
					} else {
						result = "N/A";
					}
					break;
				case 2:
					shape = info.getBounds();
					if (shape != null) {
						result = toString(shape);
					} else {
						result = "N/A";
					}
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

	private static final Log log = LogFactory.getLog(JDinkShowCollisionInfoPanel.class);

	private static final long serialVersionUID = 1L;

	private final JDinkContext context;
	private final CollisionTableModel model;
	private final JPanel viewPanel;
	private final MouseListener mouseListener;
	private final MouseMotionListener mouseMotionListener;
	private final KeyListener keyListener;
	private final JLabel label;
	private final JDinkSwingPaintListener paintListener;
	private JDinkCollisionTestType testType;
	private boolean updatesPaused;
	private boolean enabled;
	private boolean mouseExited;

	public JDinkShowCollisionInfoPanel(JDinkContext context) {
		super(new BorderLayout());
		this.context = context;
		this.setMinimumSize(new Dimension(100, 100));
		this.setSize(new Dimension(100, 100));

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(new JToggleButton(new ShowHardnessAction(context)));
		bottomPanel.add(label = new JLabel("move the mouse over the canvas to update"), BorderLayout.SOUTH);
		this.add(bottomPanel, BorderLayout.SOUTH);

		final JComboBox comboBox = new JComboBox(JDinkCollisionTestType.values());
		testType = (JDinkCollisionTestType) comboBox.getSelectedItem();
		comboBox.setEditable(false);
		comboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				testType = (JDinkCollisionTestType) comboBox.getSelectedItem();
				update();
			}});
		this.add(comboBox, BorderLayout.NORTH);
		JPanel panel = new JPanel(new BorderLayout());
		JTable table = new JTable(model = new CollisionTableModel());
		panel.add(table.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		this.add(panel, BorderLayout.CENTER);
		mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getButton() == MouseEvent.BUTTON1) {
					updatesPaused = !updatesPaused;
					if (updatesPaused) {
						JDinkObjectOutputUtil.writeObject("collisions_" + event.getWhen(), model.getRows());
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				mouseExited = true;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		};
		mouseMotionListener = new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mouseExited = false;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						update();
					}
				});
			}
		};
		keyListener = new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (Character.toUpperCase(e.getKeyChar()) == 'H') {
					updatesPaused = !updatesPaused;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (Character.toUpperCase(e.getKeyChar()) == 'H') {
					updatesPaused = !updatesPaused;
				}
			}
		};
		JDinkView view = this.context.getView();
		if (view instanceof JDinkDebugView) {
			Object canvas = ((JDinkDebugView) view).getCanvas();
			if (canvas instanceof JPanel) {
				viewPanel = (JPanel) canvas;
			} else {
				viewPanel = null;
			}
		} else {
			viewPanel = null;
		}
		paintListener = new JDinkSwingPaintListener() {
			@Override
			public void onPaint(Graphics g) {
				paintResult(g);
			}
		};
		model.setRows(new LinkedList<CollisionInformation>());
//		enable();
	}

	public void dispose() {
		if (viewPanel != null) {
			viewPanel.removeMouseListener(mouseListener);
			viewPanel.removeMouseMotionListener(mouseMotionListener);
			viewPanel.removeKeyListener(keyListener);
			if (viewPanel instanceof JDinkSwingCanvas) {
				((JDinkSwingCanvas) viewPanel).removePaintListener(paintListener);
			}
			viewPanel.repaint();
		}
		enabled = false;
	}

	@Override
	public void enable() {
		if (!enabled) {
			if (viewPanel != null) {
				viewPanel.addMouseListener(mouseListener);
				viewPanel.addMouseMotionListener(mouseMotionListener);
				viewPanel.addKeyListener(keyListener);
				if (viewPanel instanceof JDinkSwingCanvas) {
					((JDinkSwingCanvas) viewPanel).addPaintListener(paintListener);
				}
			}
			enabled = true;
			updatesPaused = false;
		}
	}

	private void paintResult(Graphics g) {
		List<CollisionInformation> rows = model.getRows();
		Color boundsColor = Color.RED;
		Color collisionColor = Color.BLUE;
		int index = 0;
		for (CollisionInformation collisionInformation: rows) {
			int alpha = 250 - Math.min(200, index * 20);
			String text = collisionInformation.getText();
			JDinkPoint location = collisionInformation.getLocation();
			JDinkShape boundsShape = collisionInformation.getBounds();
			if ((boundsShape != null) && (location != null)) {
				JDinkRectangle r = boundsShape.getBounds();
				g.setColor(new Color(
						boundsColor.getRed(),
						boundsColor.getGreen(),
						boundsColor.getBlue(),
						alpha));
				g.draw3DRect(r.getX() + location.getX(), r.getY() + location.getY(),
						r.getWidth(), r.getHeight(), false);
				g.drawString(text, r.getX() + location.getX(), r.getY() + location.getY());
			}
			JDinkShape collisionShape = collisionInformation.getCollsionShape();
			if ((collisionShape != null) && (location != null)) {
				JDinkRectangle r = collisionShape.getBounds();
				g.setColor(new Color(
						collisionColor.getRed(),
						collisionColor.getGreen(),
						collisionColor.getBlue(),
						alpha));
				g.draw3DRect(r.getX() + location.getX(), r.getY() + location.getY(),
						r.getWidth(), r.getHeight(), true);
			}
			index++;
		}
	}

	public void update() {
		if (updatesPaused) {
			log.debug("updates paused");
			return;
		}
		final JDinkCollisionTestType testType = this.testType;
		final boolean mouseExited = this.mouseExited;
		context.getController().invokeLater(new Runnable() {
			@Override
			public void run() {
				JDinkController controller = context.getController();
				final JDinkPoint mousePosition = controller.getMousePosition();
				final int mapNumber = controller.getCurrentMapNumber();
				final int vision = controller.getVision(context);
				JDinkCollisionTester collisionTester = context.getCollisionTester();
				if (collisionTester != null) {
					List<JDinkCollision> collisions;
					if (mouseExited) {
						collisions = Collections.emptyList();
					} else {
						collisions = collisionTester.getCollisionsAt(
								testType, mousePosition.getX(), mousePosition.getY());
					}
					final List<CollisionInformation> rows = new ArrayList<CollisionInformation>(collisions.size());
					for (JDinkCollision collision: collisions) {
						rows.add(new CollisionInformation(collision));
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							label.setText("mouse: " + mousePosition.getX() + ", " + mousePosition.getY() +
									" (map: " + mapNumber + ", vision: " + vision + ")");
							model.setRows(rows);
							if (viewPanel != null) {
								viewPanel.repaint();
							}
						}
					});
				}
			}
		});
	}

}
