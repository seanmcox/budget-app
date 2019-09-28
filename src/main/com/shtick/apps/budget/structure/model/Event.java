/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class Event {
	/**
	 * @author scox
	 *
	 */
	public enum Type{
		/**
		 * Purchase of something untracked, gift, charity, theft, loss.
		 * The transaction will only have a valid outCategory.
		 */
		EXPENSE,
		/**
		 * Money coming in. This could be a gift, due to work done, or due to the sale of some untracked good.
		 * The transaction will only have a valid inCategory.
		 */
		INCOME,
		/**
		 * Incoming and outgoing currencies differ.
		 * ie. Value transfer between categories with dissimilar currencies. Incoming values probably don't match, and may not translate in a consistent or straightforward way, but that's expected.
		 */
		EXCHANGE,
		/**
		 * Incoming and outgoing currencies remain the same.
		 * ie. Simple fund transfer between categories with similar currencies. Incoming value should match outgoing value.
		 */
		TRANSFER,
		/**
		 * All other types involve creating a transaction. This item describes deleting or undoing a transaction.
		 */
		DELETE,
		/**
		 * Describes undeleting a previously deleted transaction.
		 */
		UNDELETE
	};
	
	private EventID id;
	private UserID userID;
	private TransactionID transactionID;
	private LocalDateTime when;
	private Type type;
	private String note;
	
	/**
	 * 
	 * @param id
	 * @param userID
	 * @param transactionID
	 * @param when
	 * @param type
	 * @param total 
	 * @param note
	 */
	public Event(EventID id, UserID userID, TransactionID transactionID, LocalDateTime when, Type type, String note) {
		super();
		this.id = id;
		this.userID = userID;
		this.transactionID = transactionID;
		this.when = when;
		this.type = type;
		this.note = note;
	}

	/**
	 * @return the id
	 */
	public EventID getId() {
		return id;
	}

	/**
	 * @return the userID
	 */
	public UserID getUserID() {
		return userID;
	}

	/**
	 * @return the transactionID
	 */
	public TransactionID getTransactionID() {
		return transactionID;
	}

	/**
	 * @return the when
	 */
	public LocalDateTime getWhen() {
		return when;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}
}
