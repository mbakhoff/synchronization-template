package tasks;

import java.util.concurrent.atomic.AtomicInteger;

public class MyLatch {

    private int count;

    public MyLatch(int count) {
        this.count = count;
    }

    public void await() throws InterruptedException {
        synchronized (this) {
            while (count > 0)
                this.wait();
        }
    }

    public void countDown() {
        synchronized (this) {
            count--;
            this.notifyAll();
        }
    }

    // run this to test your solution. should print "looks good!"
    // note that this test cannot fully test the correctness. check the sample solution
    public static void main(String[] args) throws Exception {
        AtomicInteger done = new AtomicInteger();
        MyLatch latch = new MyLatch(3);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                    done.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        for (int i = 0; i < 3; i++) {
            if (done.get() != 0)
                throw new RuntimeException("released too early");
            latch.countDown();
        }
        Thread.sleep(500);
        if (done.get() == 5) {
            System.out.println("looks good!");
        } else {
            throw new RuntimeException("did not release all");
        }
    }
}
