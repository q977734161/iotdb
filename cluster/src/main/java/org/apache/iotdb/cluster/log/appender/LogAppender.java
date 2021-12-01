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


package org.apache.iotdb.cluster.log.appender;

import java.util.List;
import org.apache.iotdb.cluster.log.Log;
import org.apache.iotdb.cluster.rpc.thrift.AppendEntryResult;

/**
 * LogAppender appends newly incoming entries to the local log of a member, providing different
 * policies for out-of-order entries and other cases.
 */
public interface LogAppender {

  AppendEntryResult appendEntries(
      long prevLogIndex, long prevLogTerm, long leaderCommit, List<Log> logs);

  AppendEntryResult appendEntry(
      long prevLogIndex, long prevLogTerm, long leaderCommit, Log log);
}
