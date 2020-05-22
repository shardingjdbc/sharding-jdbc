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

package org.apache.shardingsphere.encrypt.strategy.impl;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shardingsphere.encrypt.strategy.spi.Encryptor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

/**
 * AES encryptor.
 */
@Getter
@Setter
public final class AESEncryptor implements Encryptor {

    private static final String AES_KEY = "aes.key.value";
    private static final String ENCRYPTOR_TYPE = "AES";
    private Properties properties = new Properties();
    private static Cipher encryptCipher = null;
    private static Cipher decryptCipher = null;

    @Override
    public String getType() {
        return ENCRYPTOR_TYPE;
    }

    @Override
    @SneakyThrows
    public void init() {
        Preconditions.checkArgument(properties.containsKey(AES_KEY), "No available secret key for `%s`.", AESEncryptor.class.getName());
        SecretKeySpec skeySpec = new SecretKeySpec(createSecretKey(), getType());
        encryptCipher = Cipher.getInstance(ENCRYPTOR_TYPE);
        encryptCipher.init(Cipher.ENCRYPT_MODE, skeySpec);

        decryptCipher = Cipher.getInstance(ENCRYPTOR_TYPE);
        decryptCipher.init(Cipher.DECRYPT_MODE, skeySpec);
    }

    @SneakyThrows
    @Override
    public String encrypt(final Object plaintext) {
        if (null == plaintext) {
            return null;
        }
        byte[] result = encryptCipher.doFinal(StringUtils.getBytesUtf8(String.valueOf(plaintext)));
        return Base64.encodeBase64String(result);
    }

    @SneakyThrows
    @Override
    public Object decrypt(final String ciphertext) {
        if (null == ciphertext) {
            return null;
        }
        byte[] result = decryptCipher.doFinal(Base64.decodeBase64(ciphertext));
        return new String(result, StandardCharsets.UTF_8);
    }

    private byte[] createSecretKey() {
        Preconditions.checkArgument(null != properties.get(AES_KEY), String.format("%s can not be null.", AES_KEY));
        return Arrays.copyOf(DigestUtils.sha1(properties.get(AES_KEY).toString()), 16);
    }
}
