/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class Transaction {
	private TransactionID id;
	private LocalDate when;
	private CategoryID sourceCategoryID;
	private long sourceCurrency;
	private CategoryID destinationCategoryID;
	private long destinationCurrency;
	private LocalDateTime addedDate;
	private LocalDateTime deletedDate;
	
	/**
	 * 
	 * @param id
	 * @param when
	 * @param sourceCategoryID
	 * @param sourceCurrency
	 * @param destinationCategoryID
	 * @param destinationCurrency
	 * @param addedDate 
	 * @param deletedDate
	 */
	public Transaction(TransactionID id, LocalDate when, CategoryID sourceCategoryID, long sourceCurrency,
			CategoryID destinationCategoryID, long destinationCurrency, LocalDateTime addedDate, LocalDateTime deletedDate) {
		super();
		this.id = id;
		this.when = when;
		this.sourceCategoryID = sourceCategoryID;
		this.sourceCurrency = sourceCurrency;
		this.destinationCategoryID = destinationCategoryID;
		this.destinationCurrency = destinationCurrency;
		this.deletedDate = deletedDate;
		this.addedDate = addedDate;
	}
	
	/**
	 * @return the id
	 */
	public TransactionID getId() {
		return id;
	}
	/**
	 * @return the when
	 */
	public LocalDate getWhen() {
		return when;
	}
	/**
	 * @return the sourceCategoryID
	 */
	public CategoryID getSourceCategoryID() {
		return sourceCategoryID;
	}
	/**
	 * @return the sourceCurrency
	 */
	public long getSourceCurrency() {
		return sourceCurrency;
	}
	/**
	 * @return the destinationCategoryID
	 */
	public CategoryID getDestinationCategoryID() {
		return destinationCategoryID;
	}
	/**
	 * @return the destinationCurrency
	 */
	public long getDestinationCurrency() {
		return destinationCurrency;
	}
	/**
	 * @return the addedDate
	 */
	public LocalDateTime getAddedDate() {
		return addedDate;
	}

	/**
	 * @return the deletedDate
	 */
	public LocalDateTime getDeletedDate() {
		return deletedDate;
	}

	/**
	 * @return the deleted
	 */
	public boolean isDeleted() {
		return deletedDate!=null;
	}
}
