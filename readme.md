# ⚡︎ BoxLang Azure Functions Starter Template

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

## 🚀 Welcome

This is the starter template for building **BoxLang serverless applications on Microsoft Azure Functions**. It bootstraps everything you need: the [BoxLang Azure Functions runtime](https://github.com/ortus-boxlang/boxlang-azure-functions), the convention-based `handlers/` routing setup, a Gradle build wired to the official Azure Functions plugin, and a ready-to-run test suite.

> 💡 This template is intentionally structured the same way as our [AWS Lambda](https://github.com/ortus-boxlang/boxlang-starter-aws-lambda) and [Google Cloud Functions](https://github.com/ortus-boxlang/boxlang-starter-google-functions) starter templates. Your `.bx` handler code can move between all three providers unmodified - only the deployment step differs.

## 📦 What This Starter Includes

- The BoxLang Azure Functions runtime, wired into Azure's Java worker via a thin `Function.java` entry point
- Convention-based `handlers/` routing, backed by a build-time `manifest.json`
- A Gradle build wired to the official `com.microsoft.azure.azurefunctions` plugin for local run/deploy
- JUnit integration tests that exercise the full request pipeline with mock Azure request/context objects
- Ready-to-use GitHub Actions workflows for test, snapshot, and release builds

## 📋 Prerequisites

- **Java 21+**
- **[Azure Functions Core Tools](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local)** - for local testing (`azureFunctionsRun`)
- **[Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)**, logged in (`az login`) - for deployment
- An Azure subscription with a resource group ready

## 🏗️ Project Structure

```
.
├── build.gradle                        # Gradle build, generateManifest task, azurefunctions {} config
├── host.json                           # Azure Functions host config (routePrefix is "")
├── local.settings.json.example         # Copy to local.settings.json for local runs
├── src/
│   ├── main/
│   │   ├── java/com/myproject/
│   │   │   └── Function.java           # Thin Azure entry point - forwards to AzureFunctionRunner
│   │   └── bx/
│   │       ├── Application.bx          # Application lifecycle hooks
│   │       ├── Lambda.bx               # Default handler (fallback for unmatched routes)
│   │       └── handlers/
│   │           ├── Products.bx         # Routed handler -> /products
│   │           └── api/
│   │               └── Test.bx         # Nested routed handler -> /api/test
│   ├── resources/
│   │   └── boxlang.json                # BoxLang runtime configuration
│   └── test/
│       └── java/com/myproject/         # JUnit integration tests + mocks
├── .github/workflows/                  # Test, snapshot, and release CI/CD pipelines
├── box.json                            # BoxLang module dependencies
└── gradle.properties                   # version, jdkVersion, boxlangVersion
```

## 🧭 URI Routing with `handlers/`

Only files under `src/main/bx/handlers/` (or listed in the build-time-generated `manifest.json`) are ever reachable by URI. `Application.bx` and the default `Lambda.bx` are never routable, no matter what's on disk.

| Incoming URI | Handler File |
|---|---|
| `/products` | `handlers/Products.bx` |
| `/api/test` | `handlers/api/Test.bx` |
| `/user-profiles` | `handlers/UserProfiles.bx` (hyphens map to PascalCase) |
| `/` or anything unmatched | `Lambda.bx` (the default handler) |

Add a new route by creating a `.bx` file under `handlers/`:

```boxlang
// src/main/bx/handlers/Orders.bx
class {
    function run( event, context, response ) {
        return { "message": "Fetching orders" };
    }
}
```

`./gradlew generateManifest` scans `handlers/` and writes `src/main/bx/manifest.json` - it's wired via `dependsOn` into `test`, `azureFunctionsRun`, `azureFunctionsPackage`, and `azureFunctionsDeploy`, so it's always regenerated fresh and can never silently drift. `manifest.json` is gitignored, never hand-edited or committed.

If `manifest.json` is ever missing or invalid, the runtime falls back to scanning `handlers/` directly, and if that directory doesn't exist either, to scanning the function root for backward compatibility with pre-`handlers/` deployments; set `BOXLANG_ENABLE_ROOT_SCAN=false` to disable that last-resort scan entirely and restrict routing to the default `Lambda.bx` handler only.

`manifest.json`'s `reserved` and `defaultHandler` fields are enforced by the runtime, not just documentation - a manifest can never route to a reserved file (`Application.bx`, `Lambda.bx`, or anything else it lists), and `defaultHandler.file`/`method` is honored as the fallback handler for unmatched routes when present.

As with `Lambda.bx`, the `x-bx-function` header can call an alternative method on a handler - only ever a method you declared, since BoxLang's public/remote scope rules are exactly what gates it: don't make a method public if you don't want it externally callable.

## 🧑‍💻 The `Function.java` Wrapper (Azure-Specific)

Unlike AWS Lambda or Google Cloud Functions, Azure's build plugins only scan **your own project's compiled classes** for `@FunctionName` methods when generating `function.json` - they never look inside dependency jars. Since the actual routing/execution logic lives in the `boxlang-azure-functions` runtime dependency, this template includes a two-line wrapper class (`src/main/java/com/myproject/Function.java`) that carries the `@FunctionName`/`@HttpTrigger` annotations and forwards every request straight to `AzureFunctionRunner`. You should never need to touch this file - add BoxLang code under `handlers/` instead.

## 🔧 Application Lifecycle

Use `src/main/bx/Application.bx` for initialization and per-request hooks. It fires for every request, whether served by `Lambda.bx` or by a routed handler under `handlers/`:

```java
class {
    this.name = "My-Azure-Function"

    function onApplicationStart() {
        // Initialize databases, caches, etc. - runs once, on cold start
        return true;
    }

    function onRequestStart( targetPage ) {
        // Per-request initialization
        return true;
    }
}
```

`run()`, `onRequestEnd` and `onError` all receive the same `response` struct as their last argument. A returned value is stored in `response.body` before `onRequestEnd` runs, so a hook can wrap it, and a handled error defaults to status `500` unless `onError` sets one:

```js
class {

    function onRequestEnd( target, event, context, response ) {
        response.body = { ok: true, data: response.body }
    }

    function onError( exception, eventName, event, context, response ) {
        response.body = { ok: false, error: exception.message }
    }

}
```

If `Application.bx` defines `onError`, the error counts as handled; rethrow from the hook to fail the invocation.

## 📋 Handler Contract

Every handler - `Lambda.bx` or anything under `handlers/` - implements `run( event, context, response )` (or an alternate method called via the `x-bx-function` header):

```boxlang
class{
    function run( event, context, response ){
        response.body = {
            "error": false,
            "messages": [],
            "data": "Incoming event: " & event.toString()
        }
        response.statusCode = 200
    }

    // Call with header: x-bx-function: anotherLambda
    function anotherLambda( event, context, response ){
        return "Hola!!"
    }
}
```

- **`event`** - the event struct that triggered the function (method, path, headers, body, query parameters) - the same shape across all three BoxLang serverless runtimes
- **`context`** - the Azure runtime context struct: `functionName`, `invocationId`, `requestId`
- **`response`** - the struct returned to the caller, with a standard shape: `statusCode` (default `200`), `headers`, `body`, `cookies` (array), plus any other property you add

You can either populate `response` or simply `return` a value - both are auto-serialized to JSON.

## 🛠️ Local Development

```bash
# Copy the local settings template
cp local.settings.json.example local.settings.json

./gradlew test              # run the test suite
./gradlew azureFunctionsRun # start the local Azure Functions host (via Core Tools)
```

Then test with curl in another terminal:

```bash
curl http://localhost:7071/
curl http://localhost:7071/products
curl http://localhost:7071/api/test
curl http://localhost:7071/ -H "x-bx-function: anotherLambda"
```

Set `BOXLANG_AZURE_DEBUGMODE=true` in `local.settings.json` to enable verbose logging and disable handler-class caching, so `.bx` changes are picked up without restarting the host.

## 🧪 Testing

```bash
./gradlew test
```

Tests in `src/test/java/com/myproject/` exercise the full request pipeline using `AzureFunctionRunner` directly with mock Azure request/context objects (`mocks/`) - no live Azure environment or Core Tools required, so they run fast in CI. Test report: `build/reports/tests/test/index.html`.

## 🔨 Build Tasks

| Task | Description |
|---|---|
| `build` | Full build lifecycle (clean, compile, test, package) |
| `test` | Run the JUnit test suite |
| `generateManifest` | Scan `handlers/` and (re)generate `manifest.json` |
| `azureFunctionsRun` | Start the local Azure Functions host (via Core Tools) |
| `azureFunctionsPackage` | Package the deployment artifact |
| `azureFunctionsDeploy` | Deploy to Azure |
| `spotlessApply` / `spotlessCheck` | Auto-format / check Java source formatting |

## ☁️ Deploying to Azure

```bash
az login

export AZURE_SUBSCRIPTION_ID=<your-subscription-id>
export AZURE_RESOURCE_GROUP=<your-resource-group>
export AZURE_FUNCTION_APP_NAME=<your-function-app-name>
export AZURE_REGION=eastus

./gradlew azureFunctionsDeploy
```

The `azurefunctions {}` block in `build.gradle` reads all deployment settings from these environment variables - nothing is hard-coded, so the same `build.gradle` works in CI/CD and locally.

## 🤖 CI/CD Workflows

`.github/workflows/` ships three ready-to-use pipelines:

| Workflow | Trigger | Purpose |
|---|---|---|
| `tests.yml` | Called by the other workflows | Reusable Java 21 test run with report artifacts |
| `snapshot.yml` | Push to any non-`main` branch, PRs | Development builds with snapshot versioning |
| `release.yml` | Push to `main`, manual dispatch | Full build, test, and package; optional Azure deployment (commented out by default) |

To enable automatic Azure deployment on release: deploy once via `azureFunctionsDeploy`, add your `AZURE_*` credentials as GitHub Secrets, then uncomment the deployment step in `release.yml`.

## ⚙️ Configuration

### `boxlang.json`

`src/resources/boxlang.json` controls BoxLang runtime behavior: class-generation caching (`trustedCache`), debug mode, logging, request timeouts, and more. Set `trustedCache: false` and `debugMode: true` for local development; flip both for production. Note the `requestTimeout` default is tuned for Azure's 5-minute Consumption plan limit.

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `BOXLANG_AZURE_ROOT` | Root directory for `.bx` files | Azure-provided `AzureWebJobsScriptRoot`, else `/home/site/wwwroot` |
| `BOXLANG_AZURE_CLASS` | Override the default `Lambda.bx` path | *(unset)* |
| `BOXLANG_AZURE_DEBUGMODE` | Verbose logging, disables handler-class caching | `false` |
| `BOXLANG_AZURE_CONFIG` | Path to a custom `boxlang.json` | `boxlang.json` in root |
| `BOXLANG_ENABLE_ROOT_SCAN` | Allow the legacy root-directory routing fallback (see URI Routing above) | `true`. Shared across every BoxLang serverless runtime (AWS/GCP/Azure). |

## 📦 Adding BoxLang Modules

```bash
box install {moduleName} --production --directory=src/resources/boxlang_modules
```

Or declare them in `box.json` under `dependencies`/`installPaths` and run `box install --production`. Modules are automatically packaged into your deployment artifact under `boxlang_modules/`.

## 🐛 Troubleshooting

| Problem | Solution |
|---|---|
| Tests fail with `ClassNotFoundException` | `./gradlew clean build` to refresh dependency resolution |
| `azureFunctionsRun` fails to start | Confirm Azure Functions Core Tools is installed and on `PATH` |
| Deployment fails with auth errors | Re-run `az login`; confirm `AZURE_SUBSCRIPTION_ID`/`AZURE_RESOURCE_GROUP` are correct |
| Function times out | Check `requestTimeout` in `boxlang.json` against your hosting plan's execution limit |
| Routing looks off after a deploy | Check Application Insights / the portal's log stream for a manifest `WARNING`; confirm `generateManifest` ran |

## 📚 Additional Resources

- **BoxLang Azure Functions Runtime** - [boxlang-azure-functions](https://github.com/ortus-boxlang/boxlang-azure-functions)
- **BoxLang Documentation** - [boxlang.ortusbooks.com](https://boxlang.ortusbooks.com)
- **AWS Lambda Starter** - [boxlang-starter-aws-lambda](https://github.com/ortus-boxlang/boxlang-starter-aws-lambda)
- **Google Cloud Functions Starter** - [boxlang-starter-google-functions](https://github.com/ortus-boxlang/boxlang-starter-google-functions)

## License

Apache License, Version 2.0.

## Open-Source & Professional Support

This project is a professional open source project and is available as FREE and open source to use.  Ortus Solutions, Corp provides commercial support, training and commercial subscriptions which include the following:

- Professional Support and Priority Queuing
- Remote Assistance and Troubleshooting
- New Feature Requests and Custom Development
- Custom SLAs
- Application Modernization and Migration Services
- Performance Audits
- Enterprise Modules and Integrations
- Much More

https://www.boxlang.io/plans

<p>&nbsp;</p>

<blockquote>
"We ❤️ Open Source and BoxLang" - Luis Majano
</blockquote>

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
