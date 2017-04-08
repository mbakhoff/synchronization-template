package tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

public class COWTask {

    // TODO Task #1:
    // fix timeObservers synchronization.
    // write down how long it takes to get 500M observations.
    // TODO Task #2:
    // replace timeObservers ArrayList with CopyOnWriteArrayList and remove redundant synchronization.
    // measure 500M observations again; compare with ArrayList+synchronized.

    public static void main(String[] args) throws Exception {
        List<LongConsumer> timeObservers = new ArrayList<>();
        LongConsumer countingTimeObserver = new LongConsumer() {
            long start = System.currentTimeMillis();
            long observations;
            @Override
            public void accept(long time) {
                observations++;
                if (observations == 500_000_000) {
                    long duration = System.currentTimeMillis() - start;
                    System.out.println(observations + " observations in " + duration + "ms");
                    observations = 0;
                    start = System.currentTimeMillis();
                }
            }
        };

        Thread timeAnnouncerThread = new Thread(() -> {
            while (true) {
                long time = System.currentTimeMillis();
                for (LongConsumer observer : timeObservers) {
                    observer.accept(time);
                }
            }
        });
        timeAnnouncerThread.start();

        while (timeAnnouncerThread.isAlive()) {
            timeObservers.add(countingTimeObserver);
            Thread.sleep(200);
        }
    }
}
