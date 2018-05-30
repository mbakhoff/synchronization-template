# Synchronization

This practice session is about yet more ways threads can be synchronized.
Read the introduction for each section and try to solve the tasks for it.
Try to read the javadoc of the relevant classes before googling stackoverflow.

## CompletableFuture

The basic way to run some code in parallel is to use threads.
A thread is given a `Runnable` and the thread will start the `run` method.

```
new Thread(new Runnable() {
    @Override
    public void run() {
        try {
            // calculate some result
        } catch (Exception e) {
            // maybe something will catch it :/
            throw new RuntimeException(e);
        }
    }
}).start();
```

When the result is needed in some other thread, then some trickery is needed to pass it around.
One way is to use a `BlockingQueue`, another is to use shared state with explicit synchronization.
Most of the time, the exceptions are forgotten and the multithreaded program is full of bugs.

`CompletableFuture` is a class that makes it easy to pass around the calculated result between threads while also correctly handling exceptions.
Here is how it looks in action:

```
static CompletableFuture<List<Integer>> findNumbers() {
    CompletableFuture<List<Integer>> cf = new CompletableFuture<>();
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                List<Integer> result = new ArrayList<>();
                for (int i = 0; i < 10; i++)
                    result.add(i);
                cf.complete(result);
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        }
    }).start();
    return cf;
}

public static void main(String[] args) throws Exception {
    CompletableFuture<List<Integer>> numbersHolder = findNumbers();
    List<Integer> numbers = numbersHolder.get();
    System.out.println(numbers);
}
```

What is going on here?
1. `main` calls `findNumbers`.
2. `findNumbers` creates a new `CompletableFuture` object.
   a CompletableFuture is a placeholder that can hold the result of some computation.
   Initially, it is empty.
3. A new thread is started to calculate some result.
4. `findNumbers` returns the CompletableFuture immediately, without waiting for the thread to complete its work.
5. `main` will call `get` on the CompletableFuture.
   This will **block** the main thread (cause the thread to sleep) until the CompletableFuture is completed (some result is stored in it).

Meanwhile the thread is calculating the result.
When it's done, it will complete the CompletableFuture by storing the result in it (either the value or an exception).
This will unblock any threads waiting at the `get` method.

Note that if the CompletableFuture was completed with an exception, then calling the `get` method on it will throw the same exception.
This makes it easy to pass any exceptions from the result calculating thread to the thread that uses the result.

The real strength of CompletableFuture is **composability**.
It's easy to transform and combine the results in the CompletableFuture objects.

The `findNumbers` method calculates some numbers in parallel.
Imagine we need to find two results based on the calculated numbers: the sum of the numbers and the product of the numbers.
One way would be to modify the original `findNumbers` method and add the calculation there.
CompletableFuture provides a more flexible alternative:

```
public static void main(String[] args) throws Exception {
    CompletableFuture<List<Integer>> cfNumbers = findNumbers();
    CompletableFuture<Integer> cfSum = cfNumbers.thenApply(numbers -> {
        int sum = 0;
        for (int number : numbers)
            sum += number;
        return sum;
    });
    CompletableFuture<Integer> cfProduct = cfNumbers.thenApply(numbers -> {
        int product = 0;
        for (int number : numbers)
            product *= number;
        return product;
    });

    System.out.println("sum " + cfSum.get());
    System.out.println("product " + cfProduct.get());
}
```

The method `thenApply` takes the value of one CompletableFuture, computes a new value based on it and stores it in a new CompletableFuture.
The new CompletableFuture is immediately returned, but the actual computation may happen later.

In the sample above, `findNumbers` starts the thread for calculating the numbers.
`thenApply` is used to create `cfSum` and `cfProduct`, whose values are then printed out.
Calling `get` on `cfSum` and `cfProduct` and will block the calling thread until the values are actually available.
Once the numbers have been calculated and `cfNumbers` is completed, then both `cfSum` and `cfProduct` can be computed and their `get` methods will no longer block.

CompletableFutures have many other useful methods.
Among others is the `whenComplete` method which can be used to specify a function to be run when the CompletableFuture is completed.
For example, the previous example (without the product part) could be rewritten as follows:

```
public static void main(String[] args) throws Exception {
    findNumbers().thenApply(numbers -> {
        int sum = 0;
        for (int number : numbers)
            sum += number;
        return sum;
    }).whenComplete((sum, error) -> {
        if (error == null) {
            System.out.println("the sum is " + sum);
        } else {
            System.out.println("failed to compute the sum: " + error);
        }
    });
}
```

If the inlined lambdas are confusing, then try writing them out.
Here's the last example again, with some helping variables:
```
public static void main(String[] args) throws Exception {
    // both Function and BiConsumer are built-in functional interfaces

    // function that takes List<Integer> as an argument, returns Integer
    Function<List<Integer>, Integer> numberListToSum = numbers -> {
        int sum = 0;
        for (int number : numbers)
            sum += number;
        return sum;
    };

    // function that takes two arguments: Integer, Throwable
    BiConsumer<Integer, Throwable> printResults = (sum, error) -> {
        if (error == null) {
            System.out.println("the sum is " + sum);
        } else {
            System.out.println("failed to compute the sum: " + error);
        }
    };

    findNumbers().thenApply(numberListToSum).whenComplete(printResults);
}
```

### Task: WikiAnalyzer1

1. Write a method for downloading articles from wikipedia:
   ```
   CompletableFuture<String> download(String url) { .. }
   ```
2. Write a method for counting the dots (`.`) in a string:
   ```
   long countDots(String str) { .. }
   ```
3. Use `thenApply` and `whenComplete` to download some articles in parallel and print out the number of dots in each of them.
   Don't use `get` on the CompletableFuture.

### Task: WikiAnalyzer2

1. Make a copy of WikiAnalyzer1
2. Change the copy so the articles are downloaded in parallel, but the results are printed out by the main method.
   1. use the same `thenApply`
   2. replace `whenComplete` with `get`
   3. make sure the downloads run in parallel (create all CompletableFutures before calling `get` on any of them)
   4. try to download an invalid article (use some garbage for the url) and ensure the exception reaches the main method

## CountdownLatch

Suppose you want to write your own CompletableFuture class.
How to make the threads block when `get` is called, but the value is not available yet?
How to unblock the threads when the value becomes available?

`java.util.concurrent.CountDownLatch` is a tool for this exact purpose.
When creating the latch, a **count** (non-negative integer) must be specified.
The latch has a method named `await` - when it is called and the count is larger than zero, then the calling thread will be blocked.
The latch also has the `countDown` method - this will reduce the count by one.
Once the count reaches zero, all threads that are blocked at `await` are unblocked.

Here's an example that compiles some java source code and starts it when everything is ready:
```
List<String> sources = Arrays.asList("Main.java", "Service.java", "Item.java");
CountDownLatch latch = new CountDownLatch(sources.size());
for (String source : sources) {
    new Thread(() -> {
        compile(source);
        latch.countDown();
    }).start();
}

latch.await(); // blocks until everything's compiled
startApplication();
```

### Task: MyFuture

Implement your own CompletableFuture.
It should have the following functionality:

* a `get` method that returns the value or blocks the calling thread until it's available.
* a `complete` method to set the value and unblock the waiting threads.
* a `completeExceptionally` method to set an exception and unblock the waiting threads.

## wait/notifyAll

Suppose you want to write your own CountDownLatch class.
It should have the methods `countDown` and `await`.
Most importantly, `await` should efficiently block until the count reaches zero.

This task can be solved using the most basic synchronization tools that Java provides: `Object#wait` and `Object#notifyAll`.
The idea is not complicated: the threads calling `wait` on some object will block until another thread calls `notifyAll` on the same object.
Both `wait` and `notifyAll` must be called on the same object, but it doesn't really matter which object - it's only purpose is bring together the waiters and notifiers.

Here's an example of wait/notifyAll:
```
class SimpleBlockingQueue<T> {

    private final List<T> items = new ArrayList<>();

    public void put(T item) {
        synchronized (items) {
            items.add(item);
            items.notifyAll();
        }
    }

    public T take() throws InterruptedException {
        synchronized (items) {
            while (items.isEmpty())
                items.wait();
            return items.remove(0);
        }
    }
}
```

While the idea of wait/notifyAll sounds simple, there are a few gotchas to this mechanism.

[JLS 17.1](https://docs.oracle.com/javase/specs/jls/se9/html/jls-17.html#jls-17.1)
*Each object in Java is associated with a monitor, which a thread can lock or unlock.
Only one thread at a time may hold a lock on a monitor.
Any other threads attempting to lock that monitor are blocked until they can obtain a lock on that monitor.*

Note that a monitor remembers the thread that has locked, but not yet unlocked it.
The thread is "holding a lock" on that monitor.

```
// thread locks the monitor of this (can block)
synchronized (this) {
  // code runs while holding the lock
}
// thread unlocks the monitor of this
```

* Calling `notifyAll` on an object wakes up all threads that are calling `wait` on the same object.
  To call `notifyAll` or `wait` on an object, the calling thread must be holding a lock on the object's monitor.
* Calling `wait` will block the thread and **unlock the monitor**.
  When a waiting thread is unblocked, then it must lock the monitor again before it can continue (and possibly wait for the lock).
* A call to `wait` can sometimes randomly unblock even before `notifyAll` is called.
  This is called a [*spurious wake-up*](https://docs.oracle.com/javase/specs/jls/se9/html/jls-17.html#jls-17.2.1).
  Calling `wait` should always be in a loop that checks if waiting should continue.

### Task: MyLatch

Implement your own CountDownLatch.
The latch must have the `await` and `countDown` methods.
`await` must block efficiently using wait/notifyAll.

## InterruptedException

The `Object#wait`, `Thread#sleep` and methods built on them can throw the InterruptedException.
What does it mean and when is it thrown?

It's not possible to just stop/kill threads, because that would prevent unlocking all the locks the thread is holding and running the finally blocks.
As an alternative, a thread can be interrupted.
Interrupting a thread is a way to tell it to please shut down as soon as possible.

InterruptedException is thrown if and only if the `interrupt` method is called on the thread that is running the interruptible code.
If the thread is currently waiting in an interruptible method (wait, sleep etc.) then the method immediately stops waiting and throws an InterruptedException.
Otherwise, the interrupted flag is set in the thread and the next time it reaches an interruptible method, the exception is thrown.
Interrupts can only successfully stop the thread when the program's code doesn't ignore the InterruptedException and/or the interrupt flag.

The interrupted flag can be checked using `Thread#isInterrupted` and cleared using `Thread#interrupted` (stupid and confusing naming).
