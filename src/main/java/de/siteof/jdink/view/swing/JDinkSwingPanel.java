package de.siteof.jdink.view.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.Action;
import javax.swing.JPanel;

import de.siteof.jdink.events.JDinkKeyEvent;
import de.siteof.jdink.events.JDinkMouseEvent;
import de.siteof.jdink.geom.JDinkPoint;
import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.view.JDinkImage;
import de.siteof.jdink.view.swing.debug.JDinkSwingShowInfoAction;

public class JDinkSwingPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JDinkSwingImageCanvas imageCanvas;
	private JDinkSwingCanvas canvas;
	private JDinkContext context;
	private boolean showingImageCanvas = true;
	private Action showInfoAction;

	public JDinkSwingPanel(JDinkContext context) {
		showInfoAction = new JDinkSwingShowInfoAction(context);
		this.context = context;
		canvas = new JDinkSwingCanvas(context);
		canvas.setPreferredSize(new Dimension(640, 480));
		imageCanvas = new JDinkSwingImageCanvas();
		imageCanvas.setPreferredSize(new Dimension(640, 480));
		this.setLayout(new BorderLayout());
		//this.add(canvas, BorderLayout.CENTER);
//		this.add(new JDinkSwingDebugControlPanel(context), BorderLayout.NORTH);
		this.add(imageCanvas, BorderLayout.CENTER);
//		int delay = 50; // milliseconds
		this.setFocusable(true);
		this.grabFocus();
		this.addListeners();
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				JDinkSwingPanel.this.validate();				
			}
		});
//		ActionListener taskPerformer = new ActionListener() {
//			public void actionPerformed(ActionEvent evt) {
//				updateView();
//			}
//		};
//		new Timer(delay, taskPerformer).start();
	}


	private void addListeners() {
		canvas.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				context.getController().addEvent(getJDinkMouseEvent(e));
				JDinkSwingPanel.this.requestFocusInWindow();
			}
			public void mousePressed(MouseEvent e) {
				context.getController().addEvent(getJDinkMouseEvent(e));
			}
			public void mouseReleased(MouseEvent e) {
				context.getController().addEvent(getJDinkMouseEvent(e));
			}
			public void mouseEntered(MouseEvent e) {
				context.getController().addEvent(getJDinkMouseEvent(e));
			}
			public void mouseExited(MouseEvent e) {
				context.getController().addEvent(getJDinkMouseEvent(e));
			}});
		canvas.addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			public void mouseMoved(MouseEvent e) {
				context.getController().addEvent(getJDinkMouseEvent(e));
				context.getController().setMousePosition(
						getTranslated(e.getX(), e.getY()));
			}});
		this.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == 'i') {
					if (showInfoAction != null) {
						showInfoAction.actionPerformed(new ActionEvent(
								e.getSource(), ActionEvent.ACTION_PERFORMED, ""));
					}
				}
				context.getController().addEvent(getJDinkKeyEvent(e));				
			}
			public void keyPressed(KeyEvent e) {
				context.getController().addEvent(getJDinkKeyEvent(e));
				context.getController().setKeyPressed(e.getKeyCode(), true);
			}
			public void keyReleased(KeyEvent e) {
				context.getController().addEvent(getJDinkKeyEvent(e));
				context.getController().setKeyPressed(e.getKeyCode(), false);
			}});
	}
	
	private JDinkPoint getTranslated(int x, int y) {
		return canvas.getTranslated(x, y);
	}
	
	private JDinkMouseEvent getJDinkMouseEvent(MouseEvent e) {
		int id;
		switch (e.getID()) {
		case MouseEvent.MOUSE_CLICKED:
			id = JDinkMouseEvent.MOUSE_CLICKED;
			break;
		case MouseEvent.MOUSE_PRESSED:
			id = JDinkMouseEvent.MOUSE_PRESSED;
			break;
		case MouseEvent.MOUSE_RELEASED:
			id = JDinkMouseEvent.MOUSE_RELEASED;
			break;
		case MouseEvent.MOUSE_MOVED:
			id = JDinkMouseEvent.MOUSE_MOVED;
			break;
		case MouseEvent.MOUSE_ENTERED:
			id = JDinkMouseEvent.MOUSE_ENTERED;
			break;
		case MouseEvent.MOUSE_EXITED:
			id = JDinkMouseEvent.MOUSE_EXITED;
			break;
		case MouseEvent.MOUSE_DRAGGED:
			id = JDinkMouseEvent.MOUSE_DRAGGED;
			break;
		case MouseEvent.MOUSE_WHEEL:
			id = JDinkMouseEvent.MOUSE_WHEEL;
			break;
		default:
			id = -1;
		}
		JDinkPoint p = getTranslated(e.getX(), e.getY());
		return new JDinkMouseEvent(e, id, p.getX(), p.getY());
	}
	
	private static JDinkKeyEvent getJDinkKeyEvent(KeyEvent e) {
		int id;
		switch (e.getID()) {
		case KeyEvent.KEY_TYPED:
			id = JDinkKeyEvent.KEY_TYPED;
			break;
		case KeyEvent.KEY_PRESSED:
			id = JDinkKeyEvent.KEY_PRESSED;
			break;
		case KeyEvent.KEY_RELEASED:
			id = JDinkKeyEvent.KEY_RELEASED;
			break;
		default:
			id = -1;
		}
		return new JDinkKeyEvent(e, id, e.getKeyCode(), e.getKeyChar());
	}


	public void setSplashImage(JDinkImage image) {
		if (image != null) {
			imageCanvas.setImage(((AwtJDinkImage) image).getImage());
			if (!showingImageCanvas) {
				this.remove(canvas);
				this.add(imageCanvas, BorderLayout.CENTER);
				showingImageCanvas = true;
				this.validate();
			}
		} else if (showingImageCanvas) {
			this.remove(imageCanvas);
			this.add(canvas, BorderLayout.CENTER);
			showingImageCanvas = false;
			this.validate();
			if (imageCanvas != null) {
				// remove the reference
				imageCanvas.setImage(null);
			}
		}
		this.updateView();
	}

	void updateView() {
		if (showingImageCanvas) {
			imageCanvas.repaint();
		} else {
			canvas.repaint();
		}
	}

	public void setHardnessVisible(boolean hardnessVisible) {
		canvas.setHardnessVisible(hardnessVisible);
	}
	
	public boolean isHardnessVisible() {
		return canvas.isHardnessVisible();
	}


	public JDinkSwingCanvas getCanvas() {
		return canvas;
	}

}
