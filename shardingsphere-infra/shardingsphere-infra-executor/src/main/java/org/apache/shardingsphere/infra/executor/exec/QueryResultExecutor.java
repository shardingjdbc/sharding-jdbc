package org.apache.shardingsphere.infra.executor.exec;

import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.infra.executor.exec.meta.Row;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;

import java.sql.SQLException;

public class QueryResultExecutor extends AbstractExecutor implements Executor {
    
    private QueryResult queryResult;
    
    public QueryResultExecutor(final ExecContext execContext, QueryResult queryResult) {
        super(execContext);
        this.queryResult = queryResult;
    }
    
    @Override
    protected void executeInit() {
        
    }
    
    @Override
    public boolean executeMove() {
        try {
            return queryResult.next();
        } catch (SQLException sqlException) {
            throw new ShardingSphereException("move next error", sqlException);
        }
    }
    
    public Row current() {
        QueryResultMetaData metaData = this.getMetaData();
        try {
            int columnCount = metaData.getColumnCount();
            Object[] rowVal = new Object[columnCount];
            for(int i = 0; i < rowVal.length; i++) {
                rowVal[i] = queryResult.getValue(i+1, Object.class);
            }
            return new Row(rowVal);
        } catch (Exception t) {
            throw new ShardingSphereException("load row error", t);
        }
    }
    
    @Override
    public QueryResultMetaData getMetaData() {
        return queryResult.getMetaData();
    }
}