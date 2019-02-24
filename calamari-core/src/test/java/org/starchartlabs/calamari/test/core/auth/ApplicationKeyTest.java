/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.test.core.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.starchartlabs.calamari.core.auth.ApplicationKey;
import org.starchartlabs.calamari.core.exception.KeyLoadingException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.gson.Gson;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;

public class ApplicationKeyTest {

    private static final Path TEST_RESOURCE_FOLDER = Paths.get("org", "starchartlabs", "calamari", "test", "core",
            "auth");

    private String privateKey;

    private String invalidKey;

    @BeforeClass
    public void readKeys() {
        privateKey = readPrivateKey();
        invalidKey = readInvalidPrivateKey();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullGitHubAppId() throws Exception {
        new ApplicationKey(null, () -> "string");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void constructNullPrivateKeySupplier() throws Exception {
        new ApplicationKey("gitHubAppId", null);
    }

    @Test(expectedExceptions = KeyLoadingException.class)
    public void getInvalidPrivateKey() throws Exception {
        ApplicationKey key = new ApplicationKey("gitHubAppId", () -> invalidKey);

        key.get();
    }

    @Test(expectedExceptions = KeyLoadingException.class)
    public void getUnparsablePrivateKey() throws Exception {
        ApplicationKey key = new ApplicationKey("gitHubAppId", () -> "notAPem");

        key.get();
    }

    @Test
    public void get() throws Exception {
        ApplicationKey key = new ApplicationKey("gitHubAppId", () -> privateKey);

        String result = key.get();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.startsWith("Bearer "));

        String jwt = result.substring("Bearer ".length());

        try (PEMParser r = new PEMParser(new StringReader(privateKey))) {
            PEMKeyPair pemKeyPair = Optional.ofNullable((PEMKeyPair) r.readObject())
                    .orElseThrow(() -> new KeyLoadingException(
                            "Unable to parse valid private key data from provided content"));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            KeyPair keyPair = converter.getKeyPair(pemKeyPair);
            Key publicKey = keyPair.getPublic();

            Jws<Claims> generatedKey = Jwts.parser()
                    .deserializeJsonWith(new GsonDeserializer<>())
                    .setSigningKey(publicKey)
                    .parseClaimsJws(jwt);

            Claims claims = generatedKey.getBody();

            Assert.assertEquals(claims.getIssuer(), "gitHubAppId");
            Assert.assertNotNull(claims.getIssuedAt());
            Assert.assertNotNull(claims.getExpiration());
            Assert.assertTrue(claims.getIssuedAt().before(claims.getExpiration()));
        }
    }

    @Test
    public void getCached() throws Exception {
        CountingSupplier<String> privateKeySupplier = new CountingSupplier<>(() -> privateKey);
        ApplicationKey key = new ApplicationKey("gitHubAppId", privateKeySupplier);

        for (int i = 0; i < 10; i++) {
            Assert.assertNotNull(key.get());
        }

        // The cache time is 9-10 minutes - getting 10 keys should only require reading the private key once, for the
        // first cached call
        Assert.assertEquals(privateKeySupplier.getCount(), 1);
    }

    private String readPrivateKey() {
        // Note: The test key was generated from a GitHub App, and immediately removed as a valid key, and so is not a
        // security issue
        try (BufferedReader reader = getClasspathReader(
                TEST_RESOURCE_FOLDER.resolve("orphaned-github-private-key.pem"))) {
            String key = reader.lines()
                    .collect(Collectors.joining("\n"));
            System.out.println(key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readInvalidPrivateKey() {
        // Note: The test key was generated from a GitHub App, and immediately removed as a valid key, and so is not a
        // security issue
        try (BufferedReader reader = getClasspathReader(
                TEST_RESOURCE_FOLDER.resolve("invalid-github-private-key.pem"))) {
            String key = reader.lines()
                    .collect(Collectors.joining("\n"));
            System.out.println(key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedReader getClasspathReader(Path filePath) {
        return new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath.toString()),
                        StandardCharsets.UTF_8));
    }

    private static class CountingSupplier<T> implements Supplier<T> {

        private final Supplier<T> delegate;

        private int count;

        public CountingSupplier(Supplier<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate);
            count = 0;
        }

        @Override
        public T get() {
            count++;
            return delegate.get();
        }

        public int getCount() {
            return count;
        }

    }

    private static class GsonDeserializer<T> implements Deserializer<T> {

        private final Class<T> returnType;

        private final Gson gson;

        @SuppressWarnings("unchecked")
        public GsonDeserializer() {
            this.gson = new Gson();
            this.returnType = (Class<T>) Object.class;
        }

        @Override
        public T deserialize(byte[] bytes) throws DeserializationException {
            try {
                return readValue(bytes);
            } catch (Exception e) {
                String msg = "Unable to deserialize bytes into a " + returnType.getName() + " instance: "
                        + e.getMessage();
                throw new DeserializationException(msg, e);
            }
        }

        protected T readValue(byte[] bytes) throws IOException {
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), returnType);
        }
    }

}
