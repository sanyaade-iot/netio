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

public class NetIOBuilder {

	protected String host = "";

	protected Integer port = 1234;

	protected String user = "admin";

	protected String pass = "admin";

	public NetIOBuilder() {
		this(null, null);
	}
	
	public NetIOBuilder(final String host) {
		this(host, null);
	}
	
	public NetIOBuilder(final String host, final Integer port) {
		if (host != null) {
			this.host = host;
		}
		if (port != null) {
			this.port = port;
		}
	}

	public NetIOBuilder setHost(final String host) {
		if (host != null) {
			this.host = host;
		}
		return this;
	}

	public NetIOBuilder setPort(final Integer port) {
		if (port != null) {
			this.port = port;
		}
		return this;
	}

	public NetIOBuilder setUsername(final String user) {
		if (user != null) {
			this.user = user;
		}
		return this;
	}

	public NetIOBuilder setPassword(final String pass) {
		if (pass != null) {
			this.pass = pass;
		}
		return this;
	}

	public NetIO build() {
		return new NetIO(this);
	}
}