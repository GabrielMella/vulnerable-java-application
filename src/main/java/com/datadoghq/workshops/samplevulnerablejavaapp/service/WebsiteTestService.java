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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class WebsiteTestService {
    @Autowired
    private RestTemplate rest;

    private static final Set<String> ALLOWED_HEADER_NAMES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "Accept", "Accept-Language", "Accept-Encoding", "Cache-Control",
            "Content-Type", "User-Agent", "X-Request-ID", "X-Correlation-ID"
        ))
    );

    private static final Pattern SENSITIVE_HEADER_PATTERN = Pattern.compile(
        "(?i)^(Authorization|Cookie|Host|X-Forwarded-.*)$"
    );

    private static final Pattern CRLF_PATTERN = Pattern.compile("[\\r\\n]");

    public String testWebsite(WebsiteTestRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (request.customHeaderKey != null && !request.customHeaderKey.isEmpty()) {
                if (!ALLOWED_HEADER_NAMES.contains(request.customHeaderKey)
                        || SENSITIVE_HEADER_PATTERN.matcher(request.customHeaderKey).matches()) {
                    return "Custom header name is not permitted: " + request.customHeaderKey;
                }
                String sanitizedValue = CRLF_PATTERN.matcher(
                    request.customHeaderValue != null ? request.customHeaderValue : ""
                ).replaceAll("");
                headers.set(request.customHeaderKey, sanitizedValue);
            }

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            return this.rest.exchange(request.url, HttpMethod.GET, entity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return "URL returned status code: " + e.getStatusCode();
        }
    }
}
