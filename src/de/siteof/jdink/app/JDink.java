/*
 * Created on 28.01.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.siteof.jdink.app;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * @author user
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class JDink {

	private static final Log log	= LogFactory.getLog(JDink.class);

	static {
		//PropertyConfigurator.configure("config/log4j.properties");
		DOMConfigurator.configure("config/log4j.xml");
	}

	public static void main(String[] args) throws Throwable {
		if (log.isDebugEnabled()) {
			log.debug("java.version=" + System.getProperty("java.version"));
		}
		JDinkApp app = new JDinkApp();
		app.start();
//		JDinkSwingFrame frame = new JDinkSwingFrame();
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.pack();
//		frame.setTitle("JDink");
//		frame.setVisible(true);
	}
}
