/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class Category {
	private CategoryID id;
	private CategoryID parentID;
	private String name;
	private CurrencyID currency;
	private int total;
	private LocalDateTime timeAdded;
	private LocalDateTime timeDeleted;
	
	/**
	 * 
	 * @param id
	 * @param parentID
	 * @param name
	 * @param currency
	 * @param total
	 * @param permissions
	 * @param timeAdded
	 * @param timeDeleted 
	 */
	public Category(CategoryID id, CategoryID parentID, String name, CurrencyID currency, int total,
			LocalDateTime timeAdded, LocalDateTime timeDeleted) {
		super();
		this.id = id;
		this.parentID = parentID;
		this.name = name;
		this.currency = currency;
		this.total = total;
		this.timeAdded = timeAdded;
		this.timeDeleted = timeDeleted;
	}

	/**
	 * @return the id
	 */
	public CategoryID getId() {
		return id;
	}

	/**
	 * @return the parentID
	 */
	public CategoryID getParentID() {
		return parentID;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the currency
	 */
	public CurrencyID getCurrency() {
		return currency;
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

	/**
	 * @return the timeDeleted
	 */
	public LocalDateTime getTimeDeleted() {
		return timeDeleted;
	}
}
