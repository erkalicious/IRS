package ISIS.database;

import ISIS.customer.Customer;
import ISIS.gui.ErrorLogger;
import ISIS.misc.Phone;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages third party relational database software used to manage and organize data used to implement functionalities
 * in IRS. Makes available functionality to execute statements, queries, and updates.
 */
public final class DB {
    /* Fields omitted */

    private static int timeout = 100;
    private Connection connection;

    /**
     * Public constructor. Opens the database from the specified location. The specified file must be a valid database.
     *
     * @pre new File(DBLocation).exists() == true
     * @post isOpen == true
     */
    public DB(String DBLocation) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            ErrorLogger.error("Driver not found.", true, true);
            System.exit(1);
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:test.db");
            initializeDB();
        } catch (SQLException ex) {
            ErrorLogger.error(ex, "Failed to open or initialize database.", true, true);
            System.exit(1);
        }
    }

    public static ArrayList<HashMap<String, Field>> mapResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        ArrayList<HashMap<String, Field>> rows = new ArrayList<>();
        while (rs.next()) {
            HashMap<String, Field> row = new HashMap<>(md.getColumnCount());
            for (int i = 1; i <= md.getColumnCount(); ++i) {
                Field field = new Field(true);
                field.initField(rs.getObject(i));
                row.put(md.getColumnName(i), field);
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Initialize tables
     */
    public void initializeDB() throws SQLException {
        String datesSql = "createDate BIGINT NOT NULL, createUser INT REFERENCES user(pkey), modDate BIGINT NOT NULL, " + "modUser INT REFERENCES user(pkey)";

        // user
        // 40 characters for hash, 4 for salt
        executeUpdate("CREATE TABLE IF NOT EXISTS user (pkey INTEGER PRIMARY KEY, active BOOLEAN NOT NULL, " + "username VARCHAR(255) UNIQUE NOT NULL, password VARCHAR(44) NOT NULL, fname VARCHAR(255) NOT NULL, " + "lname VARCHAR(255) NOT NULL, note TEXT NOT NULL, " + datesSql + ")");
        // add base user (pkey = 1)
        executeUpdate("INSERT OR IGNORE INTO user (pkey, active, username, password, fname, lname, note, createDate, modDate) " + "VALUES (1, 0, 'base', '0', 'fname', 'lname', 'note', 0, 0)");

        // settings
        // null user for global setting
        executeUpdate("CREATE TABLE IF NOT EXISTS setting (pkey INTEGER PRIMARY KEY, key VARCHAR(255) NOT NULL, value TEXT NOT NULL, " + "user INT REFERENCES user(pkey))");

        //phone
        executeUpdate("CREATE TABLE IF NOT EXISTS phone (pkey INTEGER PRIMARY KEY, primary_num BOOLEAN NOT NULL, " +
                              "type VARCHAR(255) NOT NULL, " + "number VARCHAR(255) NOT NULL, " + datesSql + ")");

        // address
        executeUpdate("CREATE TABLE IF NOT EXISTS address (pkey INTEGER PRIMARY KEY, active BOOLEAN NOT NULL, title VARCHAR(255) NOT NULL, " + "city VARCHAR(255) NOT NULL, state VARCHAR(255) NOT NULL, zip VARCHAR(10) NOT NULL, county VARCHAR(255) NOT NULL, " +
                              "country VARCHAR(3) NOT NULL, st_address TEXT NOT NULL, type VARCHAR(255) NOT NULL, " + datesSql + ")");

        // billing
        executeUpdate("CREATE TABLE IF NOT EXISTS billing (pkey INTEGER PRIMARY KEY, active BOOLEAN NOT NULL, " + "number VARCHAR(255), expiration VARCHAR(5), CCV VARCHAR(5) NOT NULL, " + "address INT REFERENCES address(pkey), " + datesSql + ")");

        // customer
        executeUpdate("CREATE TABLE IF NOT EXISTS customer (pkey INTEGER PRIMARY KEY, active BOOLEAN NOT NULL, " + "password VARCHAR(255) NOT NULL, fname VARCHAR(255) NOT NULL, lname VARCHAR(255) NOT NULL, " + "email TEXT NOT NULL, note TEXT NOT NULL, " + datesSql + ")");
        // customer-phone
        executeUpdate("CREATE TABLE IF NOT EXISTS customer_phone (pkey INTEGER PRIMARY KEY, customer INT REFERENCES customer" +
                              "(pkey) NOT NULL, " + "phone INT REFERENCES phone(pkey) NOT NULL)");
        // customer-address
        executeUpdate("CREATE TABLE IF NOT EXISTS customer_address (pkey INTEGER PRIMARY KEY, customer INT REFERENCES customer(pkey) NOT NULL, " + "address INT REFERENCES address(pkey) NOT NULL)");
        // customer-billing
        executeUpdate("CREATE TABLE IF NOT EXISTS customer_billing (pkey INTEGER PRIMARY KEY, customer INT REFERENCES customer(pkey) NOT NULL, " + "billing INT REFERENCES billing(pkey) NOT NULL)");

        // customer-search
        String customer_search_columns = "pkey, fname, lname, email, note";
        String phoneNoSql = "SELECT group_concat(number, ' ') FROM customer_phone AS cp LEFT JOIN phone AS p ON cp.phone=p.pkey WHERE cp.customer=";
        String addressSqlColumns = "title || ' ' || city || ' ' || state || ' ' || zip || ' ' || county || ' ' || st_address";
        String addressSql = "SELECT group_concat(" + addressSqlColumns + ", ' ') FROM customer_address AS ca LEFT JOIN address AS a ON " +
                "ca.address=a.pkey WHERE ca.customer=";
        String customer_search_insert = "INSERT INTO customer_search SELECT csv.* FROM customer_search_view AS csv WHERE csv.pkey=";
        // view representing data inside customer_search
        executeUpdate("CREATE VIEW IF NOT EXISTS customer_search_view AS SELECT pkey AS docid, " + customer_search_columns + ", " +
                              "(" + phoneNoSql + "customer.pkey), ("+addressSql+"customer.pkey) FROM customer");
        // virtual table for searching customers
        executeUpdate("CREATE VIRTUAL TABLE IF NOT EXISTS customer_search USING fts3(content=\"customer_search_view\", " +
                              "" + customer_search_columns + ", phone, address)");

        // triggers to populate virtual table
        executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_insert AFTER INSERT ON customer BEGIN\n" + customer_search_insert +
                              "new.rowid; END;");
        executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_update BEFORE UPDATE ON customer BEGIN\n" + "  DELETE FROM customer_search WHERE docid=old.pkey;\n" + "END;\n");
        executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_update_after AFTER UPDATE ON customer BEGIN\n" +
                              customer_search_insert + "new.rowid; END;\n");
        executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_delete BEFORE DELETE ON customer BEGIN\n" + "  DELETE FROM customer_search WHERE docid=old.pkey;\n" + "END;\n");

        // update virtual table when grouped columns are updated
        for (String junction : new String[]{"customer_phone", "customer_address"}) {
            executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_" + junction + "_insert AFTER INSERT ON " + junction + " BEGIN\n" +
                                  "DELETE FROM customer_search WHERE docid=new.customer;\n" + customer_search_insert + "new.customer; END;");
            executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_" + junction + "_update BEFORE UPDATE ON " + junction + " BEGIN\n" +
                                  "DELETE FROM " + "customer_search WHERE docid=old.customer;\n" + "END;\n");
            executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_" + junction + "_update_after AFTER UPDATE ON " + junction + " BEGIN\n" +
                                  customer_search_insert + "new.customer; END;\n");
            executeUpdate("CREATE TRIGGER IF NOT EXISTS customer_search_" + junction + "_delete BEFORE DELETE ON " + junction + " BEGIN\n" + " DELETE" +
                                  " FROM customer_search WHERE docid=old.customer;\n" + "END;\n");
        }

        // item
        executeUpdate("CREATE TABLE IF NOT EXISTS item (pkey INTEGER PRIMARY KEY, active BOOLEAN NOT NULL, " + "name VARCHAR(255) NOT NULL, SKU VARCHAR(255) NOT NULL, price VARCHAR(30) NOT NULL, onhand_qty VARCHAR(30) NOT NULL, " +
                              "cost VARCHAR(30) NOT NULL, description TEXT NOT NULL, uom VARCHAR(10), reorder_qty VARCHAR(30) NOT NULL, lastest BOOLEAN NOT NULL, " + datesSql + ")");

        // transaction
        executeUpdate("CREATE TABLE IF NOT EXISTS transaction_ (pkey INTEGER PRIMARY KEY, status VARCHAR(20) NOT NULL, " + "type VARCHAR(20) NOT NULL, modified BOOLEAN NOT NULL, parent_transaction INT REFERENCES transaction_(pkey), " + datesSql + ")");
        // transaction-item
        executeUpdate("CREATE TABLE IF NOT EXISTS transaction_item (pkey INTEGER PRIMARY KEY, transaction_ INT REFERENCES transaction_(pkey) NOT NULL, " + "item INT REFERENCES item(pkey) NOT NULL, price VARCHAR(30) NOT NULL, adjustment VARCHAR(30) NOT NULL, description TEXT, " + datesSql + ")");
        // transaction-address
        executeUpdate("CREATE TABLE IF NOT EXISTS transaction_address (pkey INTEGER PRIMARY KEY, transaction_ INT REFERENCES transaction_(pkey) NOT NULL, " + "address INT REFERENCES address(pkey) NOT NULL)");
        // transaction-billing
        executeUpdate("CREATE TABLE IF NOT EXISTS transaction_billing (pkey INTEGER PRIMARY KEY, transaction_ INT REFERENCES transaction_(pkey) NOT NULL, " + "billing INT REFERENCES billing(pkey) NOT NULL)");

        // TODO: add indices
        // TODO: keywords

    }

    public void sampleData() throws SQLException {
        Customer customer = new Customer("Joe", "Doe", "sammich@penis.info", "This is a note.", "this is a password?", true);
        customer.addPhoneNum(new Phone("404040404", true, Phone.PhoneType.HOME));
        customer.addPhoneNum(new Phone("987654321", true, Phone.PhoneType.HOME));
        customer.addPhoneNum(new Phone("123456789", true, Phone.PhoneType.HOME));
        customer.save();
        customer = new Customer("Sammich", "Bob", "whuh@what.com", "This is a note.", "this is a password?", true);
        customer.addPhoneNum(new Phone("301231213", true, Phone.PhoneType.HOME));
        customer.save();
        customer = new Customer("Jizzle", "Dizzle", "cookies@gmail.com", "This is a note.", "this is a password?", true);
        customer.addPhoneNum(new Phone("56565656", true, Phone.PhoneType.HOME));
        customer.save();
    }

    /**
     * For creating tables and stuff. Returns number of affected rows.
     */
    private int executeUpdate(String sql) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeUpdate(sql);
    }

    /**
     * Checks if the database is open.
     */
    public boolean isOpen() {
        try {
            return connection.isValid(timeout);
        } catch (SQLException ex) {
            ErrorLogger.error(ex.getLocalizedMessage(), false, false);
            return false;
        }
    }

    /**
     * Closes the database.
     *
     * @pre isOpen == true
     * @post isOpen == false
     */
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            ErrorLogger.error(ex.getLocalizedMessage(), false, false);
        }
    }

    /**
     * Creates and returns a prepared statement given the given sql.
     *
     * @pre isOpen == true
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
    }

    /**
     * Starts a transaction in the database.
     *
     * @pre isOpen == true
     * @pre transactionActive() == false
     * @post transactionActive() == true
     */
    public void startTransaction() {
    }

    /**
     * Closes a transaction in the database.
     *
     * @pre isOpen == true
     * @pre transactionActive() == true
     * @post transactionActive() == false
     */
    public void closeTransaction() {
    }

    /**
     * Checks if a transaction is active.
     *
     * @pre isOpen == true
     */
    public boolean transactionActive() {
        return false;
    }
}
