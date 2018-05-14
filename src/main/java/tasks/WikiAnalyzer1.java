package tasks;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class WikiAnalyzer1 {

    public static void main(String[] args) throws Exception {
        // TODO: download all the articles in parallel
        //  calculate and print out the number of dots in the main method
        //  https://en.wikipedia.org/wiki/Linux
        //  https://en.wikipedia.org/wiki/Microsoft_Windows
        var linux = download("https://en.wikipedia.org/wiki/Linux");
        var windows = download("https://en.wikipedia.org/wiki/Microsoft_Windows");
        System.out.println("linux: " + countDots(linux.get()));
        System.out.println("windows: " + countDots(windows.get()));
    }

    public static long countDots(String str) {
        return str.chars().filter(c -> c == '.').count();
    }

    public static CompletableFuture<String> download(String url) {
        // a web page url is basically a link to a text file (html file that the browser can render)
        // hint: use URL#openStream + InputStream#readAllBytes
        var result = new CompletableFuture<String>();
        new Thread(() -> {
            try (var in = new URL(url).openStream()) {
                result.complete(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }).start();
        return result;
    }
}
