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

package org.apache.iotdb.commons.udf.builtin.relational;

import org.apache.iotdb.commons.udf.builtin.relational.tvf.HOPTableFunction;
import org.apache.iotdb.commons.udf.builtin.relational.tvf.RepeatExample;
import org.apache.iotdb.udf.api.relational.TableFunction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum TableBuiltinTableFunction {
  HOP("hop"),
  REPEAT("repeat"),
  ;

  private final String functionName;

  TableBuiltinTableFunction(String functionName) {
    this.functionName = functionName;
  }

  public String getFunctionName() {
    return functionName;
  }

  private static final Set<String> BUILT_IN_TABLE_FUNCTION_NAME =
      new HashSet<>(
          Arrays.stream(TableBuiltinTableFunction.values())
              .map(TableBuiltinTableFunction::getFunctionName)
              .collect(Collectors.toList()));

  public static Set<String> getBuiltInTableFunctionName() {
    return BUILT_IN_TABLE_FUNCTION_NAME;
  }

  public static boolean isBuiltInTableFunction(String functionName) {
    return BUILT_IN_TABLE_FUNCTION_NAME.contains(functionName.toLowerCase());
  }

  public static TableFunction getBuiltinTableFunction(String functionName) {
    switch (functionName.toLowerCase()) {
      case "hop":
        return new HOPTableFunction();
      case "repeat":
        return new RepeatExample();
      default:
        throw new UnsupportedOperationException("Unsupported table function: " + functionName);
    }
  }
}
