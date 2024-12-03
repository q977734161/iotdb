/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.subscription.it.triple.regression.auto_create_db;

import org.apache.iotdb.it.framework.IoTDBTestRunner;
import org.apache.iotdb.itbase.category.MultiClusterIT2SubscriptionRegressionMisc;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.subscription.consumer.SubscriptionPullConsumer;
import org.apache.iotdb.subscription.it.triple.regression.AbstractSubscriptionRegressionIT;

import org.apache.thrift.TException;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.write.record.Tablet;
import org.apache.tsfile.write.schema.IMeasurementSchema;
import org.apache.tsfile.write.schema.MeasurementSchema;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(IoTDBTestRunner.class)
@Category({MultiClusterIT2SubscriptionRegressionMisc.class})
public class IoTDBDefaultPullConsumerDataSetIT extends AbstractSubscriptionRegressionIT {
  public static SubscriptionPullConsumer consumer;
  private int deviceCount = 3;
  private static final String databasePrefix = "root.DefaultPullConsumerDataSet";
  private static String topicName = "topic_autodb_DefaultPullConsumerDataSet";
  private static List<IMeasurementSchema> schemaList = new ArrayList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    createTopic_s(topicName, null, null, null, false);
    schemaList.add(new MeasurementSchema("s_0", TSDataType.INT32));
    schemaList.add(new MeasurementSchema("s_1", TSDataType.DOUBLE));
    subs.getTopics().forEach((System.out::println));
    assertTrue(subs.getTopic(topicName).isPresent(), "Create show topics");
    for (int i = 0; i < deviceCount; i++) {
      session_src.executeNonQueryStatement("create database " + databasePrefix + i);
      session_dest.executeNonQueryStatement("create database " + databasePrefix + i);
    }
  }

  @Override
  @After
  public void tearDown() throws Exception {
    consumer.close();
    subs.dropTopic(topicName);
    for (int i = 0; i < deviceCount; i++) {
      session_src.executeNonQueryStatement("drop database " + databasePrefix + i);
      session_dest.executeNonQueryStatement("drop database " + databasePrefix + i);
    }
    super.tearDown();
  }

  private void insert_data(long timestamp, String device)
      throws IoTDBConnectionException, StatementExecutionException {
    Tablet tablet = new Tablet(device, schemaList, 10);
    int rowIndex = 0;
    for (int row = 0; row < 5; row++) {
      rowIndex = tablet.getRowSize();
      tablet.addTimestamp(rowIndex, timestamp);
      tablet.addValue("s_0", rowIndex, row * 20 + row);
      tablet.addValue("s_1", rowIndex, row + 2.45);
      timestamp += 2000;
    }
    session_src.insertTablet(tablet);
  }

  @Test
  public void do_test()
      throws InterruptedException,
          TException,
          IoTDBConnectionException,
          IOException,
          StatementExecutionException {
    List<String> devices = new ArrayList<>(deviceCount);
    for (int i = 0; i < deviceCount; i++) {
      devices.add(databasePrefix + i + ".d_0");
    }
    consumer = create_pull_consumer("pull_auto_create_db", "default_pattern_dataset", false, null);
    for (int i = 0; i < deviceCount; i++) {
      // Write data before subscribing
      insert_data(1706659200000L, devices.get(i)); // 2024-01-31 08:00:00+08:00
    }
    // Subscribe
    consumer.subscribe(topicName);
    assertEquals(
        subs.getSubscriptions(topicName).size(), 1, "show subscriptions after subscription");
    for (int i = 0; i < deviceCount; i++) {
      insert_data(System.currentTimeMillis(), devices.get(i));
    }
    String sql = "select count(s_0) from " + databasePrefix + "0.d_0";
    System.out.println(FORMAT.format(new Date()) + " src: " + getCount(session_src, sql));
    // Consumption data
    consume_data(consumer, session_dest);
    for (int i = 0; i < deviceCount; i++) {
      check_count(10, "select count(s_0) from " + devices.get(i), i + ":Consumption Data:s_0");
    }
    // Unsubscribe
    consumer.unsubscribe(topicName);
    // Unsubscribe and then write data
    for (int i = 0; i < deviceCount; i++) {
      insert_data(1707782400000L, devices.get(i)); // 2024-02-13 08:00:00+08:00
    }
    consumer.subscribe(topicName);
    assertEquals(
        subs.getSubscriptions(topicName).size(), 1, "show subscriptions after re-subscribing");
    System.out.println(FORMAT.format(new Date()) + " src: " + getCount(session_src, sql));
    // Consumption data: Progress is not retained when re-subscribing after cancellation. Full
    // synchronization.
    consume_data(consumer, session_dest);
    for (int i = 0; i < deviceCount; i++) {
      check_count(15, "select count(s_0) from " + devices.get(i), i + ":consume data again:s_0");
    }
  }
}
