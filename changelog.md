# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

----

## [Unreleased]

### Added

- Initial release: BoxLang Azure Functions starter template, structured the same way as `boxlang-starter-aws-lambda` and `boxlang-starter-google-functions`. Includes the `src/main/bx/handlers/` convention with a flat (`Products.bx`) and a nested (`api/Test.bx`) example, a `generateManifest` Gradle task wired into `test`/`azureFunctionsRun`/`azureFunctionsPackage`/`azureFunctionsDeploy`, and the official `com.microsoft.azure.azurefunctions` Gradle plugin for local run/deploy.

* First release
