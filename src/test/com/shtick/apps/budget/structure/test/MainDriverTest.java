/**
 * 
 */
package com.shtick.apps.budget.structure.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shtick.apps.budget.structure.model.Category;
import com.shtick.apps.budget.structure.model.CategoryID;
import com.shtick.apps.budget.structure.model.Permission;
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
			List<Category> categories = mainDriver.getCategories(null);
			assertEquals(0,categories.size());
			// TODO
		}
		catch(IOException t) {
			fail(t);
		}
	}

}
