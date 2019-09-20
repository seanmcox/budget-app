/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class Permission {
	/**
	 * 
	 * @author scox
	 *
	 */
	public enum Permissions {
		/**
		 * Can view category/transactions.
		 * Defined per-category.
		 */
		READ,
		/**
		 * Can create transactions.
		 * Defined per-category.
		 */
		WRITE,
		/**
		 * Can create subcategories.
		 * If defined for any category, then applies to all subcategories as well.
		 */
		ADMIN
	};

	private CategoryID categoryID;
	private UserID userID;
	private Permissions permissions;
	private LocalDateTime addedDate;
	
	/**
	 * 
	 * @param categoryID
	 * @param userID
	 * @param permissions
	 * @param addedDate
	 */
	public Permission(CategoryID categoryID, UserID userID, Permissions permissions, LocalDateTime addedDate) {
		super();
		this.categoryID = categoryID;
		this.userID = userID;
		this.permissions = permissions;
		this.addedDate = addedDate;
	}

	/**
	 * @return the categoryID
	 */
	public CategoryID getCategoryID() {
		return categoryID;
	}

	/**
	 * @return the userID
	 */
	public UserID getUserID() {
		return userID;
	}

	/**
	 * @return the permission
	 */
	public Permissions getPermissions() {
		return permissions;
	}

	/**
	 * @return the addedDate
	 */
	public LocalDateTime getAddedDate() {
		return addedDate;
	}
}
