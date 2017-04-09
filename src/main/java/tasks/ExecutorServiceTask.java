package tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;

public class ExecutorServiceTask {
    public static void main(String[] args) throws Exception {
        List<File> filesToProcess = getFileList();

        List<Future<FileResult>> futureList = new ArrayList<>();
        // TODO:
        // create an ExecutorService with exactly 4 threads
        // submit tasks that run readFile, one task per file from getFileList; use Callable<FileResult>
        // collect the task futures into futureList

        List<FileResult> resultList = new ArrayList<>();
        // TODO put all FileResults into resultList

        System.out.println(findTotal(resultList));
        // TODO make sure the program prints 720 and then stops
    }

    private static List<File> getFileList() throws IOException {
        File[] files = new File("data").listFiles();
        if (files == null)
            throw new IOException("not a directory: data");
        return Arrays.asList(files);
    }

    private static FileResult readFile(File file) throws Exception {
        long result = 0;
        try (Scanner s = new Scanner(file, "UTF-8")) {
            while (s.hasNextLong())
                result += s.nextLong();
        }
        return new FileResult(file, result);
    }

    private static long findTotal(List<FileResult> results) {
        long total = 0;
        for (FileResult result : results) {
            total += result.sum;
        }
        return total;
    }

    static class FileResult {
        public final File file;
        public final long sum;

        FileResult(File file, long sum) {
            this.file = file;
            this.sum = sum;
        }
    }
}
