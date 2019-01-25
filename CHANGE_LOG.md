# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

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
