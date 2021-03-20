package org.apache.shardingsphere.infra.optimize.sql;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlDynamicParam;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.Util;
import org.apache.shardingsphere.infra.optimize.tools.SqlNodeUtil;

import java.util.List;

public class ExtractTableNameSqlShuttle extends ExtendedSqlShuttle {
    
    @Getter
    private List<SqlDynamicValueParam<String>> tableNames;
    
    public ExtractTableNameSqlShuttle() {
        tableNames = Lists.newArrayList();
    }
    
    @Override
    public SqlNode visit(final SqlLiteral literal) {
        return clone(literal);
    }
    
    @Override
    public SqlNode visit(final SqlIdentifier id) {
        return clone(id);
    }
    
    @Override
    public SqlNode visit(final SqlDataTypeSpec type) {
        return clone(type);
    }
    
    @Override
    public SqlNode visit(final SqlDynamicParam param) {
        return clone(param);
    }
    
    @Override
    public SqlNode visit(final SqlIntervalQualifier intervalQualifier) {
        return clone(intervalQualifier);
    }
    
    @Override
    public SqlNode visit(final SqlNodeList nodeList) {
        return clone(nodeList);
    }
    
    @Override
    public SqlSelect visit(final SqlSelect sqlSelect) {
        SqlSelect newSqlSelect = SqlNode.clone(sqlSelect);
        
        if(newSqlSelect.getSelectList() != null) {
            newSqlSelect.setSelectList((SqlNodeList) sqlSelect.getSelectList().accept(this));
        }
        
        if(newSqlSelect.getFrom() != null) {
            SqlNode from = newSqlSelect.getFrom();
            SqlKind fromKind = from.getKind();
            
            SqlNode convertedFrom = null;
            if (fromKind == SqlKind.IDENTIFIER) {
                convertedFrom = rebuildFromWithIdentifier((SqlIdentifier)from);
            } else if(fromKind == SqlKind.AS) {
                convertedFrom = rebuildFromWithAsBasicCall((SqlBasicCall)from);
            } else if(fromKind == SqlKind.JOIN) {
                convertedFrom = visit((SqlJoin) from);
            }
            newSqlSelect.setFrom(convertedFrom);
        }
        
        if(newSqlSelect.getWhere() != null) {
            newSqlSelect.setWhere(newSqlSelect.getWhere().accept(this));
        }
        
        if(newSqlSelect.getHaving() != null) {
            newSqlSelect.setHaving(newSqlSelect.getHaving().accept(this));
        }
        
        return newSqlSelect;
        
    }
    
    @Override
    public SqlJoin visit(final SqlJoin sqlJoin) {
        SqlJoin join = SqlNode.clone(sqlJoin);
        
        SqlNode left = rebuildJoinOperand(join.getLeft());
        join.setLeft(left);
        
        SqlNode right = rebuildJoinOperand(join.getRight());
        join.setRight(right);
        
        return join;
    }
    
    @Override
    SqlOrderBy visit(final SqlOrderBy sqlOrderBy) {
        SqlOrderBy newSqlOrderBy = SqlNode.clone(sqlOrderBy);
        return (SqlOrderBy)SqlOrderBy.OPERATOR.createCall(null,
                newSqlOrderBy.getParserPosition(), visit((SqlSelect) newSqlOrderBy.query), 
                SqlNodeUtil.clone(newSqlOrderBy.orderList), SqlNodeUtil.clone(newSqlOrderBy.offset), SqlNodeUtil.clone(newSqlOrderBy.fetch));
    }
    
    private SqlNode clone(SqlNode sqlNode) {
        return SqlNodeUtil.clone(sqlNode);
    }
    
    private SqlNode rebuildJoinOperand(SqlNode joinOperand) {
        if(joinOperand.getKind() == SqlKind.IDENTIFIER) {
            return rebuildFromWithIdentifier((SqlIdentifier)joinOperand);
        } else if(joinOperand instanceof SqlCall) {
            SqlCall operand = (SqlCall) joinOperand;
            if(operand.getKind() == SqlKind.AS) {
                return rebuildFromWithAsBasicCall((SqlBasicCall)operand);
            } else {
                return visit(operand);
            }
        }
        
        throw new UnsupportedOperationException("unsupported join operand " + joinOperand.getKind());
    }
    
    private SqlNode rebuildFromWithIdentifier(SqlIdentifier from) {
        String tableName = tableName(from.names);
        SqlDynamicValueParam<String> tableParamNode = new SqlDynamicValueParam<>(tableName, Integer.MAX_VALUE, SqlParserPos.ZERO);
        this.tableNames.add(tableParamNode);
        SqlIdentifier asName = new SqlIdentifier(tableName, from.getParserPosition());
        return SqlStdOperatorTable.AS.createCall(from.getParserPosition(), tableParamNode, asName);
    }
    
    private SqlNode rebuildFromWithAsBasicCall(SqlBasicCall from) {
        List<SqlNode> operands = from.getOperandList();
        SqlNode operand = operands.get(0);
        SqlNode as = operands.get(1);
        
        if(as.getKind() != SqlKind.IDENTIFIER) {
            throw new IllegalArgumentException("sql kind is not SqlKind.IDENTIFIER");
        }
        
        if(operand.getKind() == SqlKind.IDENTIFIER) {
            String tableName = tableName(((SqlIdentifier)operand).names);
            SqlDynamicValueParam tableParamNode = new SqlDynamicValueParam(tableName, Integer.MAX_VALUE, SqlParserPos.ZERO);
            tableNames.add(tableParamNode);
            operand = tableParamNode;
        } else if(operand instanceof SqlCall) {
            operand = visit((SqlCall) operand);
        }
        SqlIdentifier asIdentifier = new SqlIdentifier(((SqlIdentifier)as).getSimple(), as.getParserPosition());
        return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, operand, asIdentifier);
    }
    
    private String tableName(List<String> qualifiedName) {
        return Util.last(qualifiedName);
    }
}