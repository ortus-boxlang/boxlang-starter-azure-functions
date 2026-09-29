## Quick context

This is a BoxLang Azure Functions template that wraps the `boxlang-azure-functions` runtime as a Java library dependency, invoked through a thin project-owned entry point. Key artifacts are produced by Gradle and packaged/deployed via the official `com.microsoft.azure.azurefunctions` Gradle plugin.

## Big-picture architecture (short)

- The BoxLang runtime is a Maven dependency: `io.boxlang:boxlang-azure-functions:<version>`.
- **Important Azure-specific detail**: the Azure Functions Gradle/Maven plugins only scan *this project's own compiled classes* for `@FunctionName` methods when generating `function.json` - they never scan dependency jars. That's why `src/main/java/com/myproject/Function.java` exists: it's a tiny wrapper with the `@FunctionName`/`@HttpTrigger` annotations that forwards every request straight to `AzureFunctionRunner` (from the dependency). You should not need to touch this file.
- BoxLang sources live under `src/main/bx` (notably `Lambda.bx` and `Application.bx`). `Lambda.bx` exposes `run(event, context, response)` by convention - the same convention used by the AWS Lambda and Google Cloud Functions BoxLang runtimes, so `.bx` code is portable across all three.
- Additional routed handlers live under `src/main/bx/handlers/` (see `Products.bx`, `handlers/api/Test.bx`). Only files under `handlers/` (or listed in `manifest.json`) are ever reachable by URI.
- `host.json` sets `http.routePrefix` to `""` so request paths have no Azure-specific prefix, matching the AWS/GCF path shape exactly.

## What an AI agent should know immediately

- **Build system**: use the Gradle wrapper (`./gradlew`) to ensure correct plugin versions and JVM settings.
- **Tests**: JUnit tests are in `src/test/java/com/myproject`. Run `./gradlew test`.
- **Local run**: `./gradlew azureFunctionsRun` (official plugin task, requires Azure Functions Core Tools) starts a real local HTTP server - test with plain `curl`, no synthetic event files needed.
- **Manifest generation**: `./gradlew generateManifest` scans `src/main/bx/handlers/` and writes `src/main/bx/manifest.json`. It's wired via `dependsOn` into `test`, `azureFunctionsRun`, `azureFunctionsPackage`, and `azureFunctionsDeploy`, so it can never silently drift. `manifest.json` is gitignored (regenerated, not committed).
- **Deployment**: `./gradlew azureFunctionsDeploy`, configured entirely via environment variables (`AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `AZURE_FUNCTION_APP_NAME`, `AZURE_REGION`) - no hard-coded values in `build.gradle`.
- **Runtime config**: `src/resources/boxlang.json` controls caching, class generation, logging, timeouts, and trustedCache - change these for dev vs prod (e.g. `trustedCache`, `debugMode`).
- **BoxLang modules**: add modules to `src/resources/boxlang_modules` or declare them in `box.json`.

## Commands (exact examples)

- **Build**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Local run**: `./gradlew azureFunctionsRun`
- **Deploy**: `AZURE_SUBSCRIPTION_ID=... AZURE_RESOURCE_GROUP=... AZURE_FUNCTION_APP_NAME=... ./gradlew azureFunctionsDeploy`
- **Regenerate the routing manifest manually**: `./gradlew generateManifest`

## Project-specific conventions & patterns

- Entrypoint convention: BoxLang handlers expose `run(event, context, response)` (see `src/main/bx/Lambda.bx`). Alternate methods are callable via the `x-bx-function` request header.
- New routed handler: create a `.bx` file under `src/main/bx/handlers/` (e.g. `handlers/Orders.bx` → reachable at `/orders`; nested dirs are allowed and matched case-insensitively).
- `box.json` is used to declare BoxLang modules for publishing/install; local modules for packaging belong in `src/resources/boxlang_modules`.
- Application lifecycle hooks live in `src/main/bx/Application.bx` (`onApplicationStart`/`onRequest*`), not in the Java layer.
- `src/main/java/com/myproject/Function.java` is the only Java file in this template, and it should almost never need to change - it exists solely so the Azure plugin's annotation scan finds an entry point.

## Integration points & external dependencies

- **Azure Functions Gradle plugin**: `com.microsoft.azure.azurefunctions`, provides `azureFunctionsRun`/`azureFunctionsPackage`/`azureFunctionsDeploy`. Generates `function.json` from `@FunctionName`/`@HttpTrigger` automatically - never hand-edit it.
- **Runtime JAR**: Maven dependency `io.boxlang:boxlang-azure-functions:<version>`, resolved automatically by Gradle.

## Useful file pointers (examples to inspect)

- `src/main/bx/Lambda.bx` - default handler and examples of the response shape.
- `src/main/bx/Application.bx` - lifecycle hooks (`onApplicationStart`, `onRequest`, etc.).
- `src/main/bx/handlers/Products.bx`, `src/main/bx/handlers/api/Test.bx` - routed handler examples (flat and nested).
- `src/main/java/com/myproject/Function.java` - the Azure entry point wrapper (see note above).
- `build.gradle` - `generateManifest` task, `azurefunctions {}` config block, test wiring.
- `src/resources/boxlang.json` - runtime configuration (debug/trustedCache/logging/timeouts).

## Quick checklist for code edits

1. If you add Java dependencies, update `build.gradle`.
2. If you add BoxLang modules, put them in `src/resources/boxlang_modules` or declare them in `box.json`.
3. New routed handlers go under `src/main/bx/handlers/` - never edit `manifest.json` by hand, it's regenerated.

## Code formatting standards

- **Spacing around symbols**: Always add spaces around parentheses `( )`, brackets `[ ]`, braces `{ }`, and operators for readability
- **Examples**:
  - ✅ `function run( event, context, response )`
  - ❌ `function run(event,context,response)`
  - ✅ `var results = [ 1, 2, 3 ]`
  - ❌ `var results = [1,2,3]`
  - ✅ `if ( condition ) { doSomething(); }`
  - ❌ `if(condition){doSomething();}`
- Apply this spacing standard to all BoxLang, Java, and configuration code in the project
