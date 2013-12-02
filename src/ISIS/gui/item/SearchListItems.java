package ISIS.gui.item;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JScrollPane;

import ISIS.customer.Customer;
import ISIS.database.DB;
import ISIS.database.Field;
import ISIS.database.Record;
import ISIS.gui.ErrorLogger;
import ISIS.gui.IRSTableModel;
import ISIS.gui.SearchListView;
import ISIS.gui.SplitPane;
import ISIS.gui.View;
import ISIS.gui.transaction.AddEditTransactionLineItem;
import ISIS.item.Item;

/**
 * List of items. Allows you to query and act on items.
 */
public class SearchListItems extends SearchListView<Item> {
	private static final long	serialVersionUID	= 1L;
	private final View			pusher;
	private final Customer		customer;
	private JButton				editButton, selectButton;
	
	/**
	 * Constructs new Customer list view.
	 */
	public SearchListItems(SplitPane splitPane) {
		super(splitPane);
		this.pusher = null;
		this.customer = null;
		this.setLayout(new GridBagLayout());
		GridBagConstraints c;
		
		int x = 0, y = 0;
		int buttonNameSel = 0;
		JButton addButton = new JButton(this.buttonNames[buttonNameSel++]);
		this.editButton = new JButton(this.buttonNames[buttonNameSel++]);
		// JButton activeButton = new
		// JButton(this.buttonNames[buttonNameSel++]);
		// TODO: toggle button
		
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// SearchListCustomers.this.splitPane.push(new AddEditCustomer(
				// SearchListCustomers.this.splitPane),
				// SplitPane.LayoutType.HORIZONTAL);
				SearchListItems.this.splitPane.push(new AddEditItem(
						SearchListItems.this.splitPane),
						SplitPane.LayoutType.HORIZONTAL, SearchListItems.this);
			}
		});
		
		this.editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selected = SearchListItems.this.table.getSelectedRow();
				
				if (selected == -1) {
					selected = SearchListItems.this.selected;
					if (selected == -1) {
						return;
					}
					SearchListItems.this.table.setRowSelectionInterval(
							selected, selected);
				}
				
				int pkey = SearchListItems.this.keys.get(selected);
				
				try {
					// SearchListItems.this.splitPane.push(new AddEditItem(
					// SearchListItems.this.splitPane, pkey),
					// SplitPane.LayoutType.HORIZONTAL);
					SearchListItems.this.splitPane.push(new AddEditItem(
							SearchListItems.this.splitPane, pkey),
							SplitPane.LayoutType.HORIZONTAL,
							SearchListItems.this);
					
				} catch (SQLException ex) {
					ErrorLogger.error(ex, "Failed to open the item record.",
							true, true);
				}
			}
		});
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = x++;
		c.gridy = y;
		this.add(addButton, c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = x++;
		this.add(this.editButton, c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = x++;
		c.weightx = 1;
		this.add(this.searchField, c);
		
		this.setTableModel(new IRSTableModel() {
			private static final long	serialVersionUID	= 1L;
			
			@Override
			public void addRow(Record record) {
				Item item = (Item) record;
				Object[] array = new Object[this.getColumnCount()];
				int i = 0;
				
				array[i++] = item.getSKU();
				array[i++] = item.getName();
				array[i++] = item.getPrice();
				array[i++] = item.getOnHandQty();
				array[i++] = item.getUOM();
				
				super.addRow(array);
				SearchListItems.this.keys.add(item.getPkey());
			}
		});
		this.tableModel.setColumnTitles("SKU", "name", "price", "qty", "UOM");
		this.fillTable();
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = ++y;
		c.gridwidth = x;
		c.gridx = x = 0;
		c.weighty = 1;
		this.add(new JScrollPane(this.table), c);
	}
	
	public SearchListItems(SplitPane splitPane, View pusher, Customer customer,
			Integer key) {
		super(splitPane);
		this.setLayout(new GridBagLayout());
		this.pusher = pusher;
		this.customer = customer;
		
		this.selectButton = new JButton("Select");
		this.selectAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selected = SearchListItems.this.table.getSelectedRow();
				if (selected == -1) {
					return;
				}
				
				int pkey = SearchListItems.this.keys.get(selected);
				try {
					SearchListItems.this.splitPane.push(
							new AddEditTransactionLineItem(
									SearchListItems.this.splitPane,
									SearchListItems.this.customer, new Item(
											pkey, true)),
							SplitPane.LayoutType.HORIZONTAL,
							SearchListItems.this.pusher);
				} catch (SQLException ex) {
					ErrorLogger.error(ex, "Failed to add item to transaction.",
							true, true);
				}
			}
		});
		GridBagConstraints c = new GridBagConstraints();
		int x = 0, y = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = x++;
		c.gridy = y;
		this.add(this.selectButton, c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = x++;
		c.weightx = 1;
		this.add(this.searchField, c);
		
		this.editButton = new JButton("Edit");
		// TODO
		
		this.setTableModel(new IRSTableModel() {
			private static final long	serialVersionUID	= 1L;
			
			@Override
			public void addRow(Record record) {
				Item item = (Item) record;
				Object[] array = new Object[this.getColumnCount()];
				int i = 0;
				
				array[i++] = item.getSKU();
				array[i++] = item.getName();
				array[i++] = item.getPrice();
				array[i++] = item.getOnHandQty();
				array[i++] = item.getUOM();
				
				super.addRow(array);
				SearchListItems.this.keys.add(item.getPkey());
			}
		});
		this.tableModel.setColumnTitles("SKU", "name", "price", "qty", "UOM");
		this.fillTable();
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = ++y;
		c.gridwidth = x;
		c.gridx = x = 0;
		c.weighty = 1;
		this.add(new JScrollPane(this.table), c);
	}
	
	@Override
	protected DB.TableName getTableName() {
		return DB.TableName.item;
	}
	
	@Override
	protected ArrayList<Item> mapResults(
			ArrayList<HashMap<String, Field>> results) {
		ArrayList<Item> items = new ArrayList<Item>(results.size());
		for (HashMap<String, Field> result : results) {
			items.add(new Item(result));
		}
		return items;
	}
	
	public void selectAction(ActionListener listener) {
		this.selectButton.addActionListener(listener);
	}
	
	@Override
	protected void tableItemAction() {
		this.editButton.doClick();
	}
}
