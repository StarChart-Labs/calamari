# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Add org.starchartlabs.calamari.core.MediaTypes as a centralized definition of common media types used with interacting with GitHub
- Add org.starchartlabs.calamari.core.auth.ApplicationKey for streamlined handling of authenticating with GitHub via private key as the App itself
- Add org.starchartlabs.calamari.core.auth.InstallationAccessToken for streamlined handling of authenticating with GitHub as a specific installation of the App
- Add org.starchartlabs.calamari.core.exception.KeyLoadingException as a typed indication of an isue loading the GitHub App private key
- Add org.starchartlabs.calamari.core.exception.WebhookVerifier to streamline checking webhook payloads against known secret keys for authenticity