package de.siteof.jdink.view.swing.debug;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.jdink.functions.ini.util.JDinkSequenceLazyLoader;
import de.siteof.jdink.model.JDinkContext;
import de.siteof.jdink.model.JDinkSequence;
import de.siteof.jdink.model.JDinkSequenceFrame;

public class JDinkShowSequencesInfoPanel extends JPanel implements EnableDisposePanel {
	
	private static class SequenceInformation {
		private final int sequenceNumber;
		private final String fileName;
		
		public SequenceInformation(int sequenceNumber, JDinkSequence sequence) {
			this.sequenceNumber = sequenceNumber;
			String fileName = null;
			if (sequence.getLazyLoader() instanceof JDinkSequenceLazyLoader) {
				JDinkSequenceLazyLoader lazyLoader =
					((JDinkSequenceLazyLoader) sequence.getLazyLoader());
				if (lazyLoader != null) {
					fileName = lazyLoader.getFileNamePrefix();
				}
			}
			if (fileName == null) {
				JDinkSequenceFrame frame = sequence.getFrame(
						sequence.getFirstFrameNumber(), false);
				if (frame != null) {
					fileName = frame.getFileName();
				} else {
					fileName = "?";
				}
			}
			if (sequence.isLoaded()) {
				fileName = fileName + " ("  + sequence.getFrameCount() + ")";
			} else {
				fileName = fileName + " (not loaded)";
			}
			this.fileName = fileName;
		}

		public int getSequenceNumber() {
			return sequenceNumber;
		}

		public String getFileName() {
			return fileName;
		}
	}
	
	private static class SequenceTableModel extends AbstractTableModel {
		
		private static final long serialVersionUID = 1L;
		
		private static final String[] columnNames = { "Seq", "File" };
		private List<SequenceInformation> rows;
		
		public SequenceTableModel() {
		}

		@Override
		public String toString() {
			return "CollisionTableModel [rows=" + rows + "]";
		}

		public void setRows(List<SequenceInformation> rows) {
			this.rows = rows;
			this.fireTableDataChanged();
		}
		
//		public List<VariableInformation> getRows() {
//			return this.rows;
//		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return (rows != null ? rows.size() : 0);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object result = null;
			if ((rows != null) && (rowIndex < rows.size())) {
				SequenceInformation info = rows.get(rowIndex);
				switch (columnIndex) {
				case 0:
					result = Integer.valueOf(info.getSequenceNumber());
					break;
				case 1:
					result = info.getFileName();
					break;
				}
			}
			return result;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}
		
	}
	
	private static final Log log = LogFactory.getLog(JDinkShowSequencesInfoPanel.class);

	private static final long serialVersionUID = 1L;
	
	private final JDinkContext context;
	private final SequenceTableModel model;
	private final JLabel label;
	private boolean updatesPaused;
	private boolean enabled;

	public JDinkShowSequencesInfoPanel(JDinkContext context) {
		super(new BorderLayout());
		this.context = context;
		this.setMinimumSize(new Dimension(100, 100));
		this.setSize(new Dimension(100, 100));
		
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(label = new JLabel("last updated: "), BorderLayout.SOUTH);
		this.add(bottomPanel, BorderLayout.SOUTH);

		final JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}});
		this.add(refreshButton, BorderLayout.NORTH);
		JPanel panel = new JPanel(new BorderLayout());
		JTable table = new JTable(model = new SequenceTableModel());
		panel.add(table.getTableHeader(), BorderLayout.PAGE_START);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		this.add(panel, BorderLayout.CENTER);
//		model.setRows(Arrays.asList(new CollisionInformation(new JDinkHardnessCollisionImpl((byte) 10))));
//		enable();
//		update();
	}
	
	public void dispose() {
		enabled = false;
	}
	
	public void enable() {
		if (!enabled) {
			enabled = true;
			update();
		}
	}
	
	public void update() {
		if (updatesPaused) {
			log.debug("updates paused");
			return;
		}
		context.getController().invokeLater(new Runnable() {			
			@Override
			public void run() {
				Collection<Integer> sequenceNumbers = context.getSequenceNumbers();
				final List<SequenceInformation> rows = new ArrayList<SequenceInformation>(sequenceNumbers.size());
				for (Integer sequenceNumber: sequenceNumbers) {
					JDinkSequence sequence = context.getSequence(sequenceNumber.intValue(), false);
					if (sequence != null) {
						rows.add(new SequenceInformation(sequenceNumber.intValue(), sequence));
					}
				}
				SwingUtilities.invokeLater(new Runnable() {						
					@Override
					public void run() {
						label.setText("last updated: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
						model.setRows(new ArrayList<SequenceInformation>(rows));
					}
				});
			}
		});
	}

}
