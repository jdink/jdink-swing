package de.siteof.swing.util;

import java.awt.EventQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SwingUtil {

	private static final Log log	= LogFactory.getLog(SwingUtil.class);


	public static void invokeLater(Runnable runnable) {
		if (!EventQueue.isDispatchThread()) {
			log.debug("queueing runnable");
			EventQueue.invokeLater(runnable);
		} else {
			log.debug("executing runnable");
			runnable.run();
		}
	}

}
