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

package org.apache.shardingsphere.core.rewrite.feature.sharding.token;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.rewrite.sql.token.generator.builder.SQLTokenGeneratorBuilder;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.IgnoreForSingleRoute;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.SQLRouteResultAware;
import org.apache.shardingsphere.core.rewrite.sql.token.generator.SQLTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.ShardingRuleAware;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.AggregationDistinctTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.IndexTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.TableTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.InsertValuesTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.OffsetTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.OrderByTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.ProjectionsTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.RowCountTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.DistinctProjectionPrefixTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.keygen.GeneratedKeyAssignmentTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.keygen.GeneratedKeyForUseDefaultColumnsTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.keygen.GeneratedKeyInsertColumnTokenGenerator;
import org.apache.shardingsphere.core.rewrite.feature.sharding.token.generator.impl.keygen.GeneratedKeyInsertValuesTokenGenerator;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.apache.shardingsphere.core.rule.ShardingRule;

import java.util.Collection;
import java.util.LinkedList;

/**
 * SQL token generator builder for sharding.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class ShardingTokenGenerateBuilder implements SQLTokenGeneratorBuilder {
    
    private final ShardingRule shardingRule;
    
    private final SQLRouteResult sqlRouteResult;
    
    @Override
    public Collection<SQLTokenGenerator> getSQLTokenGenerators() {
        Collection<SQLTokenGenerator> result = buildSQLTokenGenerators();
        for (SQLTokenGenerator each : result) {
            if (each instanceof ShardingRuleAware) {
                ((ShardingRuleAware) each).setShardingRule(shardingRule);
            }
            if (each instanceof SQLRouteResultAware) {
                ((SQLRouteResultAware) each).setSqlRouteResult(sqlRouteResult);
            }
        }
        return result;
    }
    
    private Collection<SQLTokenGenerator> buildSQLTokenGenerators() {
        Collection<SQLTokenGenerator> result = new LinkedList<>();
        addSQLTokenGenerator(result, new TableTokenGenerator());
        addSQLTokenGenerator(result, new DistinctProjectionPrefixTokenGenerator());
        addSQLTokenGenerator(result, new ProjectionsTokenGenerator());
        addSQLTokenGenerator(result, new OrderByTokenGenerator());
        addSQLTokenGenerator(result, new AggregationDistinctTokenGenerator());
        addSQLTokenGenerator(result, new IndexTokenGenerator());
        addSQLTokenGenerator(result, new OffsetTokenGenerator());
        addSQLTokenGenerator(result, new RowCountTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyInsertColumnTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyForUseDefaultColumnsTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyAssignmentTokenGenerator());
        addSQLTokenGenerator(result, new InsertValuesTokenGenerator());
        addSQLTokenGenerator(result, new GeneratedKeyInsertValuesTokenGenerator());
        return result;
    }
    
    private void addSQLTokenGenerator(final Collection<SQLTokenGenerator> sqlTokenGenerators, final SQLTokenGenerator toBeAddedSQLTokenGenerator) {
        if (toBeAddedSQLTokenGenerator instanceof IgnoreForSingleRoute && sqlRouteResult.getRoutingResult().isSingleRouting()) {
            return;
        }
        sqlTokenGenerators.add(toBeAddedSQLTokenGenerator);
    }
}
