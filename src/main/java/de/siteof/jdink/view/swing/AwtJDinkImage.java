package de.siteof.jdink.view.swing;

import java.awt.Component;
import java.awt.Image;

import de.siteof.jdink.view.JDinkImage;

public class AwtJDinkImage implements JDinkImage {

	private final Image image;
	private final Component imageOberserver;

	public AwtJDinkImage(Image image, Component imageOberserver) {
		this.image = image;
		this.imageOberserver = imageOberserver;
	}

	public int getHeight() {
		int result;
		if (image != null) {
			result = image.getHeight(imageOberserver);
		} else {
			result = -1;
		}
		return result;
	}

	public int getWidth() {
		int result;
		if (image != null) {
			result = image.getWidth(imageOberserver);
		} else {
			result = -1;
		}
		return result;
	}

	/**
	 * @return the image
	 */
	public Image getImage() {
		return image;
	}

}
