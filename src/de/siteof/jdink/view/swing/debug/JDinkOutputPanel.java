package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class JDinkOutputPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JTextArea textArea;
	private final int maxLineCount;

	public JDinkOutputPanel(int maxRows) {
		super(new BorderLayout());
		this.maxLineCount = maxRows;
		this.add(new JScrollPane(textArea = new JTextArea()), BorderLayout.CENTER);
	}

	public void appendLine(String line) {
		this.append(line + "\n");
	}

//	private int countLines(String s) {
//		int currentIndex = 0;
//		int lineCount = 0;
//		while (currentIndex < s.length()) {
//			int pos = s.indexOf('\n', currentIndex);
//			if (pos >= 0) {
//				lineCount++;
//				currentIndex = pos + 1;
//			} else {
//				currentIndex = s.length();
//			}
//		}
//		return lineCount;
//	}

//	public void append(String s) {
//		try {
//			Document document = textArea.getDocument();
//			s = s.replace("\r\n", "\n").replace('\r', '\n');
//			int lineCount = countLines(s);
//			document.insertString(document.getLength(), s, null);
//			if (lineCount > 0) {
//				this.rowCount += lineCount;
//				if (this.rowCount > this.maxRows) {
//					int rowsToDelete = this.rowCount - this.maxRows;
//					String text = textArea.getText();
//					int rowsDeleted = 0;
//					int currentIndex = 0;
//					while ((rowsDeleted < rowsToDelete) && (currentIndex < text.length())) {
//						int pos = text.indexOf('\n', currentIndex);
//						if (pos >= 0) {
//							rowsDeleted++;
//							currentIndex = pos + 1;
//						}
//					}
//					document.remove(0, currentIndex);
//				}
//			}
//		} catch (BadLocationException e) {
//			// do nothing
//		}
//	}

	public void append(String s) {
		try {
			Document document = textArea.getDocument();
			s = s.replace("\r\n", "\n").replace('\r', '\n');
			document.insertString(document.getLength(), s, null);
			int lineCount = this.textArea.getLineCount();
			if (lineCount > this.maxLineCount) {
				int linesToDelete = lineCount - this.maxLineCount;
				int lineIndex = this.textArea.getLineStartOffset(linesToDelete + 1);
				document.remove(0, lineIndex);
			}
		} catch (BadLocationException e) {
			// do nothing
		}
	}

}
