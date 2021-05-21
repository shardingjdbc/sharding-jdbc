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

package org.apache.shardingsphere.governance.core.registry.service.schema;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.governance.core.registry.RegistryCenterNode;
import org.apache.shardingsphere.governance.core.yaml.schema.pojo.YamlSchema;
import org.apache.shardingsphere.governance.core.yaml.schema.swapper.SchemaYamlSwapper;
import org.apache.shardingsphere.governance.repository.spi.RegistryCenterRepository;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.yaml.engine.YamlEngine;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Schema registry service.
 */
@RequiredArgsConstructor
public final class SchemaRegistryService {
    
    private final RegistryCenterRepository repository;
    
    private final RegistryCenterNode node = new RegistryCenterNode();
    
    /**
     * Load all schema names.
     *
     * @return all schema names
     */
    public Collection<String> loadAllSchemaNames() {
        String schemaNames = repository.get(node.getMetadataNodePath());
        return Strings.isNullOrEmpty(schemaNames) ? new LinkedList<>() : node.splitSchemaName(schemaNames);
    }
    
    /**
     * Persist ShardingSphere schema.
     *
     * @param schemaName schema name
     * @param schema ShardingSphere schema
     */
    public void persistSchema(final String schemaName, final ShardingSphereSchema schema) {
        repository.persist(node.getMetadataSchemaPath(schemaName), YamlEngine.marshal(new SchemaYamlSwapper().swapToYamlConfiguration(schema)));
    }
    
    /**
     * Load ShardingSphere schema.
     *
     * @param schemaName schema name
     * @return ShardingSphere schema
     */
    public Optional<ShardingSphereSchema> loadSchema(final String schemaName) {
        String path = repository.get(node.getMetadataSchemaPath(schemaName));
        if (Strings.isNullOrEmpty(path)) {
            return Optional.empty();
        }
        return Optional.of(new SchemaYamlSwapper().swapToObject(YamlEngine.unmarshal(path, YamlSchema.class)));
    }
    
    /**
     * Delete schema.
     *
     * @param schemaName schema name
     */
    public void deleteSchema(final String schemaName) {
        repository.delete(node.getSchemaNamePath(schemaName));
    }
}
