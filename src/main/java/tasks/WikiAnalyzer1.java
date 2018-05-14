package tasks;

import java.util.concurrent.CompletableFuture;

public class WikiAnalyzer1 {

    public static void main(String[] args) {
        // TODO: use download + thenApply + whenComplete
        // print out the number of dots in the following
        // articles or an exception on failure:
        // https://en.wikipedia.org/wiki/Linux
        // https://en.wikipedia.org/wiki/Microsoft_Windows
    }

    public static long countDots(String str) {
        // TODO: implement
        return 0;
    }

    public static CompletableFuture<String> download(String url) {
        // TODO: implement
        return null;
    }
}
