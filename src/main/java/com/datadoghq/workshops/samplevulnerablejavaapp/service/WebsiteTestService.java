package com.datadoghq.workshops.samplevulnerablejavaapp.service;

import com.datadoghq.workshops.samplevulnerablejavaapp.http.WebsiteTestRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Service
public class WebsiteTestService {
    @Autowired
    private RestTemplate rest;

    /**
     * Case-insensitive deny-list for security-sensitive header names.
     * Matches Authorization, Cookie, Host, and any X-Forwarded-* variant.
     */
    private static final Pattern DENIED_HEADER_PATTERN =
        Pattern.compile("(?i)^(authorization|cookie|host|x-forwarded-.*)$");

    public String testWebsite(WebsiteTestRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (request.customHeaderKey != null && !request.customHeaderKey.isEmpty()) {
                if (DENIED_HEADER_PATTERN.matcher(request.customHeaderKey).matches()) {
                    throw new IllegalArgumentException(
                        "Header name is not permitted: " + request.customHeaderKey);
                }
                // Strip CR and LF to prevent HTTP header splitting.
                String sanitizedValue = request.customHeaderValue == null
                    ? ""
                    : request.customHeaderValue.replaceAll("[\r\n]", "");
                headers.set(request.customHeaderKey, sanitizedValue);
            }

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            return this.rest.exchange(request.url, HttpMethod.GET, entity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return "URL returned status code: " + e.getStatusCode();
        }
    }
}
