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

package org.apache.shardingsphere.proxy.backend.text.admin.mysql.handler;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.auth.ShardingSphereUser;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.raw.metadata.RawQueryResultColumnMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.raw.metadata.RawQueryResultMetaData;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.impl.QueryHeaderBuilder;
import org.apache.shardingsphere.proxy.backend.text.admin.DatabaseAdminBackendHandler;
import org.apache.shardingsphere.sharding.merge.dal.common.SingleLocalDataMergedResult;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Show databases backend handler.
 */
@RequiredArgsConstructor
public final class ShowDatabasesBackendHandler implements DatabaseAdminBackendHandler {
    
    private final BackendConnection backendConnection;
    
    private QueryResultMetaData queryResultMetaData;
    
    private MergedResult mergedResult;
    
    @Override
    public ResponseHeader execute() throws SQLException {
        queryResultMetaData = createQueryResultMetaData();
        mergedResult = new SingleLocalDataMergedResult(getSchemaNames());
        return new QueryResponseHeader(Collections.singletonList(QueryHeaderBuilder.build(queryResultMetaData, ProxyContext.getInstance().getMetaData(backendConnection.getSchemaName()), 1)));
    }
    
    private QueryResultMetaData createQueryResultMetaData() {
        return new RawQueryResultMetaData(
                Collections.singletonList(new RawQueryResultColumnMetaData("SCHEMATA", "Database", "SCHEMA_NAME", Types.VARCHAR, "VARCHAR", 255, 0)));
    }
    
    private Collection<Object> getSchemaNames() {
        Collection<Object> result = new LinkedList<>(ProxyContext.getInstance().getAllSchemaNames());
        Optional<ShardingSphereUser> user = ProxyContext.getInstance().getMetaDataContexts().getAuthentication().findUser(backendConnection.getUsername());
        Collection<String> authorizedSchemas = user.isPresent() ? user.get().getAuthorizedSchemas() : Collections.emptyList();
        if (!authorizedSchemas.isEmpty()) {
            result.retainAll(authorizedSchemas);
        }
        return result;
    }
    
    @Override
    public boolean next() throws SQLException {
        return mergedResult.next();
    }
    
    @Override
    public Collection<Object> getRowData() throws SQLException {
        return Collections.singletonList(mergedResult.getValue(1, Object.class));
    }
}
