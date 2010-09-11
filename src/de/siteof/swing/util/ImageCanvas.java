/*
 * Created on 28.01.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.siteof.swing.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author user
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ImageCanvas extends JPanel {

	private static final long serialVersionUID = 1L;

	private Image image;

	private static final Log log	= LogFactory.getLog(ImageCanvas.class);


	public ImageCanvas() {

	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		this.repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		if (image != null) {
			d.width = Math.max(image.getWidth(this), d.width);
			d.height = Math.max(image.getHeight(this), d.height);
		}
		return d;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			if (log.isDebugEnabled()) {
				log.debug("image " + image.getWidth(this) + "x" + image.getHeight(this));
			}
			g.drawImage(image, 0, 0, this);
		}
	}
}
