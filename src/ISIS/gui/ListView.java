package ISIS.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import ISIS.database.DB;
import ISIS.database.Field;
import ISIS.database.Record;
import ISIS.session.Session;

public abstract class ListView<E extends Record> extends View {
	private static final long		serialVersionUID	= 1L;
	protected JTable				table;
	protected ArrayList<E>			records;
	protected IRSTableModel			tableModel;
	protected ArrayList<Integer>	keys				= new ArrayList<Integer>();
	
	public ListView(SplitPane splitPane, boolean multiSelect) {
		super(splitPane);
		
		// watch the DB table associated with this list for changes.
		Session.watchTable(this.getTableName(), new TableUpdateListener() {
			private static final long	serialVersionUID	= 1L;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ListView.this.doFillTable();
			}
		});
		
		// add the table.
		this.table = new JTable();
		this.table
				.setSelectionMode(multiSelect ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
						: ListSelectionModel.SINGLE_SELECTION);
		this.table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
		this.table.setAutoCreateRowSorter(true);
		this.setFocusCycleRoot(true);
		this.setOpaque(false);
	}
	
	/**
	 * Cancel is not supported.
	 */
	@Override
	public void cancel() {
		throw new UnsupportedOperationException("Not supported.");
	}
	
	// Populate records in the instance.
	protected abstract void doFillTable();
	
	// Do any pre-population of records, then call doFillTable (supposedly).
	protected abstract void fillTable();
	
	/**
	 * This type of view doesn't own a record.
	 */
	@Override
	public Record getCurrentRecord() {
		return null;
	}
	
	// DB table the data is coming from.
	protected abstract DB.TableName getTableName();
	
	@Override
	public Boolean isAnyFieldDifferentFromDefault() {
		return null;
	}
	
	// Helper method. Map results of query to an arraylist of records.
	protected abstract ArrayList<E> mapResults(
			ArrayList<HashMap<String, Field>> results);
	
	/**
	 * This type of view needs not be saved.
	 */
	@Override
	public boolean needsSave() {
		return false;
	}
	
	protected final void populateTable() {
		this.table.removeAll();
		this.keys.clear();
		this.tableModel.rowColors = new ArrayList<Color>(this.records.size());
		this.tableModel.setRowCount(0);
		for (E i : this.records) {
			this.tableModel.rowColors.add(IRSTableModel.DEFAULT_COLOR);
			// this.tableModel.rowColors.add(null);
			this.tableModel.addRow(i);
			if (!i.isActive()) {
				this.tableModel.setColorAt(
						this.tableModel.rowColors.size() - 1,
						IRSTableModel.INACTIVE_COLOR);
			}
		}
		for (int i = 0; i < this.table.getColumnCount(); ++i) {
			this.table.getColumn(this.table.getColumnName(i)).setCellRenderer(
					new DefaultTableCellRenderer() {
						private static final long	serialVersionUID	= 1L;
						
						@Override
						public Component getTableCellRendererComponent(
								JTable table, Object value, boolean isSelected,
								boolean hasFocus, int row, int column) {
							Component c = super.getTableCellRendererComponent(
									table, value, isSelected, hasFocus, row,
									column);
							if (isSelected) {
								c.setBackground(table.getSelectionBackground());
							} else {
								c.setBackground(ListView.this.tableModel.rowColors
										.get(row));
							}
							return c;
						}
					});
		}
	}
	
	/**
	 * Save is not supported.
	 */
	@Override
	public void save() throws SQLException {
		throw new UnsupportedOperationException("Not supported.");
	}
	
	// Sets the table's model.
	protected final void setTableModel(IRSTableModel model) {
		this.tableModel = model;
		this.table.setModel(model);
		this.table.setFocusable(false);
	}
}
