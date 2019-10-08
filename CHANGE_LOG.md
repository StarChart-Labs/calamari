# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Changed
- Add missing handling for HTTP response body in InstallationAccessToken
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
