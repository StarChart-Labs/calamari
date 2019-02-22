# Calamari

[![Travis CI](https://img.shields.io/travis/StarChart-Labs/calamari.svg?branch=master)](https://travis-ci.org/StarChart-Labs/calamari) [![codecov](https://codecov.io/gh/StarChart-Labs/calamari/branch/master/graph/badge.svg)](https://codecov.io/gh/StarChart-Labs/calamari)

Octo-App(itizers): Utilities for building GitHub Apps

## Importing

Calamari is available on JCenter and Maven Central, but requires the BouncyCastle library for certain operations.

Due to a security patch, this currently requires that consuming projects also allow `https://maven.repository.redhat.com/ga/` as a maven repository source in build configuration

## Contributing

Information for how to contribute to Calamari can be found in [the contribution guidelines](./CONTRIBUTING.md)

## Legal

Calamari is distributed under the [MIT License](https://opensource.org/licenses/MIT). There are no requirements for using it in your own project (a line in a NOTICES file is appreciated but not necessary for use)

The requirement for a copy of the license being included in distributions is fulfilled by a copy of the [LICENSE](./LICENSE) file being included in constructed JAR archives

## Reporting Vulnerabilities

If you discover a security vulnerability, contact the development team by e-mail at `vulnerabilities@starchartlabs.org`

## Projects

### calamari-core

[![Maven Central](https://img.shields.io/maven-central/v/org.starchartlabs.calamari/calamari-core.svg)](https://mvnrepository.com/artifact/org.starchartlabs.calamari/calamari-core)

Contains utilities for operations expected to be necessary for the vast majority of GitHub App implementations, such as authentication
