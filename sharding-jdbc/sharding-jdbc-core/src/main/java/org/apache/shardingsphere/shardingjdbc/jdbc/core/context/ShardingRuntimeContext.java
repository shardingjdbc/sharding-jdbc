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

package org.apache.shardingsphere.shardingjdbc.jdbc.core.context;

import lombok.Getter;
import org.apache.shardingsphere.core.metadata.TableMetaDataInitializerEntry;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.encrypt.metadata.decorator.EncryptTableMetaDataDecorator;
import org.apache.shardingsphere.sharding.execute.metadata.loader.ShardingTableMetaDataLoader;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.metadata.CachedDatabaseMetaData;
import org.apache.shardingsphere.shardingjdbc.jdbc.metadata.JDBCConnectionManager;
import org.apache.shardingsphere.spi.database.type.DatabaseType;
import org.apache.shardingsphere.transaction.ShardingTransactionManagerEngine;
import org.apache.shardingsphere.underlying.common.config.DatabaseAccessConfiguration;
import org.apache.shardingsphere.underlying.common.constant.properties.PropertiesConstant;
import org.apache.shardingsphere.underlying.common.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.underlying.common.metadata.datasource.DataSourceMetas;
import org.apache.shardingsphere.underlying.common.metadata.table.TableMetas;
import org.apache.shardingsphere.underlying.common.metadata.table.init.TableMetaDataInitializer;
import org.apache.shardingsphere.underlying.common.rule.BaseRule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Runtime context for sharding.
 * 
 * @author gaohongtao
 * @author panjuan
 * @author zhangliang
 */
@Getter
public final class ShardingRuntimeContext extends AbstractRuntimeContext<ShardingRule> {
    
    private final DatabaseMetaData cachedDatabaseMetaData;
    
    private final ShardingSphereMetaData metaData;
    
    private final ShardingTransactionManagerEngine shardingTransactionManagerEngine;
    
    public ShardingRuntimeContext(final Map<String, DataSource> dataSourceMap, final ShardingRule shardingRule, final Properties props, final DatabaseType databaseType) throws SQLException {
        super(shardingRule, props, databaseType);
        cachedDatabaseMetaData = createCachedDatabaseMetaData(dataSourceMap, shardingRule);
        metaData = createMetaData(dataSourceMap, databaseType);
        shardingTransactionManagerEngine = new ShardingTransactionManagerEngine();
        shardingTransactionManagerEngine.init(databaseType, dataSourceMap);
    }
    
    private DatabaseMetaData createCachedDatabaseMetaData(final Map<String, DataSource> dataSourceMap, final ShardingRule rule) throws SQLException {
        try (Connection connection = dataSourceMap.values().iterator().next().getConnection()) {
            return new CachedDatabaseMetaData(connection.getMetaData(), dataSourceMap, rule);
        }
    }
    
    private ShardingSphereMetaData createMetaData(final Map<String, DataSource> dataSourceMap, final DatabaseType databaseType) throws SQLException {
        DataSourceMetas dataSourceMetas = new DataSourceMetas(databaseType, getDatabaseAccessConfigurationMap(dataSourceMap));
        TableMetas tableMetas = createTableMetaDataInitializerEntry(dataSourceMap, dataSourceMetas).initAll();
        return new ShardingSphereMetaData(dataSourceMetas, tableMetas);
    }
    
    private Map<String, DatabaseAccessConfiguration> getDatabaseAccessConfigurationMap(final Map<String, DataSource> dataSourceMap) throws SQLException {
        Map<String, DatabaseAccessConfiguration> result = new LinkedHashMap<>(dataSourceMap.size(), 1);
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            DataSource dataSource = entry.getValue();
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                result.put(entry.getKey(), new DatabaseAccessConfiguration(metaData.getURL(), metaData.getUserName(), null));
            }
        }
        return result;
    }
    
    private TableMetaDataInitializerEntry createTableMetaDataInitializerEntry(final Map<String, DataSource> dataSourceMap, final DataSourceMetas dataSourceMetas) {
        Map<BaseRule, TableMetaDataInitializer> tableMetaDataInitializes = new HashMap<>(2, 1);
        tableMetaDataInitializes.put(getRule(), new ShardingTableMetaDataLoader(dataSourceMetas, getExecutorEngine(), new JDBCConnectionManager(dataSourceMap), 
                getProperties().<Integer>getValue(PropertiesConstant.MAX_CONNECTIONS_SIZE_PER_QUERY), getProperties().<Boolean>getValue(PropertiesConstant.CHECK_TABLE_METADATA_ENABLED)));
        if (!getRule().getEncryptRule().getEncryptTableNames().isEmpty()) {
            tableMetaDataInitializes.put(getRule().getEncryptRule(), new EncryptTableMetaDataDecorator());
        }
        return new TableMetaDataInitializerEntry(tableMetaDataInitializes);
    }
    
    @Override
    public void close() throws Exception {
        shardingTransactionManagerEngine.close();
        super.close();
    }
}
