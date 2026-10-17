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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

@Service
public class WebsiteTestService {
    @Autowired
    private RestTemplate rest;

    public String testWebsite(WebsiteTestRequest request) {
        validateUrl(request.url);
        try {
            HttpHeaders headers = new HttpHeaders();
            if (request.customHeaderKey != null && !request.customHeaderKey.isEmpty()) {
                headers.set(request.customHeaderKey, request.customHeaderValue);
            }

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            return this.rest.exchange(request.url, HttpMethod.GET, entity, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return "URL returned status code: " + e.getStatusCode();
        }
    }

    /**
     * Validates that the URL is safe to fetch:
     * - scheme must be http or https
     * - resolved host must not be a private/loopback/link-local address
     *   (blocks 127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16)
     *
     * @throws IllegalArgumentException if the URL fails any check
     */
    private static void validateUrl(String rawUrl) {
        URL parsed;
        try {
            parsed = new URL(rawUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + e.getMessage());
        }

        String scheme = parsed.getProtocol();
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("URL scheme must be http or https");
        }

        String host = parsed.getHost();
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }

        for (InetAddress address : addresses) {
            if (isPrivateAddress(address)) {
                throw new IllegalArgumentException("URL resolves to a private or internal IP address");
            }
        }
    }

    private static boolean isPrivateAddress(InetAddress addr) {
        // Java API covers: loopback (127.x/::1), link-local (169.254.x), site-local
        // (10.x, 172.16-31.x, 192.168.x), and any-local (0.0.0.0)
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
            return true;
        }
        // Explicit byte-level checks (defence-in-depth for IPv4)
        byte[] ip = addr.getAddress();
        if (ip.length == 4) {
            int b0 = ip[0] & 0xFF;
            int b1 = ip[1] & 0xFF;
            if (b0 == 10) return true;                           // 10.0.0.0/8
            if (b0 == 172 && b1 >= 16 && b1 <= 31) return true; // 172.16.0.0/12
            if (b0 == 192 && b1 == 168) return true;            // 192.168.0.0/16
            if (b0 == 127) return true;                          // 127.0.0.0/8
            if (b0 == 169 && b1 == 254) return true;            // 169.254.0.0/16
        }
        return false;
    }
}

