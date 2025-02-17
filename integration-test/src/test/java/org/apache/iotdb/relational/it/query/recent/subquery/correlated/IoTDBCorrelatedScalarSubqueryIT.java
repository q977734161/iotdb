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

package org.apache.iotdb.relational.it.query.recent.subquery.correlated;

import org.apache.iotdb.it.env.EnvFactory;
import org.apache.iotdb.it.framework.IoTDBTestRunner;
import org.apache.iotdb.itbase.category.TableClusterIT;
import org.apache.iotdb.itbase.category.TableLocalStandaloneIT;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.apache.iotdb.db.it.utils.TestUtils.prepareTableData;
import static org.apache.iotdb.db.it.utils.TestUtils.tableAssertTestFail;
import static org.apache.iotdb.db.it.utils.TestUtils.tableResultSetEqualTest;
import static org.apache.iotdb.relational.it.query.recent.subquery.SubqueryDataSetUtils.CREATE_SQLS;
import static org.apache.iotdb.relational.it.query.recent.subquery.SubqueryDataSetUtils.DATABASE_NAME;
import static org.apache.iotdb.relational.it.query.recent.subquery.SubqueryDataSetUtils.NUMERIC_MEASUREMENTS;

@RunWith(IoTDBTestRunner.class)
@Category({TableLocalStandaloneIT.class, TableClusterIT.class})
public class IoTDBCorrelatedScalarSubqueryIT {

  @BeforeClass
  public static void setUp() throws Exception {
    EnvFactory.getEnv().getConfig().getCommonConfig().setSortBufferSize(128 * 1024);
    EnvFactory.getEnv().getConfig().getCommonConfig().setMaxTsBlockSizeInByte(4 * 1024);
    EnvFactory.getEnv().initClusterEnvironment();
    prepareTableData(CREATE_SQLS);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EnvFactory.getEnv().cleanClusterEnvironment();
  }

  @Test
  public void testCorrelatedScalarSubqueryInWhereClause() {
    String sql;
    String[] expectedHeader;
    String[] retArray;

    // Test case: Aggregation with correlated filter in scalar subquery
    sql =
        "SELECT cast(%s AS INT32) as %s FROM table1 t1 WHERE device_id = 'd01' and %s >= (SELECT max(%s) from table3 t3 where t1.%s = t3.%s)";
    retArray = new String[] {"30,", "40,"};
    for (String measurement : NUMERIC_MEASUREMENTS) {
      expectedHeader = new String[] {measurement};
      tableResultSetEqualTest(
          String.format(
              sql, measurement, measurement, measurement, measurement, measurement, measurement),
          expectedHeader,
          retArray,
          DATABASE_NAME);
    }

    // Test case: Non-Aggregation with correlated filter in scalar subquery
    sql =
        "SELECT cast(%s AS INT32) as %s FROM table1 t1 WHERE device_id = 'd01' and %s >= (SELECT distinct %s from table3 t3 where t1.%s = t3.%s and %s > 30)";
    retArray = new String[] {"40,"};
    for (String measurement : NUMERIC_MEASUREMENTS) {
      expectedHeader = new String[] {measurement};
      tableResultSetEqualTest(
          String.format(
              sql,
              measurement,
              measurement,
              measurement,
              measurement,
              measurement,
              measurement,
              measurement),
          expectedHeader,
          retArray,
          DATABASE_NAME);
    }
  }

  @Test
  public void testNonComparisonFilterInCorrelatedScalarSubquery() {
    // Legality check: Correlated subquery with Non-equality comparison is not support for now.
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 > (select max(s1) from table3 t3 where t1.s1 > t3.s1)",
        "For now, FullOuterJoin and LeftJoin only support EquiJoinClauses",
        DATABASE_NAME);
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 > (select max(s1) from table3 t3 where t1.s1 >= t3.s1)",
        "For now, FullOuterJoin and LeftJoin only support EquiJoinClauses",
        DATABASE_NAME);
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 > (select max(s1) from table3 t3 where t1.s1 < t3.s1)",
        "For now, FullOuterJoin and LeftJoin only support EquiJoinClauses",
        DATABASE_NAME);
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 > (select max(s1) from table3 t3 where t1.s1 <= t3.s1)",
        "For now, FullOuterJoin and LeftJoin only support EquiJoinClauses",
        DATABASE_NAME);
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 > (select max(s1) from table3 t3 where t1.s1 != t3.s1)",
        "For now, FullOuterJoin and LeftJoin only support EquiJoinClauses",
        DATABASE_NAME);
  }

  @Test
  public void testCorrelatedScalarSubqueryLegalityCheck() {
    // Legality check: Correlated subqueries can only access columns from the immediately outer
    // scope and cannot access columns from the further outer queries.
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 > (select s1 from table3 t3 where t1.s1 = t3.s1 and s1 > (select s1 from table2 t2 where t2.s1 = t1.s1 limit 1) limit 1)",
        "701: Given correlated subquery is not supported",
        DATABASE_NAME);

    // Legality check: Correlated subqueries with limit clause and limit count greater than 1 is not
    // supported for now
    tableAssertTestFail(
        "select s1 from table3 t3 where 30 = t3.s1 and s1 > (select max(s1) from table2 t2 where t2.s1 = t3.s1 limit 2)",
        "701: Given correlated subquery is not supported",
        DATABASE_NAME);
  }

  // todo: find out why this fails occasionally
  @Test
  public void testMultipleRowsReturnedByScalarSubquery() {
    // Legality check: Scalar subquery should only return one row
    tableAssertTestFail(
        "select s1 from table1 t1 where s1 >= (select s1 from table3 t3 where t3.s1 = t1.s1)",
        "701: Scalar sub-query has returned multiple rows",
        DATABASE_NAME);
  }
}
