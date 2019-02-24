/*
 * Copyright (C) 2018 StarChart-Labs@github.com Authors
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.starchartlabs.calamari.core.auth;

import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyPair;
import java.security.Security;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.starchartlabs.alloy.core.Strings;
import org.starchartlabs.alloy.core.Suppliers;
import org.starchartlabs.calamari.core.exception.KeyLoadingException;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Represents an authentication key used to validate web requests to GitHub as a
 * <a href="https://developer.github.com/apps/">GitHub App</a>
 *
 * <p>
 * Handles logic for reading a private (signing) key, signing a JWT token, and caching that token until it has expired,
 * as described in <a href=
 * "https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-a-github-app">authenticating
 * as a GitHub App</a>
 *
 * <p>
 * Uses Java {@link Supplier} pattern to allow re-generation of tokens as needed
 *
 * @author romeara
 * @since 0.1.0
 */
public class ApplicationKey implements Supplier<String> {

    // The maximum is 10, include a tolerance to reduce caching error potential
    private static final int EXPIRATION_MINUTES = 9;

    // Add security provider required for reading and using the private key which signed tokens
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final String githubAppId;

    private final Supplier<String> privateKeySupplier;

    private final Supplier<String> headerSupplier;

    /**
     * @param githubAppId
     *            Unique identifier provided by GitHub for the App
     * @param privateKeySupplier
     *            Supplier which allows lookup of the private (signing) key issued by GitHub for creating JWT tokens
     *            used in web requests
     * @since 0.1.0
     */
    public ApplicationKey(String githubAppId, Supplier<String> privateKeySupplier) {
        this.githubAppId = Objects.requireNonNull(githubAppId);
        this.privateKeySupplier = Objects.requireNonNull(privateKeySupplier);

        this.headerSupplier = Suppliers.map(
                Suppliers.memoizeWithExpiration(this::generateNewPayload, EXPIRATION_MINUTES, TimeUnit.MINUTES),
                ApplicationKey::toAuthorizationHeader);
    }

    /**
     * @return Authorization header value to authenticate as a GitHub App
     * @throws KeyLoadingException
     *             If the is an error reading the signing key prior to use
     * @since 0.1.0
     */
    @Override
    public String get() throws KeyLoadingException {
        return headerSupplier.get();
    }

    /**
     * Generates a new JWT token from the private (signing) key reference and application ID
     *
     * <p>
     * Some conversion logic is based on discussion on <a href=
     * "https://stackoverflow.com/questions/22920131/read-an-encrypted-private-key-with-bouncycastle-spongycastle">StackOverflow</a>
     *
     * @return Generated JWT valid for up to ten minutes after this function is called
     * @throws KeyLoadingException
     *             If the is an error reading the signing key prior to use
     */
    private String generateNewPayload() throws KeyLoadingException {
        String privateKey = privateKeySupplier.get();

        try (PEMParser r = new PEMParser(new StringReader(privateKey))) {
            PEMKeyPair pemKeyPair = Optional.ofNullable((PEMKeyPair) r.readObject())
                    .orElseThrow(() -> new KeyLoadingException(
                            "Unable to parse valid private key data from provided content"));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            KeyPair keyPair = converter.getKeyPair(pemKeyPair);

            Key key = keyPair.getPrivate();

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime expiration = now.plusMinutes(Math.min(EXPIRATION_MINUTES + 1, 10));

            JwtBuilder builder = Jwts.builder().setId(null)
                    .setIssuedAt(toDate(now))
                    .setExpiration(toDate(expiration))
                    .setIssuer(githubAppId)
                    .signWith(key, SignatureAlgorithm.RS256);

            return builder.compact();
        } catch (IOException e) {
            throw new KeyLoadingException("Error reading signing key", e);
        }
    }

    /**
     * @param jwt
     *            JWT token to use in requests to GitHub for authorization as a GitHub App
     * @return Value for the {@code Authorization} header of HTTP requests to authorize with the application key
     */
    private static String toAuthorizationHeader(String jwt) {
        Objects.requireNonNull(jwt);

        return Strings.format("Bearer %s", jwt);
    }

    /**
     * @param input
     *            A {@link ZonedDateTime} representation to convert to a legacy representation
     * @return A legacy {@link Date} representation for use with a JWT token builder
     */
    private static Date toDate(ZonedDateTime input) {
        Objects.requireNonNull(input);
        Instant instant = input.toInstant();

        return new Date(instant.toEpochMilli());
    }

}
