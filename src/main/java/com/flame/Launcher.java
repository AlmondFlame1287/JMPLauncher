package com.flame;

import javax.swing.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Launcher extends JDialog {
    private static final String LAUNCHER_VER = "v1.0.0";
    private static final String RELEASE_API = "https://api.github.com/repos/AlmondFlame1287/JMP/releases/latest";

    public Launcher() {
        this.setup();
        this.initLauncher();
        try {
            this.checkForUpdates();
        } catch (IOException ignored) {}
    }

    private void initLauncher() {
    }

    private void checkForUpdates() throws IOException {
        final URL api = new URL(RELEASE_API);
        HttpURLConnection connection = (HttpURLConnection) api.openConnection();
        connection.setRequestMethod("GET");

        final String response = parseResponse(connection.getInputStream());
        final String tag = extractTag(response);
        final String jarURL = extractJar(response);

        if(tag == null || jarURL == null) {
            JOptionPane.showMessageDialog(this, "Wasn't able to update");
            return;
        }

        final URL downloadURL = new URL(jarURL);

        this.downloadUpdate(downloadURL);
    }

    private String extractTag(String response) {
        final int index = response.indexOf("\"tag_name\"");
        if (index == -1) return null;

        final int start = response.indexOf(":", index) + 1;
        final int quoteStart = response.indexOf("\"", start) + 1;
        final int quoteEnd = response.indexOf("\"", quoteStart);

        return response.substring(quoteStart, quoteEnd);
    }

    private String extractJar(String response) {
        String marker = "\"browser_download_url\"";
        int markerIndex = response.indexOf(marker);

        while (markerIndex != -1) {
            int urlStart = response.indexOf("\"", markerIndex + marker.length()) + 1;
            int urlEnd = response.indexOf("\"", urlStart);
            String urlStr = response.substring(urlStart, urlEnd);

            if (urlStr.endsWith(".jar")) {
                return urlStr;
            }

            markerIndex = response.indexOf(marker, urlEnd);
        }

        return null;
    }

    private String parseResponse(InputStream stream) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        final StringBuilder response = new StringBuilder();

        String line;

        while((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();
        return response.toString();
    }

    private void downloadUpdate(URL url) {
        try(BufferedInputStream in = new BufferedInputStream(url.openStream());
            FileOutputStream fileOut = new FileOutputStream("jmp.jar")) {
            byte[] buffer = new byte[4096];

            int count;
            while((count = in.read(buffer, 0, buffer.length)) != -1) {
                fileOut.write(buffer, 0, count);
            }
        } catch (IOException ioe) {
            final String message = "Something went wrong with the update procedure: " + ioe.getMessage();
            JOptionPane.showMessageDialog(this, message);
            System.exit(-1);
        }
    }

    private void setup() {
        this.setTitle("JMP Update Checker v" + LAUNCHER_VER);
        this.setSize(300, 100);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }
}
