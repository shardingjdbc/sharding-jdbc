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

package org.apache.shardingsphere.sql.rewriter.feature.sharding.token.pojo.impl;

import org.apache.shardingsphere.core.route.type.RoutingUnit;
import org.apache.shardingsphere.core.rule.DataNode;
import org.apache.shardingsphere.sql.rewriter.feature.sharding.token.pojo.RoutingUnitAware;
import org.apache.shardingsphere.sql.rewriter.sql.token.pojo.generic.InsertValue;
import org.apache.shardingsphere.sql.rewriter.sql.token.pojo.generic.InsertValuesToken;

/**
 * Insert values token for sharding.
 *
 * @author zhangliang
 */
public final class ShardingInsertValuesToken extends InsertValuesToken implements RoutingUnitAware {
    
    public ShardingInsertValuesToken(final int startIndex, final int stopIndex) {
        super(startIndex, stopIndex);
    }
    
    @Override
    public String toString(final RoutingUnit routingUnit) {
        StringBuilder result = new StringBuilder();
        appendInsertValue(routingUnit, result);
        result.delete(result.length() - 2, result.length());
        return result.toString();
    }
    
    private void appendInsertValue(final RoutingUnit routingUnit, final StringBuilder stringBuilder) {
        for (InsertValue each : getInsertValues()) {
            if (isAppend(routingUnit, each)) {
                stringBuilder.append(each).append(", ");
            }
        }
    }
    
    private boolean isAppend(final RoutingUnit routingUnit, final InsertValue insertValueToken) {
        if (insertValueToken.getDataNodes().isEmpty() || null == routingUnit) {
            return true;
        }
        for (DataNode each : insertValueToken.getDataNodes()) {
            if (routingUnit.getTableUnit(each.getDataSourceName(), each.getTableName()).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
