package de.siteof.jdink.view.swing;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.view.AbstractJDinkView;
import de.siteof.jdink.view.JDinkDebugView;
import de.siteof.jdink.view.JDinkImage;
import de.siteof.jdink.view.JDinkImageFactory;
import de.siteof.jdink.view.JDinkView;
import de.siteof.swing.util.SwingUtil;

public class JDinkSwingView extends AbstractJDinkView implements JDinkView, JDinkDebugView {

	private static final Log log	= LogFactory.getLog(JDinkSwingView.class);

	private ImageIOImageLoader imageIOImageLoader;

	private JDinkContext context;
	private JDinkSwingPanel panel;
	private JDinkImage splashImage;

	private boolean initialising;
	private boolean initialisingFailed;

	public JDinkSwingView() {
	}

	private void waitInitialised() {
		while ((initialising) && (!initialisingFailed)) {
			try {
				synchronized (this) {
					this.wait();
				}
			} catch (InterruptedException e) {
				if (log.isDebugEnabled()) {
					log.debug("Wait interrupted - " + e, e);
				}
			}
		}
	}

	public void updateView() {
		context.getController().setChanged(true);
		SwingUtil.invokeLater(new Runnable() {
			public void run() {
				if (panel != null) {
					panel.repaint();
				}
			}});
		Thread.yield();
	}

	public JDinkImageFactory getImageLoader() {
		waitInitialised();
		return imageIOImageLoader;
	}

	public void setSplashImage(JDinkImage image) {
		waitInitialised();
		this.splashImage = image;
		SwingUtil.invokeLater(new Runnable() {
			public void run() {
				if (panel != null) {
					panel.setSplashImage(splashImage);
					splashImage = null;
				}
			}});
	}

	private void doInit() {
		panel = new JDinkSwingPanel(context);
		imageIOImageLoader = new ImageIOImageLoader(panel);
		JDinkImage image = splashImage;
		if ((panel != null) && (image != null)) {
			panel.setSplashImage(image);
			splashImage = null;
		}
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setTitle("JDink");
		frame.setVisible(true);
		initialising = false;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void init(JDinkContext context) {
		initialising = true;
		this.context = context;
		SwingUtil.invokeLater(new Runnable() {
			public void run() {
				try {
					doInit();
				} catch (Throwable e) {
					initialisingFailed = true;
					log.error("Initialising the view failed - " + e, e);
				}
			}});
	}

	@Override
	public void setHardnessVisible(boolean hardnessVisible) {
		panel.setHardnessVisible(hardnessVisible);
	}

	@Override
	public boolean isHardnessVisible() {
		return panel.isHardnessVisible();
	}

	@Override
	public Object getCanvas() {
		return panel.getCanvas();
	}

	@Override
	public boolean isStopping() {
		return false;
	}

	@Override
	public void waitForView(long timeout) {
		if (panel != null) {
			panel.getCanvas().waitForView(timeout);
		}
	}

}
