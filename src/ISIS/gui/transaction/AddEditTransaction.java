package ISIS.gui.transaction;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;

import ISIS.customer.Customer;
import ISIS.gui.AddEditView;
import ISIS.gui.ErrorLogger;
import ISIS.gui.HintField;
import ISIS.gui.ListButtonListener;
import ISIS.gui.ListView;
import ISIS.gui.SplitPane;
import ISIS.gui.WrapLayout;
import ISIS.gui.item.SearchListItems;
import ISIS.gui.simplelists.ListAddress;
import ISIS.gui.simplelists.ListBilling;
import ISIS.gui.simplelists.ListTransactionLineItem;
import ISIS.misc.Address;
import ISIS.misc.Billing;
import ISIS.transaction.Transaction;
import ISIS.transaction.Transaction.TransactionStatus;

/**
 * View for adding and editing customers.
 */
public class AddEditTransaction extends AddEditView {
	private static final long			serialVersionUID	= 1L;
	JCheckBox							returnTransaction;
	final JComboBox<TransactionStatus>	status;
	HintField							address;
	HintField							billing;
	Transaction							transaction;
	Customer							customer;
	JPanel								otherListsContainer;
	CardLayout							otherListsCardLayout;
	JToggleButton						address_select, billing_select,
			item_select;
	ArrayList<JToggleButton>			cardLayoutViewButtons;
	static double						dividerRatio		= 0;
	JPanel								buttonHolder;
	
	/**
	 * Public constructor: returns new instance of add/edit customer view.
	 * 
	 * @param splitPane
	 * @param customer
	 */
	public AddEditTransaction(SplitPane splitPane, Customer customer) {
		super(splitPane);
		this.customer = customer;
		this.transaction = new Transaction(this.customer);
		try {
			this.transaction.save();
		} catch (SQLException e) {
			ErrorLogger.error(e, "Failed to save a new transaction.", true,
					true);
			throw new RuntimeException(e);
		}
		this.status = new JComboBox<>(TransactionStatus.values());
		this.status.setSelectedItem(this.transaction.getStatus());
		
		this.populateElements();
	}
	
	/**
	 * Public constructor: returns new instance of add/edit transaction view.
	 * For viewing/modifying a transaction.
	 * 
	 * @param splitPane
	 * @param pkey
	 * @throws SQLException
	 * @wbp.parser.constructor
	 */
	public AddEditTransaction(SplitPane splitPane, int pkey)
			throws SQLException {
		super(splitPane);
		this.transaction = new Transaction(pkey, true);
		
		try {
			this.customer = this.transaction.getCustomer();
		} catch (SQLException e) {
			throw new SQLException("Failed to fetch customer.", e);
		}
		if (this.transaction.getStatus() != TransactionStatus.ACTIVE) {
			TransactionStatus[] statuses = Arrays.copyOfRange(
					TransactionStatus.values(), 1,
					TransactionStatus.values().length);
			this.status = new JComboBox<>(statuses);
		} else {
			this.status = new JComboBox<>(TransactionStatus.values());
		}
		this.status.setSelectedItem(this.transaction.getStatus());
		
		this.populateElements();
		this.reloadAddress();
		this.reloadBilling();
	}
	
	/**
	 * Discards any modifications.
	 */
	@Override
	public void cancel() {}
	
	/*
	 * (non-Javadoc)
	 * @see ISIS.gui.AddEditView#newWasSaved()
	 */
	@Override
	protected void doSaveRecordAction() {
		
		@SuppressWarnings("rawtypes")
		ListView l;
		
		final ListAddress listAddress;
		// Add the other lists to the JPanel and register with the layout
		this.otherListsContainer.add(listAddress = new ListAddress(
				this.splitPane, this, this.customer, this.customer.getPkey(),
				true));
		this.otherListsCardLayout.addLayoutComponent(listAddress, "Address");
		
		final ListBilling listBilling;
		// next
		this.otherListsContainer.add(listBilling = new ListBilling(
				this.splitPane, this, this.customer, this.customer.getPkey()));
		this.otherListsCardLayout.addLayoutComponent(listBilling, "Billing");
		
		// next
		this.otherListsContainer.add(l = new SearchListItems(this.splitPane,
				this, this.customer, this.transaction));
		this.otherListsCardLayout.addLayoutComponent(l, "Items");
		
		// Add action listeners to the buttons
		this.address_select
				.addActionListener(new ListButtonListener(
						this.otherListsCardLayout, this.otherListsContainer,
						"Address"));
		listAddress.setSelectAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int pkey = listAddress.getSelectedPkey();
				if (pkey == -1) {
					return;
				}
				try {
					AddEditTransaction.this.transaction.setAddress(new Address(
							pkey, true));
					AddEditTransaction.this.transaction.save();
					AddEditTransaction.this.reloadAddress();
					AddEditTransaction.this.reloadBilling();
				} catch (SQLException ex) {
					ErrorLogger.error(ex, "Failed to add item to transaction.",
							true, true);
				}
			}
		});
		listBilling.setSelectAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int pkey = listBilling.getSelectedPkey();
				if (pkey == -1) {
					return;
				}
				try {
					AddEditTransaction.this.transaction.setBilling(new Billing(
							pkey, true));
					AddEditTransaction.this.transaction.save();
				} catch (SQLException ex) {
					ErrorLogger
							.error(ex, "Failed to add billing to transaction.",
									true, true);
				}
			}
		});
		
		if (this.transaction.getStatus() != TransactionStatus.ACTIVE) {
			TransactionStatus[] statuses = Arrays.copyOfRange(
					TransactionStatus.values(), 1,
					TransactionStatus.values().length);
			DefaultComboBoxModel<TransactionStatus> model = new DefaultComboBoxModel<>(
					statuses);
			this.status.setModel(model);
			this.status.setSelectedItem(this.transaction.getStatus());
			
			if (this.cardLayoutViewButtons.contains(this.item_select)) {
				if (this.item_select.isSelected()) {
					this.address_select.doClick();
				}
				this.cardLayoutViewButtons.remove(this.item_select);
				this.buttonHolder.remove(this.item_select);
			}
		}
		
		// next
		this.billing_select
				.addActionListener(new ListButtonListener(
						this.otherListsCardLayout, this.otherListsContainer,
						"Billing"));
		
		// next
		if (this.transaction != null
				&& this.transaction.getStatus() == TransactionStatus.ACTIVE) {
			this.item_select.addActionListener(new ListButtonListener(
					this.otherListsCardLayout, this.otherListsContainer,
					"Items"));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see ISIS.gui.View#getCurrentRecord()
	 */
	@Override
	public Transaction getCurrentRecord() {
		if (this.transaction == null) {
			if (!this.isAnyFieldDifferentFromDefault()) {
				return null;
			}
			this.transaction = new Transaction(this.customer);
			this.transaction.setStatus((TransactionStatus) this.status
					.getSelectedItem());
			this.transaction.setStatus((TransactionStatus) this.status
					.getSelectedItem());
		} else {
			this.transaction.setStatus((TransactionStatus) this.status
					.getSelectedItem());
		}
		return this.transaction;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ISIS.gui.View#isAnyFieldsDifferentFromDefault()
	 */
	@Override
	public boolean isAnyFieldDifferentFromDefault() {
		return true;
		// TODO: This
		// return !(this.active.isSelected() && this.email.getText().isEmpty()
		// && this.password.getText().isEmpty() && this.note.getText()
		// .isEmpty());
	}
	
	/**
	 * Draws all necessary components on the window.
	 */
	private void populateElements() {
		this.setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane() {
			private static final long	serialVersionUID	= 1L;
			
			@Override
			public void doLayout() {
				this.setDividerLocation((int) (this.getWidth() * (AddEditTransaction.dividerRatio == 0 ? .55
						: AddEditTransaction.dividerRatio)));
				super.doLayout();
			}
			
			@Override
			public void setDividerLocation(int location) {
				AddEditTransaction.dividerRatio = location
						/ (double) this.getWidth();
				super.setDividerLocation(location);
			}
		};
		split.setOpaque(false);
		split.setBorder(null);
		JPanel main = new JPanel(new GridBagLayout());
		main.setOpaque(false);
		GridBagConstraints c;
		this.cardLayoutViewButtons = new ArrayList<>();
		int x = 0, y = 0;
		
		c = new GridBagConstraints();
		c.weightx = 0;
		c.gridx = x++;
		c.gridy = y;
		c.fill = GridBagConstraints.BOTH;
		main.add(new JLabel("Return"), c);
		
		c = new GridBagConstraints();
		c.weightx = 1;
		c.gridx = x--;
		c.gridy = y++;
		c.fill = GridBagConstraints.BOTH;
		main.add(this.returnTransaction = new JCheckBox("", true), c);
		this.returnTransaction.setEnabled(false);
		
		c = new GridBagConstraints();
		c.weightx = 0;
		c.gridx = x++;
		c.gridy = y;
		c.fill = GridBagConstraints.BOTH;
		main.add(new JLabel("Status"), c);
		
		c = new GridBagConstraints();
		c.weightx = 1;
		c.gridx = x--;
		c.gridy = y++;
		c.fill = GridBagConstraints.BOTH;
		main.add(this.status, c);
		
		c = new GridBagConstraints();
		c.weightx = 0;
		c.gridx = x++;
		c.gridy = y;
		c.fill = GridBagConstraints.BOTH;
		main.add(new JLabel("Address"), c);
		
		c = new GridBagConstraints();
		c.weightx = 1;
		c.gridx = x--;
		c.gridy = y++;
		c.fill = GridBagConstraints.BOTH;
		main.add((this.address = new HintField()).make(), c);
		this.address.setEnabled(false);
		
		c = new GridBagConstraints();
		c.weightx = 0;
		c.gridx = x++;
		c.gridy = y;
		c.fill = GridBagConstraints.BOTH;
		main.add(new JLabel("Billing"), c);
		
		c = new GridBagConstraints();
		c.weightx = 1;
		c.gridx = x--;
		c.gridy = y++;
		c.fill = GridBagConstraints.BOTH;
		main.add((this.billing = new HintField()).make(), c);
		this.billing.setEnabled(false);
		
		c = new GridBagConstraints();
		c.weightx = 0;
		c.gridx = x++;
		c.gridy = y;
		c.fill = GridBagConstraints.BOTH;
		main.add(new JLabel("Items"), c);
		
		c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = x--;
		c.gridy = y++;
		c.fill = GridBagConstraints.BOTH;
		main.add(new ListTransactionLineItem(this.splitPane, this,
				this.transaction), c);
		
		split.setLeftComponent(main);
		
		JPanel otherArea = new JPanel(new BorderLayout());
		otherArea.setOpaque(false);
		this.buttonHolder = new JPanel(new WrapLayout());
		this.buttonHolder.setOpaque(false);
		
		// Add buttons for the cards (Other lists)
		this.buttonHolder.add(
				this.address_select = new JToggleButton("Address"), c);
		this.buttonHolder.add(
				this.billing_select = new JToggleButton("Billing"), c);
		if (this.transaction != null
				&& this.transaction.getStatus() == TransactionStatus.ACTIVE) {
			this.buttonHolder.add(
					this.item_select = new JToggleButton("Items"), c);
		}
		
		// Add buttons to the buttons ArrayList
		this.cardLayoutViewButtons.add(this.address_select);
		this.cardLayoutViewButtons.add(this.billing_select);
		if (this.transaction != null
				&& this.transaction.getStatus() == TransactionStatus.ACTIVE) {
			this.cardLayoutViewButtons.add(this.item_select);
		}
		
		// Add the button holder at the top of the right section
		otherArea.add(this.buttonHolder, BorderLayout.NORTH);
		
		// Add the JPanel(card layout) to the right section center
		otherArea.add(this.otherListsContainer = new JPanel(
				this.otherListsCardLayout = new CardLayout()),
				BorderLayout.CENTER);
		this.otherListsContainer.setOpaque(false);
		
		split.setRightComponent(otherArea);
		split.setResizeWeight(.5);
		
		this.add(split, BorderLayout.CENTER);
		
		ButtonGroup group = new ButtonGroup();
		for (JToggleButton b : this.cardLayoutViewButtons) {
			b.setFont(new Font("Small", Font.PLAIN, 11));
			group.add(b);
		}
		
		this.address_select.doClick();
		
		this.doSaveRecordAction();
	}
	
	/**
	 * @pre - this.transaction.hasAddress() == true
	 * @post - gets an address and sets it.
	 */
	public void reloadAddress() {
		try {
			if (this.transaction.hasAddress()) {
				this.address.setText(this.transaction.getAddress()
						.getStreetAddress()
						+ this.transaction.getAddress().getZIP());
			}
		} catch (SQLException e) {
			ErrorLogger.error(e, "Failed to update address", true, true);
		}
	}
	
	/**
	 * @pre - this.transaction.getBilling() == true
	 * @post - gets and sets the billing.
	 */
	public void reloadBilling() {
		try {
			if (this.transaction.getBilling() != null) {
				this.billing.setText(this.transaction.getBilling()
						.getBillingType().toString());
			}
		} catch (SQLException e) {
			ErrorLogger.error(e, "Failed to update billing", true, true);
		}
	}
}