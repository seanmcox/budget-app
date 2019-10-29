/**
 * 
 */
package com.shtick.apps.budget.structure.model;

import java.time.LocalDateTime;

/**
 * @author scox
 *
 */
public class Currency {
	private CurrencyID id;
	private String name;
	private String type;
	private String config;
	private LocalDateTime timeAdded;
	
	/**
	 * 
	 * @param id
	 * @param name
	 * @param type
	 * @param config
	 * @param timeAdded
	 */
	public Currency(CurrencyID id, String name, String type, String config,
			LocalDateTime timeAdded) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.config = config;
		this.timeAdded = timeAdded;
	}

	/**
	 * @return the id
	 */
	public CurrencyID getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return config;
	}

	/**
	 * @return the timeAdded
	 */
	public LocalDateTime getTimeAdded() {
		return timeAdded;
	}
}
