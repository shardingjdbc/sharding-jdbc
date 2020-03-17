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

package org.apache.shardingsphere.underlying.common.properties.common;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import lombok.Getter;
import org.apache.shardingsphere.underlying.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Typed properties with a specified enum.
 */
public class TypedProperties<E extends Enum & TypedPropertiesKey> {
    
    private final Class<E> keyClass;
    
    @Getter
    private final Properties props;
    
    private final Map<Enum, Object> cachedProperties = new ConcurrentHashMap<>(64, 1);
    
    public TypedProperties(final Class<E> keyClass, final Properties props) {
        this.keyClass = keyClass;
        this.props = props;
        validate();
    }
    
    private void validate() {
        Set<String> propertyNames = props.stringPropertyNames();
        Collection<String> errorMessages = new ArrayList<>(propertyNames.size());
        for (String each : propertyNames) {
            Optional<E> typedEnum = find(each);
            if (!typedEnum.isPresent()) {
                continue;
            }
            Class<?> type = typedEnum.get().getType();
            String value = props.getProperty(each);
            if (type == boolean.class && !StringUtil.isBooleanValue(value)) {
                errorMessages.add(getErrorMessage(typedEnum.get(), value));
            } else if (type == int.class && !StringUtil.isIntValue(value)) {
                errorMessages.add(getErrorMessage(typedEnum.get(), value));
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new IllegalArgumentException(Joiner.on(" ").join(errorMessages));
        }
    }
    
    private Optional<E> find(final String key) {
        for (E each : keyClass.getEnumConstants()) {
            if (each.getKey().equals(key)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }
    
    private String getErrorMessage(final E typedEnum, final String invalidValue) {
        return String.format("Value '%s' of '%s' cannot convert to type '%s'.", invalidValue, typedEnum.getKey(), typedEnum.getType().getName());
    }
    
    /**
     * Get property value.
     *
     * @param typedEnum properties constant
     * @param <T> class type of return value
     * @return property value
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(final E typedEnum) {
        if (cachedProperties.containsKey(typedEnum)) {
            return (T) cachedProperties.get(typedEnum);
        }
        String value = props.getProperty(typedEnum.getKey());
        if (Strings.isNullOrEmpty(value)) {
            Object obj = props.get(typedEnum.getKey());
            if (null == obj) {
                value = typedEnum.getDefaultValue();
            } else {
                value = obj.toString();
            }
        }
        Object result;
        if (boolean.class == typedEnum.getType()) {
            result = Boolean.valueOf(value);
        } else if (int.class == typedEnum.getType()) {
            result = Integer.valueOf(value);
        } else if (long.class == typedEnum.getType()) {
            result = Long.valueOf(value);
        } else {
            result = value;
        }
        cachedProperties.put(typedEnum, result);
        return (T) result;
    }
}
