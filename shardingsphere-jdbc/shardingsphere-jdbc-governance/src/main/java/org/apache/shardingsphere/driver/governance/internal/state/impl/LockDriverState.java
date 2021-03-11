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

package org.apache.shardingsphere.driver.governance.internal.state.impl;

import org.apache.shardingsphere.driver.governance.internal.state.DriverState;
import org.apache.shardingsphere.driver.governance.internal.state.DriverStateContext;
import org.apache.shardingsphere.infra.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.context.metadata.MetaDataContexts;
import org.apache.shardingsphere.infra.exception.CommonErrorCode;
import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.apache.shardingsphere.transaction.core.TransactionType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 * Lock driver state.
 */
public final class LockDriverState implements DriverState {
    
    @Override
    public Connection getConnection(final Map<String, DataSource> dataSourceMap, 
                                    final MetaDataContexts metaDataContexts, final TransactionContexts transactionContexts, final TransactionType transactionType) {
        block(metaDataContexts);
        return DriverStateContext.getConnection(dataSourceMap, metaDataContexts, transactionContexts, transactionType);
    }
    
    private void block(final MetaDataContexts metaDataContexts) {
        long lockTimeoutMilliseconds = metaDataContexts.getProps().<Long>getValue(ConfigurationPropertyKey.LOCK_WAIT_TIMEOUT_MILLISECONDS);
        if (metaDataContexts.getLock().isPresent() && !metaDataContexts.getLock().get().await(lockTimeoutMilliseconds)) {
            throw new ShardingSphereException(CommonErrorCode.SHARDING_SERVICE_LOCK_WAIT_TIMEOUT_ERROR, lockTimeoutMilliseconds);
        }
    }
}
