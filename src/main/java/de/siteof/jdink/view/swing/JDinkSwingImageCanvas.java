package de.siteof.jdink.view.swing;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

/**
 * <p>Simple swing panel showing an image</p>
 */
public class JDinkSwingImageCanvas extends JPanel {

	private static final long serialVersionUID = 1L;

	private Image image;
	
	public JDinkSwingImageCanvas() {
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			g.drawImage(image, 0, 0, this);
		}
	}

	public Image getImage() {
		return image;
	}
	
	public void setImage(Image image) {
		this.image = image;
	}
}
