/**
 * 
 */
package com.shtick.apps.budget;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;
import com.shtick.apps.budget.structure.model.Event;
import com.shtick.apps.budget.structure.model.EventID;
import com.shtick.apps.budget.structure.model.Permission;
import com.shtick.apps.budget.structure.model.Transaction;
import com.shtick.apps.budget.structure.model.TransactionID;
import com.shtick.apps.budget.structure.model.User;
import com.shtick.apps.budget.structure.model.UserID;

/**
 * @author scox
 *
 */
public abstract class Driver {
	/**
	 * Return eventCount sequential events from the event log, starting with the most recent event and going back.
	 * 
	 * @param eventCount
	 * @return A list of events. 
	 * @throws IOException 
	 */
	public abstract List<Event> getEvents(int eventCount) throws IOException;
	
	/**
	 * Return eventCount sequential events from the event log, starting with the given event and going back.
	 * 
	 * @param eventCount
	 * @param eventID
	 * @return A list of events. 
	 * @throws IOException 
	 */
	public abstract List<Event> getEvents(int eventCount, EventID eventID) throws IOException;
	
	/**
	 * Return all events from the event log for the given date.
	 * 
	 * @param when
	 * @return A list of events. 
	 * @throws IOException 
	 */
	public abstract List<Event> getEvents(LocalDate when) throws IOException;

	/**
	 * Get all Events with the given TransactionID, ordered by date, descending.
	 * 
	 * Note: There can only be one event creating the transaction, but it will be the first, chronologically, (the last in the returned list) and the rest will be deletes and undeletes.
	 * 
	 * @param transactionID
	 * @return A list of events. 
	 * @throws IOException 
	 */
	public abstract List<Event> getEventsByTransaction(TransactionID transactionID) throws IOException;
	
	/**
	 * 
	 * @param transaction If the date of the transaction (when) is not null and is not in the future, then the transaction will be back-dated, but the generated event will always be generated using the current date.
	 * @param note A note that will be recorded in the event for this action.
	 * @return The ID of the newly created transaction.
	 * @throws IOException
	 */
	public abstract TransactionID addTransactions(Transaction transaction, String note) throws IOException;

	/**
	 * Return all the transactions for a given category on the given date.
	 * 
	 * @param categoryID
	 * @param date
	 * @param includeDeleted
	 * @return A list of transactions. 
	 * @throws IOException 
	 */
	public abstract List<Transaction> getTransactions(CategoryID categoryID, LocalDate date, boolean includeDeleted) throws IOException;
	
	/**
	 * 
	 * @param transactionID
	 * @param note A note that will be recorded in the event for this action.
	 * @return true on success or false otherwise
	 * @throws IOException
	 */
	public abstract boolean deleteTransaction(TransactionID transactionID, String note) throws IOException;
	
	/**
	 * 
	 * @param transactionID
	 * @param note A note that will be recorded in the event for this action.
	 * @return true on success or false otherwise
	 * @throws IOException
	 */
	public abstract boolean undeleteTransaction(TransactionID transactionID, String note) throws IOException;
	
	/**
	 * 
	 * @param parentCategory 
	 * @return A list of categories that are children of the identified parent category, or all the main categories if parentCategory is null.
	 * @throws IOException
	 */
	public abstract List<Category> getCategories(CategoryID parentCategory) throws IOException;
	
	/**
	 * 
	 * @param id
	 * @return The Category with the given ID.
	 * @throws IOException
	 */
	public abstract Category getCategory(CategoryID id) throws IOException;

	/**
	 * 
	 * @param category
	 * @return true on success or false otherwise
	 * @throws IOException
	 */
	public abstract CategoryID addCategory(Category category) throws IOException;

	/**
	 * Can be a soft delete or a hard delete.
	 * 
	 * @param categoryID
	 * @return true on success or false otherwise
	 * @throws IOException
	 */
	public abstract boolean removeCategory(CategoryID categoryID) throws IOException;
	
	/**
	 * In principle, this doesn't have to be implemented. It will be impossible to restore a category that has undergone a hard delete.
	 * 
	 * @param categoryID
	 * @return true on success or false otherwise
	 * @throws IOException
	 */
	public abstract boolean restoreCategory(CategoryID categoryID) throws IOException;
	
	/**
	 * 
	 * @param categoryID
	 * @param name
	 * @return true on success or false otherwise
	 * @throws IOException
	 */
	public abstract boolean renameCategory(CategoryID categoryID, String name) throws IOException;
	
	/**
	 * Log in the given user.
	 * 
	 * @param username
	 * @param password
	 * @return The UserID for the logged in user, or null if login failed.
	 * @throws IOException
	 */
	public abstract UserID login(String username, String password) throws IOException;
	
	/**
	 * Logs out the current user.
	 */
	public abstract void logout();
	
	/**
	 * 
	 * @param oldPassword
	 * @param newPassword
	 * @return true if successful, or false if the oldPassword didn't match
	 * @throws IOException
	 */
	public abstract boolean changePassword(String oldPassword, String newPassword) throws IOException;
	
	/**
	 * 
	 * @return A list of all usernames.
	 * @throws IOException
	 */
	public abstract List<String> getUsernames() throws IOException;
	
	/**
	 * 
	 * @param user 
	 * @param password 
	 * @return The UserID of the created user, or null if not successful.
	 * @throws IOException
	 */
	public abstract UserID addUser(User user, String password) throws IOException;

	/**
	 * 
	 * @param categoryID
	 * @return A list of permissions.
	 * @throws IOException
	 */
	public abstract List<Permission> getPermissions(CategoryID categoryID) throws IOException;
	
	/**
	 * 
	 * @param categoryID
	 * @param permission
	 * @throws IOException
	 */
	public abstract void setPermission(CategoryID categoryID, Permission permission) throws IOException;

	/**
	 * 
	 * @param categoryID
	 * @param userID
	 * @throws IOException
	 */
	public abstract void deletePermission(CategoryID categoryID, UserID userID) throws IOException;
	
	/**
	 * 
	 * @param categoryID 
	 * @return true if the logged-in user can administer the identified category and false otherwise.
	 * @throws IOException
	 */
	public abstract boolean isAdmin(CategoryID categoryID) throws IOException;

	/**
	 * 
	 * @param categoryID 
	 * @return true if the logged-in user can create transactions for the identified category and false otherwise.
	 * @throws IOException
	 */	
	public abstract boolean canWrite(CategoryID categoryID) throws IOException;
	
	/**
	 * 
	 * @param categoryID
	 * @return true if the logged-in user can see the identified category and false otherwise.
	 * @throws IOException
	 */
	public abstract boolean canRead(CategoryID categoryID) throws IOException;
}
