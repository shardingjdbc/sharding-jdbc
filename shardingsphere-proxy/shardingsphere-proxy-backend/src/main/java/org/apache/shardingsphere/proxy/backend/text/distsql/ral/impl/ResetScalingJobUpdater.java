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

package org.apache.shardingsphere.proxy.backend.text.distsql.ral.impl;

import org.apache.shardingsphere.proxy.backend.exception.ScalingJobOperateException;
import org.apache.shardingsphere.infra.distsql.update.RALUpdater;
import org.apache.shardingsphere.scaling.core.api.ScalingAPI;
import org.apache.shardingsphere.scaling.core.api.ScalingAPIFactory;
import org.apache.shardingsphere.scaling.distsql.statement.ResetScalingJobStatement;

import java.sql.SQLException;

/**
 * Reset scaling job updater.
 */
public final class ResetScalingJobUpdater implements RALUpdater<ResetScalingJobStatement> {
    
    private final ScalingAPI scalingAPI = ScalingAPIFactory.getScalingAPI();
    
    @Override
    public void executeUpdate(final ResetScalingJobStatement sqlStatement) {
        try {
            scalingAPI.reset(sqlStatement.getJobId());
        } catch (final SQLException ex) {
            throw new ScalingJobOperateException(ex.getMessage());
        }
    }
    
    @Override
    public String getType() {
        return ResetScalingJobStatement.class.getCanonicalName();
    }
}