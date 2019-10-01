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

package org.apache.shardingsphere.core.rewrite.parameter.builder.group;

import org.apache.shardingsphere.core.rewrite.parameter.builder.ParameterBuilder;
import org.apache.shardingsphere.core.route.router.sharding.condition.ShardingCondition;
import org.apache.shardingsphere.core.route.router.sharding.condition.ShardingConditions;
import org.apache.shardingsphere.core.route.type.RoutingUnit;
import org.apache.shardingsphere.core.rule.DataNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Grouped Parameter builder.
 *
 * @author panjuan
 * @author zhangliang
 */
public final class GroupedParameterBuilder implements ParameterBuilder {
    
    private final Collection<ParametersGroup> parametersGroups;
    
    private final ShardingConditions shardingConditions;
    
    public GroupedParameterBuilder(final List<List<Object>> rewritedGroupedParameters, final ShardingConditions shardingConditions) {
        parametersGroups = createParametersGroup(rewritedGroupedParameters);
        this.shardingConditions = shardingConditions;
    }
    
    private Collection<ParametersGroup> createParametersGroup(final List<List<Object>> rewritedGroupedParameters) {
        Collection<ParametersGroup> result = new LinkedList<>();
        for (List<Object> each : rewritedGroupedParameters) {
            result.add(new ParametersGroup(each));
        }
        return result;
    }
    
    @Override
    public List<Object> getParameters(final List<Object> originalParameters) {
        List<Object> result = new LinkedList<>();
        for (ParametersGroup each : parametersGroups) {
            result.addAll(each.getParameters());
        }
        return result;
    }
    
    @Override
    public List<Object> getParameters(final List<Object> originalParameters, final RoutingUnit routingUnit) {
        List<Object> result = new LinkedList<>();
        Iterator<ShardingCondition> shardingConditionIterator = null == shardingConditions ? null : shardingConditions.getConditions().iterator();
        for (ParametersGroup each : parametersGroups) {
            if (null == shardingConditionIterator || isInSameDataNode(shardingConditionIterator.next(), routingUnit)) {
                result.addAll(each.getParameters());
            }
        }
        return result;
    }
    
    private boolean isInSameDataNode(final ShardingCondition shardingCondition, final RoutingUnit routingUnit) {
        for (DataNode each : shardingCondition.getDataNodes()) {
            if (routingUnit.getTableUnit(each.getDataSourceName(), each.getTableName()).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
