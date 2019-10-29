/**
 * 
 */
package com.shtick.apps.budget.structure.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;
import com.shtick.apps.budget.structure.model.Currency;
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
class MainDriverTest {
	private static File DEFAULT_WORKING_DIRECTORY;
	private static File TEST_WORKING_DIRECTORY;
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
		TEST_WORKING_DIRECTORY = new File(DEFAULT_WORKING_DIRECTORY,"test");
		
	}

	@BeforeEach
	void setUp() throws Exception {
		if(TEST_WORKING_DIRECTORY.exists()) {
			for(File subfile:TEST_WORKING_DIRECTORY.listFiles())
				subfile.delete();
		}
		else {
			TEST_WORKING_DIRECTORY.mkdirs();
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		if(TEST_WORKING_DIRECTORY.exists()) {
			for(File subfile:TEST_WORKING_DIRECTORY.listFiles())
				subfile.delete();
			TEST_WORKING_DIRECTORY.delete();
		}
	}

	@Test
	void testConstructor() {
		new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
	}

	@Test
	void testUserManagement() {
		try {
			com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
			List<String> usernames = mainDriver.getUsernames();
			assertEquals(0,usernames.size());
			assertNull(mainDriver.login("tester", "password"));
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			assertNotNull(userID);
			usernames = mainDriver.getUsernames();
			assertEquals(1,usernames.size());
			assertEquals("tester",usernames.get(0));
			assertNull(mainDriver.login("tester", "not_password"));
			assertEquals(userID,mainDriver.login("tester", "password"));
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testCategories() {
		com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
		try {
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			List<Category> categories = mainDriver.getCategories(null,true);
			assertEquals(0,categories.size());
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			assertNotNull(categoryID1);
			categories = mainDriver.getCategories(null,true);
			assertEquals(1,categories.size());
			assertEquals(categoryID1,categories.get(0).getId());
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("12"),120,null,null));
			assertNotNull(categoryID2);
			categories = mainDriver.getCategories(null,true);
			assertEquals(2,categories.size());
			{ // Check that expected categories returned in list.
				HashSet<CategoryID> foundIds = new HashSet<>();
				for(Category category:categories)
					foundIds.add(category.getId());
				assertTrue(foundIds.contains(categoryID1));
				assertTrue(foundIds.contains(categoryID2));
			}
			
			// Check subcategory function
			categories = mainDriver.getCategories(categoryID1,true);
			assertEquals(0,categories.size());
			categories = mainDriver.getCategories(categoryID2,true);
			assertEquals(0,categories.size());
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,categoryID1,"games",new CurrencyID("123"),10,null,null));
			categories = mainDriver.getCategories(categoryID1,true);
			assertEquals(1,categories.size());
			assertEquals(categoryID3,categories.get(0).getId());
			categories = mainDriver.getCategories(categoryID2,true);
			assertEquals(0,categories.size());
			categories = mainDriver.getCategories(categoryID3,true);
			assertEquals(0,categories.size());
			
			// Check individual category grabbing
			{
				Category category = mainDriver.getCategory(categoryID1);
				assertEquals(categoryID1,category.getId());
				assertEquals(null,category.getParentID());
				assertEquals("toys",category.getName());
				assertEquals(new CurrencyID("1"),category.getCurrency());
				assertEquals(0,category.getTotal());
				assertNotNull(category.getTimeAdded());
				assertNull(category.getTimeDeleted());
				category = mainDriver.getCategory(categoryID2);
				assertEquals(categoryID2,category.getId());
				assertEquals(null,category.getParentID());
				assertEquals("tools",category.getName());
				assertEquals(new CurrencyID("12"),category.getCurrency());
				assertEquals(0,category.getTotal());
				assertNotNull(category.getTimeAdded());
				assertNull(category.getTimeDeleted());
				category = mainDriver.getCategory(categoryID3);
				assertEquals(categoryID3,category.getId());
				assertEquals(categoryID1,category.getParentID());
				assertEquals("games",category.getName());
				assertEquals(new CurrencyID("123"),category.getCurrency());
				assertEquals(0,category.getTotal());
				assertNotNull(category.getTimeAdded());
				assertNull(category.getTimeDeleted());
			}
			 
			// Check category remove
			{
				try{
					mainDriver.removeCategory(categoryID1);
					fail("Failure expected when deleting a category with subcategories.");
				}
				catch(IllegalArgumentException t) {
					// Expected
				}
				assertTrue(mainDriver.removeCategory(categoryID2));
				assertTrue(mainDriver.removeCategory(categoryID3));
				categories = mainDriver.getCategories(null,true);
				assertEquals(2, categories.size());
				categories = mainDriver.getCategories(null,false);
				assertEquals(1, categories.size());
				assertEquals(categoryID1,categories.get(0).getId());
				categories = mainDriver.getCategories(categoryID1,true);
				assertEquals(1, categories.size());
				assertEquals(categoryID3,categories.get(0).getId());
				categories = mainDriver.getCategories(categoryID1,false);
				assertEquals(0, categories.size());

				assertTrue(mainDriver.removeCategory(categoryID1));
				categories = mainDriver.getCategories(null,true);
				assertEquals(2, categories.size());
				categories = mainDriver.getCategories(null,false);
				assertEquals(0, categories.size());
			}

			// Check category restore
			{
				try{
					mainDriver.restoreCategory(categoryID3);
					fail("Failure expected when restoring a category with a deleted parent.");
				}
				catch(IllegalArgumentException t) {
					// Expected
				}
				assertTrue(mainDriver.restoreCategory(categoryID2));
				assertTrue(mainDriver.restoreCategory(categoryID1));
				categories = mainDriver.getCategories(null,true);
				assertEquals(2, categories.size());
				categories = mainDriver.getCategories(null,false);
				assertEquals(2, categories.size());
				categories = mainDriver.getCategories(categoryID1,true);
				assertEquals(1, categories.size());
				assertEquals(categoryID3,categories.get(0).getId());
				categories = mainDriver.getCategories(categoryID1,false);
				assertEquals(0, categories.size());

				assertTrue(mainDriver.restoreCategory(categoryID3));
				categories = mainDriver.getCategories(categoryID1,true);
				assertEquals(1, categories.size());
				categories = mainDriver.getCategories(categoryID1,false);
				assertEquals(1, categories.size());
			}
			
			// Test category renaame
			{
				assertTrue(mainDriver.renameCategory(categoryID3, "vgames"));
				Category category = mainDriver.getCategory(categoryID3);
				assertEquals("vgames",category.getName());
			}
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testCurrencies() {
		com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
		try {
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			List<Currency> currencies = mainDriver.getCurrencies();
			assertEquals(0,currencies.size());
			CurrencyID currencyID1 = mainDriver.addCurrency(new Currency(null, "dollars", "blah", "{}", null));
			assertNotNull(currencyID1);
			currencies = mainDriver.getCurrencies();
			assertEquals(1,currencies.size());
			assertEquals(currencyID1,currencies.get(0).getId());
			
			CurrencyID currencyID2 = mainDriver.addCurrency(new Currency(null,"alpacas","qwerty","-",null));
			assertNotNull(currencyID2);
			currencies = mainDriver.getCurrencies();
			assertEquals(2,currencies.size());
			{ // Check that expected categories returned in list.
				HashMap<CurrencyID,Currency> currencyByID = new HashMap<>();
				for(Currency currency:currencies)
					currencyByID.put(currency.getId(),currency);
				assertTrue(currencyByID.containsKey(currencyID1));
				assertTrue(currencyByID.containsKey(currencyID2));
				assertEquals("dollars",currencyByID.get(currencyID1).getName());
				assertEquals("blah",currencyByID.get(currencyID1).getType());
				assertEquals("{}",currencyByID.get(currencyID1).getConfig());
				assertEquals("alpacas",currencyByID.get(currencyID2).getName());
				assertEquals("qwerty",currencyByID.get(currencyID2).getType());
				assertEquals("-",currencyByID.get(currencyID2).getConfig());
			}
			
			mainDriver.updateCurrencyName(currencyID2, "sheep");
			currencies = mainDriver.getCurrencies();
			assertEquals(2,currencies.size());
			{ // Check that expected categories returned in list.
				HashMap<CurrencyID,Currency> currencyByID = new HashMap<>();
				for(Currency currency:currencies)
					currencyByID.put(currency.getId(),currency);
				assertEquals("dollars",currencyByID.get(currencyID1).getName());
				assertEquals("sheep",currencyByID.get(currencyID2).getName());
				assertEquals("qwerty",currencyByID.get(currencyID2).getType());
				assertEquals("-",currencyByID.get(currencyID2).getConfig());
			}

			mainDriver.deleteCurrency(currencyID2);
			currencies = mainDriver.getCurrencies();
			assertEquals(1,currencies.size());
			assertEquals(currencyID1,currencies.get(0).getId());
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testAddTransferTransaction() {
		LocalDate today = LocalDate.now();
		try { // Basic transfer transaction
			com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("1"),100,null,null));
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,null,"alpacas",new CurrencyID("12"),100,null,null));
			List<Transaction> transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			List<Transaction> transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			List<Transaction> transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			TransactionID transactionID = mainDriver.addTransactions(new Transaction(null, today, categoryID1, 5, categoryID2, 5, null, null), "A test transaction.");
			assertNotNull(transactionID);
			transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(1,transactions1.size());
			assertEquals(1,transactions2.size());
			assertEquals(0,transactions3.size());
			assertEquals(transactionID,transactions1.get(0).getId());
			assertEquals(transactionID,transactions2.get(0).getId());
			assertEquals(today,transactions2.get(0).getWhen());
			assertEquals(5,transactions2.get(0).getSourceCurrency());
			assertEquals(5,transactions2.get(0).getDestinationCurrency());
			assertNull(transactions2.get(0).getDeletedDate());
			assertNotNull(transactions2.get(0).getAddedDate());
			List<Event> events = mainDriver.getEvents(10);
			assertEquals(1,events.size());
			assertEquals(transactionID,events.get(0).getTransactionID());
			assertEquals("A test transaction.",events.get(0).getNote());
			assertEquals(Event.Type.TRANSFER,events.get(0).getType());
			assertEquals(userID,events.get(0).getUserID());
			assertNotNull(events.get(0).getWhen());
			assertNotNull(events.get(0).getId());
			List<LedgerItem> ledgerItems1 = mainDriver.getLedgerItems(categoryID1, today);
			List<LedgerItem> ledgerItems2 = mainDriver.getLedgerItems(categoryID2, today);
			List<LedgerItem> ledgerItems3 = mainDriver.getLedgerItems(categoryID3, today);
			assertEquals(1,ledgerItems1.size());
			assertEquals(1,ledgerItems2.size());
			assertEquals(0,ledgerItems3.size());
			assertEquals(categoryID1,ledgerItems1.get(0).getCategoryID());
			assertEquals(-5,ledgerItems1.get(0).getChange());
			assertEquals(events.get(0).getId(),ledgerItems1.get(0).getEventID());
			assertEquals(-5,ledgerItems1.get(0).getTotal());
			assertEquals(transactionID,ledgerItems1.get(0).getTransactionID());
			assertNotNull(ledgerItems1.get(0).getTimeAdded());
			assertEquals(categoryID2,ledgerItems2.get(0).getCategoryID());
			assertEquals(5,ledgerItems2.get(0).getChange());
			assertEquals(events.get(0).getId(),ledgerItems2.get(0).getEventID());
			assertEquals(5,ledgerItems2.get(0).getTotal());
			assertEquals(transactionID,ledgerItems2.get(0).getTransactionID());
			assertNotNull(ledgerItems2.get(0).getTimeAdded());
			
			try { // Test transfer with unequal currency amounts.
				transactionID = mainDriver.addTransactions(new Transaction(null, today, categoryID1, 5, categoryID2, 4, null, null), "A test transaction.");
				fail("Invalid transaction should throw exception.");
			}
			catch(IllegalArgumentException t) {
				// Expected
			}
			
			try { // Test self-transfer.
				transactionID = mainDriver.addTransactions(new Transaction(null, today, categoryID1, 5, categoryID1, 5, null, null), "A test transaction.");
				fail("Invalid transaction should throw exception.");
			}
			catch(IllegalArgumentException t) {
				// Expected
			}
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testAddExchangeTransaction() {
		LocalDate today = LocalDate.now();
		try { // Basic transfer transaction
			com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("1"),100,null,null));
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,null,"alpacas",new CurrencyID("12"),100,null,null));
			List<Transaction> transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			List<Transaction> transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			List<Transaction> transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			TransactionID transactionID = mainDriver.addTransactions(new Transaction(null, today, categoryID1, 5, categoryID3, 1, null, null), "A test transaction.");
			assertNotNull(transactionID);
			transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(1,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(1,transactions3.size());
			assertEquals(transactionID,transactions1.get(0).getId());
			assertEquals(transactionID,transactions3.get(0).getId());
			assertEquals(today,transactions3.get(0).getWhen());
			assertEquals(5,transactions3.get(0).getSourceCurrency());
			assertEquals(1,transactions3.get(0).getDestinationCurrency());
			assertNull(transactions3.get(0).getDeletedDate());
			assertNotNull(transactions3.get(0).getAddedDate());
			List<Event> events = mainDriver.getEvents(10);
			assertEquals(1,events.size());
			assertEquals(transactionID,events.get(0).getTransactionID());
			assertEquals("A test transaction.",events.get(0).getNote());
			assertEquals(Event.Type.EXCHANGE,events.get(0).getType());
			assertEquals(userID,events.get(0).getUserID());
			assertNotNull(events.get(0).getWhen());
			assertNotNull(events.get(0).getId());
			List<LedgerItem> ledgerItems1 = mainDriver.getLedgerItems(categoryID1, today);
			List<LedgerItem> ledgerItems2 = mainDriver.getLedgerItems(categoryID2, today);
			List<LedgerItem> ledgerItems3 = mainDriver.getLedgerItems(categoryID3, today);
			assertEquals(1,ledgerItems1.size());
			assertEquals(0,ledgerItems2.size());
			assertEquals(1,ledgerItems3.size());
			assertEquals(categoryID1,ledgerItems1.get(0).getCategoryID());
			assertEquals(-5,ledgerItems1.get(0).getChange());
			assertEquals(events.get(0).getId(),ledgerItems1.get(0).getEventID());
			assertEquals(-5,ledgerItems1.get(0).getTotal());
			assertEquals(transactionID,ledgerItems1.get(0).getTransactionID());
			assertNotNull(ledgerItems1.get(0).getTimeAdded());
			assertEquals(categoryID3,ledgerItems3.get(0).getCategoryID());
			assertEquals(1,ledgerItems3.get(0).getChange());
			assertEquals(events.get(0).getId(),ledgerItems3.get(0).getEventID());
			assertEquals(1,ledgerItems3.get(0).getTotal());
			assertEquals(transactionID,ledgerItems3.get(0).getTransactionID());
			assertNotNull(ledgerItems3.get(0).getTimeAdded());
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testAddIncomeTransaction() {
		LocalDate today = LocalDate.now();
		try { // Basic transfer transaction
			com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("1"),100,null,null));
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,null,"alpacas",new CurrencyID("12"),100,null,null));
			List<Transaction> transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			List<Transaction> transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			List<Transaction> transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			TransactionID transactionID = mainDriver.addTransactions(new Transaction(null, today, null, 0, categoryID3, 1, null, null), "A test transaction.");
			assertNotNull(transactionID);
			transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(1,transactions3.size());
			assertEquals(transactionID,transactions3.get(0).getId());
			assertEquals(today,transactions3.get(0).getWhen());
			assertEquals(0,transactions3.get(0).getSourceCurrency());
			assertEquals(1,transactions3.get(0).getDestinationCurrency());
			assertNull(transactions3.get(0).getDeletedDate());
			assertNotNull(transactions3.get(0).getAddedDate());
			List<Event> events = mainDriver.getEvents(10);
			assertEquals(1,events.size());
			assertEquals(transactionID,events.get(0).getTransactionID());
			assertEquals("A test transaction.",events.get(0).getNote());
			assertEquals(Event.Type.INCOME,events.get(0).getType());
			assertEquals(userID,events.get(0).getUserID());
			assertNotNull(events.get(0).getWhen());
			assertNotNull(events.get(0).getId());
			List<LedgerItem> ledgerItems1 = mainDriver.getLedgerItems(categoryID1, today);
			List<LedgerItem> ledgerItems2 = mainDriver.getLedgerItems(categoryID2, today);
			List<LedgerItem> ledgerItems3 = mainDriver.getLedgerItems(categoryID3, today);
			assertEquals(0,ledgerItems1.size());
			assertEquals(0,ledgerItems2.size());
			assertEquals(1,ledgerItems3.size());
			assertEquals(categoryID3,ledgerItems3.get(0).getCategoryID());
			assertEquals(1,ledgerItems3.get(0).getChange());
			assertEquals(events.get(0).getId(),ledgerItems3.get(0).getEventID());
			assertEquals(1,ledgerItems3.get(0).getTotal());
			assertEquals(transactionID,ledgerItems3.get(0).getTransactionID());
			assertNotNull(ledgerItems3.get(0).getTimeAdded());
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testAddExpenseTransaction() {
		LocalDate today = LocalDate.now();
		try { // Basic transfer transaction
			com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("1"),100,null,null));
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,null,"alpacas",new CurrencyID("12"),100,null,null));
			List<Transaction> transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			List<Transaction> transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			List<Transaction> transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			TransactionID transactionID = mainDriver.addTransactions(new Transaction(null, today, categoryID1, 5, null, 0, null, null), "A test transaction.");
			assertNotNull(transactionID);
			transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(1,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			assertEquals(transactionID,transactions1.get(0).getId());
			assertEquals(today,transactions1.get(0).getWhen());
			assertEquals(5,transactions1.get(0).getSourceCurrency());
			assertEquals(0,transactions1.get(0).getDestinationCurrency());
			assertNull(transactions1.get(0).getDeletedDate());
			assertNotNull(transactions1.get(0).getAddedDate());
			List<Event> events = mainDriver.getEvents(10);
			assertEquals(1,events.size());
			assertEquals(transactionID,events.get(0).getTransactionID());
			assertEquals("A test transaction.",events.get(0).getNote());
			assertEquals(Event.Type.EXPENSE,events.get(0).getType());
			assertEquals(userID,events.get(0).getUserID());
			assertNotNull(events.get(0).getWhen());
			assertNotNull(events.get(0).getId());
			List<LedgerItem> ledgerItems1 = mainDriver.getLedgerItems(categoryID1, today);
			List<LedgerItem> ledgerItems2 = mainDriver.getLedgerItems(categoryID2, today);
			List<LedgerItem> ledgerItems3 = mainDriver.getLedgerItems(categoryID3, today);
			assertEquals(1,ledgerItems1.size());
			assertEquals(0,ledgerItems2.size());
			assertEquals(0,ledgerItems3.size());
			assertEquals(categoryID1,ledgerItems1.get(0).getCategoryID());
			assertEquals(-5,ledgerItems1.get(0).getChange());
			assertEquals(events.get(0).getId(),ledgerItems1.get(0).getEventID());
			assertEquals(-5,ledgerItems1.get(0).getTotal());
			assertEquals(transactionID,ledgerItems1.get(0).getTransactionID());
			assertNotNull(ledgerItems1.get(0).getTimeAdded());
		}
		catch(IOException t) {
			fail(t);
		}
	}

	@Test
	void testDeleteRestoreTransaction() {
		LocalDate today = LocalDate.now();
		try { // Basic transfer transaction
			com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("1"),100,null,null));
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,null,"alpacas",new CurrencyID("12"),100,null,null));
			List<Transaction> transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			List<Transaction> transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			List<Transaction> transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			TransactionID transactionID = mainDriver.addTransactions(new Transaction(null, today, categoryID1, 5, null, 0, null, null), "A test transaction.");
			mainDriver.deleteTransaction(transactionID, "Test delete");
			assertNotNull(transactionID);
			transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(1,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			assertEquals(transactionID,transactions1.get(0).getId());
			assertEquals(today,transactions1.get(0).getWhen());
			assertEquals(5,transactions1.get(0).getSourceCurrency());
			assertEquals(0,transactions1.get(0).getDestinationCurrency());
			assertNotNull(transactions1.get(0).getDeletedDate());
			assertNotNull(transactions1.get(0).getAddedDate());
			transactions1 = mainDriver.getTransactions(categoryID1, today, false);
			transactions2 = mainDriver.getTransactions(categoryID2, today, false);
			transactions3 = mainDriver.getTransactions(categoryID3, today, false);
			assertEquals(0,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			List<Event> events = mainDriver.getEvents(10);
			assertEquals(2,events.size());
			EventID deleteEventID;
			{
				HashMap<String,Event> notes = new HashMap<>();
				for(Event event:events) {
					assertEquals(transactionID,event.getTransactionID());
					assertEquals(userID,event.getUserID());
					assertNotNull(event.getWhen());
					assertNotNull(event.getId());
					notes.put(event.getNote(),event);
				}
				assertTrue(notes.containsKey("A test transaction."));
				assertTrue(notes.containsKey("Test delete"));
				assertEquals(Event.Type.DELETE,notes.get("Test delete").getType());
				deleteEventID = notes.get("Test delete").getId();
			}
			List<LedgerItem> ledgerItems1 = mainDriver.getLedgerItems(categoryID1, today);
			List<LedgerItem> ledgerItems2 = mainDriver.getLedgerItems(categoryID2, today);
			List<LedgerItem> ledgerItems3 = mainDriver.getLedgerItems(categoryID3, today);
			assertEquals(2,ledgerItems1.size());
			assertEquals(0,ledgerItems2.size());
			assertEquals(0,ledgerItems3.size());
			{
				HashMap<EventID,LedgerItem> ledgerItemsByEventID = new HashMap<>();
				for(LedgerItem item:ledgerItems1) {
					assertEquals(categoryID1,item.getCategoryID());
					assertEquals(transactionID,item.getTransactionID());
					assertNotNull(item.getTimeAdded());
					ledgerItemsByEventID.put(item.getEventID(),item);
				}
				assertTrue(ledgerItemsByEventID.containsKey(deleteEventID));
				assertEquals(5,ledgerItemsByEventID.get(deleteEventID).getChange());
				assertEquals(0,ledgerItemsByEventID.get(deleteEventID).getTotal());
			}
			mainDriver.undeleteTransaction(transactionID, "Test undelete");
			transactions1 = mainDriver.getTransactions(categoryID1, today, true);
			transactions2 = mainDriver.getTransactions(categoryID2, today, true);
			transactions3 = mainDriver.getTransactions(categoryID3, today, true);
			assertEquals(1,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			transactions1 = mainDriver.getTransactions(categoryID1, today, false);
			transactions2 = mainDriver.getTransactions(categoryID2, today, false);
			transactions3 = mainDriver.getTransactions(categoryID3, today, false);
			assertEquals(1,transactions1.size());
			assertEquals(0,transactions2.size());
			assertEquals(0,transactions3.size());
			events = mainDriver.getEvents(10);
			assertEquals(3,events.size());
			EventID undeleteEventID;
			{
				HashMap<String,Event> notes = new HashMap<>();
				for(Event event:events) {
					assertEquals(transactionID,event.getTransactionID());
					assertEquals(userID,event.getUserID());
					assertNotNull(event.getWhen());
					assertNotNull(event.getId());
					notes.put(event.getNote(),event);
				}
				assertTrue(notes.containsKey("A test transaction."));
				assertTrue(notes.containsKey("Test delete"));
				assertTrue(notes.containsKey("Test undelete"));
				assertEquals(Event.Type.UNDELETE,notes.get("Test undelete").getType());
				undeleteEventID = notes.get("Test undelete").getId();
			}
			ledgerItems1 = mainDriver.getLedgerItems(categoryID1, today);
			ledgerItems2 = mainDriver.getLedgerItems(categoryID2, today);
			ledgerItems3 = mainDriver.getLedgerItems(categoryID3, today);
			assertEquals(3,ledgerItems1.size());
			assertEquals(0,ledgerItems2.size());
			assertEquals(0,ledgerItems3.size());
			{
				HashMap<EventID,LedgerItem> ledgerItemsByEventID = new HashMap<>();
				for(LedgerItem item:ledgerItems1) {
					assertEquals(categoryID1,item.getCategoryID());
					assertEquals(transactionID,item.getTransactionID());
					assertNotNull(item.getTimeAdded());
					ledgerItemsByEventID.put(item.getEventID(),item);
				}
				assertTrue(ledgerItemsByEventID.containsKey(undeleteEventID));
				assertEquals(-5,ledgerItemsByEventID.get(undeleteEventID).getChange());
				assertEquals(-5,ledgerItemsByEventID.get(undeleteEventID).getTotal());
			}
		}
		catch(IOException t) {
			fail(t);
		}
	}
}
