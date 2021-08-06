# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

## [1.0.1]
### Changed
- Updated com.google.code.gson:gson from 2.8.6 to 2.8.7
- Updated com.squareup.okhttp3:mockwebserver from 4.8.0 to 4.9.1
- Updated com.squareup.okhttp3:okhttp from 4.8.0 to 4.9.1
- Updated commons-codec:commons-codec from 1.14 to 1.15
- Removed override of ant version and updated org.testng:testng from 7.3.0 to 7.4.0
- Updated org.bouncycastle:bcpkix-jdk15on from 1.66 to 1.69
- Updated org.bouncycastle:bcprov-jdk15on from 1.66 to 1.69
- Updated org.starchartlabs.alloy:alloy-core from 1.0.0 to 1.0.1
- Updated org.mockito:mockito-core from 3.3.3 to 3.11.2
- Updated org.slf4j:slf4j-api from 1.7.30 to 1.7.32
- Updated org.slf4j:slf4j-simple from 1.7.30 to 1.7.32

## [1.0.0]
### Changed
- (GH-37) Update OkHttp to newest major version, 4.8.0
- (GH-20) Update JJWT to 0.11.2 to allow removal of custom GSON serialization handling
- Update Commons-Codex from 1.13 to 1.14
- Update Bouncycastle bcpkix-jdk15on and bcprov-jdk15on from 1.64 to 1.66
- Update alloy-core from 0.5.0 to 1.0.0
- Update Mockito from 2.28.2 to 3.3.3
- Update SLF4J from 1.7.29 to 1.7.30
- Update TestNG from 6.14.3 to 7.3.0

## [0.4.1]
### Changed
- Updated io.jsonwebtoken:jjwt-api and io.jsonwebtoken:jjwt-impl to 0.10.7 from 0.10.6
- Updated org.bouncycastle:bcpkix-jdk15on and org.bouncycastle:bcprove-jdk15on to 1.64 from 1.63
- Updated org.slf4j:slf4j-api and org.slf4j:slf4j-simple to 1.7.29 from 1.7.28

## [0.4.0]
### Changed
- Add missing handling for HTTP response body in InstallationAccessToken
- Added Checkstyle tooling and fixed one existing formatting issue
- (GH-34) Reduce default cache/expiration times for access/installation tokens to reduce possible clock errors with GitHub handshake
- (GH-34) Add ability to configure cache expiration time for access and installation tokens
- Update dependencies to latest minor/micro revisions. Major revision updates will wait until a major revision of this library

## [0.3.3]
### Changed
- GH-23 Switched to using constant-time `MessageDigest.isEqual(byte[], byte[])` to reduce effectiveness of timing attacks in code using WebhookVerifier

## [0.3.2]
### Changed
- Updated external dependency versions to latest bugfix releases

## [0.3.1]
### Changed
- GH-17 Update to more recent bouncycastle library iterations, and switch to PEMParser from PEMReader
- Update JJWT version, and switch to separated compile/runtime dependency setup
- Remove dependency on JJWT jackson, replace with local GSON serialization

## [0.3.0]
### Added
- Added org.starchartlabs.calamari.core.content.FileContentReader for reading configuration file contents from GitHub
- Added ability to override media type used in InstallationAccessToken requests
- Added org.starchartlabs.calamari.core.paging.PagingLinks to represent GitHub paging links
- Added org.starchartlabs.calamari.core.paging.GitHubPageIterator<T> to allow traversal of GitHub paged data

## [0.2.1]
### Fixed
- Updated InstallationAccessToken.generateNewToken to close OkHttp response body

## [0.2.0]
### Added
- Add InstallationAccessToken.getInstallationUrl to allow more complex client caching behavior

## [0.1.1]
### Added
- Add description to deployment of calamari-core project

## [0.1.0]
### Added
- Add org.starchartlabs.calamari.core.MediaTypes as a centralized definition of common media types used with interacting with GitHub
- Add org.starchartlabs.calamari.core.auth.ApplicationKey for streamlined handling of authenticating with GitHub via private key as the App itself
- Add org.starchartlabs.calamari.core.auth.InstallationAccessToken for streamlined handling of authenticating with GitHub as a specific installation of the App
- Add org.starchartlabs.calamari.core.exception.KeyLoadingException as a typed indication of an isue loading the GitHub App private key
- Add org.starchartlabs.calamari.core.exception.WebhookVerifier to streamline checking webhook payloads against known secret keys for authenticity
