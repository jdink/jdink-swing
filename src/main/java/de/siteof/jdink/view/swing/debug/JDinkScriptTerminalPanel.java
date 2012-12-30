package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.format.dinkc.JDinkCLoader;
import de.siteof.jdink.functions.JDinkExecutionContext;
import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.script.JDinkObjectType;
import de.siteof.jdink.script.JDinkScope;
import de.siteof.jdink.script.JDinkScriptCall;
import de.siteof.jdink.script.JDinkScriptConstants;
import de.siteof.jdink.script.JDinkScriptInstance;
import de.siteof.jdink.script.JDinkVariable;

public class JDinkScriptTerminalPanel extends JPanel implements EnableDisposePanel {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(JDinkScriptTerminalPanel.class);

	private final JDinkContext context;
	private final JDinkOutputPanel outputPanel;
	private final JTextField inputField;
	private final List<String> commandHistory = new LinkedList<String>();
	private JDinkExecutionContext executionContext;
	private int commandHistoryIndex;

	public JDinkScriptTerminalPanel(JDinkContext context) {
		super(new BorderLayout());
		this.context = context;
		this.add(outputPanel = new JDinkOutputPanel(100), BorderLayout.CENTER);
		this.add(inputField = new JTextField(), BorderLayout.SOUTH);
		inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent event) {
				if (event.getKeyChar() == '\n') {
					processSubmit();
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				switch (event.getKeyCode()) {
				case KeyEvent.VK_UP:
					processHistoryUp();
					break;
				case KeyEvent.VK_DOWN:
					processHistoryDown();
					break;
				}
			}});
		outputPanel.appendLine("[Script Terminal]");
	}

	private void processHistoryUp() {
		if (this.commandHistoryIndex > 0) {
			int historyIndex = this.commandHistoryIndex;
			addCommandHistory(this.inputField.getText().trim());
			this.commandHistoryIndex = historyIndex - 1;
			this.inputField.setText(this.commandHistory.get(this.commandHistoryIndex));
		}
	}

	private void processHistoryDown() {
		if (this.commandHistoryIndex + 1 < this.commandHistory.size()) {
			int historyIndex = this.commandHistoryIndex;
			addCommandHistory(this.inputField.getText().trim());
			this.commandHistoryIndex = historyIndex + 1;
			this.inputField.setText(this.commandHistory.get(this.commandHistoryIndex));
		} else {
			addCommandHistory(this.inputField.getText().trim());
			this.inputField.setText("");
			this.commandHistoryIndex = this.commandHistory.size();
		}
	}

	private void processSubmit() {
		String s = inputField.getText().trim();
		if (s.length() > 0) {
			addCommandHistory(s);
			inputField.setText("");
			this.commandHistoryIndex = this.commandHistory.size();
			executeScript(s);
		}
	}

	private void addCommandHistory(String s) {
		if (s.length() > 0) {
			if (this.commandHistoryIndex < this.commandHistory.size()) {
				String historyLine = this.commandHistory.get(this.commandHistoryIndex);
				if (!historyLine.equals(s)) {
					if (!this.commandHistory.get(this.commandHistory.size() - 1).equals(s)) {
						this.commandHistory.add(s);
					}
					this.commandHistoryIndex = this.commandHistory.size() - 1;
				}
			} else {
				if ((this.commandHistory.isEmpty()) ||
						(!this.commandHistory.get(this.commandHistory.size() - 1).equals(s))) {
					this.commandHistory.add(s);
				}
				this.commandHistoryIndex = this.commandHistory.size() - 1;
			}
		}
	}

	private void executeScript(final String s) {
		outputPanel.appendLine(">" + s);
		context.getController().invokeAndWait(new Runnable() {
			@Override
			public void run() {
				doExecuteScript(s);
			}
		});
	}

	private void doExecuteScript(String s) {
		String outputLine;
		try {
			JDinkCLoader loader = new JDinkCLoader();
			loader.setContext(context);
			Object o = loader.parseStatement(1, s);
			if (o instanceof JDinkScriptCall) {
				if (executionContext == null) {
					JDinkScope scope = new JDinkScope(context.getGlobalScope());
					JDinkScriptInstance scriptInstance = new JDinkScriptInstance(null, scope);
					scope.addInternalVariable(JDinkScriptConstants.THIS_INTERNAL_VARNAME, new JDinkVariable(
							JDinkObjectType.getObjectTypeInstance(JDinkScriptInstance.class),
							scriptInstance));
					executionContext =
						new JDinkExecutionContext(context, scope, null);
				}
				Object result = ((JDinkScriptCall) o).invoke(executionContext);
				outputLine = "result: " + result;
			} else {
				outputLine = "literal: " + o;
			}
		} catch (Throwable e) {
			String message = e.getMessage();
			if (message == null) {
				message = e.getClass().getSimpleName();
			}
			outputLine = "error: " + message;
			log.info("[executeScript] error executing line due to " + e, e);
		}
		if (outputLine != null) {
			final String finalOutputLine = outputLine;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					outputPanel.appendLine(finalOutputLine);
				}
			});
		}
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}
