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

package org.apache.shardingsphere.proxy.backend.text.distsql.rql.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.properties.PropertiesConverter;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.text.distsql.rql.RuleQueryResultSet;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.distsql.parser.statement.ShowReadwriteSplittingRulesStatement;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Result set for show readwrite splitting rule.
 */
public final class ReadwriteSplittingRuleQueryResultSet implements RuleQueryResultSet<ShowReadwriteSplittingRulesStatement> {
    
    private Iterator<ReadwriteSplittingDataSourceRuleConfiguration> data;
    
    private Map<String, ShardingSphereAlgorithmConfiguration> loadBalancers;
    
    @Override
    public void init(final String schemaName, final ShowReadwriteSplittingRulesStatement sqlStatement) {
        Optional<ReadwriteSplittingRuleConfiguration> ruleConfig = ProxyContext.getInstance().getMetaData(schemaName).getRuleMetaData().getConfigurations()
                .stream().filter(each -> each instanceof ReadwriteSplittingRuleConfiguration).map(each -> (ReadwriteSplittingRuleConfiguration) each).findAny();
        data = ruleConfig.map(optional -> optional.getDataSources().iterator()).orElse(Collections.emptyIterator());
        loadBalancers = ruleConfig.map(ReadwriteSplittingRuleConfiguration::getLoadBalancers).orElse(Maps.newHashMap());
    }
    
    @Override
    public Collection<String> getColumnNames() {
        return Arrays.asList("name", "autoAwareDataSourceName", "writeDataSourceName", "readDataSourceNames", "loadBalancerType", "loadBalancerProps");
    }
    
    @Override
    public boolean next() {
        return data.hasNext();
    }
    
    @Override
    public Collection<Object> getRowData() {
        ReadwriteSplittingDataSourceRuleConfiguration ruleConfig = data.next();
        return Arrays.asList(ruleConfig.getName(), ruleConfig.getAutoAwareDataSourceName(), ruleConfig.getWriteDataSourceName(), Joiner.on(",").join(ruleConfig.getReadDataSourceNames()),
                null == loadBalancers.get(ruleConfig.getLoadBalancerName()) ? null : loadBalancers.get(ruleConfig.getLoadBalancerName()).getType(),
                PropertiesConverter.convert(loadBalancers.get(ruleConfig.getLoadBalancerName()).getProps()));
    }
}
