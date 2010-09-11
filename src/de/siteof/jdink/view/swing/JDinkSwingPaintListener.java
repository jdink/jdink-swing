package de.siteof.jdink.view.swing;

import java.awt.Graphics;
import java.util.EventListener;

public interface JDinkSwingPaintListener extends EventListener {
	
	void onPaint(Graphics g);

}
