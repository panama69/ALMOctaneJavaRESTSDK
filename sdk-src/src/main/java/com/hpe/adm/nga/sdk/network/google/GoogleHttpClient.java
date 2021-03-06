/*
 * Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.adm.nga.sdk.network.google;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.hpe.adm.nga.sdk.authentication.Authentication;
import com.hpe.adm.nga.sdk.exception.OctaneException;
import com.hpe.adm.nga.sdk.model.ErrorModel;
import com.hpe.adm.nga.sdk.network.OctaneHttpClient;
import com.hpe.adm.nga.sdk.network.OctaneHttpRequest;
import com.hpe.adm.nga.sdk.network.OctaneHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Optional;

/**
 * HTTP Client using Google's API
 * <p>This will be refactored in future releases to enable the use of different underlying APIs</p>
 */
public class GoogleHttpClient implements OctaneHttpClient {

    private static final String LOGGER_REQUEST_FORMAT = "Request: {} - {} - {}";
    private static final String LOGGER_RESPONSE_FORMAT = "Response: {} - {} - {}";

    private static final String SET_COOKIE = "set-cookie";
    private static final String HTTP_MEDIA_TYPE_MULTIPART_NAME = "multipart/form-data";
    private static final String HTTP_MULTIPART_BOUNDARY_NAME = "boundary";
    private static final String HTTP_MULTIPART_BOUNDARY_VALUE = "---------------------------92348603315617859231724135434";
    private static final String HTTP_MULTIPART_PART_DISPOSITION_NAME = "Content-Disposition";
    private static final String HTTP_MULTIPART_PART1_DISPOSITION_FORMAT = "form-data; name=\"%s\"; filename=\"blob\"";
    private static final String HTTP_MULTIPART_PART1_DISPOSITION_ENTITY_VALUE = "entity";
    private static final String HTTP_MULTIPART_PART2_DISPOSITION_FORMAT = "form-data; name=\"content\"; filename=\"%s\"";
    private static final int HTTP_REQUEST_RETRY_COUNT = 1;

    private final Logger logger = LogManager.getLogger(GoogleHttpClient.class.getName());
    protected HttpRequestFactory requestFactory;
    protected String lwssoValue = "";
    protected final String urlDomain;
    protected Authentication lastUsedAuthentication;

    /**
     * Request initializer called on every request made by the requestFactory
     */
    protected HttpRequestInitializer requestInitializer = request -> {
        request.setResponseInterceptor(response -> {
            // retrieve new LWSSO in response if any
            HttpHeaders responseHeaders = response.getHeaders();
            updateLWSSOCookieValue(responseHeaders);
        });

        request.setUnsuccessfulResponseHandler((httpRequest, httpResponse, b) -> false);

        if (lwssoValue != null && !lwssoValue.isEmpty()) {
            request.getHeaders().setCookie(LWSSO_COOKIE_KEY + "=" + lwssoValue);
        }

        if (lastUsedAuthentication != null) {
            String clientTypeHeader = lastUsedAuthentication.getClientHeader();
            if (clientTypeHeader != null && !clientTypeHeader.isEmpty()) {
                request.getHeaders().set(HPE_CLIENT_TYPE, clientTypeHeader);
            }
        }
        request.setReadTimeout(60000);
    };

    public GoogleHttpClient(final String urlDomain) {
        this.urlDomain = urlDomain;
        HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
        requestFactory = HTTP_TRANSPORT.createRequestFactory(requestInitializer);
    }

    /**
     * @return - Returns true if the authentication succeeded, false otherwise.
     */
    public boolean authenticate(Authentication authentication) {
        lastUsedAuthentication = authentication;
        try {
            final ByteArrayContent content = ByteArrayContent.fromString("application/json", authentication.getAuthenticationString());
            HttpRequest httpRequest = requestFactory.buildPostRequest(new GenericUrl(urlDomain + OAUTH_AUTH_URL), content);

            HttpResponse response = executeRequest(httpRequest);

            // Initialize Cookies keys
            return response.isSuccessStatusCode();
        } catch (Exception e) {
            lastUsedAuthentication = null; //not reusable
            ErrorModel errorModel = new ErrorModel(e.getMessage());
            logger.error("Error in contacting server: {}, ", e.getMessage());
            throw new OctaneException(errorModel);
        }
    }

    public void signOut() {
        GenericUrl genericUrl = new GenericUrl(urlDomain + OAUTH_SIGNOUT_URL);
        try {
            HttpRequest httpRequest = requestFactory.buildPostRequest(genericUrl, null);
            HttpResponse response = executeRequest(httpRequest);

            if (response.isSuccessStatusCode()) {
                HttpHeaders hdr1 = response.getHeaders();
                updateLWSSOCookieValue(hdr1);
                lastUsedAuthentication = null;
            }
        } catch (Exception e) {
            ErrorModel errorModel = new ErrorModel(e.getMessage());
            logger.error("Error in contacting server: ", e);
            throw new OctaneException(errorModel);
        }
    }

    /**
     * Convert the abstract {@link OctaneHttpRequest}Request object to a specific {@link HttpRequest} for the google http client
     *
     * @param octaneHttpRequest input {@link OctaneHttpRequest}
     * @return {@link HttpRequest}
     */
    protected HttpRequest convertOctaneRequestToGoogleHttpRequest(OctaneHttpRequest octaneHttpRequest) {
        final HttpRequest httpRequest;
        try {
            switch (octaneHttpRequest.getOctaneRequestMethod()) {
                case GET: {
                    GenericUrl domain = new GenericUrl(octaneHttpRequest.getRequestUrl());
                    httpRequest = requestFactory.buildGetRequest(domain);
                    httpRequest.getHeaders().setAccept(((OctaneHttpRequest.GetOctaneHttpRequest) octaneHttpRequest).getAcceptType());
                    break;
                }
                case POST: {
                    OctaneHttpRequest.PostOctaneHttpRequest postOctaneHttpRequest = (OctaneHttpRequest.PostOctaneHttpRequest) octaneHttpRequest;
                    GenericUrl domain = new GenericUrl(octaneHttpRequest.getRequestUrl());
                    httpRequest = requestFactory.buildPostRequest(domain, ByteArrayContent.fromString(null, postOctaneHttpRequest.getContent()));
                    httpRequest.getHeaders().setAccept(postOctaneHttpRequest.getAcceptType());
                    httpRequest.getHeaders().setContentType(postOctaneHttpRequest.getContentType());
                    break;
                }
                case POST_BINARY: {
                    httpRequest = buildBinaryPostRequest((OctaneHttpRequest.PostBinaryOctaneHttpRequest) octaneHttpRequest);
                    break;
                }
                case PUT: {
                    OctaneHttpRequest.PutOctaneHttpRequest putHttpOctaneHttpRequest = (OctaneHttpRequest.PutOctaneHttpRequest) octaneHttpRequest;
                    GenericUrl domain = new GenericUrl(octaneHttpRequest.getRequestUrl());
                    httpRequest = requestFactory.buildPutRequest(domain, ByteArrayContent.fromString(null, putHttpOctaneHttpRequest.getContent()));
                    httpRequest.getHeaders().setAccept(putHttpOctaneHttpRequest.getAcceptType());
                    httpRequest.getHeaders().setContentType(putHttpOctaneHttpRequest.getContentType());
                    break;
                }
                case DELETE: {
                    GenericUrl domain = new GenericUrl(octaneHttpRequest.getRequestUrl());
                    httpRequest = requestFactory.buildDeleteRequest(domain);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Request method not known!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Problem creating httprequest", e);
        }
        return httpRequest;
    }

    /**
     * Convert google implementation of {@link HttpResponse} to an implementation abstract {@link OctaneHttpResponse}
     * @param httpResponse implementation specific {@link HttpResponse}
     * @return {@link OctaneHttpResponse} created from the impl response object
     * @throws IOException if the response output stream stream cannot be read
     */
    protected OctaneHttpResponse convertHttpResponseToOctaneHttpResponse(HttpResponse httpResponse) throws IOException {
        return new OctaneHttpResponse(httpResponse.getStatusCode(), httpResponse.parseAsString(), httpResponse.getContent());
    }

    @Override
    public OctaneHttpResponse execute(OctaneHttpRequest octaneHttpRequest) {
        return execute(octaneHttpRequest, HTTP_REQUEST_RETRY_COUNT);
    }

    /**
     * This method can be used internally to retry the request in case of auth token timeout
     * Careful, this method calls itself recursively to retry the request
     *
     * @param octaneHttpRequest abstract request, has to be converted into a specific implementation of http request
     * @param retryCount        number of times the method should retry the request if it encounters an HttpResponseException
     * @return OctaneHttpResponse
     */
    private OctaneHttpResponse execute(OctaneHttpRequest octaneHttpRequest, int retryCount) {

        final HttpRequest httpRequest = convertOctaneRequestToGoogleHttpRequest(octaneHttpRequest);
        final HttpResponse httpResponse;

        try {
            httpResponse = executeRequest(httpRequest);
            return convertHttpResponseToOctaneHttpResponse(httpResponse);
        } catch (HttpResponseException e) {

            //Try to handle the exception
            if (retryCount > 0) {

                //Try to handle 401/403 exceptions
                //Attempt to re-authenticate in case of auth issue
                if ((e.getStatusCode() == 401 || e.getStatusCode() == 403) && lastUsedAuthentication != null) {
                    logger.debug("Auth token invalid, trying to re-authenticate");

                    try {
                        //NOTE: if the credentials are somehow invalidated after your Octane objects has been created,
                        // this will retry authentication @code retryCount times, even if the @method authenticate() throws the exception
                        authenticate(lastUsedAuthentication);
                    } catch (OctaneException ex) {
                        logger.debug("Exception while retrying authentication: {}", ex.getMessage());
                    }

                    //Only retry if you've actually handled the exception in some way
                    //Retrying a 400 bad request makes no sense without some kind of handling
                    logger.debug("Retrying request, retries left: {}", retryCount);
                    return execute(octaneHttpRequest, --retryCount);
                }
            }

            logger.debug("The request was retried {} time(s), the exception could not be handled",
                            HTTP_REQUEST_RETRY_COUNT - retryCount);

            throw new RuntimeException("Problem executing httprequest", e);

        } catch (IOException e) {
            throw new RuntimeException("Problem executing httprequest", e);
        }

    }

    private HttpResponse executeRequest(final HttpRequest httpRequest) throws IOException {
        logger.debug(LOGGER_REQUEST_FORMAT, httpRequest.getRequestMethod(), httpRequest.getUrl().toString(), httpRequest.getHeaders().toString());
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final HttpContent content = httpRequest.getContent();
        if (content != null) {
            content.writeTo(byteArrayOutputStream);

            //Do not print the body of the sign in request
            if (!httpRequest.getUrl().toString().contains(OAUTH_AUTH_URL)) {
                logger.debug("Content: " + byteArrayOutputStream.toString());
            }
        }

        HttpResponse response = httpRequest.execute();
        logger.debug(LOGGER_RESPONSE_FORMAT, response.getStatusCode(), response.getStatusMessage(), response.getHeaders().toString());
        return response;
    }

    private HttpRequest buildBinaryPostRequest(OctaneHttpRequest.PostBinaryOctaneHttpRequest octaneHttpRequest) throws IOException {
        GenericUrl domain = new GenericUrl(octaneHttpRequest.getRequestUrl());

        final HttpRequest httpRequest = requestFactory.buildPostRequest(domain,
                generateMultiPartContent(octaneHttpRequest));
        httpRequest.getHeaders().setAccept(octaneHttpRequest.getAcceptType());

        return httpRequest;
    }

    /**
     * Generates HTTP content based on input parameters and stream.
     *
     * @param octaneHttpRequest - JSON entity model.
     * @return - Generated HTTP content.
     */
    private MultipartContent generateMultiPartContent(OctaneHttpRequest.PostBinaryOctaneHttpRequest octaneHttpRequest) {
        // Add parameters
        MultipartContent content = new MultipartContent()
                .setMediaType(new HttpMediaType(HTTP_MEDIA_TYPE_MULTIPART_NAME)
                        .setParameter(HTTP_MULTIPART_BOUNDARY_NAME, HTTP_MULTIPART_BOUNDARY_VALUE));

        MultipartContent.Part part1 = new MultipartContent.Part(new JsonHttpContent(new JacksonFactory(), octaneHttpRequest.getContent()));
        part1.setHeaders(new HttpHeaders().set(HTTP_MULTIPART_PART_DISPOSITION_NAME,
                String.format(HTTP_MULTIPART_PART1_DISPOSITION_FORMAT,
                        HTTP_MULTIPART_PART1_DISPOSITION_ENTITY_VALUE)));
        content.addPart(part1);

        // Add Stream
        InputStreamContent inputStreamContent = new InputStreamContent(octaneHttpRequest.getBinaryContentType(), octaneHttpRequest.getBinaryInputStream());
        MultipartContent.Part part2 = new MultipartContent.Part(inputStreamContent);
        part2.setHeaders(new HttpHeaders().set(HTTP_MULTIPART_PART_DISPOSITION_NAME, String.format(HTTP_MULTIPART_PART2_DISPOSITION_FORMAT, octaneHttpRequest.getBinaryContentName())));
        content.addPart(part2);
        return content;
    }

    /**
     * Retrieve new cookie from set-cookie header
     *
     * @param headers The headers containing the cookie
     * @return true if LWSSO cookie is renewed
     */
    private boolean updateLWSSOCookieValue(HttpHeaders headers) {
        boolean renewed = false;
        List<String> strHPSSOCookieCsrf1 = headers.getHeaderStringValues(SET_COOKIE);
        if (strHPSSOCookieCsrf1.isEmpty()) {
            return false;
        }

        /* Following code failed to parse set-cookie to get LWSSO cookie due to cookie version, check RFC 2965
        String strCookies = strHPSSOCookieCsrf1.toString();
        List<HttpCookie> Cookies = java.net.HttpCookie.parse(strCookies.substring(1, strCookies.length()-1));
        lwssoValue = Cookies.stream().filter(a -> a.getName().equals(LWSSO_COOKIE_KEY)).findFirst().get().getValue();*/
        for (String strCookie : strHPSSOCookieCsrf1) {
            List<HttpCookie> cookies;
            try {
                // Sadly the server seems to send back empty cookies for some reason
                cookies = HttpCookie.parse(strCookie);
            } catch (Exception ex){
                logger.error(ex);
                continue;
            }
            Optional<HttpCookie> lwssoCookie = cookies.stream().filter(a -> a.getName().equals(LWSSO_COOKIE_KEY)).findFirst();
            if (lwssoCookie.isPresent()) {
                lwssoValue = lwssoCookie.get().getValue();
                renewed = true;
                break;
            }
        }

        return renewed;
    }

    public static int getHttpRequestRetryCount() {
        return HTTP_REQUEST_RETRY_COUNT;
    }
}
