package de.siteof.jdink.view.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.io.Resource;
import de.siteof.jdink.view.JDinkImage;
import de.siteof.jdink.view.JDinkImageFactory;

public class ImageIOImageLoader implements JDinkImageFactory {

	private static final Log log	= LogFactory.getLog(ImageIOImageLoader.class);

	private Component imageObserver;

	private boolean imageIOInitialised;
	private Object imageIOLock = new Object();

	public ImageIOImageLoader(Component imageObserver) {
		this.imageObserver = imageObserver;
	}

	@Override
	public JDinkImage getImage(Resource resource) throws IOException {
		if (resource == null) {
			throw new IllegalArgumentException("resource == null");
		}
		JDinkImage result = null;
		try {
			InputStream in = resource.getInputStream();
			if (in != null) {
				try {
					Image image;
					synchronized(imageIOLock) {
						if (!imageIOInitialised) {
							ImageIO.scanForPlugins();
							imageIOInitialised = true;
						}
						image = ImageIO.read(in);
					}
					if (image == null) {
						if (log.isDebugEnabled()) {
							log.debug("image could not be loaded:" + resource.getName());
						}
						return null;
					}
					result = new AwtJDinkImage(image, imageObserver);
					if (log.isDebugEnabled()) {
						log.debug("image loaded:" + resource.getName());
					}
				} finally {
					in.close();
				}
			} else {
				log.error("input stream not found for resource: " + resource.getName());
			}
		} catch (IOException e) {
			log.error("error reading image (" + resource.getName() + ")" + " - " + e, e);
		}
		return result;
	}

	@Override
	public JDinkImage getMaskedImage(JDinkImage image, int backgroundColor)
			throws IOException {
		Image awtImage = ((AwtJDinkImage) image).getImage();
		final int backgroundRgb = backgroundColor & 0xFFFFFF;
		ImageFilter filter = new RGBImageFilter() {
			public int filterRGB(int x, int y, int rgb) {
				if ((rgb & 0xFFFFFF) == backgroundRgb) {
					rgb = 0;
				}
				return rgb;
			}};
		awtImage = Toolkit.getDefaultToolkit().createImage(
				new FilteredImageSource(awtImage.getSource(), filter));
//		if (mediaTracker == null) {
//		mediaTracker = new MediaTracker(getImageOberserver());
//	}
//	mediaTracker.addImage(image, 0);
//	try {
//		mediaTracker.waitForAll();
//	} catch (InterruptedException e1) {
//		e1.printStackTrace();
//	}
		return new AwtJDinkImage(awtImage, imageObserver);
	}

	/**
	 * @return the imageObserver
	 */
	public Component getImageObserver() {
		return imageObserver;
	}

	/**
	 * @param imageObserver the imageObserver to set
	 */
	public void setImageObserver(Component imageObserver) {
		this.imageObserver = imageObserver;
	}

}
