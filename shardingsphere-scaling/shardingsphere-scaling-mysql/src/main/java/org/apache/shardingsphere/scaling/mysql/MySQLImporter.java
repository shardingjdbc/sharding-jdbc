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

package org.apache.shardingsphere.scaling.mysql;

import org.apache.shardingsphere.scaling.core.config.ImporterConfiguration;
import org.apache.shardingsphere.scaling.core.datasource.DataSourceManager;
import org.apache.shardingsphere.scaling.core.execute.executor.importer.AbstractJDBCImporter;
import org.apache.shardingsphere.scaling.core.execute.executor.importer.AbstractSqlBuilder;

/**
 * MySQL importer.
 */
public final class MySQLImporter extends AbstractJDBCImporter {
    
    public MySQLImporter(final ImporterConfiguration importerConfiguration, final DataSourceManager dataSourceManager) {
        super(importerConfiguration, dataSourceManager);
    }
    
    @Override
    protected AbstractSqlBuilder createSqlBuilder() {
        return new AbstractSqlBuilder() {
            
            @Override
            public String getLeftIdentifierQuoteString() {
                return "`";
            }
            
            @Override
            public String getRightIdentifierQuoteString() {
                return "`";
            }
        };
    }
}
