package tasks;

public class WikiAnalyzer2 {

    private static final String LINUX = "https://en.wikipedia.org/wiki/Linux";
    private static final String WINDOWS = "https://en.wikipedia.org/wiki/Microsoft_Windows";

    public static void main(String[] args) throws Exception {
        // TODO: use download + thenApply + get
        //  reuse download, countDots from WikiAnalyzer1
        //  count the dots in the background thread, print the counts in main
        //  https://en.wikipedia.org/wiki/Linux
        //  https://en.wikipedia.org/wiki/Microsoft_Windows
        //  potato (use this as the url to get an exception)
        var linux = WikiAnalyzer1.download(LINUX).thenApply(text -> WikiAnalyzer1.countDots(text));
        var windows = WikiAnalyzer1.download(WINDOWS).thenApply(text -> WikiAnalyzer1.countDots(text));
        System.out.println("linux: " + linux.get());
        System.out.println("windows: " + windows.get());
    }
}
