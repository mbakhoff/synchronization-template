package tasks;

import java.util.concurrent.CompletableFuture;

public class WikiAnalyzer1 {

    public static void main(String[] args) throws Exception {
        // TODO: download all the articles in parallel
        //  calculate and print out the number of dots in the main method
        //  https://en.wikipedia.org/wiki/Linux
        //  https://en.wikipedia.org/wiki/Microsoft_Windows
    }

    public static long countDots(String str) {
        // TODO: implement
        return 0;
    }

    public static CompletableFuture<String> download(String url) {
        // a web page url is basically a link to a text file (html file that the browser can render)
        // hint: use URL#openStream + InputStream#readAllBytes
        // TODO: implement
        return null;
    }
}
