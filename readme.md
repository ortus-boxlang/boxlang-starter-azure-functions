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
│   │   ├── bx/
│   │   │   ├── Application.bx          # Application lifecycle hooks
│   │   │   ├── Lambda.bx               # Default handler (root "/" and fallback)
│   │   │   └── handlers/
│   │   │       ├── Products.bx         # Routed handler → /products
│   │   │       └── api/
│   │   │           └── Test.bx         # Nested routed handler → /api/test
│   │   └── ...
│   └── resources/
│       └── boxlang.json                # BoxLang runtime configuration
└── src/test/java/com/myproject/
    ├── AzureFunctionIntegrationTest.java
    └── mocks/                          # Mock Azure types for fast, no-network tests
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

`./gradlew generateManifest` scans `handlers/` and writes `src/main/bx/manifest.json` — it's wired via `dependsOn` into `test`, `azureFunctionsRun`, `azureFunctionsPackage`, and `azureFunctionsDeploy`, so it's always regenerated fresh and can never silently drift. `manifest.json` is gitignored, never hand-edited or committed.

If `manifest.json` is ever missing or invalid, the runtime falls back to scanning `handlers/` directly, and if that directory doesn't exist either, to scanning the function root for backward compatibility with pre-`handlers/` deployments; set `BOXLANG_ENABLE_ROOT_SCAN=false` to disable that last-resort scan entirely and restrict routing to the default `Lambda.bx` handler only.

`manifest.json`'s `reserved` and `defaultHandler` fields are enforced by the runtime, not just documentation - a manifest can never route to a reserved file (`Application.bx`, `Lambda.bx`, or anything else it lists), and `defaultHandler.file`/`method` is honored as the fallback handler for unmatched routes when present.

## 🧑‍💻 The `Function.java` wrapper

Unlike AWS Lambda or Google Cloud Functions, Azure's build plugins only scan **your own project's compiled classes** for `@FunctionName` methods when generating `function.json` — they never look inside dependency jars. Since the actual routing/execution logic lives in the `boxlang-azure-functions` runtime dependency, this template includes a two-line wrapper class (`src/main/java/com/myproject/Function.java`) that carries the `@FunctionName`/`@HttpTrigger` annotations and forwards every request straight to `AzureFunctionRunner`. You should never need to touch this file — add BoxLang code under `handlers/` instead.

## 🛠️ Local Development

```bash
# Clone and enter the project
git clone <your-repo-url>
cd <your-project>

# Copy the local settings template
cp local.settings.json.example local.settings.json

# Run the tests
./gradlew test

# Start the local Azure Functions host (via Core Tools)
./gradlew azureFunctionsRun
```

Then test with curl in another terminal:

```bash
curl http://localhost:7071/
curl http://localhost:7071/products
curl http://localhost:7071/api/test
curl http://localhost:7071/ -H "x-bx-function: anotherLambda"
```

Set `BOXLANG_AZURE_DEBUGMODE=true` in `local.settings.json` to enable verbose logging and disable handler-class caching, so `.bx` changes are picked up without restarting the host.

## ☁️ Deploying to Azure

```bash
az login

export AZURE_SUBSCRIPTION_ID=<your-subscription-id>
export AZURE_RESOURCE_GROUP=<your-resource-group>
export AZURE_FUNCTION_APP_NAME=<your-function-app-name>
export AZURE_REGION=eastus

./gradlew azureFunctionsDeploy
```

The `azurefunctions {}` block in `build.gradle` reads all deployment settings from these environment variables — nothing is hard-coded, so the same `build.gradle` works in CI/CD and locally.

## 🧪 Testing

```bash
./gradlew test
```

Tests in `src/test/java/com/myproject/` exercise the full request pipeline using `AzureFunctionRunner` directly with mock Azure request/context objects (`src/test/java/com/myproject/mocks/`) — no live Azure environment or Core Tools required, so they run fast in CI.

## ⚙️ Configuration

### `boxlang.json`

`src/resources/boxlang.json` controls BoxLang runtime behavior: class-generation caching (`trustedCache`), debug mode, logging, request timeouts, and more. Set `trustedCache: false` and `debugMode: true` for local development; flip both for production.

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `BOXLANG_AZURE_ROOT` | Root directory for `.bx` files | Azure-provided `AzureWebJobsScriptRoot`, else `/home/site/wwwroot` |
| `BOXLANG_AZURE_CLASS` | Override the default `Lambda.bx` path | *(unset)* |
| `BOXLANG_AZURE_DEBUGMODE` | Verbose logging, disables handler-class caching | `false` |
| `BOXLANG_AZURE_CONFIG` | Path to a custom `boxlang.json` | `boxlang.json` in root |

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
