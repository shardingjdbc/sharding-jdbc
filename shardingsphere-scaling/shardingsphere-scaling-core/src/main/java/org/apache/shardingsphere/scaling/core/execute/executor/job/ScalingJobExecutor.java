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

package org.apache.shardingsphere.scaling.core.execute.executor.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.infra.pojo.JobConfigurationPOJO;
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.OneOffJobBootstrap;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent;
import org.apache.shardingsphere.infra.yaml.engine.YamlEngine;
import org.apache.shardingsphere.scaling.core.api.JobSchedulerCenter;
import org.apache.shardingsphere.scaling.core.api.ScalingAPIFactory;
import org.apache.shardingsphere.scaling.core.constant.ScalingConstant;
import org.apache.shardingsphere.scaling.core.execute.executor.AbstractScalingExecutor;
import org.apache.shardingsphere.scaling.core.execute.executor.ScalingExecutor;
import org.apache.shardingsphere.scaling.core.job.ScalingJob;
import org.apache.shardingsphere.scaling.core.utils.ElasticJobUtil;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Scaling job executor.
 */
@Slf4j
public final class ScalingJobExecutor extends AbstractScalingExecutor implements ScalingExecutor {
    
    private static final Pattern CONFIG_PATTERN = Pattern.compile(ScalingConstant.SCALING_ROOT + "/(\\d+)/config");
    
    @Override
    public void start() {
        super.start();
        log.info("Start scaling job executor.");
        watchRegistryRepositoryConfig();
    }
    
    private void watchRegistryRepositoryConfig() {
        ScalingAPIFactory.getRegistryRepositoryAPI().watch(ScalingConstant.SCALING_ROOT, event -> {
            Optional<JobConfigurationPOJO> jobConfigPOJOOptional = getJobConfigPOJO(event);
            if (!jobConfigPOJOOptional.isPresent()) {
                return;
            }
            JobConfigurationPOJO jobConfigPOJO = jobConfigPOJOOptional.get();
            if (DataChangedEvent.Type.DELETED.equals(event.getType()) || jobConfigPOJO.isDisabled()) {
                JobSchedulerCenter.stop(Long.parseLong(jobConfigPOJO.getJobName()));
                return;
            }
            switch (event.getType()) {
                case ADDED:
                case UPDATED:
                    new OneOffJobBootstrap(ElasticJobUtil.createRegistryCenter(), new ScalingJob(), jobConfigPOJO.toJobConfiguration()).execute();
                    break;
                default:
                    break;
            }
        });
    }
    
    private Optional<JobConfigurationPOJO> getJobConfigPOJO(final DataChangedEvent event) {
        try {
            if (CONFIG_PATTERN.matcher(event.getKey()).matches()) {
                log.info("{} job config: {} = {}", event.getType(), event.getKey(), event.getValue());
                return Optional.of(YamlEngine.unmarshal(event.getValue(), JobConfigurationPOJO.class));
            }
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.error("analyze job config pojo failed.", ex);
        }
        return Optional.empty();
    }
}
