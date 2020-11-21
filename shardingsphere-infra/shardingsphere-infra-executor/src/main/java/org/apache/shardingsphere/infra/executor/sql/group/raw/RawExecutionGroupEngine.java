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

package org.apache.shardingsphere.infra.executor.sql.group.raw;

import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroup;
import org.apache.shardingsphere.infra.executor.sql.ConnectionMode;
import org.apache.shardingsphere.infra.executor.sql.context.ExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.context.SQLUnit;
import org.apache.shardingsphere.infra.executor.sql.group.AbstractExecutionGroupEngine;
import org.apache.shardingsphere.infra.executor.sql.group.SQLUintGroupResult;
import org.apache.shardingsphere.infra.executor.sql.raw.RawSQLExecuteUnit;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Raw execution group engine.
 */
public final class RawExecutionGroupEngine extends AbstractExecutionGroupEngine<RawSQLExecuteUnit> {
    
    public RawExecutionGroupEngine(final int maxConnectionsSizePerQuery, final Collection<ShardingSphereRule> rules) {
        super(maxConnectionsSizePerQuery, rules);
    }
    
    @Override
    protected List<ExecutionGroup<RawSQLExecuteUnit>> group(final String dataSourceName, final List<SQLUnit> sqlUnits) {
        List<ExecutionGroup<RawSQLExecuteUnit>> result = new LinkedList<>();
        SQLUintGroupResult sqlUintGroupResult = createSQLUintGroupResult(sqlUnits);
        for (List<SQLUnit> each : sqlUintGroupResult.getSqlUnitGroups()) {
            result.add(createSQLExecutionGroup(dataSourceName, each, sqlUintGroupResult.getConnectionMode()));
        }
        return result;
    }
    
    private ExecutionGroup<RawSQLExecuteUnit> createSQLExecutionGroup(final String dataSourceName, final List<SQLUnit> sqlUnitGroup, final ConnectionMode connectionMode) {
        List<RawSQLExecuteUnit> rawSQLExecuteUnits = new LinkedList<>();
        for (SQLUnit each : sqlUnitGroup) {
            rawSQLExecuteUnits.add(new RawSQLExecuteUnit(new ExecutionUnit(dataSourceName, each), connectionMode));
        }
        return new ExecutionGroup<>(rawSQLExecuteUnits);
    }
}
