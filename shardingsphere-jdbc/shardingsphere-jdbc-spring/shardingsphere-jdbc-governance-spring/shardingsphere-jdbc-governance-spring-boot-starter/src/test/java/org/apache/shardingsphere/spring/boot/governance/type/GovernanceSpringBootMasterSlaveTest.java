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

package org.apache.shardingsphere.spring.boot.governance.type;

import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.shardingsphere.driver.governance.internal.datasource.GovernanceShardingSphereDataSource;
import org.apache.shardingsphere.infra.context.schema.SchemaContexts;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.replication.primaryreplica.rule.MasterSlaveDataSourceRule;
import org.apache.shardingsphere.replication.primaryreplica.rule.MasterSlaveRule;
import org.apache.shardingsphere.spring.boot.governance.util.EmbedTestingServer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = GovernanceSpringBootMasterSlaveTest.class)
@SpringBootApplication
@ActiveProfiles("masterslave")
public class GovernanceSpringBootMasterSlaveTest {
    
    @Resource
    private DataSource dataSource;
    
    @BeforeClass
    public static void init() {
        EmbedTestingServer.start();
    }
    
    @Test
    @SneakyThrows(ReflectiveOperationException.class)
    public void assertDataSource() {
        assertTrue(dataSource instanceof GovernanceShardingSphereDataSource);
        Field field = GovernanceShardingSphereDataSource.class.getDeclaredField("schemaContexts");
        field.setAccessible(true);
        SchemaContexts schemaContexts = (SchemaContexts) field.get(dataSource);
        for (DataSource each : schemaContexts.getDefaultSchemaContext().getSchema().getDataSources().values()) {
            assertThat(((BasicDataSource) each).getMaxTotal(), is(16));
            assertThat(((BasicDataSource) each).getUsername(), is("sa"));
        }
        Collection<ShardingSphereRule> rules = schemaContexts.getDefaultSchemaContext().getSchema().getRules();
        assertThat(rules.size(), is(1));
        assertMasterSlaveRule((MasterSlaveRule) rules.iterator().next());
    }
    
    private void assertMasterSlaveRule(final MasterSlaveRule rule) {
        MasterSlaveDataSourceRule dataSourceRule = rule.getSingleDataSourceRule();
        assertThat(dataSourceRule.getName(), is("ds_ms"));
        assertThat(dataSourceRule.getName(), is("ds_ms"));
        assertThat(dataSourceRule.getMasterDataSourceName(), is("ds_master"));
        assertThat(dataSourceRule.getSlaveDataSourceNames().size(), is(2));
        assertThat(dataSourceRule.getSlaveDataSourceNames().get(0), is("ds_slave_0"));
        assertThat(dataSourceRule.getSlaveDataSourceNames().get(1), is("ds_slave_1"));
    }
}
