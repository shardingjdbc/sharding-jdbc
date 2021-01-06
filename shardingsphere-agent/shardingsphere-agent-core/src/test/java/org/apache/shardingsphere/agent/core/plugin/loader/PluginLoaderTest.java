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

package org.apache.shardingsphere.agent.core.plugin.loader;

import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.apache.shardingsphere.agent.api.point.PluginInterceptorPoint;
import org.apache.shardingsphere.agent.core.mock.advice.MockConstructor;
import org.apache.shardingsphere.agent.core.mock.advice.MockMethodAroundAdvice;
import org.apache.shardingsphere.agent.core.mock.advice.MockStaticMethodAroundAdvice;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldReader;
import org.mockito.internal.util.reflection.FieldSetter;

import java.util.Map;

public class PluginLoaderTest {
    
    private static final PluginLoader PLUGIN_LOADER = PluginLoader.getInstance();
    
    private static final TypePool POOL = TypePool.Default.ofSystemLoader();
    
    private static final TypeDescription FAKE = POOL.describe("java.lang.String").resolve();
    
    private static final TypeDescription MATERIAL = POOL.describe("org.apache.shardingsphere.agent.core.mock.Material").resolve();
    
    @BeforeClass
    @SneakyThrows
    public static void setup() {
        FieldReader objectPoolReader = new FieldReader(PLUGIN_LOADER, PLUGIN_LOADER.getClass().getDeclaredField("objectPool"));
        Map<String, Object> objectPool = (Map<String, Object>) objectPoolReader.read();
        objectPool.put(MockConstructor.class.getTypeName(), new MockConstructor());
        objectPool.put(MockMethodAroundAdvice.class.getTypeName(), new MockMethodAroundAdvice());
        objectPool.put(MockStaticMethodAroundAdvice.class.getTypeName(), new MockStaticMethodAroundAdvice());
        Map<String, PluginInterceptorPoint> interceptorPointMap = Maps.newHashMap();
        PluginInterceptorPoint interceptorPoint = PluginInterceptorPoint.intercept("org.apache.shardingsphere.agent.core.mock.Material")
                .aroundInstanceMethod(ElementMatchers.named("mock"))
                .implement(MockMethodAroundAdvice.class.getTypeName())
                .build()
                .aroundClassStaticMethod(ElementMatchers.named("staticMock"))
                .implement(MockStaticMethodAroundAdvice.class.getTypeName())
                .build()
                .onConstructor(ElementMatchers.takesArguments(1))
                .implement(MockConstructor.class.getTypeName())
                .build()
                .install();
        interceptorPointMap.put(interceptorPoint.getClassNameOfTarget(), interceptorPoint);
        FieldSetter.setField(PLUGIN_LOADER, PLUGIN_LOADER.getClass().getDeclaredField("interceptorPointMap"), interceptorPointMap);
    }
    
    @Test
    public void assertTypeMatcher() {
        Assert.assertThat(PLUGIN_LOADER.typeMatcher().matches(MATERIAL), Matchers.is(true));
        Assert.assertThat(PLUGIN_LOADER.typeMatcher().matches(FAKE), Matchers.is(false));
    }
    
    @Test
    public void assertContainsType() {
        Assert.assertThat(PLUGIN_LOADER.containsType(MATERIAL), Matchers.is(true));
        Assert.assertThat(PLUGIN_LOADER.containsType(FAKE), Matchers.is(false));
    }
    
    @Test
    public void assertLoadPluginInterceptorPoint() {
        Assert.assertNull(PLUGIN_LOADER.loadPluginInterceptorPoint(FAKE));
        Assert.assertNotNull(PLUGIN_LOADER.loadPluginInterceptorPoint(MATERIAL));
    }
}
