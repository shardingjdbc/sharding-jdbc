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

package org.apache.shardingsphere.sharding.rewrite.token.pojo;

import lombok.Getter;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.infra.rewrite.sql.token.pojo.generic.InsertValue;

import java.util.Collection;
import java.util.List;

/**
 * Insert value for sharding.
 */
@Getter
public final class ShardingInsertValue extends InsertValue {
    
    private final Collection<DataNode> dataNodes;
    
    public ShardingInsertValue(final List<ExpressionSegment> values, final Collection<DataNode> dataNodes) {
        super(values);
        this.dataNodes = dataNodes;
    }
}
