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

package org.apache.shardingsphere.core.rule;

import lombok.Getter;
import org.apache.shardingsphere.api.config.masterslave.MasterSlaveGroupConfiguration;
import org.apache.shardingsphere.api.config.masterslave.MasterSlaveRuleConfiguration;
import org.apache.shardingsphere.underlying.common.rule.DataSourceRoutedRule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Master-slave rule.
 */
@Getter
public final class MasterSlaveRule implements DataSourceRoutedRule {
    
    private final Map<String, MasterSlaveDataSourceRule> dataSourceRules;
    
    public MasterSlaveRule(final MasterSlaveRuleConfiguration configuration) {
        dataSourceRules = new HashMap<>(configuration.getDataSources().size(), 1);
        for (MasterSlaveGroupConfiguration each : configuration.getDataSources()) {
            dataSourceRules.put(each.getName(), new MasterSlaveDataSourceRule(each));
        }
    }
    
    /**
     * Get slave data source names.
     *
     * @param groupName master-slave group name
     * @return available slave data source names
     */
    public List<String> getSlaveDataSourceNames(final String groupName) {
        return dataSourceRules.containsKey(groupName) ? dataSourceRules.get(groupName).getSlaveDataSourceNames() : Collections.emptyList();
    }
    
    /**
     * Update disabled data source names.
     *
     * @param dataSourceName data source name
     * @param isDisabled is disabled
     */
    public void updateDisabledDataSourceNames(final String dataSourceName, final boolean isDisabled) {
        for (Entry<String, MasterSlaveDataSourceRule> entry : dataSourceRules.entrySet()) {
            entry.getValue().updateDisabledDataSourceNames(dataSourceName, isDisabled);
        }
    }
    
    @Override
    public Map<String, Collection<String>> getDataSourceMapper() {
        Map<String, Collection<String>> result = new HashMap<>();
        for (Entry<String, MasterSlaveDataSourceRule> entry : dataSourceRules.entrySet()) {
            result.putAll(entry.getValue().getDataSourceMapper());
        }
        return result;
    }
}
