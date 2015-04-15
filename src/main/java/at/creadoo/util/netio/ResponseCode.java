/*
 * Copyright 2015 crea-doo.at
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
package at.creadoo.util.netio;

public enum ResponseCode {

	HELLO(100),
	BYE(110),
	REBOOTING(120),
	CONNECTION_TIMEOUT(130),
	OK(250),
	INVALID_VALUE(500),
	INVALID_PARAMETER(501),
	UNKNOWN_COMMAND(502),
	INVALID_LOGIN(503),
	ALREADY_LOGGED_IN(504),
	FORBIDDEN(505),
	INPUT_LINE_TOO_LONG(506),
	TOO_MANY_CONNECTIONS(507);
	
	private final Integer value;
	
	private ResponseCode(final Integer value) {
		this.value = value;
	}
	
	public String getName() {
		return this.name();
	}
	
	public Integer getValue() {
		return this.value;
	}
	
	public static ResponseCode getResponseCode(final Integer value) {
		for (ResponseCode responseCode : values()) {
			if (responseCode.getValue().equals(value)) {
				return responseCode;
			}
		}
		return null;
	}
}