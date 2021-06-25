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

package org.apache.shardingsphere.proxy.backend.text.distsql.rdl.impl.updater;

import com.google.common.base.Splitter;
import org.apache.shardingsphere.infra.distsql.RDLStatementUpdater;
import org.apache.shardingsphere.infra.metadata.resource.ShardingSphereResource;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.proxy.backend.exception.DuplicateBindingTablesException;
import org.apache.shardingsphere.proxy.backend.exception.ShardingBindingTableRuleNotExistsException;
import org.apache.shardingsphere.proxy.backend.exception.ShardingTableRuleNotExistedException;
import org.apache.shardingsphere.readwritesplitting.spi.ReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingAutoTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.converter.ShardingRuleStatementConverter;
import org.apache.shardingsphere.sharding.distsql.parser.statement.AlterShardingBindingTableRulesStatement;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Alter sharding binding table rule statement updater.
 */
public final class AlterShardingBindingTableRuleStatementUpdater implements RDLStatementUpdater<AlterShardingBindingTableRulesStatement, ShardingRuleConfiguration> {
    
    static {
        // TODO consider about register once only
        ShardingSphereServiceLoader.register(ReplicaLoadBalanceAlgorithm.class);
    }
    
    @Override
    public void checkSQLStatement(final String schemaName, final AlterShardingBindingTableRulesStatement sqlStatement, 
                                  final ShardingRuleConfiguration currentRuleConfig, final ShardingSphereResource resource) {
        if (null == currentRuleConfig) {
            throw new ShardingBindingTableRuleNotExistsException(schemaName);
        }
        Collection<String> invalidBindingTables = new HashSet<>();
        Collection<String> existLogicTables = getLogicTables(currentRuleConfig);
        Collection<String> bindingTables = ShardingRuleStatementConverter.convert(sqlStatement).getBindingTables();
        for (String bindingTable : bindingTables) {
            for (String logicTable : Splitter.on(",").splitToList(bindingTable)) {
                if (!existLogicTables.contains(logicTable.trim())) {
                    invalidBindingTables.add(logicTable);
                }
            }
        }
        if (!invalidBindingTables.isEmpty()) {
            throw new ShardingTableRuleNotExistedException(schemaName, invalidBindingTables);
        }
        Collection<String> duplicateBindingTables = bindingTables.stream().filter(distinct()).collect(Collectors.toList());
        if (!duplicateBindingTables.isEmpty()) {
            throw new DuplicateBindingTablesException(duplicateBindingTables);
        }
    }
    
    @Override
    public void updateCurrentRuleConfiguration(final String schemaName, final AlterShardingBindingTableRulesStatement sqlStatement, final ShardingRuleConfiguration currentRuleConfig) {
        Collection<String> bindingTableGroups = currentRuleConfig.getBindingTableGroups();
        bindingTableGroups.clear();
        bindingTableGroups.addAll(ShardingRuleStatementConverter.convert(sqlStatement).getBindingTables());
    }
    
    private Collection<String> getLogicTables(final ShardingRuleConfiguration currentRuleConfig) {
        Collection<String> result = new HashSet<>();
        result.addAll(currentRuleConfig.getTables().stream().map(ShardingTableRuleConfiguration::getLogicTable).collect(Collectors.toSet()));
        result.addAll(currentRuleConfig.getAutoTables().stream().map(ShardingAutoTableRuleConfiguration::getLogicTable).collect(Collectors.toSet()));
        return result;
    }
    
    private Predicate<String> distinct() {
        Collection<String> tables = new HashSet<>();
        return table -> notEquals(table, tables);
    }
    
    private boolean notEquals(final String table, final Collection<String> tables) {
        for (String each : tables) {
            if (table.equals(each) || (table.length() == each.length() && Splitter.on(",").splitToList(each).containsAll(Splitter.on(",").splitToList(table)))) {
                return true;
            }
        }
        tables.add(table);
        return false;
    }
    
    @Override
    public String getType() {
        return AlterShardingBindingTableRulesStatement.class.getCanonicalName();
    }
}
