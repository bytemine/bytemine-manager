/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer,                    E-Mail:  rauer@bytemine.net,  *
 *         Florian Reichel                           reichel@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.tests;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

import net.bytemine.manager.action.UserAction;
import net.bytemine.manager.bean.User;
import net.bytemine.manager.bean.X509;
import net.bytemine.utility.DNUtil;
import net.bytemine.utility.DateUtils;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

public class UserActionTest {
    
    @BeforeClass
    public static void setUpBeforeClass() {
        System.err.println("\n\n\n>>> Setting up UserActionTest");
		ManagerTestSuite.setUpTest();
		ManagerTestSuite.rootCreation();
	}
	
    @AfterClass
    public static void tearDownAfterClass() {
		ManagerTestSuite.tearDownTest();
		System.err.println("\n\n\n>>> Teared down UserActionTest");
	}
    
    // testing user : creation,update,removal
    @Test
    public void testUser() throws Exception {
        int id = testUserCreation();
        testUserUpdate(id);
        testUserRename(id);
        testUserRemoval(id);
    }
    
    public int testUserCreation() throws Exception {
    	int id = UserAction.createUserAndCertificate("dan_ra", "123456", "dra", "technik", "a", "90");
        User user = User.getUserByID(id);
        assertEquals("dan_ra", user.getUsername());
        assertEquals("dra", user.getCn());
        assertNotNull(user.getPassword());
        assertEquals("technik", user.getOu());
        
        SimpleDateFormat validToFormat = new SimpleDateFormat("dd. MMMMM yyyy hh:mm:ss");
        Date validTo = validToFormat.parse(X509.getX509ById(user.getX509id()).getValidTo());
        Calendar now = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        to.setTime(validTo);
        
        // The certificate is valid till now+90 days at 0 p.m. That means it is 
        // not valid for whole 90 days, but only 89 whole days
        assertEquals(DateUtils.daysBetween(now, to), 89);

        return id;
    }
    
    public void testUserUpdate(int id) throws Exception {
    	UserAction.updateUser(Integer.toString(id), "test", "k123", "k", "1");
    	User user = User.getUserByID(id);
    	
    	assertEquals("test", user.getUsername());
        assertEquals("k", user.getCn());
        assertNotNull(user.getPassword());
        assertEquals("1", user.getOu());
    }
    
    public void testUserRemoval(int id) throws Exception {
    	UserAction.deleteUser(Integer.toString(id));
    	User user = User.getUserByID(id);
    	if (user != null)
    		fail();
    }
    
    public void testUserRename(int id) throws Exception {
        DNUtil util = new DNUtil();
        User user = User.getUserByID(id);
        
        // update the user without changing the CN, but change the username
        UserAction.updateUser(Integer.toString(id), "test2", "k123", "k", "1");
        
        X509 x509 = X509.getX509ById(user.getX509id());
        util.split(x509.getSubject());
        assertEquals("k", util.getCn());
        
        // update the user, change the CN
        UserAction.updateUser(Integer.toString(id), "test2", "k123", "new_cn", "1");
        
        // the new CN has been inserted into the certificate
        x509 = X509.getX509ById(user.getX509id());
        util.split(x509.getSubject());
        assertEquals("new_cn", util.getCn());
    }
}