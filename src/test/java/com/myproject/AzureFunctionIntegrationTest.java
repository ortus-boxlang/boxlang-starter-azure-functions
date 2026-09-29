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

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.microsoft.azure.functions.HttpResponseMessage;
import com.myproject.mocks.MockExecutionContext;
import com.myproject.mocks.MockHttpRequestMessage;

import ortus.boxlang.runtime.azure.AzureFunctionRunner;

@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class AzureFunctionIntegrationTest {

	private AzureFunctionRunner	runner;
	private MockExecutionContext	context;

	@BeforeEach
	void setUp() {
		Path validPath = Path.of( "src", "main", "bx", "Lambda.bx" );
		runner	= new AzureFunctionRunner( validPath, true );
		context	= new MockExecutionContext();
	}

	@DisplayName( "Test your Lambda.bx" )
	@Test
	public void testBasicExecution() {
		var request = new MockHttpRequestMessage( "GET", "/" )
		    .withQueryParam( "when", Instant.now().toString() );

		HttpResponseMessage response = runner.run( request, context );

		assertThat( response ).isNotNull();
		assertThat( response.getStatus().value() ).isEqualTo( 200 );
		assertThat( response.getBody().toString() ).contains( "Incoming event" );
	}

	@Test
	@DisplayName( "Test /products routes to the Products handler" )
	public void testHandlerRouting() {
		var request = new MockHttpRequestMessage( "GET", "/products" );

		HttpResponseMessage response = runner.run( request, context );

		assertThat( response ).isNotNull();
		assertThat( response.getStatus().value() ).isEqualTo( 200 );
		assertThat( response.getBody().toString() ).contains( "Products handler" );
	}

	@Test
	@DisplayName( "Test nested /api/test route" )
	public void testNestedHandlerRouting() {
		var request = new MockHttpRequestMessage( "GET", "/api/test" );

		HttpResponseMessage response = runner.run( request, context );

		assertThat( response ).isNotNull();
		assertThat( response.getStatus().value() ).isEqualTo( 200 );
		assertThat( response.getBody().toString() ).contains( "api/test handler" );
	}

	@Test
	@DisplayName( "Test x-bx-function header routes to an alternative method" )
	public void testAlternativeMethod() {
		var request = new MockHttpRequestMessage( "GET", "/" )
		    .withHeader( "x-bx-function", "anotherLambda" );

		HttpResponseMessage response = runner.run( request, context );

		assertThat( response ).isNotNull();
		assertThat( response.getStatus().value() ).isEqualTo( 200 );
		assertThat( response.getBody() ).isEqualTo( "Hola!!" );
	}

	@Test
	@DisplayName( "Test with empty request" )
	public void testEmptyRequest() {
		var request = new MockHttpRequestMessage( "GET", "/" );

		HttpResponseMessage response = runner.run( request, context );

		assertThat( response ).isNotNull();
		assertThat( response.getStatus().value() ).isEqualTo( 200 );
	}

	@Test
	@DisplayName( "Test performance with a large payload" )
	public void testLargePayload() {
		StringBuilder largeData = new StringBuilder();
		for ( int i = 0; i < 1000; i++ ) {
			largeData.append( "This is test data line " ).append( i ).append( ". " );
		}

		var		request			= new MockHttpRequestMessage( "POST", "/" ).withBody( largeData.toString() );

		long	startTime		= System.currentTimeMillis();
		HttpResponseMessage response = runner.run( request, context );
		long	executionTime	= System.currentTimeMillis() - startTime;

		assertThat( response ).isNotNull();
		assertThat( response.getStatus().value() ).isEqualTo( 200 );

		// Performance assertion - should complete within reasonable time
		assertThat( executionTime ).isLessThan( 5000L ); // 5 seconds max

		System.out.println( "Large payload test completed in " + executionTime + "ms" );
	}
}
