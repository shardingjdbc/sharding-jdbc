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

package org.apache.shardingsphere.authority.engine;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.authority.spi.AuthorityCheckAlgorithm;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;

import java.util.Collection;
import java.util.Optional;

/**
 * Authority engine.
*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthorityEngine {
    
    static {
        ShardingSphereServiceLoader.register(AuthorityCheckAlgorithm.class);
    }
    
    /**
     * Find SPI authority.
     * 
     * @return authority
     */
    public static Optional<AuthorityCheckAlgorithm> findSPIAuthority() {
        Collection<AuthorityCheckAlgorithm> authorities = ShardingSphereServiceLoader.newServiceInstances(AuthorityCheckAlgorithm.class);
        return authorities.isEmpty() ? Optional.empty() : Optional.of(authorities.iterator().next());
    }
}
