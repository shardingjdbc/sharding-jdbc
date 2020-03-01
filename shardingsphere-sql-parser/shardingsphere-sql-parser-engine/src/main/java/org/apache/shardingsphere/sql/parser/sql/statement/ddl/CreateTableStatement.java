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

package org.apache.shardingsphere.sql.parser.sql.statement.ddl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.sql.parser.sql.segment.ddl.column.ColumnDefinitionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.ddl.constraint.ConstraintDefinitionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.ddl.index.IndexSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.generic.IndexSegmentsAvailable;
import org.apache.shardingsphere.sql.parser.sql.statement.generic.TableSegmentsAvailable;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Create table statement.
 */
@RequiredArgsConstructor
@Getter
public final class CreateTableStatement extends DDLStatement implements TableSegmentsAvailable, IndexSegmentsAvailable {
    
    private final TableSegment table;
    
    private final Collection<ColumnDefinitionSegment> columnDefinitions = new LinkedList<>();
    
    private final Collection<ConstraintDefinitionSegment> constraintDefinitions = new LinkedList<>();
    
    private final Collection<IndexSegment> indexes = new LinkedList<>();
    
    @Override
    public Collection<TableSegment> getAllTables() {
        Collection<TableSegment> result = new LinkedList<>();
        result.add(table);
        for (ColumnDefinitionSegment each : columnDefinitions) {
            result.addAll(each.getReferencedTables());
        }
        for (ConstraintDefinitionSegment each : constraintDefinitions) {
            if (each.getReferencedTable().isPresent()) {
                result.add(each.getReferencedTable().get());
            }
        }
        return result;
    }
    
    @Override
    public Object getTablesContext() {
        return null;
    }
}
