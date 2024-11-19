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

package org.apache.iotdb.commons.udf;

import org.apache.iotdb.common.rpc.thrift.Model;
import org.apache.iotdb.commons.udf.service.UDFClassLoader;
import org.apache.iotdb.commons.utils.TestOnly;

import org.apache.tsfile.utils.Pair;
import org.apache.tsfile.utils.ReadWriteIOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * UDFTable is a table that stores UDF information. Only manage external UDFs, built-in UDFs are not
 * managed here.
 */
public class UDFTable {

  /** functionName -> information * */
  private final Map<Pair<Model, String>, UDFInformation> udfInformationMap;

  /** maintain a map for creating instance, functionName -> class */
  private final Map<Pair<Model, String>, Class<?>> functionToClassMap;

  public UDFTable() {
    udfInformationMap = new ConcurrentHashMap<>();
    functionToClassMap = new ConcurrentHashMap<>();
  }

  public void addUDFInformation(String functionName, UDFInformation udfInformation) {
    if (udfInformation.getUdfType().isTreeModel()) {
      udfInformationMap.put(new Pair<>(Model.TREE, functionName.toUpperCase()), udfInformation);
    } else {
      udfInformationMap.put(new Pair<>(Model.TABLE, functionName.toUpperCase()), udfInformation);
    }
  }

  public void removeUDFInformation(Model model, String functionName) {
    udfInformationMap.remove(new Pair<>(model, functionName.toUpperCase()));
  }

  public UDFInformation getUDFInformation(Model model, String functionName) {
    return udfInformationMap.get(new Pair<>(model, functionName.toUpperCase()));
  }

  public void addFunctionAndClass(Model model, String functionName, Class<?> clazz) {
    functionToClassMap.put(new Pair<>(model, functionName.toUpperCase()), clazz);
  }

  public Class<?> getFunctionClass(Model model, String functionName) {
    return functionToClassMap.get(new Pair<>(model, functionName.toUpperCase()));
  }

  public void removeFunctionClass(Model model, String functionName) {
    functionToClassMap.remove(new Pair<>(model, functionName.toUpperCase()));
  }

  public void updateFunctionClass(UDFInformation udfInformation, UDFClassLoader classLoader)
      throws ClassNotFoundException {
    Class<?> functionClass = Class.forName(udfInformation.getClassName(), true, classLoader);
    if (udfInformation.getUdfType().isTreeModel()) {
      functionToClassMap.put(
          new Pair<>(Model.TREE, udfInformation.getFunctionName().toUpperCase()), functionClass);
    } else {
      functionToClassMap.put(
          new Pair<>(Model.TABLE, udfInformation.getFunctionName().toUpperCase()), functionClass);
    }
  }

  public List<UDFInformation> getUDFInformationList(Model model) {
    return udfInformationMap.entrySet().stream()
        .filter(entry -> entry.getKey().left == model)
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  public List<UDFInformation> getAllInformationList() {
    return new ArrayList<>(udfInformationMap.values());
  }

  public boolean containsUDF(Model model, String udfName) {
    return udfInformationMap.containsKey(new Pair<>(model, udfName.toUpperCase()));
  }

  @TestOnly
  public Map<Pair<Model, String>, UDFInformation> getTable() {
    return udfInformationMap;
  }

  public void serializeUDFTable(OutputStream outputStream) throws IOException {
    List<UDFInformation> nonBuiltInUDFInformation = getAllInformationList();
    ReadWriteIOUtils.write(nonBuiltInUDFInformation.size(), outputStream);
    for (UDFInformation udfInformation : nonBuiltInUDFInformation) {
      ReadWriteIOUtils.write(udfInformation.serialize(), outputStream);
    }
  }

  public void deserializeUDFTable(InputStream inputStream) throws IOException {
    int size = ReadWriteIOUtils.readInt(inputStream);
    while (size > 0) {
      UDFInformation udfInformation = UDFInformation.deserialize(inputStream);
      if (udfInformation.getUdfType().isTreeModel()) {
        udfInformationMap.put(
            new Pair<>(Model.TREE, udfInformation.getFunctionName()), udfInformation);
      } else {
        udfInformationMap.put(
            new Pair<>(Model.TABLE, udfInformation.getFunctionName()), udfInformation);
      }
      size--;
    }
  }

  // only clear external UDFs
  public void clear() {
    udfInformationMap.clear();
    functionToClassMap.clear();
  }
}
