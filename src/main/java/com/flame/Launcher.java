package com.flame;

import javax.swing.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Launcher extends JFrame {
    private static final String LAUNCHER_VER = "v1.0.0";
    private static final String RELEASE_API = "https://api.github.com/repos/AlmondFlame1287/JMP/releases/latest";
    private static final JProgressBar progressBar = new JProgressBar(0, 100);
    private static int contentLength;

    public Launcher() {
        this.setup();
        this.setupProgressBar();
        try {
            this.checkForUpdates();
        } catch (IOException ignored) {}
        this.setVisible(true);
    }

    private void setupProgressBar() {
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        this.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        this.add(new JLabel("Checking and downloading new updates..."));
        this.add(progressBar);
        this.add(Box.createVerticalStrut(10));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void checkForUpdates() throws IOException {
        final URL api = new URL(RELEASE_API);
        HttpURLConnection connection = (HttpURLConnection) api.openConnection();
        connection.setRequestMethod("GET");
        contentLength = connection.getContentLength();

        final String response = parseResponse(connection.getInputStream());
        final String tag = extractTag(response);
        final String jarURL = extractJar(response);

        if(tag == null || jarURL == null) {
            JOptionPane.showMessageDialog(this, "Wasn't able to update");
            return;
        }

        SwingWorker<Void, Integer> worker = getVoidIntegerSwingWorker(jarURL);
        worker.execute();
    }

    private SwingWorker<Void, Integer> getVoidIntegerSwingWorker(String jarURL) throws MalformedURLException {
        final URL downloadURL = new URL(jarURL);

        return new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                downloadUpdate(downloadURL);
                return null;
            }

            @Override
            protected void done() {
                dispose();
                try {
                    launchMainApp();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(Launcher.this, "Failed to launch app: " + e.getMessage());
                }
            }
        };
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
            int totalRead = 0;
            while((count = in.read(buffer, 0, buffer.length)) != -1) {
                fileOut.write(buffer, 0, count);
                totalRead += count;

                if(contentLength <= 0) continue;

                int percent = (int) (((double) totalRead / contentLength) * 100);
                SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
            }
        } catch (IOException ioe) {
            final String message = "Something went wrong with the update procedure: " + ioe.getMessage();
            JOptionPane.showMessageDialog(this, message);
            System.exit(-1);
        }
    }

    private void launchMainApp() throws IOException {
        new ProcessBuilder("java", "-jar", "jmp.jar").inheritIO().start();
    }

    private void setup() {
        this.setTitle("JMP Update Checker v" + LAUNCHER_VER);
        this.setSize(300, 100);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }
}
