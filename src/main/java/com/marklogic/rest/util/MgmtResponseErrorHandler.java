/*
 * Copyright (c) 2023 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.rest.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;

public class MgmtResponseErrorHandler extends DefaultResponseErrorHandler {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Allows clients to disable error logging. Typical use case is for when error logging isn't useful and may
	 * confuse someone looking at the logs into thinking there's an actual problem.
	 */
	public static boolean errorLoggingEnabled = true;

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		try {
			super.handleError(response);
		} catch (HttpClientErrorException | HttpServerErrorException ex) {
			String message = "Logging HTTP response body to assist with debugging: " + ex.getResponseBodyAsString();
			if (HttpStatus.SERVICE_UNAVAILABLE.equals(ex.getStatusCode())) {
				if (logger.isDebugEnabled()) {
					logger.debug(message);
				}
			} else if (logger.isErrorEnabled() && errorLoggingEnabled) {
				logger.error(message);
			}
			throw ex;
		}
	}

}
