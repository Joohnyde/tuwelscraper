/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.petkap.tuwelscraper;

/**
 *
 * @author denijal
 */
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TuwelLogin {

    public static final CookieManager cookieManager = new CookieManager();

    static {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .cookieHandler(cookieManager)
            .build();

    public static String login(String username, String password) throws Exception {

        // Step 1: GET https://tuwel.tuwien.ac.at/auth/saml2/login.php
        HttpRequest initialRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://tuwel.tuwien.ac.at/auth/saml2/login.php"))
                .build();
        HttpResponse<String> initialResponse = client.send(initialRequest, HttpResponse.BodyHandlers.ofString());

        // Step 2: Parse AuthState from hidden input
        Document doc = Jsoup.parse(initialResponse.body());
        Element authStateInput = doc.selectFirst("input[type=hidden][name=AuthState]");
        if (authStateInput == null) {
            throw new IllegalStateException("AuthState input not found!");
        }
        String authState = authStateInput.attr("value");

        System.out.println("🔐 AuthState = " + authState);

        // Step 3: POST username/password/AuthState to TU Wien login
        String formData = String.format("username=%s&password=%s&AuthState=%s",
                encode(username), encode(password), encode(authState));

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .uri(URI.create("https://idp.zid.tuwien.ac.at/simplesaml/module.php/core/loginuserpass.php"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        // Step 4: Extract SAMLResponse and RelayState from returned HTML
        Document loginDoc = Jsoup.parse(loginResponse.body());
        String samlResponse = getInputValue(loginDoc, "SAMLResponse");
        String relayState = getInputValue(loginDoc, "RelayState");

        System.out.println("✅ Extracted SAMLResponse and RelayState");

        // Step 5: POST to TUWEL ACS endpoint
        String samlFormData = String.format("SAMLResponse=%s&RelayState=%s",
                encode(samlResponse), encode(relayState));

        HttpRequest samlRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(samlFormData))
                .uri(URI.create("https://tuwel.tuwien.ac.at/auth/saml2/sp/saml2-acs.php/tuwel.tuwien.ac.at"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        client.send(samlRequest, HttpResponse.BodyHandlers.ofString());

        // Step 6: Extract and print cookies
        URI tuwelUri = URI.create("https://tuwel.tuwien.ac.at");

        String cookies = cookieManager.getCookieStore().get(tuwelUri).stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));

        System.out.println("\n🎯 Final cookie map:\n" + cookies);
        return cookies;

    }

    private static String encode(String val) {
        return java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String getInputValue(Document doc, String name) {
        Element input = doc.selectFirst("input[name=" + name + "]");
        if (input == null) {
            throw new IllegalStateException(name + " not found!");
        }
        return input.attr("value");
    }
}
