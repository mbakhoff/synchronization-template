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
static Map<Path, Long> findFileSizes(Path root) throws IOException {
  Map<Path, Long> files = new HashMap<>();
  Files.walkFileTree(root, new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes fileInfo) {
      files.put(file, fileInfo.size());
      return FileVisitResult.CONTINUE;
    }
  });
  return files;
}

static CompletableFuture<Map<Path, Long>> findFileSizesAsync(Path root) {
  CompletableFuture<Map<Path, Long>> cf = new CompletableFuture<>();
  new Thread(() -> {
    try {
      cf.complete(findFileSizes(root));
    } catch (Exception e) {
      cf.completeExceptionally(e);
    }
  }).start();
  return cf;
}

public static void main(String[] args) throws Exception {
  CompletableFuture<Map<Path, Long>> srcFuture = findFileSizesAsync(Path.of("src"));
  CompletableFuture<Map<Path, Long>> nonexistingFuture = findFileSizesAsync(Path.of("nonexisting"));
  // both src,nonexisting run in parallel
  Set<Path> sourceFiles = srcFuture.get().keySet(); // get waits until result is ready
  System.out.println(sourceFiles);
  Set<Path> nothing = nonexistingFuture.get().keySet(); // throws exception
}
```

What is going on here?
1. `main` calls `findFileSizesAsync`.
2. `findFileSizesAsync` creates a new `CompletableFuture` object that works as a placeholder for the result.
   Initially, it is empty, because the result is not computed yet.
3. A new background thread is started to find the files and their sizes.
4. `findFileSizesAsync` returns the CompletableFuture immediately, without waiting for the thread to complete its work.
5. `main` calls another `findFileSizesAsync` to find the file sizes of another directory, which starts another background thread.
6. `main` will call `get` on both CompletableFutures.
   Calling `get` will **block** the main thread (cause the thread to sleep) until the background thread has finished its work and stored the result in the CompletableFuture.

Note that if finding the file sizes fails, then the exception is stored inside the CompletableFuture instead of the result.
When `get` is called, it will throw the same exception.
This makes it easy to pass any exceptions from the background thread to the thread that uses the result.

The real strength of CompletableFuture is **composability**.
It's very easy to add more tasks to the background thread that should only be done when the result is ready.

The `findFileSizesAsync` method finds the file sizes.
It may be useful to also find the total size of all the files, but that can only be done after the files are collected.
One way would be to modify the original `findFileSizesAsync` method and add the calculation there.
Unfortunately that would make it more difficult to return the result to some other thread, because now there's two pieces of data: the files and the total.
Also, in case of exceptions, it's hard to figure out which part of the calculation failed.
CompletableFuture provides a more flexible alternative:

```
public static void main(String[] args) throws Exception {
  CompletableFuture<Map<Path, Long>> filesFuture = findFileSizesAsync(Path.of("src"));
  CompletableFuture<Long> totalSizeFuture = alsoGetTheTotalSize(filesFuture);

  filesFuture.thenAccept(files -> {
    for (Map.Entry<Path, Long> e : files.entrySet()) {
      System.out.println("file=" + e.getKey() + " size=" + e.getValue());
    }
  });

  // do other stuff while the background thread does its thing
  System.out.println("total size " + totalSizeFuture.get());
}

static CompletableFuture<Long> alsoGetTheTotalSize(CompletableFuture<Map<Path, Long>> filesFuture) {
  return filesFuture.thenApply(files -> {
    long total = 0;
    for (long size : files.values())
      total += size;
    return total;
  });
}
```

Here `main` calls `alsoGetTheTotalSize` and passes the CompletableFuture of the files.
Next, `thenApply` is used to tell the background thread that after it has stored the result in `filesFuture`, it should also calculate the total size of the files and store that in another CompletableFuture.
The new CompletableFuture for the total is immediately returned and the actual calculation will happen in the background thread once all the files have been found.

`main` also gives the background thread a new task using `thenAccept`: when the background thread is done with the files, it should print them all out.

Both these extra tasks are done when the background thread calls `complete` on the first CompletableFuture.

CompletableFutures have many other useful methods.
Among others is the `whenComplete` method which can be used to specify a function to be run when the CompletableFuture is completed, either successfully or with an exception.
For example, finding and printing the total could be rewritten as follows:

```
public static void main(String[] args) throws Exception {
  CompletableFuture<Map<Path, Long>> filesFuture = findFileSizesAsync(Path.of("src"));
  filesFuture.thenApply(files -> {
    long total = 0;
    for (long size : files.values())
      total += size;
    return total;
  }).whenComplete((total, exception) -> {
    if (exception != null) {
      System.out.println("it failed because " + exception);
    } else {
      System.out.println("the total is " + total);
    }
  });
}
```

Now the result is printed in the background thread instead of main.

### Task: WikiAnalyzer1

1. Write a method for downloading articles from wikipedia:
   ```
   CompletableFuture<String> download(String url) { .. }
   ```
2. Write a method for counting the dots (`.`) in a string:
   ```
   long countDots(String str) { .. }
   ```
3. Start downloading the articles in the background in parallel.
   Make sure the downloads run in parallel (start all background threads before calling `get` on anything).
   Finally call `get` on each CompletableFuture, count the dots in the downloaded articles and print out the counts.

### Task: WikiAnalyzer2

1. Make a copy of WikiAnalyzer1
2. Change the copy so that the dots are also counted in the background thread.
   1. use `thenApply` to move the counting to the background thread
   2. call `get` and print the counts in the main thread (`get` should return just the number of dots)
   3. try to download an invalid article (use some garbage for the url) and check that the exception is thrown when calling `get`

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
            items.notifyAll(); // wake up waiting threads
        }
    }

    public T take() throws InterruptedException {
        synchronized (items) {
            while (items.isEmpty())
                items.wait(); // wait for notifyAll
            return items.remove(0);
        }
    }
}
```

While the idea of wait/notifyAll sounds simple, there are a few gotchas to this mechanism.

The main idea of synchronized blocks is that only a single thread can lock an object at a time.
Let's take another look at the `SimpleBlockingQueue` example above.
If one thread is inside the `take` method waiting for a new item to be added to the queue, then how can another thread `put` anything to the same queue?
The thread calling `put` would wait for the thread inside `take` to finish, which in turn is waiting for something to finish `put`.
It turns out that calling `items.wait()` will **unlock** the `items` object until the thread wakes up again.
When the thread does wake up, it must first wait until it can lock `items` again before it can continue.

Note that to call wait/notifyAll on some object, the thread must be inside a synchronised block that has locked that same object.

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
