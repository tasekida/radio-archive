/**
 *  Copyright (C) 2021 tasekida
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import com.google.api.client.auth.oauth2.StoredCredential;

import cyou.obliquerays.config.RadioProperties;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * GoogleAPIのAccessTokenを取得
 */
public class GoogleOAuth2AccessToken implements Supplier<String> {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(GoogleOAuth2AccessToken.class.getName());

    /** インスタンス */
	private static GoogleOAuth2AccessToken INSTANCE;

	/** HTTPクライアント */
	private final HttpClient client;

	/**
	 * コンストラクタ
	 * @throws Exception GoogleAPIのAccessToken取得処理の初期化に失敗
	 */
	private GoogleOAuth2AccessToken() {
		this.client = HttpClient.newBuilder()
                .version(Version.HTTP_2)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(HttpClient.Builder.NO_PROXY)
                .build();
	}

	/**
	 * GoogleAPIのAccessTokenを取得
	 */
	@Override
	public String get() {

		StoredCredential storedObj = null;
		try (InputStream inStore = ClassLoader.getSystemResourceAsStream(RadioProperties.getProperties().getProperty("google.credentials.stored"));
				ObjectInputStream objectInStore = new ObjectInputStream(new ByteArrayInputStream(inStore.readAllBytes()))) {

			@SuppressWarnings("unchecked")
			Map<String, byte[]> stored = (Map<String, byte[]>) objectInStore.readObject();
			storedObj = (StoredCredential) new ObjectInputStream(new ByteArrayInputStream(stored.get("user"))).readObject();
		} catch (Exception e) {

			throw new IllegalStateException(e);
		} finally {

			LOGGER.log(Level.CONFIG, storedObj.toString());
			Objects.requireNonNull(storedObj);
		}

		String accessToken = null;
		try (InputStream inJson = ClassLoader.getSystemResourceAsStream(RadioProperties.getProperties().getProperty("google.credentials.json"));
				Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(false))) {

			LocalDateTime expiration =
					LocalDateTime.ofInstant(Instant.ofEpochMilli(storedObj.getExpirationTimeMilliseconds()), ZoneId.systemDefault());

			if (expiration.isBefore(LocalDateTime.now())) {
				@SuppressWarnings("unchecked")
				Map<String, Object> credentials = jsonb.fromJson(inJson, Map.class);
				@SuppressWarnings("unchecked")
				Map<String, Object> installed = (Map<String, Object>) credentials.get("installed");

				StringBuilder strPostParam =
						new StringBuilder("grant_type=refresh_token")
						.append("&refresh_token=").append(storedObj.getRefreshToken())
						.append("&client_id=").append(installed.get("client_id").toString())
						.append("&client_secret=").append(installed.get("client_secret").toString());

				HttpRequest refreshRequest = HttpRequest.newBuilder()
		                .uri(URI.create("https://oauth2.googleapis.com/token"))
		                .timeout(Duration.ofMinutes(30))
		                .header("Accept-Encoding", "gzip")
		                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
		                .POST(BodyPublishers.ofString(strPostParam.toString()))
		                .build();

				HttpResponse<InputStream> refreshResponse = this.client.send(refreshRequest, BodyHandlers.ofInputStream());
				@SuppressWarnings("unchecked")
				Map<String, Object> refreshToken = jsonb.fromJson(new GZIPInputStream(refreshResponse.body()), Map.class);

				accessToken = refreshToken.get("access_token").toString();

			} else {

				accessToken = storedObj.getAccessToken();

			}
		} catch (Exception e) {

			throw new IllegalStateException(e);
		} finally {

			LOGGER.log(Level.CONFIG, accessToken);
			Objects.requireNonNull(accessToken);
		}

		return accessToken;
	}

	/**
	 * インスタンス取得
	 * @return インスタンス取得へアクセス
	 */
	public static GoogleOAuth2AccessToken getInstance() {
		if (null == INSTANCE) {
			synchronized (GoogleOAuth2AccessToken.class) {
				if (null == INSTANCE) {
					INSTANCE = new GoogleOAuth2AccessToken();
				}
			}
		}
		return INSTANCE;
	}
}
