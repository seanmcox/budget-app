/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class LedgerItem {
	private TransactionID transactionID;
	private EventID eventID;
	private CategoryID categoryID;
	private int change;
	private int total;
	private LocalDateTime timeAdded;

	/**
	 * @param transactionID 
	 * @param eventID 
	 * @param categoryID 
	 * @param change 
	 * @param total 
	 * @param timeAdded 
	 * 
	 */
	public LedgerItem(TransactionID transactionID, EventID eventID, CategoryID categoryID, int change, int total, LocalDateTime timeAdded) {
		super();
		this.transactionID = transactionID;
		this.eventID = eventID;
		this.categoryID = categoryID;
		this.change = change;
		this.total = total;
		this.timeAdded = timeAdded;
	}

	/**
	 * @return the transactionID
	 */
	public TransactionID getTransactionID() {
		return transactionID;
	}

	/**
	 * @return the eventID
	 */
	public EventID getEventID() {
		return eventID;
	}

	/**
	 * @return the categoryID
	 */
	public CategoryID getCategoryID() {
		return categoryID;
	}

	/**
	 * @return the change
	 */
	public int getChange() {
		return change;
	}

	/**
	 * @return the total
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @return the timeAdded
	 */
	public LocalDateTime getTimeAdded() {
		return timeAdded;
	}
}
