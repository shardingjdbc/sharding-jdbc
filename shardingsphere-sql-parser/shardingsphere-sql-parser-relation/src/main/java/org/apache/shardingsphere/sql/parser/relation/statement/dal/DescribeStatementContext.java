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

package org.apache.shardingsphere.sql.parser.relation.statement.dal;

import org.apache.shardingsphere.sql.parser.relation.statement.CommonSQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dal.dialect.mysql.DescribeStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.generic.TableSegmentsAvailable;

import java.util.Collection;
import java.util.Collections;

/**
 * Describe statement context.
 */
public class DescribeStatementContext extends CommonSQLStatementContext implements TableSegmentsAvailable {
    
    public DescribeStatementContext(final DescribeStatement sqlStatement) {
        super(sqlStatement);
    }
    
    @Override
    public Collection<TableSegment> getAllTables() {
        DescribeStatement describeStatement = (DescribeStatement) getSqlStatement();
        return null == describeStatement.getTable() ? Collections.<TableSegment>emptyList() : Collections.singletonList(describeStatement.getTable());
    }
}
