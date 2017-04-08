package tasks;

import java.util.concurrent.atomic.AtomicInteger;

public class WaitNotifyTask {

    public static void main(String[] args) throws Exception {
        SimpleLatch latch = new SimpleLatch(2);
        AtomicInteger realCount = new AtomicInteger(2);
        new Thread(() -> waitAndCountdown(latch, realCount, 1000)).start();
        new Thread(() -> waitAndCountdown(latch, realCount, 1500)).start();

        long start = System.currentTimeMillis();
        System.out.println("the final countdown");
        latch.await();
        if (realCount.get() != 0) {
            throw new IllegalStateException("2x countdown not called yet");
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("latch waited for " + duration + "ms");
    }

    public static class SimpleLatch {

        private int count;

        public SimpleLatch(int count) {
            this.count = count;
        }

        public void await() throws InterruptedException {
            // TODO wait for count to reach zero
            // you only need to change await() and countDown()
            // use only wait, notifyAll and the synchronized keyword.
        }

        public void countDown() {
            count--;
        }
    }

    private static void waitAndCountdown(SimpleLatch latch, AtomicInteger realCount, int millis) {
        try {
            Thread.sleep(millis);
            realCount.decrementAndGet();
            latch.countDown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
