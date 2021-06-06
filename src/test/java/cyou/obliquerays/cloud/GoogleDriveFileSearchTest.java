/**
 * Copyright (C) 2021 tasekida
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package cyou.obliquerays.cloud;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import cyou.obliquerays.config.RadioProperties;

/** GoogleDriveFileSearchTestのUnitTest */
class GoogleDriveFileSearchTest {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(GoogleDriveFileSearchTest.class.getName());

	/** @throws java.lang.Exception */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
    	try (InputStream resource = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        } catch (Throwable t) {
        	LOGGER.log(Level.SEVERE, "エラー終了", t);
        }
	}

	/** @throws java.lang.Exception */
	@AfterAll
	static void tearDownAfterClass() throws Exception {}

	/** @throws java.lang.Exception */
	@BeforeEach
	void setUp() throws Exception {	}

	/** @throws java.lang.Exception */
	@AfterEach
	void tearDown() throws Exception {}

	/**
	 * {@link cyou.obliquerays.cloud.GoogleDriveFileSearch#apply(java.lang.String)} のためのテスト・メソッド。
	 */
	@Test
	void testApply() {
		GoogleJsonWebToken jwt = GoogleJsonWebToken.getInstance();
		GoogleAccessToken gtoken = GoogleAccessToken.getInstance();
		GoogleDriveFileSearch gDriveFileSearch = GoogleDriveFileSearch.getInstance();

		String strToken = gtoken.apply(jwt.get());
		gDriveFileSearch.apply(strToken);

		LOGGER.log(Level.INFO, strToken);
	}

	/**
	 * {@link cyou.obliquerays.cloud.GoogleDriveFileSearch#apply(java.lang.String)} のためのテスト・メソッド。
	 * @throws Exception
	 */
	@Test
	void testExample() throws Exception {

		try(InputStream in = ClassLoader.getSystemResourceAsStream(RadioProperties.getProperties().getProperty("service.accounts.json"))) {
			GoogleCredential credential = GoogleCredential.fromStream(in);
			PrivateKey privateKey = credential.getServiceAccountPrivateKey();
			String privateKeyId = credential.getServiceAccountPrivateKeyId();

			Instant now = Instant.now();

		    Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
		    String signedJwt = JWT.create()
		        .withKeyId(privateKeyId)
		        .withIssuer(RadioProperties.getProperties().getProperty("service.accounts.issuer"))
		        .withSubject(RadioProperties.getProperties().getProperty("service.accounts.issuer"))
		        .withAudience("https://oauth2.googleapis.com/token")
		        .withIssuedAt(Date.from(now))
		        .withExpiresAt(Date.from(now.plusSeconds(30L)))
		        .sign(algorithm);

		    GoogleAccessToken gtoken = GoogleAccessToken.getInstance();
		    String strToken = gtoken.apply(signedJwt);

			HttpClient client =	HttpClient.newBuilder()
	                .version(Version.HTTP_2)
	                .followRedirects(Redirect.NORMAL)
	                .connectTimeout(Duration.ofSeconds(30))
	                .proxy(HttpClient.Builder.NO_PROXY)
	                .build();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create("https://www.googleapis.com/drive/v3/files?fields=nextPageToken,%20files(id,%20name)"))
	                .timeout(Duration.ofMinutes(30))
	                .header("Accept-Encoding", "gzip")
	                .header("Authorization", "Bearer " + strToken)
	                .GET()
	                .build();
	        LOGGER.log(Level.INFO, request.toString());

	        HttpResponse<String> response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

	        LOGGER.log(Level.INFO, response.body());

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "JWTトークン取得エラー", e);
			throw e;
		}
	}
}
