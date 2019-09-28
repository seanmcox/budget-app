/**
 * 
 */
package com.shtick.apps.budget.structure;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.security.sasl.AuthenticationException;

import com.shtick.apps.budget.Driver;
import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;
import com.shtick.apps.budget.structure.model.CurrencyID;
import com.shtick.apps.budget.structure.model.Event;
import com.shtick.apps.budget.structure.model.EventID;
import com.shtick.apps.budget.structure.model.LedgerItem;
import com.shtick.apps.budget.structure.model.Permission;
import com.shtick.apps.budget.structure.model.Transaction;
import com.shtick.apps.budget.structure.model.TransactionID;
import com.shtick.apps.budget.structure.model.User;
import com.shtick.apps.budget.structure.model.UserID;

/**
 * @author scox
 *
 */
public class MainDriver extends Driver{
	private static File DEFAULT_WORKING_DIRECTORY;
	private static final String OS = (System.getProperty("os.name")).toUpperCase();
	private static String DB_URL;
	private static final Object DB_LOCK = new Object();
	private User user;
	private Map<CategoryID,Permission.Permissions> userPermissions;
	
	static{
		if (OS.contains("WIN")){
		    String workingDirectory = System.getenv("AppData");
		    DEFAULT_WORKING_DIRECTORY = new File(workingDirectory);
		}
		else{ // Try Linux or related.
		    String workingDirectory = System.getProperty("user.home");
		    DEFAULT_WORKING_DIRECTORY = new File(workingDirectory+"/Library/Application Support");
		    if(!DEFAULT_WORKING_DIRECTORY.exists())
		    	DEFAULT_WORKING_DIRECTORY = new File(workingDirectory+"/.local/share");
		    if(!DEFAULT_WORKING_DIRECTORY.exists())
		    	DEFAULT_WORKING_DIRECTORY = null;
		}
		if((DEFAULT_WORKING_DIRECTORY==null)||(!DEFAULT_WORKING_DIRECTORY.canWrite())){
			// TODO Find the current application folder.
		}
		
	}
	
	/**
	 * 
	 */
	public MainDriver() {
		this(DEFAULT_WORKING_DIRECTORY);
	}
	
	/**
	 * @param workingDirectory 
	 * 
	 */
	public MainDriver(File workingDirectory) {
		DB_URL = "jdbc:sqlite:"+workingDirectory.toString()+"/budget.app.db";
		try{
			
			Class.forName("org.sqlite.JDBC");
		}
		catch(ClassNotFoundException t){
			throw new RuntimeException(t);
		}
		
		synchronized(DB_LOCK){
			System.out.println("DB_URL:"+DB_URL);
			try (Connection connection = DriverManager.getConnection(DB_URL)) {
				// Build the database if necessary.
				if(connection==null)
					throw new RuntimeException("Connection to database cannot be established.");
				Statement statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS users" +
		                   "(name           TEXT    NOT NULL," +
		                   " is_admin       INT     NOT NULL," +
		                   " hash           TEXT    NOT NULL," +
		                   " time_added     TEXT    NOT NULL" +
		                   ")");
				statement.executeUpdate("CREATE INDEX idx_user_name ON users (name,hash);");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS categories" +
		                   "(parent_id     INT," +
		                   " currency_id   INT  NOT NULL," +
		                   " name          TEXT NOT NULL," +
		                   " total         INT  NOT NULL," +
		                   " permissions   TEXT NOT NULL," +
		                   " time_added    TEXT NOT NULL," +
		                   " time_deleted  TEXT NOT NULL" +
		                   ")");
				statement.executeUpdate("CREATE INDEX idx_category_parent ON categories (parent_id);");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS category_permissions" +
		                   "(category_id   INT  NOT NULL," +
		                   " user_id       INT  NOT NULL," +
		                   " permissions   TEXT NOT NULL," +
		                   " time_added    TEXT NOT NULL" +
		                   ")");
				statement.executeUpdate("CREATE INDEX idx_permission_category ON category_permissions (category_id);");
				statement.executeUpdate("CREATE INDEX idx_permission_user ON category_permissions (user_id);");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS transactions " +
		                   "(src_category_id  INT," +
		                   " dest_category_id INT," +
		                   " src_currency     INT," +
		                   " dest_currency    INT," +
		                   " time_deleted     TEXT," +
		                   " date             TEXT    NOT NULL," +
		                   " time_added       TEXT    NOT NULL" +
		                   ")");
				statement.executeUpdate("CREATE INDEX idx_transaction_src ON transactions (src_category_id, date);");
				statement.executeUpdate("CREATE INDEX idx_transaction_dest ON transactions (dest_category_id, date);");
				statement.executeUpdate("CREATE INDEX idx_date ON transactions (date);");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS events " +
		                   "(user_id        INT  NOT NULL," +
		                   " transaction_id INT  NOT NULL," +
		                   " note           TEXT NOT NULL," +
		                   " type           TEXT NOT NULL," +
		                   " date           TEXT NOT NULL," +
		                   " time_added     TEXT NOT NULL" +
		                   ")");
				statement.executeUpdate("CREATE INDEX idx_event_transaction ON events (transaction_id);");
				statement.executeUpdate("CREATE INDEX idx_event_user ON events (user_id);");
				statement.executeUpdate("CREATE INDEX idx_event_date ON events (date);");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS ledger " +
		                   "(transaction_id INT  NOT NULL," +
		                   " event_id       INT  NOT NULL," +
		                   " category_id    INT  NOT NULL," +
		                   " change         INT  NOT NULL," +
		                   " total          INT  NOT NULL," +
		                   " date           TEXT NOT NULL," +
		                   " time_added     TEXT NOT NULL" +
		                   ")");
				statement.executeUpdate("CREATE INDEX idx_ledger_transaction ON ledger (transaction_id);");
				statement.executeUpdate("CREATE INDEX idx_ledger_category ON ledger (category_id, date);");
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS currencies " +
		                   "(name          TEXT    NOT NULL," +
		                   " type          TEXT    NOT NULL," +
		                   " config        TEXT    NOT NULL," +
		                   " time_added    TEXT    NOT NULL" +
		                   ")");
				statement.close();
				connection.close();
			}
			catch(SQLException t){
				throw new RuntimeException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#addCategory(com.shtick.apps.budget.structure.model.Category)
	 */
	@Override
	public CategoryID addCategory(Category category) throws IOException {
		if(!isAdmin(category.getParentID()))
			throw new AuthenticationException("The current user is not an authorized administrator.");
		String sql = "INSERT INTO categories " +
                "(parent_id, currency_id, name, total, permissions, time_added) " +
				"VALUES (?,?,?,0,?,?)";
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				statement.setInt(1, (category.getParentID()!=null)?Integer.parseInt(category.getParentID().toString()):null);
				statement.setInt(2, Integer.parseInt(category.getCurrency().toString()));
				statement.setString(3, category.getName());
				statement.setString(4, category.getPermissions().toString());
				statement.setString(5, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
				statement.executeUpdate();
				ResultSet resultSet = statement.getGeneratedKeys();
				if(!resultSet.next()) {
					resultSet.close();
					return null;
				}
				resultSet.close();
				return new CategoryID(""+resultSet.getInt(1));
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getCategories(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public List<Category> getCategories(CategoryID parentCategory) throws IOException {
		if(!canRead(parentCategory))
			throw new AuthenticationException("The current user is not authorized to read.");
		
		String sql = "SELECT rowid, parent_id, name, currency_id, total, permissions, time_added, time_deleted " +
				"FROM categories WHERE";
		if(parentCategory==null)
			sql += " parent_id IS NULL";
		else
			sql += " parent_id = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				if(parentCategory!=null)
					statement.setInt(1, Integer.parseInt(parentCategory.toString()));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Category> retval = new LinkedList<>();
				while(resultSet.next())
					retval.add(getCategoryFromResultSetRow(resultSet));
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#removeCategory(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public boolean removeCategory(CategoryID categoryID) throws IOException {
		// Auth
		if(!isAdmin(categoryID))
			throw new AuthenticationException("The current user is not an authorized admin.");

		// Check to see if there are non-deleted subcategories that would be affected.
		List<Category> subcategories = getCategories(categoryID);
		for(Category category:subcategories)
			if(category.getTimeDeleted()!=null)
				throw new IllegalArgumentException("The category given contains undeleted subcategories;");
		
		String sql = "UPDATE categories " +
                "SET time_deleted = ? " +
				"WHERE rowid = ? AND time_deleted IS NULL";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				statement.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
				statement.setInt(2, Integer.parseInt(categoryID.toString()));
				rowsUpdated=statement.executeUpdate();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#restoreCategory(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public boolean restoreCategory(CategoryID categoryID) throws IOException {
		if(!isAdmin(categoryID))
			throw new AuthenticationException("The current user is not an authorized admin.");
		String sql = "UPDATE categories " +
                "SET time_deleted = null " +
				"WHERE rowid = ? AND time_deleted IS NOT NULL";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				statement.setInt(1, Integer.parseInt(categoryID.toString()));
				rowsUpdated=statement.executeUpdate();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getCategory(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public Category getCategory(CategoryID id) throws IOException {
		if(!canRead(id))
			throw new AuthenticationException("The current user is not authorized to read.");
		String sql = "SELECT rowid, parent_id, name, currency_id, total, permissions, time_added, time_deleted " +
				"FROM categories WHERE rowid = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(id.toString()));
				ResultSet resultSet = statement.executeQuery();
				Category category = null;
				if(resultSet.next())
					category = getCategoryFromResultSetRow(resultSet);
				resultSet.close();
				return category;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#renameCategory(com.shtick.apps.budget.structure.model.CategoryID, java.lang.String)
	 */
	@Override
	public boolean renameCategory(CategoryID categoryID, String name) throws IOException {
		if(!isAdmin(categoryID))
			throw new AuthenticationException("The current user is not an authorized admin.");
		String sql = "UPDATE categories " +
                "SET name = ? " +
				"WHERE rowid = ?";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				statement.setString(1, name);
				statement.setInt(2, Integer.parseInt(categoryID.toString()));
				rowsUpdated=statement.executeUpdate();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#addTransactions(com.shtick.apps.budget.structure.model.Transaction)
	 */
	@Override
	public TransactionID addTransactions(Transaction transaction, String note) throws IOException {
		if((transaction.getDestinationCategoryID()!=null)&&(!canWrite(transaction.getDestinationCategoryID())))
			throw new AuthenticationException("The current user is not authorized to write to destination.");
		if((transaction.getSourceCategoryID()!=null)&&(!canWrite(transaction.getSourceCategoryID())))
			throw new AuthenticationException("The current user is not authorized to write to source.");

		if((transaction.getSourceCategoryID()==null)&&(transaction.getDestinationCategoryID()==null))
			throw new IllegalArgumentException("The transaction must have either a source category ID or a destination category ID.");
		String sql;
		int transactionID;
		synchronized(DB_LOCK){
			PreparedStatement statement;
			try (Connection connection = DriverManager.getConnection(DB_URL);) {
				Event.Type eventType;
				LocalDateTime now = LocalDateTime.now();
				String nowDateString = LocalDate.from(now).format(DateTimeFormatter.ISO_DATE_TIME);
				String nowString = now.format(DateTimeFormatter.ISO_DATE_TIME);
				if(transaction.getSourceCategoryID()==null) {
					// Process income
					sql = "INSERT INTO transactions " +
			                "(dest_category_id, dest_currency, date, time_added) " +
							"VALUES (?,?,?,?)";
					statement = connection.prepareStatement(sql);
					statement.setInt(1, Integer.parseInt(transaction.getDestinationCategoryID().toString()));
					statement.setInt(2, (int)transaction.getDestinationCurrency());
					statement.setString(3, nowDateString);
					statement.setString(4, nowString);
					statement.executeUpdate();
					eventType = Event.Type.INCOME;
				}
				else if(transaction.getDestinationCategoryID()==null) {
					// Process expense
					sql = "INSERT INTO transactions " +
			                "(src_category_id, src_currency, date, time_added) " +
							"VALUES (?,?,?,?)";
					statement = connection.prepareStatement(sql);
					statement.setInt(1, Integer.parseInt(transaction.getSourceCategoryID().toString()));
					statement.setInt(2, (int)transaction.getSourceCurrency());
					statement.setString(3, nowDateString);
					statement.setString(4, nowString);
					statement.executeUpdate();
					eventType = Event.Type.EXPENSE;
				}
				else {
					// Determine transfer or exchange
					Category source = getCategory(transaction.getSourceCategoryID());
					Category destination = getCategory(transaction.getDestinationCategoryID());
					sql = "INSERT INTO transactions " +
			                "(src_category_id, src_currency, dest_category_id, dest_currency, date, time_added) " +
							"VALUES (?,?,?,?,?,?)";
					statement = connection.prepareStatement(sql);
					statement.setInt(1, Integer.parseInt(transaction.getSourceCategoryID().toString()));
					statement.setInt(2, (int)transaction.getSourceCurrency());
					statement.setInt(3, Integer.parseInt(transaction.getDestinationCategoryID().toString()));
					statement.setInt(4, (int)transaction.getDestinationCurrency());
					statement.setString(5, nowDateString);
					statement.setString(6, nowString);
					statement.executeUpdate();
					if(source.getCurrency().equals(destination.getCurrency()))
						eventType = Event.Type.TRANSFER;
					else
						eventType = Event.Type.EXCHANGE;
				}
				ResultSet resultSet = statement.getGeneratedKeys();
				if(!resultSet.next()) {
					resultSet.close();
					return null;
				}
				transactionID = resultSet.getInt(1);
				resultSet.close();
				statement.close();

				// TODO Add ledger items
				// Add event
				sql = "INSERT INTO events " +
		                "(user_id, transaction_id, note, type, date, time_added) " +
						"VALUES (?,?,?,?,?,?)";
				statement = connection.prepareStatement(sql);
				statement.setInt(1, Integer.parseInt(user.getUserID().toString()));
				statement.setInt(2, transactionID);
				statement.setString(3, note);
				statement.setString(4, eventType.toString());
				statement.setString(5, nowDateString);
				statement.setString(6, nowString);
				statement.executeUpdate();
				resultSet = statement.getGeneratedKeys();
				if(!resultSet.next()) {
					resultSet.close();
					return null;
				}
				int eventID = resultSet.getInt(1);
				resultSet.close();
				statement.close();

				// Add ledger items and update category totals
				if(transaction.getDestinationCategoryID()!=null) {
					adjustCategoryTotal(transaction.getDestinationCategoryID(),transaction.getDestinationCurrency());

					Category category = getCategory(transaction.getDestinationCategoryID());
					sql = "INSERT INTO ledger " +
			                "(category_id, user_id, transaction_id, change, total, date, time_added) " +
							"VALUES (?,?,?,?,?,?,?)";
					statement = connection.prepareStatement(sql);
					statement.setInt(1, transactionID);
					statement.setInt(2, eventID);
					statement.setInt(3, Integer.parseInt(transaction.getDestinationCategoryID().toString()));
					statement.setLong(4, transaction.getDestinationCurrency());
					statement.setLong(5, category.getTotal());
					statement.setString(6, nowDateString);
					statement.setString(7, nowString);
					statement.executeUpdate();
					statement.close();
				}
				if(transaction.getSourceCategoryID()!=null) {
					adjustCategoryTotal(transaction.getSourceCategoryID(),-transaction.getSourceCurrency());

					Category category = getCategory(transaction.getSourceCategoryID());
					sql = "INSERT INTO ledger " +
			                "(category_id, user_id, transaction_id, change, total, date, time_added) " +
							"VALUES (?,?,?,?,?,?,?)";
					statement = connection.prepareStatement(sql);
					statement.setInt(1, transactionID);
					statement.setInt(2, eventID);
					statement.setInt(3, Integer.parseInt(transaction.getSourceCategoryID().toString()));
					statement.setLong(4, -transaction.getSourceCurrency());
					statement.setLong(5, category.getTotal());
					statement.setString(6, nowDateString);
					statement.setString(7, nowString);
					statement.executeUpdate();
					statement.close();
				}
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		
		return new TransactionID(""+transactionID);
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getTransactions(com.shtick.apps.budget.structure.model.CategoryID, java.time.LocalDate, boolean)
	 */
	@Override
	public List<Transaction> getTransactions(CategoryID categoryID, LocalDate date, boolean includeDeleted)
			throws IOException {
		if(!canRead(categoryID))
			throw new AuthenticationException("The current user is not authorized to read.");
		String sql = "SELECT rowid, src_category_id, dest_category_id, src_currency, dest_currency, date, time_deleted, time_added " +
				"FROM transactions WHERE (src_category_id = ? OR dest_category_id = ?)";
		if(date !=null)
			sql += " AND date = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(categoryID.toString()));
				statement.setInt(2, Integer.parseInt(categoryID.toString()));
				if(date !=null)
					statement.setString(3, date.format(DateTimeFormatter.ISO_DATE_TIME));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Transaction> retval = new LinkedList<>();
				while(resultSet.next()) {
					Transaction transaction = getTransactionFromResultSetRow(resultSet);
					if((!includeDeleted)&&(transaction.isDeleted()))
						continue;
					retval.add(transaction);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#deleteTransaction(com.shtick.apps.budget.structure.model.TransactionID, java.lang.String)
	 */
	@Override
	public boolean deleteTransaction(TransactionID transactionID, String note) throws IOException {
		if(!isAdmin(null)) {
			Transaction transaction = getTransaction(transactionID);
			if(!(canWrite(transaction.getSourceCategoryID())&&canWrite(transaction.getDestinationCategoryID())))
				throw new AuthenticationException("The current user is not authorized to write.");
		}
		String sql = "UPDATE transactions " +
                "SET time_deleted = ? " +
				"WHERE rowid = ? AND time_deleted IS NULL";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);) {
				PreparedStatement statement = connection.prepareStatement(sql);
				statement.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
				statement.setInt(2, Integer.parseInt(transactionID.toString()));
				rowsUpdated=statement.executeUpdate();
				statement.close();

				if(rowsUpdated>0) {
					// Update category totals
					Transaction transaction = getTransaction(transactionID);
					if(transaction.getDestinationCategoryID()!=null)
						adjustCategoryTotal(transaction.getDestinationCategoryID(),-transaction.getDestinationCurrency());
					if(transaction.getSourceCategoryID()!=null)
						adjustCategoryTotal(transaction.getSourceCategoryID(),transaction.getSourceCurrency());

					// TODO Add ledger items
					// Add event
					sql = "INSERT INTO events " +
			                "(user_id, transaction_id, note, type, date, time_added) " +
							"VALUES (?,?,?,?,?)";
					statement = connection.prepareStatement(sql);
					LocalDateTime now = LocalDateTime.now();
					statement.setInt(1, Integer.parseInt(user.getUserID().toString()));
					statement.setInt(2, Integer.parseInt(transactionID.toString()));
					statement.setString(3, note);
					statement.setString(4, Event.Type.DELETE.toString());
					statement.setString(5, LocalDate.from(now).format(DateTimeFormatter.ISO_DATE_TIME));
					statement.setString(6, now.format(DateTimeFormatter.ISO_DATE_TIME));
					statement.executeUpdate();
					statement.close();
				}
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#undeleteTransaction(com.shtick.apps.budget.structure.model.TransactionID, java.lang.String)
	 */
	@Override
	public boolean undeleteTransaction(TransactionID transactionID, String note) throws IOException {
		if(!isAdmin(null)) {
			Transaction transaction = getTransaction(transactionID);
			if(!(canWrite(transaction.getSourceCategoryID())&&canWrite(transaction.getDestinationCategoryID())))
				throw new AuthenticationException("The current user is not authorized to write.");
		}
		String sql = "UPDATE transactions " +
                "SET time_deleted = null " +
				"WHERE rowid = ? AND time_deleted IS NOT NULL";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);) {
				PreparedStatement statement = connection.prepareStatement(sql);
				statement.setInt(1, Integer.parseInt(transactionID.toString()));
				rowsUpdated=statement.executeUpdate();
				statement.close();

				if(rowsUpdated>0) {
					// Update category totals
					Transaction transaction = getTransaction(transactionID);
					if(transaction.getDestinationCategoryID()!=null)
						adjustCategoryTotal(transaction.getDestinationCategoryID(),transaction.getDestinationCurrency());
					if(transaction.getSourceCategoryID()!=null)
						adjustCategoryTotal(transaction.getSourceCategoryID(),-transaction.getSourceCurrency());

					// TODO Add ledger items
					// Add event
					sql = "INSERT INTO events " +
			                "(user_id, transaction_id, note, type, date, time_added) " +
							"VALUES (?,?,?,?,?)";
					statement = connection.prepareStatement(sql);
					LocalDateTime now = LocalDateTime.now();
					statement.setInt(1, Integer.parseInt(user.getUserID().toString()));
					statement.setInt(2, Integer.parseInt(transactionID.toString()));
					statement.setString(3, note);
					statement.setString(4, Event.Type.UNDELETE.toString());
					statement.setString(5, LocalDate.from(now).format(DateTimeFormatter.ISO_DATE_TIME));
					statement.setString(6, now.format(DateTimeFormatter.ISO_DATE_TIME));
					statement.executeUpdate();
					statement.close();
				}
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}
	
	private boolean adjustCategoryTotal(CategoryID categoryID, long adjustment) throws IOException{
		String sql = "UPDATE categories " +
                "SET total = total + ? " +
				"WHERE rowid = ?";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				statement.setLong(1, adjustment);
				statement.setInt(2, Integer.parseInt(categoryID.toString()));
				rowsUpdated=statement.executeUpdate();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}
	
	private Transaction getTransaction(TransactionID transactionID) throws IOException{
		String sql = "SELECT rowid, src_category_id, dest_category_id, src_currency, dest_currency, date, time_deleted, time_added " +
				"FROM transactions WHERE rowid = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(transactionID.toString()));
				ResultSet resultSet = statement.executeQuery();
				if(resultSet.next()) {
					Transaction transaction = getTransactionFromResultSetRow(resultSet);
					resultSet.close();
					return transaction;
				}
				resultSet.close();
				return null;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getEventsByTransaction(com.shtick.apps.budget.structure.model.TransactionID)
	 */
	@Override
	public List<Event> getEventsByTransaction(TransactionID transactionID) throws IOException {
		String sql = "SELECT rowid, user_id, transaction_id, note, type, time_added " +
				"FROM events WHERE transaction_id = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(transactionID.toString()));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Event> retval = new LinkedList<>();
				while(resultSet.next()) {
					Event event = getEventFromResultSetRow(resultSet);
					retval.add(event);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getEvents(int)
	 */
	@Override
	public List<Event> getEvents(int eventCount) throws IOException {
		String sql = "SELECT rowid, user_id, transaction_id, note, type, time_added " +
				"FROM events ORDER BY rowid DESC LIMIT ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, eventCount);
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Event> retval = new LinkedList<>();
				while(resultSet.next()) {
					Event event = getEventFromResultSetRow(resultSet);
					retval.add(event);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getEvents(int, EventID)
	 */
	@Override
	public List<Event> getEvents(int eventCount, EventID eventID) throws IOException {
		String sql = "SELECT rowid, user_id, transaction_id, note, type, time_added " +
				"FROM events WHERE rowid <= ? ORDER BY rowid DESC LIMIT ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(eventID.toString()));
				statement.setInt(2, eventCount);
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Event> retval = new LinkedList<>();
				while(resultSet.next()) {
					Event event = getEventFromResultSetRow(resultSet);
					retval.add(event);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getEvents(java.time.LocalDate)
	 */
	@Override
	public List<Event> getEvents(LocalDate when) throws IOException {
		String sql = "SELECT rowid, user_id, transaction_id, note, type, time_added " +
				"FROM events WHERE date = ? ORDER BY rowid DESC";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setString(1, when.format(DateTimeFormatter.ISO_DATE_TIME));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Event> retval = new LinkedList<>();
				while(resultSet.next()) {
					Event event = getEventFromResultSetRow(resultSet);
					retval.add(event);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getLedgerItems(com.shtick.apps.budget.structure.model.CategoryID, java.time.LocalDate)
	 */
	@Override
	public List<LedgerItem> getLedgerItems(CategoryID categoryID, LocalDate when) throws IOException {
		if(!canRead(categoryID))
			throw new AuthenticationException("The current user is not authorized to read.");
		String sql = "SELECT transaction_id, event_id, category_id, change, total, date, time_added " +
				"FROM ledger WHERE category_id = ? AND date = ? ORDER BY rowid DESC";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(categoryID.getId()));
				statement.setString(2, when.format(DateTimeFormatter.ISO_DATE_TIME));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<LedgerItem> retval = new LinkedList<>();
				while(resultSet.next()) {
					LedgerItem ledgerItem = getLedgerItemFromResultSetRow(resultSet);
					retval.add(ledgerItem);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getLedgerItems(com.shtick.apps.budget.structure.model.TransactionID)
	 */
	@Override
	public List<LedgerItem> getLedgerItems(TransactionID transactionID) throws IOException {
		String sql = "SELECT transaction_id, event_id, category_id, change, total, date, time_added " +
				"FROM ledger WHERE transaction_id = ? ORDER BY rowid DESC";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(transactionID.getId()));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<LedgerItem> retval = new LinkedList<>();
				while(resultSet.next()) {
					LedgerItem ledgerItem = getLedgerItemFromResultSetRow(resultSet);
					if(canRead(ledgerItem.getCategoryID()))
						retval.add(ledgerItem);
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getUsernames()
	 */
	@Override
	public List<String> getUsernames() throws IOException {
		String sql = "SELECT name " +
				"FROM users";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				ResultSet resultSet = statement.executeQuery();
				LinkedList<String> retval = new LinkedList<>();
				while(resultSet.next())
					retval.add(resultSet.getString(1));
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#addUser(com.shtick.apps.budget.structure.model.User, java.lang.String)
	 */
	@Override
	public UserID addUser(User user, String password) throws IOException {
		String sql = "INSERT INTO users " +
                "(name, is_admin, hash, time_added) " +
				"VALUES (?,?,?,?)";
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				
				statement.setString(1, user.getUsername());
				statement.setInt(2, user.isAdmin()?1:0);
				statement.setString(3, hashPassword(password,null));
				statement.setString(4, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
				statement.executeUpdate();
				ResultSet resultSet = statement.getGeneratedKeys();
				if(!resultSet.next()) {
					resultSet.close();
					return null;
				}
				UserID retval = new UserID(""+resultSet.getInt(1));
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#login(java.lang.String, java.lang.String)
	 */
	@Override
	public UserID login(String username, String password) throws IOException {
		user = authenticate(username,password);
		if(user==null)
			return null;
		userPermissions = getUserPermissions(user.getUserID());
		return user.getUserID();
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#logout()
	 */
	@Override
	public void logout() {
		user = null;
		userPermissions = null;
	}
	
	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#changePassword(com.shtick.apps.budget.structure.model.UserID, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean changePassword(String oldPassword, String newPassword) throws IOException {
		if(authenticate(user.getUsername(),oldPassword)==null)
			return false;
		String sql = "UPDATE users " +
                "SET hash = ? " +
				"WHERE rowid = ?";
		int rowsUpdated = 0;
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);) {
				PreparedStatement statement = connection.prepareStatement(sql);
				statement.setString(1, hashPassword(newPassword,null));
				statement.setInt(1, Integer.parseInt(user.getUserID().toString()));
				rowsUpdated=statement.executeUpdate();
				statement.close();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
		return rowsUpdated>0;
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#isAdmin(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public boolean isAdmin(CategoryID categoryID) throws IOException{
		if(user==null)
			return false;
		if(user.isAdmin())
			return true;
		while(categoryID!=null) {
			if(userPermissions.get(categoryID)==Permission.Permissions.ADMIN)
				return true;
			Category category = getCategory(categoryID);
			categoryID = category.getParentID();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#canWrite(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public boolean canWrite(CategoryID categoryID) throws IOException{
		if(user==null)
			return false;
		if(categoryID==null)
			return false;
		Permission.Permissions permission = userPermissions.get(categoryID);
		if(permission==Permission.Permissions.WRITE)
			return true;
		return isAdmin(categoryID);
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#canRead(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public boolean canRead(CategoryID categoryID) throws IOException{
		if(user==null)
			return false;
		if(categoryID==null)
			return true;
		Permission.Permissions permission = userPermissions.get(categoryID);
		if(permission==Permission.Permissions.READ)
			return true;
		return isAdmin(categoryID);
	}

	private User authenticate(String username, String password) throws IOException {
		String sql = "SELECT rowid, name, is_admin, time_added, hash " +
				"FROM users WHERE name = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setString(1, username);
				ResultSet resultSet = statement.executeQuery();
				if(resultSet.next()) {
					String hash = resultSet.getString(5);
					String salt = hash.split(":")[0];
					if(hash.equals(hashPassword(password,fromHex(salt)))) {
						User user = getUserFromResultSetRow(resultSet);
						resultSet.close();
						return user;
					}
				}
				resultSet.close();
				return null;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}
	
	private static String hashPassword(String password, byte[] salt) {
		if(salt==null) {
			SecureRandom random = new SecureRandom();
			salt = new byte[16];
			random.nextBytes(salt);
		}
		byte[] hashedPassword;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.reset();
			md.update(salt);
			md.update(password.getBytes(StandardCharsets.UTF_8));
			hashedPassword = md.digest();
		}
		catch(NoSuchAlgorithmException t) {
			throw new RuntimeException(t);
		}
		return toHex(salt)+":"+toHex(hashedPassword);
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#getPermissions(com.shtick.apps.budget.structure.model.CategoryID)
	 */
	@Override
	public List<Permission> getPermissions(CategoryID categoryID) throws IOException {
		String sql = "SELECT rowid, category_id, user_id, permissions, time_added " +
				"FROM category_permissions WHERE category_id = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(categoryID.getId()));
				ResultSet resultSet = statement.executeQuery();
				LinkedList<Permission> retval = new LinkedList<>();
				while(resultSet.next()) {
					retval.add(getPermissionFromResultSetRow(resultSet));
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	private Map<CategoryID,Permission.Permissions> getUserPermissions(UserID userID) throws IOException {
		String sql = "SELECT rowid, category_id, user_id, permissions, time_added " +
				"FROM category_permissions WHERE user_id = ?";
		synchronized(DB_LOCK){
			try (
					Connection connection = DriverManager.getConnection(DB_URL);
					PreparedStatement statement = connection.prepareStatement(sql);
			) {
				statement.setInt(1, Integer.parseInt(userID.getId()));
				ResultSet resultSet = statement.executeQuery();
				HashMap<CategoryID,Permission.Permissions> retval = new HashMap<>();
				while(resultSet.next()) {
					Permission permission = getPermissionFromResultSetRow(resultSet);
					retval.put(permission.getCategoryID(), permission.getPermissions());
				}
				resultSet.close();
				return retval;
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#setPermission(com.shtick.apps.budget.structure.model.CategoryID, com.shtick.apps.budget.structure.model.Permission)
	 */
	@Override
	public void setPermission(CategoryID categoryID, Permission permission) throws IOException {
		String sql = "INSERT INTO category_permissions " +
                "(category_id, user_id, permissions, time_added) " +
				"VALUES (?,?,?,?)";
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				
				statement.setInt(1, Integer.parseInt(categoryID.getId()));
				statement.setInt(2, Integer.parseInt(user.getUserID().getId()));
				statement.setString(3, permission.toString());
				statement.setString(4, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
				statement.executeUpdate();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.shtick.apps.budget.Driver#deletePermission(com.shtick.apps.budget.structure.model.CategoryID, com.shtick.apps.budget.structure.model.UserID)
	 */
	@Override
	public void deletePermission(CategoryID categoryID, UserID userID) throws IOException {
		String sql = "DELETE FROM category_permissions " +
                "WHERE category_id = ? and user_id = ?";
		synchronized(DB_LOCK){
			try (Connection connection = DriverManager.getConnection(DB_URL);PreparedStatement statement = connection.prepareStatement(sql);) {
				statement.setInt(1, Integer.parseInt(categoryID.getId()));
				statement.setInt(2, Integer.parseInt(user.getUserID().getId()));
				statement.executeUpdate();
			}
			catch(SQLException t){
				throw new IOException(t);
			}
		}
	}

	private static Category getCategoryFromResultSetRow(ResultSet resultSet) throws SQLException{
		String timeDeleted = resultSet.getString(8);
		return new Category(
				new CategoryID(""+resultSet.getInt(1)),
				new CategoryID(""+resultSet.getInt(2)),
				resultSet.getString(3),
				new CurrencyID(""+resultSet.getInt(4)),
				resultSet.getInt(5),
				Permission.Permissions.valueOf(resultSet.getString(6)),
				LocalDateTime.parse(resultSet.getString(7), DateTimeFormatter.ISO_DATE_TIME),
				(timeDeleted==null)?null:LocalDateTime.parse(timeDeleted, DateTimeFormatter.ISO_DATE_TIME)
				);
	}

	private static Transaction getTransactionFromResultSetRow(ResultSet resultSet) throws SQLException{
		String date = resultSet.getString(6);
		String timeDeleted = resultSet.getString(7);
		String timeAdded = resultSet.getString(8);
		return new Transaction(
				new TransactionID(""+resultSet.getInt(1)),
				(date==null)?null:LocalDate.parse(date, DateTimeFormatter.ISO_DATE_TIME),
				new CategoryID(""+resultSet.getInt(2)),
				resultSet.getLong(4),
				new CategoryID(""+resultSet.getInt(3)),
				resultSet.getLong(5),
				(timeAdded==null)?null:LocalDateTime.parse(timeAdded, DateTimeFormatter.ISO_DATE_TIME),
				(timeDeleted==null)?null:LocalDateTime.parse(timeDeleted, DateTimeFormatter.ISO_DATE_TIME)
				);
	}

	private static Event getEventFromResultSetRow(ResultSet resultSet) throws SQLException{
		String timeAdded = resultSet.getString(6);
		return new Event(
				new EventID(""+resultSet.getInt(1)),
				new UserID(""+resultSet.getInt(2)),
				new TransactionID(""+resultSet.getInt(3)),
				(timeAdded==null)?null:LocalDateTime.parse(timeAdded, DateTimeFormatter.ISO_DATE_TIME),
				Event.Type.valueOf(resultSet.getString(5)),
				resultSet.getString(4)
				);
	}

	private static LedgerItem getLedgerItemFromResultSetRow(ResultSet resultSet) throws SQLException{
		String timeAdded = resultSet.getString(6);
		return new LedgerItem(
				new TransactionID(""+resultSet.getInt(1)),
				new EventID(""+resultSet.getInt(2)),
				new CategoryID(""+resultSet.getInt(3)),
				resultSet.getInt(4),
				resultSet.getInt(5),
				(timeAdded==null)?null:LocalDateTime.parse(timeAdded, DateTimeFormatter.ISO_DATE_TIME)
				);
	}

	private static User getUserFromResultSetRow(ResultSet resultSet) throws SQLException{
		int is_admin = resultSet.getInt(3);
		String timeAdded = resultSet.getString(4);
		return new User(
				new UserID(""+resultSet.getInt(1)),
				resultSet.getString(2),
				is_admin>0,
				(timeAdded==null)?null:LocalDateTime.parse(timeAdded, DateTimeFormatter.ISO_DATE_TIME)
				);
	}
	
	private static Permission getPermissionFromResultSetRow(ResultSet resultSet) throws SQLException{
		String timeAdded = resultSet.getString(5);
		return new Permission(
				new CategoryID(""+resultSet.getInt(2)),
				new UserID(""+resultSet.getInt(3)),
				Permission.Permissions.valueOf(resultSet.getString(4)),
				(timeAdded==null)?null:LocalDateTime.parse(timeAdded, DateTimeFormatter.ISO_DATE_TIME)
				);
	}
	
	private static String toHex(byte[] bytes) {
	    return String.format("%0"+(bytes.length*2)+"x", new BigInteger(1, bytes));
	}

	private static byte[] fromHex(String hexString) {
		hexString = hexString.toUpperCase();
		if(!hexString.matches("^([0-9A-F][0-9A-F])*$"))
			throw new IllegalArgumentException("Not a hexadecimal string: "+hexString);
		byte[] retval = new byte[hexString.length()/2];
	    for(int i=0;i<hexString.length();i++) {
	    	byte b = getHexValue(hexString.charAt(i));
	    	b<<=4;
	    	i++;
	    	b += getHexValue(hexString.charAt(i));
	    	retval[i/2] = b;
	    }
	    return retval;
	}
	
	private static byte getHexValue(char c) {
		if((c>='0')&&(c<='9'))
			return (byte)(c-'0');
		if((c>='A')&&(c<='F'))
			return (byte)(c-'A'+10);
		if((c>='a')&&(c<='f'))
			return (byte)(c-'a'+10);
		throw new IllegalArgumentException("Not a hexadecimal character.");
	}
}
