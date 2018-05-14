package tasks;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class MyFuture<T> {

    // TODO: use a CountDownLatch for blocking get (count 1)

    public T get() throws Exception {
        // TODO: wait for the value or exception to be set
        // throw the exception if it set, otherwise return the value
        return null;
    }

    public void complete(T value) {
        // TODO: store the value, unblock get
    }

    public void completeExceptionally(Exception e) {
        // TODO: store the exception, unblock get
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
