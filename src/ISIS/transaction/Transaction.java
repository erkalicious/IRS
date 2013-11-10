package ISIS.transaction;

import ISIS.customer.Customer;
import ISIS.database.DB;
import ISIS.database.Field;
import ISIS.database.Record;
import ISIS.database.UninitializedFieldException;
import ISIS.gui.ErrorLogger;
import ISIS.item.Item;
import ISIS.misc.Address;
import ISIS.misc.Billing;
import ISIS.session.Session;
import ISIS.user.User;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Transaction is the exchange of goods, legal tender, or rendering of
 * services between the client and a customer. A Transaction consists of a
 * customer, an intiating user, a set of items and information pertaining to the
 * items, billing information, various date/time information, and shipping
 * information. Invariants: The Transaction references exactly one customer
 * record.
 *
 * @customer != null The Transaction references exactly one user record.
 * @user != null The Transaction references exactly one set of associated dates.
 * @dates != null The transaction has a primary key field that is always set.
 * @pkey > 0
 */
public class Transaction extends Record {
    Customer customer = null;
    Billing billing = null;
    Address address = null;
    Transaction originalTransaction = null;
    boolean itemsInitialized = false;
    private ArrayList<TransactionLineItem> items = new ArrayList<>();

    {
        return this.billing;
    }

    /**
     * Public constructor. A Transaction starts with a user and a customer.
     * These attributes cannot be changed.
     */
    public Transaction(Customer customer) {
        super("transaction_", true);
        this.initializeFields(this.getFields());

        this.setFieldValue("customer", customer.getPkey());
    }

    /**
     * Public constructor. Takes a transaction database key, and has the option
     * to populate the fields from the database.
     */
    public Transaction(int pkey, boolean populate) throws SQLException {
        super("transaction_", true);
        this.initializeFields(this.getFields());

        this.setPkey(pkey);
        if (populate) {
            this.fetch();
        }
    }

    public Transaction(HashMap<String, Field> map) {
        super("transaction_", true, map);
    }

    /**
     * Public constructor. A return Transaction starts with a modifying user and
     * a transaction. This attributes cannot be changed.
     *
     * @pre originalSale.getStatus() == status.Finalized
     * @post originalSale.getModified() == true
     */
    public Transaction(Transaction originalSale) {
        super("transaction_", true);
        this.initializeFields(this.getFields());

        this.originalTransaction = originalSale;
        this.setFieldValue("parent_transaction", originalTransaction.getPkey());
    }

    /**
     * This table's fields.
     */
    private HashMap<String, Field> getFields() {
        HashMap<String, Field> fields = new HashMap<>(7);
        fields.put("pkey", new Field(false));
        fields.put("status", new Field(true));
        fields.put("customer", new Field(false));
        fields.put("type", new Field(false));
        fields.put("modified", new Field(true));
        fields.put("parent_transaction", new Field(false));
        fields.put("address", new Field(true));
        fields.put("billing", new Field(true));
        return fields;
    }

    @Override
    protected void postSave() throws SQLException {
        //save any new LineItems.
        for (TransactionLineItem item : this.items) {
            item.save();
        }
    }

    /**
     * Gets a list of returns that have been made on the transaction, or the
     * transaction that this return is being made on as well as returns
     * associated with it. If the transaction is not associated with a return,
     * an empty list is returned.
     */
    public ArrayList<Transaction> getRelatedTransactions() {
        return null;
    }

    /**
     * Adds an item to this transaction.
     *
     * @pre getItems().contains(item) == false
     * @post getItems().contains(item) == true
     */
    public void addItem(Item item, BigDecimal adjustment, BigDecimal quantity, String note) {
        if (this.items.contains(item)) {
            throw new RuntimeException("The transaction already contains this item.");
        }
        this.items.add(new TransactionLineItem(this, item, adjustment, quantity, note));
    }

    /**
     * Removes an item from this transaction.
     *
     * @pre getItems().contains(item) == true
     * @post getItems().contains(item) == false
     */
    public void removeItem(Item item) {
        this.items.remove(item);
    }

    /**
     * Modifies attributes of an item associated with this transaction.
     *
     * @pre getItems().contains(item) == true
     */
    public void modItem(Item item, BigDecimal adjustment, BigDecimal quantity, String note) {
        TransactionLineItem itemToMod = this.items.get(this.items.indexOf(item));
        itemToMod.setAdjustment(adjustment);
        itemToMod.setQuantity(quantity);
        itemToMod.setNote(note);
    }

    /**
     * Gets information associated with all involved items in this transaction.
     */
    public ArrayList<TransactionLineItem> getItems() throws SQLException {
        if (!this.itemsInitialized) {
            try {
                this.getPkey();
            } catch (UninitializedFieldException e) {
                return this.items;
            }
            try {
                PreparedStatement stmt = Session.getDB().prepareStatement("SELECT * FROM transaction_item WHERE transaction_=?");
                stmt.setInt(1, this.getPkey());
                for (HashMap<String, Field> row : DB.mapResultSet(stmt.executeQuery())) {
                    this.items.add(new TransactionLineItem(row));
                }
            } catch (SQLException e) {
                ErrorLogger.error(e, "Failed to retrieve transaction's items.", true, true);
                throw e;
            }

        }
        return this.items;
    }

    else

    /**
     * Gets the billing information associated with this transaction.
     */
    public Billing getBilling() throws SQLException {
        if (this.billing != null) {
            return this.billing;
        }
        try {
            this.getPkey();
        } catch (UninitializedFieldException e) {
            return null;  //the transaction has never been saved, so no billing has been set.
        }
        try {
            this.billing = new Billing((Integer) this.getFieldValue("billing"), false);
        } catch (SQLException e) {
            ErrorLogger.error(e, "Failed to retrieve address.", true, true);
            throw e;
        }
        return this.billing;
    }

    /**
     * Sets the billing information for this transaction.
     */
    public void setBilling(Billing billing) throws SQLException {
        this.setFieldValue("billing", billing.getPkey());
        this.billing = billing;
    }

    /**
     * Gets the address associated with this transaction.
     */
    public Address getAddress() throws SQLException {
        if (this.address != null) {
            return this.address;
        }
        try {
            this.getPkey();
        } catch (UninitializedFieldException e) {
            return null;  //the transaction has never been saved, so no address has been set.
        }
        try {
            this.address = new Address((Integer) this.getFieldValue("address"), false);
        } catch (SQLException e) {
            ErrorLogger.error(e, "Failed to retrieve address.", true, true);
            throw e;
        }
        return this.billing;
    }

    /**
     * Sets the address for this transaction.
     */
    public void setAddress(Address address) {
        this.setFieldValue("address", address.getPkey());
        this.address = address;
    }

    /**
     * Gets status associated with this transaction.
     */
    public TransactionStatus getStatus() {
        return null;
    }

    /**
     * Checks whether any returns have been made that reference this
     * transaction.
     *
     * @pre getStatus() == status.Finalized
     */
    public boolean isModified() {
        return false;
    }

    /**
     * Gets user (employee) associated with this transaction.
     */
    public User getUser() {
        return null;
    }

    /**
     * Gets customer associated with this transaction.
     */
    public Customer getCustomer() throws SQLException {
        return new Customer((Integer) this.getFieldValue("customer"), false);
    }

    /**
     * Completes this transaction. No finalized transaction can be modified.
     *
     * @pre getStatus() != status.Finalized
     * @pre getAddress() != null
     * @pre getBilling() != null
     * @pre getItems().size() > 0
     * @post getStatus() == status.Finalized
     */
    public void finalizeTransaction() {
    }

    /**
     * TransactionStatus is a enumeration describing the status of a
     * transaction.
     */
    public static enum TransactionStatus {

        ACTIVE, CLOSED, BILLED, UNDER_REVIEW, ABORTED
    }
}
