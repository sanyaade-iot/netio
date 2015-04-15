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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

public class NetIO {

	private static final Logger log = Logger.getLogger(NetIO.class);

	private String host;

	private Integer port;

	private String user;

	private String pass;

	protected Socket socket;

	protected BufferedReader reader;

	protected BufferedWriter writer;

	private String hash;

	protected State state;

	protected NetIO(final Builder builder) {
		this.state = State.DISCONNECTED;
		this.host = builder.host.trim();
		this.port = builder.port;
		this.user = builder.user.trim();
		this.pass = builder.pass.trim();
	}

	public Boolean isConnected() {
		log.trace("isConnected()");
		return isAuthorized() || State.CONNECTED.equals(state);
	}

	public Boolean isAuthorized() {
		log.trace("isAuthorized()");
		return State.AUTHORIZED.equals(state);
	}
	
	/**
	 * Returns the firmware version
	 */
	public String getVersion() throws NetIOException {
		return getResponseMessage(command("version"));
	}

	/**
	 * Returns the uptime of the device
	 */
	public String getUpTime() throws NetIOException {
		return getResponseMessage(command("uptime"));
	}

	/**
	 * Returns the MAC address of the device
	 */
	public String getMAC() throws NetIOException {
		return getResponseMessage(command("system mac"));
	}

	/**
	 * Returns the current time
	 */
	public String getTime() throws NetIOException {
		return getResponseMessage(command("system time"));
	}

	/**
	 * Returns the HTTP port
	 */
	public Integer getHTTPPort() throws NetIOException {
		try {
			return Integer.parseInt(getResponseMessage(command("system webport")));
		} catch (NumberFormatException ex) {
			//
		}
		return null;
	}

	/**
	 * Returns the HTTP port
	 */
	public Integer getKShellPort() throws NetIOException {
		try {
			return Integer.parseInt(getResponseMessage(command("system kshport")));
		} catch (NumberFormatException ex) {
			//
		}
		return null;
	}

	/**
	 * Returns the name of the device
	 */
	public String getAlias() throws NetIOException {
		return stripQuotes(getResponseMessage(command("alias")));
	}

	/**
	 * Sets the name of the device
	 */
	public Boolean setAlias(final String alias) throws NetIOException {
		if (alias != null && !alias.isEmpty()) {
			return isResponseCodeOk(command("alias " + alias));
		}
		return false;
	}

	/**
	 * Tell to keep the connection
	 */
	public void noop() throws NetIOException {
		command("noop");
	}

	/**
	 * Reboot the device
	 */
	public Boolean reboot() throws NetIOException {
		if (isConnected()) {
			final String response = command("reboot");
			final ResponseCode responseCode = getResponseCode(response);
			if (responseCode.equals(ResponseCode.HELLO) || responseCode.equals(ResponseCode.REBOOTING)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the port status of all ports or of the given port if set
	 */
	public PortStatus getPortStatus(final Integer port) throws NetIOException {
		if (isPortValid(port)) {
			return PortStatus.getPortStatus(getResponseMessage(command("port " + port.toString())));
		}
		return null;
	}

	/**
	 * Checks if port is enabled
	 */
	public Boolean isPortOn(final Integer port) throws NetIOException {
		if (isPortValid(port)) {
			final String response = command("port " + port);
			return isResponseCodeOk(response) && "1".equals(getResponseMessage(response));
		}
		return false;
	}

	/**
	 * Checks if port is disabled
	 */
	public Boolean isPortOff(final Integer port) throws NetIOException {
		return !isPortOn(port);
	}

	/**
	 * Enable port
	 */
	public Boolean setPortOn(final Integer port) throws NetIOException {
		if (isPortValid(port) && isPortOff(port)) {
			return isResponseCodeOk("port " + port + " " + PortStatus.ACTIVATED.getValue());
		}
		return false;
	}

	/**
	 * Disable port
	 */
	public Boolean setPortOff(final Integer port) throws NetIOException {
		if (isPortValid(port) && isPortOn(port)) {
			return isResponseCodeOk(command("port " + port + " " + PortStatus.DEACTIVATED.getValue()));
		}
		return false;
	}

	/**
	 * Set all ports at once
	 */
	public Boolean setPorts(final String command) throws NetIOException {
		log.trace("send(" + command + ")");
		if (command == null || !command.matches("^[01iu]{4}$")) {
			final String error = "Invalid format (" + command + ")";
			log.debug(error);
			throw new NetIOException(error);
		}
		return isResponseCodeOk(command("port list " + command));
	}

	/**
	 * Toggle port
	 */
	public Boolean togglePort(final Integer port) throws NetIOException {
		if (isPortValid(port)) {
			if (isPortOn(port)) {
				return setPortOff(port);
			} else {
				return setPortOn(port);
			}
		}
		return false;
	}

	/**
	 * Set port mode to manual operation
	 */
	public Boolean setPortManual(final Integer port) throws NetIOException {
		if (isPortValid(port)) {
			return isResponseCodeOk(command("port " + port + " manual"));
		}
		return false;
	}

	/**
	 * Returns settings for a given port
	 */
	public String getPortSetup(final Integer port) throws NetIOException {
		if (isPortValid(port)) {
			return getResponseMessage(command("port setup " + port));
		}
		return null;
	}

	/**
	 * Returns name for a given port
	 */
	public String getPortName(final Integer port) throws NetIOException {
		if (isPortValid(port)) {
			final String response = getPortSetup(port);
			final String regex = "^\"?(.+?)\"?\\s(.+?)\\s(.+?)\\s(.+?)";
			
			final Pattern pattern = Pattern.compile(regex);
			final Matcher matcher = pattern.matcher(response);
			
			if (matcher.matches() && matcher.groupCount() == 4) {
				return stripQuotes(matcher.group(1));
			}
		}
		return null;
	}

	/**
	 * Returns name for a given port
	 */
	private String stripQuotes(final String content) {
		String tempContent = content.trim();
		if (tempContent.startsWith("\"")) {
			tempContent = tempContent.substring(1);
		}
		if (tempContent.endsWith("\"")) {
			tempContent = tempContent.substring(0, tempContent.length() - 2);
		}

		return tempContent;
	}

	/**
	 * Validates port
	 */
	public Boolean isPortValid(final Integer port) {
		if (port != null && (port >= 1) && (port <= 4)) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if a response has the response code "250 OK"
	 */
	private Boolean isResponseCodeOk(final String response) {
		return checkResponseCode(response, ResponseCode.OK);
	}

	/**
	 * Checks if a response has the desired response code
	 */
	private Boolean checkResponseCode(final String response, final ResponseCode code) {
		if (response != null && code != null && code.equals(getResponseCode(response))) {
			return true;
		}
		return false;
	}

	/**
	 * Extracts the return code out of a response
	 */
	private ResponseCode getResponseCode(final String response) {
		if (response == null || response.trim().isEmpty()) {
			return null;
		}
		
		final String tempResponse = response.trim();
		
		if ((tempResponse.length() < 3) && !(tempResponse.length() > 4)) {
			return null;
		}
		
		if (!tempResponse.substring(3, 4).equals(" ")) {
			return null;
		}
		
		try {
			final Integer code = Integer.parseInt(tempResponse.substring(0, 3));
			final ResponseCode responseCode = ResponseCode.getResponseCode(code);
			
			if (responseCode != null) {
				return responseCode;
			}
		} catch (NumberFormatException ex) {
			//
		}
		return null;
	}

	/**
	 * Extracts the return code out of a response
	 */
	private String getResponseMessage(final String response) {
		if (response == null || response.trim().isEmpty()) {
			return null;
		}
		
		final String tempResponse = response.trim();
		
		if ((tempResponse.length() < 3) && !(tempResponse.length() > 4)) {
			return null;
		}
		
		return tempResponse.substring(4).trim();
	}

	/**
	 * Sends a command to the device
	 */
	protected String command(final String command) throws NetIOException {
		try {
			if (!isAuthorized() && !authorize()) {
				final String error = "Unable to authorize";
				log.debug(error);
				throw new NetIOException(error);
			}
			log.debug("--> " + command);
			writer.write(command);
			writer.newLine();
			writer.flush();
			final String response = reader.readLine();
			log.debug("<-- " + response);
			return response;
		} catch (IOException ex) {
			try {
				socket.close();
			} catch (IOException e1) {
				log.error("Error closing Socket on Exception (" + ex.getMessage() + ")", e1);
			}
			throw new NetIOException("Error while sending command", ex);
		}
	}

	private Boolean authorize() throws NetIOException {
		try {
			if (!isConnected()) {
				connect();
			}
			
			final String sendString = "clogin " + user + " " + getPasswordHash();
			log.debug("--> " + sendString);
			writer.write(sendString);
			writer.newLine();
			writer.flush();
			final String response = reader.readLine();
			log.debug("<-- " + response);
			if (response == null || !response.startsWith("250")) {
				throw new IOException(response);
			}
			
			state = State.AUTHORIZED;
			
			return true;
		} catch (NetIOException ex) {
			log.error("Error while connecting to " + host + ":" + port, ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new NetIOException(ex);
		} catch (IOException ex) {
			throw new NetIOException(ex);
		}
		return false;
	}

	private void connect() throws NetIOException {
		try {
			socket = new Socket(host, port);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.DEFAULT_CHARSET));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.DEFAULT_CHARSET));
			final String response = reader.readLine();
			log.debug("<-- " + response);
			
			if (isResponseCodeOk(response)) {
				hash = response.substring(10, 18).trim();
				state = State.CONNECTED;
			}
		} catch (UnknownHostException ex) {
			state = State.DISCONNECTED;
			throw new NetIOException("Error while connecting", ex);
		} catch (IOException ex) {
			state = State.DISCONNECTED;
			throw new NetIOException("Error while connecting", ex);
		}
	}

	private String getPasswordHash() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return Hex.encodeHexString((DigestUtils.getMd5Digest().digest((user + pass + hash).getBytes(Constants.DEFAULT_CHARSET))));
	}
}
