/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.petkap.tuwelscraper;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import java.util.List;

/**
 *
 * @author denijal
 */
public class HtmlGenerator {

    public static String generateDiffHtml(List<String> oldLines, List<String> newLines) {
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);
        StringBuilder html = new StringBuilder();

        html.append("<html><head>")
                .append("<link rel=\"stylesheet\" href=\"https://tuwel.tuwien.ac.at/theme/styles.php/tuwien/1751345497_1751345546/all\">")
                .append("</head><body>");
        html.append("<div class=\"user-report-container\">");

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            List<String> source = delta.getSource().getLines();
            List<String> target = delta.getTarget().getLines();

            // Removed lines → red with strikethrough
            for (String line : source) {
                html.append("<div><span style=\"color:red;text-decoration:line-through;\">")
                        .append(line)
                        .append("</span></div>");
            }

            // Added lines → green
            for (String line : target) {
                html.append("<div><span style=\"color:green;\">")
                        .append(line)
                        .append("</span></div>");
            }
        }

        html.append("</div></body></html>");
        return html.toString();
    }

}
