package org.apache.iotdb.db.queryengine.plan.relational.planner;

import org.apache.iotdb.db.queryengine.common.header.DatasetHeader;
import org.apache.iotdb.db.queryengine.plan.planner.plan.node.PlanNode;
import org.apache.iotdb.db.queryengine.plan.relational.metadata.ColumnSchema;
import org.apache.iotdb.db.queryengine.plan.relational.sql.ast.Expression;
import org.apache.iotdb.db.queryengine.plan.relational.sql.ast.Literal;

import org.apache.tsfile.read.common.type.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedValue {

  PlanNode planNode;

  DatasetHeader respHeader;
  HashMap<Symbol, Type> symbolMap;
  Map<Symbol, ColumnSchema> assignments;

  // Used for indexScan to fetch device
  List<Expression> metadataExpressionList;
  List<String> attributeColumns;
  List<Literal> literalReference;

  public CachedValue(
      PlanNode planNode,
      List<Literal> literalReference,
      DatasetHeader header,
      HashMap<Symbol, Type> symbolMap,
      Map<Symbol, ColumnSchema> assignments,
      List<Expression> metadataExpressionList,
      List<String> attributeColumns) {
    this.planNode = planNode;
    this.respHeader = header;
    this.symbolMap = symbolMap;
    this.assignments = assignments;
    this.metadataExpressionList = metadataExpressionList;
    this.attributeColumns = attributeColumns;
    this.literalReference = literalReference;
  }

  public DatasetHeader getRespHeader() {
    return respHeader;
  }

  public PlanNode getPlanNode() {
    return planNode;
  }

  public HashMap<Symbol, Type> getSymbolMap() {
    return symbolMap;
  }

  public List<Expression> getMetadataExpressionList() {
    return metadataExpressionList;
  }

  public List<String> getAttributeColumns() {
    return attributeColumns;
  }

  public Map<Symbol, ColumnSchema> getAssignments() {
    return assignments;
  }

  public List<Literal> getLiteralReference() {
    return literalReference;
  }
}
