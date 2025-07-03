/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.petkap.tuwelscraper;

/**
 *
 * @author denijal
 */
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TuwelMonitor {

    private static final String URL_TEMPLATE = "https://tuwel.tuwien.ac.at/grade/report/user/index.php?id=%d";
    private static final String USERNAME = Constants.TUWEL_USERNAME;
    private static final String PASSWORD = Constants.TUWEL_PASSWORD;
    private static String COOKIE_HEADER = "";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static CyclicBarrier barier;

    public static void main(String[] args) throws Exception {

        int[] courseIds = Constants.COURSE_IDS;
        COOKIE_HEADER = TuwelLogin.login(USERNAME, PASSWORD);

        barier = new CyclicBarrier(courseIds.length + 1);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(courseIds.length);

        for (int courseId : courseIds) {
            executor.scheduleAtFixedRate(() -> checkForChanges(courseId), 0, 1, TimeUnit.MINUTES);
        }

        while (true) {
            barier.await();
            System.out.println("Trying to login:");
            COOKIE_HEADER = TuwelLogin.login(USERNAME, PASSWORD);
        }
    }

    private static void checkForChanges(int courseId) {
        try {
            String url = String.format(URL_TEMPLATE, courseId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Cookie", COOKIE_HEADER)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Failed to fetch course " + courseId + " — HTTP " + response.statusCode());
                barier.await();
                return;
            }

            Document doc = Jsoup.parse(response.body());
            Element container = doc.selectFirst(".user-report-container");

            if (container == null) {
                System.out.println("No .user-report-container found for course " + courseId);
                return;
            }

            String newContent = container.outerHtml();
            Path storageFile = Paths.get("grades_" + courseId + ".html");

            if (!Files.exists(storageFile)) {
                Files.writeString(storageFile, newContent);
                System.out.println("Initial content saved for course " + courseId);
                return;
            }

            String oldContent = Files.readString(storageFile);
            if (!oldContent.equals(newContent)) {
                System.out.println("\n🔔 Detected change in course " + courseId);

                List<String> original = Arrays.asList(oldContent.split("\n"));
                List<String> revised = Arrays.asList(newContent.split("\n"));

                String htmlDiff = HtmlGenerator.generateDiffHtml(original, revised);
                EmailSender.sendEmail("TUWEL Grade Change in Course " + courseId, htmlDiff);

                Files.writeString(storageFile, newContent); // Save updated content
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error checking course " + courseId + ": " + e.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(TuwelMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
