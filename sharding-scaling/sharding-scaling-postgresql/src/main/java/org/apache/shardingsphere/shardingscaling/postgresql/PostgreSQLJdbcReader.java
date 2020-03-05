/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingscaling.postgresql;

import org.apache.shardingsphere.shardingscaling.core.config.RdbmsConfiguration;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.reader.AbstractJDBCReader;
import org.apache.shardingsphere.shardingscaling.core.datasource.DataSourceManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL JDBC reader.
 */
public final class PostgreSQLJdbcReader extends AbstractJDBCReader {
    
    public PostgreSQLJdbcReader(final RdbmsConfiguration rdbmsConfiguration, final DataSourceManager dataSourceManager) {
        super(rdbmsConfiguration, dataSourceManager);
    }
    
    @Override
    protected PreparedStatement createPreparedStatement(final Connection conn, final String sql) throws SQLException {
        PreparedStatement result = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        result.setFetchSize(1);
        return result;
    }
}
