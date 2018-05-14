package tasks;

import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class MyFuture<T> {

    private T value;
    private Exception exception;
    private final CountDownLatch latch = new CountDownLatch(1);

    public T get() throws Exception {
        latch.await();
        if (exception != null)
            throw new ExecutionException(exception);
        return value;
    }

    public void complete(T value) {
        this.value = value;
        latch.countDown();
    }

    public void completeExceptionally(Exception e) {
        this.exception = e;
        latch.countDown();
    }

    // run this to test your solution. should print "looks good!"
    // note that this test cannot fully test the correctness. check the sample solution
    public static void main(String[] args) throws Exception {
        MyFuture<String> mySuccess = new MyFuture<>();
        AtomicInteger successes = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    if ("success".equals(mySuccess.get())) {
                        successes.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        mySuccess.complete("success");

        MyFuture<String> myFailure = new MyFuture<>();
        AtomicInteger failures = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                boolean getReturned = false;
                try {
                    myFailure.get();
                    getReturned = true; // should not happen
                } catch (ExecutionException | FileNotFoundException e) {
                    failures.incrementAndGet(); // should happen
                } catch (Exception e) {
                    throw new RuntimeException("did not expect " + e);
                }
                if (getReturned)
                    throw new RuntimeException("expected exception");
            }).start();
        }
        myFailure.completeExceptionally(new FileNotFoundException());

        Thread.sleep(500);
        if (successes.get() == 3 && failures.get() == 3) {
            System.out.println("looks good!");
        } else {
            throw new RuntimeException("there's a bug somewhere");
        }
    }
}
