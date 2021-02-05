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

package org.apache.shardingsphere.test.integration.env.database.embedded;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.test.integration.env.database.embedded.type.MySQLEmbeddedDatabaseResource;

/**
 * Embedded database resource factory.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmbeddedDatabaseResourceFactory {
    
    /**
     * Create new instance of embedded database resource.
     * 
     * @param databaseType database type
     * @param embeddedDatabaseProps embedded database properties
     * @param port database access port
     * @return instance of embedded database resource
     */
    public static EmbeddedDatabaseResource newInstance(final DatabaseType databaseType, final EmbeddedDatabaseDistributionProperties embeddedDatabaseProps, final int port) {
        if (databaseType instanceof MySQLDatabaseType) {
            return new MySQLEmbeddedDatabaseResource(embeddedDatabaseProps, port);
        }
        throw new UnsupportedOperationException(String.format("Unsupported embedded database type: `%s`", databaseType.getName()));
    }
}
