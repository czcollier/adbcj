/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj.tck.test;

import org.adbcj.*;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


// TODO Write test for result set metadata
@Test(invocationCount=50, threadPoolSize=10, timeOut = 50000)
public class SelectTest {

	private ConnectionManager connectionManager;
	
	@Parameters({"url", "user", "password"})
	@BeforeTest
	public void createConnectionManager(String url, String user, String password) {
		connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, password);
	}

	@AfterTest
	public void closeConnectionManager() {
		DbFuture<Void> closeFuture = connectionManager.close(true);
		closeFuture.getUninterruptably();
	}


//    public void testSelectWhichReturnsNothing() throws Exception{
//        Connection connection = connectionManager.connect().get();
//        final CountDownLatch latch = new CountDownLatch(1);
//        ResultSet resultSet = connection.executeQuery("SELECT int_val, str_val FROM simple_values where str_val LIKE 'Not-In-Database-Value'").addListener(new DbListener<ResultSet>() {
//            public void onCompletion(DbFuture<ResultSet> future) throws Exception {
//                future.get().size();
//                latch.countDown();
//            }
//        }).get();
//        Iterator<Row> i = resultSet.iterator();
//        Assert.assertFalse(i.hasNext());
//
//
//    }
//
//	public void testSimpleSelect() throws DbException, InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        Connection connection = connectionManager.connect().get();
//        try {
//            ResultSet resultSet = connection.executeQuery("SELECT int_val, str_val FROM simple_values ORDER BY int_val").addListener(new DbListener<ResultSet>() {
//                public void onCompletion(DbFuture<ResultSet> future) throws Exception {
//                    future.get().size();
//                    latch.countDown();
//                }
//            }).get();
//
//            Assert.assertEquals(6, resultSet.size());
//
//            Iterator<Row> i = resultSet.iterator();
//
//            Row nullRow = null;
//            Row row = i.next();
//            if (row.get(0).isNull()) {
//                nullRow = row;
//                row = i.next();
//            }
//            Assert.assertEquals(row.get(0).getInt(), 0);
//            Assert.assertEquals(row.get(1).getValue(), "Zero");
//            row = i.next();
//            Assert.assertEquals(row.get(0).getInt(), 1);
//            Assert.assertEquals(row.get(1).getValue(), "One");
//            row = i.next();
//            Assert.assertEquals(row.get(0).getInt(), 2);
//            Assert.assertEquals(row.get(1).getValue(), "Two");
//            row = i.next();
//            Assert.assertEquals(row.get(0).getInt(), 3);
//            Assert.assertEquals(row.get(1).getValue(), "Three");
//            row = i.next();
//            Assert.assertEquals(row.get(0).getInt(), 4);
//            Assert.assertEquals(row.get(1).getValue(), "Four");
//
//            if (i.hasNext() && nullRow == null) {
//                nullRow = i.next();
//            }
//
//            Assert.assertEquals(nullRow.get(0).getValue(), null);
//            Assert.assertEquals(nullRow.get(1).getValue(), null);
//
//
//            Assert.assertTrue(!i.hasNext(), "There were too many rows in result set");
//
//            Assert.assertTrue(latch.await(5, TimeUnit.SECONDS),"Expect callback call");
//        } finally {
//            connection.close(true);
//        }
//    }

    public void testSelectWithNullFields() throws DbException, InterruptedException {
        Connection connection = connectionManager.connect().get();

        ResultSet resultSet = connection.executeQuery("SELECT * FROM `table_with_some_values` WHERE `can_be_null_int` IS NULL").get();

        Assert.assertEquals(resultSet.get(0).get(1).getString(),null);
        Assert.assertEquals(resultSet.get(0).get(2).getString(),null);

        connection.close();

    }

    public void testMultipleSelectStatements() throws Exception {
        Connection connection = connectionManager.connect().get();

        List<DbFuture<ResultSet>> futures = new LinkedList<DbFuture<ResultSet>>();
        for (int i = 0; i < 50; i++) {
            futures.add(
                    connection.executeQuery(String.format("SELECT *, %d FROM simple_values", i))
            );
        }

        for (DbFuture<ResultSet> future : futures) {
            try {
                future.get(5, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                throw new AssertionError("Timed out waiting on future: " + future);
            }
        }
    }

    public void testBrokenSelect() throws Exception {
        Connection connection = connectionManager.connect().get();

        DbSessionFuture<ResultSet> future = connection.executeQuery("SELECT broken_query");
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Issues a bad query, future should have failed");
        } catch (DbException e) {
            // Pass
        } finally {
            connection.close().get();
        }
    }
	
}
