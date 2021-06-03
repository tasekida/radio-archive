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

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

/**
 * GoogleAPIのAccessTokenを取得
 */
public class GoogleDriveFileUpload implements UnaryOperator<String> {
    /** ロガー */
    private static final Logger LOGGER = Logger.getLogger(GoogleDriveFileUpload.class.getName());

    /** インスタンス */
	private static GoogleDriveFileUpload INSTANCE;

	/** HTTPクライアント */
	private final HttpClient client;

	/**
	 * コンストラクタ
	 * @throws Exception GoogleAPIのAccessToken取得処理の初期化に失敗
	 */
	private GoogleDriveFileUpload() {
		this.client =	HttpClient.newBuilder()
                .version(Version.HTTP_2)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(HttpClient.Builder.NO_PROXY)
                .build();

	}

	/**
	 * スケルトン
	 */
	@Override
	public String apply(String _skeleton) {
		String skeleton = Objects.requireNonNull(_skeleton);

		try {

			return skeleton;

		} catch (Exception e) {

			throw new IllegalStateException(e);

		}
	}

	/**
	 * インスタンス取得
	 * @return インスタンス取得へアクセス
	 */
	public static GoogleDriveFileUpload getInstance() {
		if (null == INSTANCE) {
			synchronized (GoogleDriveFileUpload.class) {
				if (null == INSTANCE) {
					INSTANCE = new GoogleDriveFileUpload();
				}
			}
		}
		return INSTANCE;
	}
}