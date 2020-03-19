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

package org.apache.shardingsphere.underlying.route.context;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class RouteUnitTest {
    
    private static final String DATASOURCE_NAME = "ds";
    
    private static final String LOGIC_TABLE = "table";
    
    private static final String SHARD_TABLE_0 = "table_0";
    
    private static final String SHARD_TABLE_1 = "table_1";
    
    private RouteUnit routeUnit;
    
    @Before
    public void setUp() {
        routeUnit = new RouteUnit(DATASOURCE_NAME);
        routeUnit.getTableMappers().addAll(mockTableMappers());
    }
    
    private Collection<RouteMapper> mockTableMappers() {
        List<RouteMapper> result = new ArrayList<>();
        result.add(new RouteMapper(LOGIC_TABLE, SHARD_TABLE_0));
        result.add(new RouteMapper(LOGIC_TABLE, SHARD_TABLE_1));
        return result;
    }
    
    @Test
    public void assertGetTableMapper() {
        Optional<RouteMapper> actual = routeUnit.getTableMapper(DATASOURCE_NAME, SHARD_TABLE_0);
        assertTrue(actual.isPresent());
        assertThat(actual.get().getLogicName(), is(LOGIC_TABLE));
        assertThat(actual.get().getActualName(), is(SHARD_TABLE_0));
    }
    
    @Test
    public void assertGetTableMapperNonExist() {
        Optional<RouteMapper> actual = routeUnit.getTableMapper(DATASOURCE_NAME, "");
        assertFalse(actual.isPresent());
    }
    
    @Test
    public void assertGetActualTableNames() {
        Set<String> actual = routeUnit.getActualTableNames(LOGIC_TABLE);
        assertThat(actual.size(), is(2));
        assertTrue(actual.contains(SHARD_TABLE_0));
        assertTrue(actual.contains(SHARD_TABLE_1));
    }
    
    @Test
    public void assertGetLogicTableNames() {
        Set<String> actual = routeUnit.getLogicTableNames();
        assertThat(actual.size(), is(1));
        assertTrue(actual.contains(LOGIC_TABLE));
    }
    
    @Test
    public void assertGetDataSourceName() {
        assertThat(routeUnit.getDataSourceMapper().getActualName(), is(DATASOURCE_NAME));
    }
    
    @Test
    public void assertGetMasterSlaveLogicDataSourceName() {
        assertThat(routeUnit.getDataSourceMapper().getLogicName(), is(DATASOURCE_NAME));
    }
    
    @Test
    public void assertEquals() {
        RouteUnit expected = new RouteUnit(DATASOURCE_NAME, DATASOURCE_NAME);
        expected.getTableMappers().addAll(mockTableMappers());
        assertTrue(expected.equals(routeUnit));
    }
    
    @Test
    public void assertToString() {
        assertThat(routeUnit.toString(), is(String.format(
                "RouteUnit(dataSourceMapper=RouteMapper(logicName=%s, actualName=%s), tableMappers=[RouteMapper(logicName=%s, actualName=%s), RouteMapper(logicName=%s, actualName=%s)])", 
                DATASOURCE_NAME, DATASOURCE_NAME, LOGIC_TABLE, SHARD_TABLE_0, LOGIC_TABLE, SHARD_TABLE_1)));
    }
}
