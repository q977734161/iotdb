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

package org.apache.iotdb.db.storageengine.dataregion.wal.recover.file;

import org.apache.iotdb.commons.path.PartialPath;
import org.apache.iotdb.db.exception.WriteProcessException;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.write.DeleteDataNode;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.write.InsertNode;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.write.InsertRowNode;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.write.InsertRowsNode;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.write.InsertTabletNode;
import org.apache.iotdb.db.service.metrics.WritingMetrics;
import org.apache.iotdb.db.storageengine.dataregion.memtable.IMemTable;
import org.apache.iotdb.db.storageengine.dataregion.memtable.PrimitiveMemTable;
import org.apache.iotdb.db.storageengine.dataregion.modification.Deletion;
import org.apache.iotdb.db.storageengine.dataregion.tsfile.TsFileResource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * This class helps redo wal logs into a TsFile. Notice: You should update time map in {@link
 * TsFileResource} before using this class to avoid duplicated insertion and this class doesn't
 * guarantee concurrency safety.
 */
public class TsFilePlanRedoer {
  private final TsFileResource tsFileResource;
  // store data when redoing logs
  private IMemTable recoveryMemTable;

  public TsFilePlanRedoer(TsFileResource tsFileResource) {
    this.tsFileResource = tsFileResource;
    this.recoveryMemTable =
        new PrimitiveMemTable(tsFileResource.getDatabaseName(), tsFileResource.getDataRegionId());
    WritingMetrics.getInstance().recordActiveMemTableCount(tsFileResource.getDataRegionId(), 1);
  }

  void redoDelete(DeleteDataNode deleteDataNode) throws IOException {
    List<PartialPath> paths = deleteDataNode.getPathList();
    for (PartialPath path : paths) {
      // path here is device path pattern
      recoveryMemTable.delete(
          path,
          path.getDevicePath(),
          deleteDataNode.getDeleteStartTime(),
          deleteDataNode.getDeleteEndTime());
      tsFileResource
          .getModFile()
          .write(
              new Deletion(
                  path,
                  tsFileResource.getTsFileSize(),
                  deleteDataNode.getDeleteStartTime(),
                  deleteDataNode.getDeleteEndTime()));
    }
  }

  void redoInsert(InsertNode node) throws WriteProcessException {
    if (!node.hasValidMeasurements()) {
      return;
    }
    if (tsFileResource != null) {
      // orders of insert node is guaranteed by storage engine, just check time in the file
      // the last chunk group may contain the same data with the logs, ignore such logs in seq file
      Optional<Long> lastEndTime = tsFileResource.getEndTime(node.getDeviceID());
      long minTimeInNode;
      if (node instanceof InsertRowNode) {
        minTimeInNode = ((InsertRowNode) node).getTime();
      } else {
        minTimeInNode = ((InsertTabletNode) node).getTimes()[0];
      }
      if (lastEndTime.isPresent() && lastEndTime.get() >= minTimeInNode) {
        return;
      }
    }

    int pointsInserted;
    if (node instanceof InsertRowNode) {
      if (node.isAligned()) {
        pointsInserted = recoveryMemTable.insertAlignedRow((InsertRowNode) node);
      } else {
        pointsInserted = recoveryMemTable.insert((InsertRowNode) node);
      }
    } else {
      if (node.isAligned()) {
        pointsInserted =
            recoveryMemTable.insertAlignedTablet(
                (InsertTabletNode) node, 0, ((InsertTabletNode) node).getRowCount());
      } else {
        pointsInserted =
            recoveryMemTable.insertTablet(
                (InsertTabletNode) node, 0, ((InsertTabletNode) node).getRowCount());
      }
    }
    recoveryMemTable.updateMemtablePointCountMetric(node, pointsInserted);
  }

  void redoInsertRows(InsertRowsNode insertRowsNode) {
    int pointsInserted = 0;
    for (InsertRowNode node : insertRowsNode.getInsertRowNodeList()) {
      if (!node.hasValidMeasurements()) {
        continue;
      }
      if (tsFileResource != null) {
        // orders of insert node is guaranteed by storage engine, just check time in the file
        // the last chunk group may contain the same data with the logs, ignore such logs in seq
        // file
        Optional<Long> lastEndTime = tsFileResource.getEndTime(node.getDeviceID());
        long minTimeInNode;
        minTimeInNode = node.getTime();
        if (lastEndTime.isPresent() && lastEndTime.get() >= minTimeInNode) {
          continue;
        }
      }
      if (node.isAligned()) {
        pointsInserted += recoveryMemTable.insertAlignedRow(node);
      } else {
        pointsInserted += recoveryMemTable.insert(node);
      }
    }
    recoveryMemTable.updateMemtablePointCountMetric(insertRowsNode, pointsInserted);
  }

  void resetRecoveryMemTable(IMemTable memTable) {
    this.recoveryMemTable = memTable;
  }

  IMemTable getRecoveryMemTable() {
    return recoveryMemTable;
  }
}
