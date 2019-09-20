/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class User {
	private UserID userID;
	private String username;
	private boolean isAdmin;
	private LocalDateTime timeAdded;
	
	/**
	 * 
	 * @param userID
	 * @param username
	 * @param isAdmin
	 * @param timeAdded
	 */
	public User(UserID userID, String username, boolean isAdmin, LocalDateTime timeAdded) {
		super();
		this.userID = userID;
		this.username = username;
		this.isAdmin = isAdmin;
		this.timeAdded = timeAdded;
	}

	/**
	 * @return the userID
	 */
	public UserID getUserID() {
		return userID;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the isAdmin
	 */
	public boolean isAdmin() {
		return isAdmin;
	}

	/**
	 * @return the timeAdded
	 */
	public LocalDateTime getTimeAdded() {
		return timeAdded;
	}
}
