/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.myproject;

import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import ortus.boxlang.runtime.azure.AzureFunctionRunner;

/**
 * Azure Functions entry point for this project.
 * <p>
 * The Azure Functions Gradle/Maven plugins only scan a project's <em>own</em> compiled
 * classes for {@code @FunctionName} methods when generating {@code function.json} - they
 * never scan dependency jars. This tiny wrapper is what makes {@code boxlang-azure-functions}
 * (a library dependency) reachable as an actual Azure Function: it just forwards every
 * request straight to {@link AzureFunctionRunner}, which does all the real work (URI
 * routing, BoxLang script execution, response building).
 * <p>
 * You should not need to change this file - add your BoxLang code under {@code src/main/bx/}
 * instead (a new {@code handlers/Orders.bx}, for example).
 */
public class Function {

	private final AzureFunctionRunner runner = new AzureFunctionRunner();

	@FunctionName( "BoxLangFunction" )
	public HttpResponseMessage run(
	    @HttpTrigger(
	        name = "req",
	        methods = { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS, HttpMethod.HEAD },
	        authLevel = AuthorizationLevel.ANONYMOUS,
	        route = "{*path}"
	    ) HttpRequestMessage<Optional<String>> request,
	    final ExecutionContext context
	) {
		return runner.run( request, context );
	}
}
