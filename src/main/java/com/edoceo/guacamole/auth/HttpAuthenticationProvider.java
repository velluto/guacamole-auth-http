/**
 * Copyright (C) 2014 Edoceo, Inc.
*/

/**
 * Leverage an Up-Stream HTTP(S) Server which provides the authentication and configuration details.
 *
 * Example `guacamole.properties`:
 *
 *  auth-provider: net.sourceforge.guacamole.net.auth.http.HttpAuthenticationProvider
 *
 * @author David Busby / Edoceo, Inc.
 * @see http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 * @see http://alvinalexander.com/blog/post/java/how-open-url-read-contents-httpurl-connection-java
 * @see http://www.java2blog.com/2013/11/jsonsimple-example-read-and-write-json.html
 */


package com.edoceo.guacamole.auth;

import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

// JSON Parser
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.properties.FileGuacamoleProperty;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


public class HttpAuthenticationProvider extends SimpleAuthenticationProvider {

    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(HttpAuthenticationProvider.class);

    /**
     * Map of all known configurations, indexed by identifier.
     */
    private Map<String, GuacamoleConfiguration> configs;

    public synchronized void init() throws GuacamoleException {
		// Nothing
    }

    @Override
    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(Credentials credentials) throws GuacamoleException {

    	// Verify Password Inputs
		if (null == credentials.getUsername()) {
			logger.info("Invalid username");
			return null;
		}

		if (null == credentials.getPassword() || 0 == credentials.getPassword().length()) {
			logger.info("Invalid password");
			return null;
		}

		Map<String, GuacamoleConfiguration> configs = new TreeMap<String, GuacamoleConfiguration>();

    	try {
    		String auth_page = GuacamoleProperties.getRequiredProperty( HttpAuthenticationProperties.AUTH_PAGE );
    		String head_auth = GuacamoleProperties.getRequiredProperty( HttpAuthenticationProperties.HEAD_AUTH );

    		logger.info("url:" + auth_page);
    		URL u = new URL(auth_page);

    		HttpURLConnection uc = (HttpURLConnection) u.openConnection();
    		uc.setDoOutput(true);
    		uc.setDoInput(true);

    		// // uc.setRequestProperty("Accept-Charset");
    		if (head_auth.length() > 0) {
    			uc.setRequestProperty("Authorization", head_auth);
    		}
    		// uc.setRequestProperty(GuacamoleProperties.getRequiredProperty("auth-http-head-add"));
    		uc.setRequestProperty("Content-Type", "application/json; charset=uf-8;");
    		uc.connect();

    		// Send
    		// @todo Build with JSON
    		JSONObject sendJSON = new JSONObject();
    		sendJSON.put("username", credentials.getUsername());
    		sendJSON.put("password", credentials.getPassword());

    		DataOutputStream os = new DataOutputStream(uc.getOutputStream());
    		os.writeBytes(sendJSON.toJSONString());
    		os.flush();
    		os.close();
    		
    		// Read Response Status Code?
    		switch (uc.getResponseCode()) {
    		case 200:

				// Parse JSON Response
				BufferedReader rd = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				JSONObject json = (JSONObject)JSONValue.parseWithException(rd);

				logger.info("Got the JSON" + json);

				GuacamoleConfiguration config = new GuacamoleConfiguration();
				config.setProtocol("vnc");
				config.setParameter("hostname", json.get("host").toString());
				config.setParameter("port", json.get("port").toString());
	
				configs.put(json.get("name").toString(), config);

				return configs;

			default:
				// What?
			}

    	} catch (Exception e) {
    		logger.info("Exception: " + Arrays.toString(e.getStackTrace()));
    		// throw e;
    	}

		return null;

    }
}