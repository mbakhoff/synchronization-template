package tasks;

public class CountdownLatchTask {

    public static void main(String[] args) throws Exception {
        SimpleFuture<String> future = new SimpleFuture<>();
        new Thread(() -> wait1000AndComplete(future)).start();

        long start = System.currentTimeMillis();
        System.out.println("waiting for the future");
        String result = future.get(); // blocks
        if (result == null) {
            throw new IllegalStateException("result was not set");
        }
        System.out.println(result);
        long duration = System.currentTimeMillis() - start;
        System.out.println("future is now (" + duration + "ms)");
    }

    public static class SimpleFuture<T> {

        // TODO add a CountDownLatch field
        private T value;

        public T get() throws InterruptedException {
            // TODO wait for the value to be set
            return value;
        }

        public void complete(T value) {
            this.value = value;
            // TODO signal that the value is now set
        }
    }

    private static void wait1000AndComplete(SimpleFuture<String> future) {
        try {
            Thread.sleep(1000);
            future.complete("this is it");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
