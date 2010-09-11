package de.siteof.jdink.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.MemoryImageSource;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.control.JDinkController;
import de.siteof.jdink.geom.JDinkPoint;
import de.siteof.jdink.geom.JDinkRectangle;
import de.siteof.jdink.geom.JDinkShape;
import de.siteof.jdink.graphics.JDinkMemoryImageData;
import de.siteof.jdink.graphics.JDinkTransparencyColorMixer;
import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.model.JDinkSequence;
import de.siteof.jdink.model.JDinkSequenceFrame;
import de.siteof.jdink.model.JDinkTextFragment;
import de.siteof.jdink.model.JDinkTile;
import de.siteof.jdink.model.view.JDinkDisplayInformation;
import de.siteof.jdink.model.view.JDinkSpriteDisplayInformation;
import de.siteof.jdink.model.view.JDinkSpriteLayerView;
import de.siteof.jdink.parser.JDinkTextParser;
import de.siteof.jdink.view.JDinkImage;
import de.siteof.swing.util.SwingUtil;

/**
 * <p>Main graphical output area.</p>
 */
public class JDinkSwingCanvas extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Log log	= LogFactory.getLog(JDinkSwingCanvas.class);

	private JDinkContext context;
	private Font textFont;
	private Image hardnessImage;
	private int[] hardnessImageData;
	private MemoryImageSource hardnessImageSource;
	private Date hardnessImageExpireDate;

	private long imageDataUpdateInterval = 500;
	private Color frameBoundsColor = new Color(55, 255, 255, 40);
	private Color spriteCollisionBoundsColor = new Color(55, 55, 255, 100);
	private Color spriteLocationColor = new Color(255, 55, 255, 100);
	
	private boolean hardnessVisible;
	
	private double scaleX = 1d;
	private double scaleY = 1d;
	
	private List<JDinkSwingPaintListener> paintListeners =
		new LinkedList<JDinkSwingPaintListener>();
		
	private Object repaintedLock = new Object();
	private long lastRepainted;

	public JDinkSwingCanvas(JDinkContext context) {
		this.context = context;
		this.textFont = new Font("Arial", Font.BOLD, 12);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				onComponentResized(e);				
			}
		});
	}
	
	public void addPaintListener(JDinkSwingPaintListener paintListener) {
		if (!paintListeners.contains(paintListener)) {
			paintListeners.add(paintListener);
		}
	}
	
	public void removePaintListener(JDinkSwingPaintListener paintListener) {
		paintListeners.remove(paintListener);
	}
	
	private void onComponentResized(ComponentEvent e) {
		recalculateScale();
		this.repaint();
	};
	
	public JDinkPoint getTranslated(int x, int y) {
		JDinkPoint result;
		double scaleX = this.scaleX;
		double scaleY = this.scaleY;
		if ((scaleX > 0) && (scaleY > 0)) {
			result = new JDinkPoint((int) (x / scaleX), (int) (y / scaleY));
		} else {
			result = new JDinkPoint(0, 0);
		}
		return result;
	}
	
	private void recalculateScale() {
		Dimension preferredSize = this.getPreferredSize();
		Dimension actualSize = this.getSize();
		if ((preferredSize == null) || (preferredSize.equals(actualSize))) {
			scaleX = 1d;
			scaleY = 1d;
		} else if ((actualSize.width > 0) && (actualSize.height > 0)) {
			scaleX = actualSize.getWidth() / preferredSize.getWidth();
			scaleY = actualSize.getHeight() / preferredSize.getHeight();
		}
	}

	private Image getAwtImage(JDinkImage image) {
		Image result = null;
		if (image != null) {
			result = ((AwtJDinkImage) image).getImage();
		}
		return result;
	}

	public void waitForView(long timeout) {
//		context.getController().setChanged(true);
		final long now = System.currentTimeMillis();
		SwingUtil.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}});
		synchronized (repaintedLock) {
			if (lastRepainted < now) {
				try {
					repaintedLock.wait(timeout);
					if (lastRepainted < now) {
						log.info("[waitForView] not repainted after timeout, giving up");
					}
				} catch (InterruptedException e) {
					log.info("[waitForView] wait interrupted");
				}
			}			
		}
	}
	
	private void notifyFullyRepainted() {
		synchronized (repaintedLock) {
			lastRepainted = System.currentTimeMillis();
			repaintedLock.notifyAll();
		}
	}

	private void drawTextLayouts(
			Graphics2D g2,
			List<TextLayout> textLayouts,
			int x, int y, int width, boolean centered) {
		int currentX = x;
		int currentY = y;
		for (TextLayout textLayout: textLayouts) {
			if (centered) {
				currentX = x + (width - (int) textLayout.getBounds().getWidth()) / 2;
			}
			if (currentX < 0) {
				log.info("currentX=" + currentX + ", x=" + x + ", textLayout=" + textLayout.getBounds());
			}
			// Move down to baseline
			currentY += textLayout.getAscent();
			// Draw line
			textLayout.draw(g2, currentX, currentY);
			// Move down to top of next line
			currentY += textLayout.getDescent() + textLayout.getLeading();
		}
	}


	private Rectangle fitRectangleInto(Rectangle r, Rectangle clipRectangle) {
		if (r.width > clipRectangle.width) {
			r.width = clipRectangle.width;
		}
		if (r.height > clipRectangle.height) {
			r.height = clipRectangle.height;
		}
		if (r.x + r.width > clipRectangle.x + clipRectangle.width) {
			r.x = clipRectangle.x + clipRectangle.width - r.width;
		}
		if (r.y + r.height > clipRectangle.y + clipRectangle.height) {
			r.y = clipRectangle.y + clipRectangle.height - r.height;
		}
		if (r.x < clipRectangle.x) {
			r.x = clipRectangle.x;
		}
		if (r.y < clipRectangle.y) {
			r.y = clipRectangle.y;
		}
		return r;
	}

	protected void paintComponent(Graphics g) {
		if ((scaleX == 1d) && (scaleY == 1d)) {
			doPaintComponent(g);
		} else if ((scaleX > 0) && (scaleY > 0)) {
			Graphics2D g2 = (Graphics2D) g;
            AffineTransform backup = g2.getTransform();
			try {
	            g2.scale(scaleX, scaleY);
	            doPaintComponent(g);
			} finally {
				g2.setTransform(backup);
			}
		}
	}

	private void doPaintComponent(Graphics g) {
		log.debug("painting canvas");
		//super.paintComponent(g);

		Date now = new Date();
		boolean updateImage = false;
		int[] imageData = null;
		JDinkMemoryImageData memoryImageData = null;
		if ((hardnessImage == null) || (hardnessImageExpireDate.before(now))) {
			byte[][] hardness = this.context.getHardnessMap().getHardness();
			if (hardness != null) {
				int width = hardness[0].length;
				int height = hardness.length;
				imageData = hardnessImageData;
				if (imageData == null) {
					imageData = new int[width * height];
				}
				int[] hardnessColors = new int[] {
					0x4000FF00,
					0x80FFFFFF,
					0x80808080,
					0x80FF0000
				};
				int offset = 0;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						imageData[offset++] =
							hardnessColors[Math.min(hardnessColors.length - 1, hardness[y][x] & 0xFF)];
					}
				}
				memoryImageData = new JDinkMemoryImageData(
						imageData,
						this.context.getHardnessMap().getX(),
						this.context.getHardnessMap().getY(),
						width,
						height);
				memoryImageData.setColorMixer(new JDinkTransparencyColorMixer());
				updateImage = true;
			}
		}

		Graphics2D g2 = (Graphics2D) g;
		Dimension size = this.getSize();
		
		Shape originalClipShape = g2.getClip();

		JDinkController controller = context.getController();

		JDinkDisplayInformation displayInformation = controller.getDisplayInformation(context);
		List<JDinkSpriteDisplayInformation> spriteInformationList = displayInformation.getSpriteInformationList();
		Map<Integer, JDinkSpriteLayerView> spriteLayerMap = displayInformation.getSpriteLayerMap();
		JDinkTile[] tiles = displayInformation.getTiles();

		Color backgroundColor;
		switch(displayInformation.getBackgroundColorIndex()) {
			case 0:
				backgroundColor = Color.BLACK;
				break;
			case 255:
				backgroundColor = Color.WHITE;
				break;
			default:
				backgroundColor = Color.GRAY;
		}

		g2.setColor(backgroundColor);
		g2.fillRect(0, 0, size.width, size.height);
		
		if (displayInformation.getTilesBounds() != null) {
			JDinkRectangle r = displayInformation.getTilesBounds();
			g2.setClip(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}
		
		for (int i = 0; i < tiles.length; i++) {
			JDinkTile tile = tiles[i];
			if ((tile.getTileSet() != null) && (tile.getTileSet().getImage() != null)) {
				JDinkRectangle r = tile.getSourceRectangle();
				g2.drawImage(getAwtImage(tile.getTileSet().getImage()),
						tile.getX(), tile.getY(), tile.getX() + r.getWidth(),
						tile.getY() + r.getHeight(),
						r.getX(), r.getY(),
						r.getX() + r.getWidth(),
						r.getY() + r.getHeight(),
						this);
			}
		}

		/**
		 * Rectangles are used for debug purpose only.
		 */
		Collection<Rectangle> rectangles = new LinkedList<Rectangle>();
		
		JDinkSpriteLayerView spriteLayer = null;
		
		JDinkRectangle layerClipBounds = null;
		JDinkShape currentClipBounds = displayInformation.getTilesBounds();
		
		// draw the sprite images...
		JDinkSpriteDisplayInformation[] sprites = spriteInformationList.toArray(
				new JDinkSpriteDisplayInformation[spriteInformationList.size()]);
		for (int i = 0; i < sprites.length; i++) {
			JDinkSpriteDisplayInformation sprite = sprites[i];
			JDinkSequence sequence = sprite.getSequence();
			JDinkSequenceFrame frame = sprite.getFrame();
			if (frame != null) {
				JDinkImage dinkImage = context.getImage(sequence, frame);
				Image image = getAwtImage(dinkImage);
				if (image != null) {
					if ((spriteLayer == null) ||
							(spriteLayer.getLayerNumber()  != sprite.getLayerNumber())) {
						spriteLayer = spriteLayerMap.get(
								Integer.valueOf(sprite.getLayerNumber()));
						JDinkShape layerBounds = null;
						if (spriteLayer != null) {
							layerBounds = spriteLayer.getBounds();
						}
						if ((layerBounds != null) && (!sprite.isNoClip())) {
							layerClipBounds = layerBounds.getBounds();
//							JDinkRectangle r = layerBounds.getBounds();
//							g2.setClip(r.getX(), r.getY(), r.getWidth(), r.getHeight());
						} else {
							layerClipBounds = null;
//							g2.setClip(originalClipShape);
						}
					}

					JDinkRectangle bounds = frame.getBounds();
					if (sprite.isPositionAbsolute()) {
						bounds = bounds.getLocatedTo(0, 0);
					}
					
					JDinkShape finalClipBounds;
					JDinkShape clipShape = sprite.getClipShape();
					if (clipShape != null) {
//						JDinkRectangle r = clipShape.getBounds().getTranslated(
//								sprite.getX(),
//								sprite.getY());
						JDinkRectangle r = clipShape.getBounds().getTranslated(
								bounds.getX() + sprite.getX(),
								bounds.getY() + sprite.getY());
						if (layerClipBounds != null) {
							finalClipBounds = r.intersection(layerClipBounds);
						} else {
							finalClipBounds = r;
						}
//						g2.setClip(r.getX(), r.getY(), r.getWidth(), r.getHeight());
					} else {
						finalClipBounds = layerClipBounds;
//						g2.setClip(originalClipShape);
					}
					
					if ((finalClipBounds != currentClipBounds) &&
							((currentClipBounds == null) || (!currentClipBounds.equals(finalClipBounds)))) {
						if (finalClipBounds != null) {
							JDinkRectangle r = finalClipBounds.getBounds();
							g2.setClip(r.getX(), r.getY(), r.getWidth(), r.getHeight());
						} else {
							g2.setClip(originalClipShape);
						}
						currentClipBounds = finalClipBounds;
					}

//					bounds = new Rectangle(sprite.getX() + bounds.x, sprite.getY() + bounds.y, bounds.width, bounds.height);
//					rectangles.add(bounds);
					JDinkPoint offset = sprite.getOffset();
					int spriteSize = sprite.getSize();
//					spriteSize = 100;
					if (spriteSize == 100) {
						if ((sprite.isFill()) && (clipShape != null) && (bounds.getWidth() > 0) && (bounds.getHeight() > 0)) {
							JDinkRectangle r = clipShape.getBounds();
							int columns = (r.getWidth() + r.getX() + bounds.getWidth() - 1) / bounds.getWidth();
							int rows = (r.getHeight() + r.getY() + bounds.getHeight() - 1) / bounds.getHeight();
							int y = sprite.getY() + bounds.getY() + offset.getY();
							for (int row = 0; row < rows; row++) {
								int x = sprite.getX() + bounds.getX() + offset.getX();
								for (int column = 0; column < columns; column++) {
									g2.drawImage(image, x, y, this);
									x += bounds.getWidth();
								}
								y += bounds.getHeight();
							}
						} else {
							g2.drawImage(image, sprite.getX() + bounds.getX() + offset.getX(),
									sprite.getY() + bounds.getY() + offset.getY(), this);
						}
					} else {
						int scaledWidth = (bounds.getWidth() * spriteSize) / 100;
						int offsetX = ((bounds.getWidth() - scaledWidth) / 2);
						int scaledHeight = (bounds.getHeight() * spriteSize) / 100;
						int offsetY = ((bounds.getHeight() - scaledHeight) / 2);
						g2.drawImage(image, sprite.getX() + bounds.getX() + offset.getX() + offsetX,
								sprite.getY() + bounds.getY() + offset.getY() + offsetY,
								scaledWidth,
								scaledHeight, this);
					}
				}
			}
		}
		
		g2.setClip(originalClipShape);


		if (updateImage) {
			final int spiteLocationSize = 6;
			for (int i = 0; i < sprites.length; i++) {
				JDinkSpriteDisplayInformation sprite = sprites[i];
				JDinkShape collisionShape = sprite.getCollisionShape();
				JDinkSequenceFrame frame = sprite.getFrame();
				if (frame != null) {
					JDinkRectangle bounds = frame.getBounds();
	//				bounds = new Rectangle(sprite.getX() + bounds.x, sprite.getY() + bounds.y, bounds.width, bounds.height);
	//				g2.setColor(frameBoundsColor);
	//				g2.fillRect(
	//						sprite.getX() + (int) bounds.getX(),
	//						sprite.getY() + (int) bounds.getY(),
	//						(int) bounds.getWidth(),
	//						(int) bounds.getHeight());
					if (bounds != null) {
						memoryImageData.fillRectangle(
								sprite.getX() + (int) bounds.getX(),
								sprite.getY() + (int) bounds.getY(),
								(int) bounds.getWidth(),
								(int) bounds.getHeight(),
								frameBoundsColor.getRGB());
					}
				}
				if (collisionShape != null) {
					JDinkRectangle bounds = collisionShape.getBounds();
	//				g2.setColor(spriteCollisionBoundsColor);
	//				g2.fillRect(sprite.getX() + bounds.getX(), sprite.getY() + bounds.getY(), bounds.getWidth(), bounds.getHeight());
//					memoryImageData.fillRectangle(
//							sprite.getX() + bounds.getX(),
//							sprite.getY() + bounds.getY(),
//							bounds.getWidth(),
//							bounds.getHeight(),
//							spriteCollisionBoundsColor.getRGB());
					memoryImageData.drawRectangle(
							sprite.getX() + bounds.getX(),
							sprite.getY() + bounds.getY(),
							bounds.getWidth(),
							bounds.getHeight(),
							3,
							spriteCollisionBoundsColor.getRGB());
				}
	//			g2.setColor(spriteLocationColor);
	//			g2.fillRect(
	//					sprite.getX() - spiteLocationSize / 2,
	//					sprite.getY() - spiteLocationSize / 2,
	//					spiteLocationSize,
	//					spiteLocationSize);
				memoryImageData.fillRectangle(
						sprite.getX() - spiteLocationSize / 2,
						sprite.getY() - spiteLocationSize / 2,
						spiteLocationSize,
						spiteLocationSize,
						spriteLocationColor.getRGB());
			}
		}


		boolean drawRectangles = false;
		if (drawRectangles) {
			for (Rectangle bounds: rectangles) {
				g2.setColor(new Color(255, 255, 255, 100));
				g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}

		JDinkTextParser textParser = new JDinkTextParser();
		int textBorder = 5;
		Rectangle textClipRectangle = new Rectangle(
				textBorder, textBorder, 620 - 2 * textBorder, 400 - 2 * textBorder);
		int[] fontColors = this.context.getFontColors();
		textParser.setFontColors(fontColors);
		int defaultFontColor = 0;
		if ((fontColors != null) && (fontColors.length > 14)) {
			defaultFontColor = fontColors[14];
		}
		for (int i = 0; i < sprites.length; i++) {
			FontRenderContext frc = g2.getFontRenderContext();
			if (frc == null) {
				frc = new FontRenderContext(null, false, false);
			}
			JDinkSpriteDisplayInformation sprite = sprites[i];
			String text = sprite.getText();
			if ((text != null) && (text.length() > 0)) {
				JDinkSpriteDisplayInformation parentSprite = sprite.getParent();
				boolean centered = true;
				Rectangle r;
				if ((parentSprite != null) && (parentSprite.getSequenceNumber() == 1200)) {
					centered = false;
				}
				if ((parentSprite == null) || (parentSprite.getSpriteNumber() == 1000)) {
					r = new Rectangle(sprite.getX(), sprite.getY(), 620, 400);
				} else {
					r = new Rectangle(sprite.getX(), sprite.getY() - 150, 150, 150);
					if (sprite.getX() + 150 > 620) {
						r.x += ((sprite.getX() + 150) - 620) - (((sprite.getX() + 150) - 620) * 2);
					}
				}
				Rectangle adjustedRectangle = fitRectangleInto(new Rectangle(r), textClipRectangle);
				r.x = adjustedRectangle.x;
				r.width = adjustedRectangle.width;
				int color = defaultFontColor;
				List<TextLayout> textLayouts = new ArrayList<TextLayout>();
				for (Iterator<JDinkTextFragment> it = textParser.getTextFragmentIterator(text); it.hasNext(); ) {
					JDinkTextFragment textFragment = (JDinkTextFragment) it.next();
					String s = textFragment.getText();
					if (s.length() > 0) {
						if (textFragment.getColor() > 0) {
							color = textFragment.getColor();
						}
						AttributedString attributedString = new AttributedString(s);
						AttributedCharacterIterator characterIterator =
							attributedString.getIterator();
						attributedString.addAttribute(TextAttribute.FONT, textFont);
						LineBreakMeasurer measurer =
							new LineBreakMeasurer(characterIterator, frc);
						int textHeight = 0;
						textLayouts.clear();
						while (measurer.getPosition() < characterIterator.getEndIndex()) {
							// Get line
							TextLayout textLayout = measurer.nextLayout(r.width);
							textLayouts.add(textLayout);
							textHeight += textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
						}
						r.height = textHeight;

						adjustedRectangle = fitRectangleInto(new Rectangle(r), textClipRectangle);
						r.y = adjustedRectangle.y;
						r.height = adjustedRectangle.height;
//						log.info("text r=" + r);
//						g2.setColor(new Color(8, 14, 21));
//						drawTextLayouts(g2, textLayouts, r.x, r.y, r.width, centered);
//						drawTextLayouts(g2, textLayouts, r.x - 2, r.y + 1, r.width, centered);
//						drawTextLayouts(g2, textLayouts, r.x - 1, r.y - 1, r.width, centered);
//						drawTextLayouts(g2, textLayouts, r.x - 1, r.y, r.width, centered);
//
//						g2.setColor(new Color(color));
//						drawTextLayouts(g2, textLayouts, r.x - 1, r.y, r.width, centered);

						g2.setColor(new Color(8, 14, 21));
						drawTextLayouts(g2, textLayouts, r.x - 1, r.y - 1, r.width, centered);
						drawTextLayouts(g2, textLayouts, r.x - 1, r.y + 1, r.width, centered);
						drawTextLayouts(g2, textLayouts, r.x + 1, r.y - 1, r.width, centered);
						drawTextLayouts(g2, textLayouts, r.x + 1, r.y + 1, r.width, centered);

						g2.setColor(new Color(color));
						drawTextLayouts(g2, textLayouts, r.x, r.y, r.width, centered);
					}
				}
			}
		}

		if (hardnessVisible) {
			if (updateImage) {
				int width = memoryImageData.getWidth();
				int height = memoryImageData.getHeight();
				hardnessImageExpireDate = new Date(now.getTime() + imageDataUpdateInterval);
				if (hardnessImage == null) {
					hardnessImageData = imageData;
					hardnessImageSource = new MemoryImageSource(
							width, height, imageData, 0, width);
					hardnessImageSource.setAnimated(true);
					hardnessImage = this.createImage(hardnessImageSource);
				} else {
					hardnessImageSource.newPixels(0, 0, width, height);
				}
			}
	
			if (hardnessImage != null) {
				g2.drawImage(
						hardnessImage,
						context.getHardnessMap().getX(),
						context.getHardnessMap().getY(),
						this);
			}
		}
		
		if (!paintListeners.isEmpty()) {
			for (JDinkSwingPaintListener paintListener: paintListeners) {
				paintListener.onPaint(g2);
			}
		}
		notifyFullyRepainted();
	}
	
	public void setHardnessVisible(boolean hardnessVisible) {
		if (this.hardnessVisible != hardnessVisible) {
			this.hardnessVisible = hardnessVisible;
			hardnessImage = null;
			this.repaint();
		}
	}
	
	public boolean isHardnessVisible() {
		return hardnessVisible;
	}
}
