/**
 * 
 */
package com.shtick.apps.budget.structure.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;
import com.shtick.apps.budget.structure.model.CurrencyID;
import com.shtick.apps.budget.structure.model.Permission;
import com.shtick.apps.budget.structure.model.Transaction;
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
	void testAddTransaction() {
		com.shtick.apps.budget.structure.MainDriver mainDriver = new com.shtick.apps.budget.structure.MainDriver(TEST_WORKING_DIRECTORY);
		try {
			UserID userID = mainDriver.addUser(new User(null, "tester", true, null), "password");
			mainDriver.login("tester", "password");
			CategoryID categoryID1 = mainDriver.addCategory(new Category(null,null,"toys",new CurrencyID("1"),100,null,null));
			CategoryID categoryID2 = mainDriver.addCategory(new Category(null,null,"tools",new CurrencyID("1"),100,null,null));
			CategoryID categoryID3 = mainDriver.addCategory(new Category(null,null,"alpacas",new CurrencyID("12"),100,null,null));
			mainDriver.addTransactions(new Transaction(null, null, categoryID1, 5, categoryID2, 5, null, null), "A test transaction.");
		}
		catch(IOException t) {
			fail(t);
		}
	}

}
