# Synchronization

This practice session is about yet more ways threads can be synchronized.
Read the introduction for each chapter and try to solve the tasks for it.
The aim is not as much to solve the tasks but to learn about Java's different built-in tools.
Read the javadocs of the relevant classes before googling stackoverflow.
This is important stuff you can use later in your career.

## ExecutorService

Suppose you have thousands of large text files with numbers in them.
The task is to find the sum of numbers in each file.
It would be unreasonable to make a new thread for each file because more time would be spent on switching between threads than on the actual work.

Thread pools allow you to create a fixed number of threads and assign them any number of tasks to them.
The thread pool will automatically distribute the tasks between the threads.

Thread pools can be created using static factory methods in `java.util.concurrent.Executors`.
After creating an ExecutorService (object representing the thread pool), you can submit tasks to it.
After submitting all the tasks, the executor should be shut down (preferably in a finally block).
Shutting down the executor disallows submitting new tasks and terminates threads after all submitted tasks have completed.
Threads of an active executor can prevent an application from closing - always shut down your executors.

The tasks submitted to the thread pools must implement either `Runnable` or `Callable<T>`.
Use Runnable if the task doesn't produce any results.
Use Callable if the task produces a result that the task submitter needs to see.
Submitting a task to the executor will return a *future*.

java.util.concurrent.Future<V>:
*A Future represents the result of an asynchronous computation.
Methods are provided to check if the computation is complete, to wait for its completion, and to retrieve the result of the computation.
The result can only be retrieved using method `get` when the computation has completed, blocking if necessary until it is ready.*

**Task:** Open the `tasks.ExecutorServiceTask` class and fill the missing parts.

## ScheduledExecutorService

Suppose you want to repeat several different tasks periodically or after a specific time.
The naive solution would be to create a bunch of threads that do Thread.sleep for a while and then execute the task.
This solution is suboptimal, because it wastes resources by creating too many threads and cancelling the tasks correctly can be a lot of work.

Thread pools allow you to efficiently schedule tasks:
* repeat at fixed rate (after a fixed period, no matter what)
* repeat at fixed delay (fixed period after the last execution finished)
* run after a fixed delay, no repeating
* cancel tasks

The scheduler (ScheduledExecutorService) can be created using static methods in `java.util.concurrent.Executors`.
Unlike ExecutorService, shutting down the ScheduledExecutorService will cancel all scheduled tasks without waiting them to finish.
Shutting down the ScheduledExecutorService is still required, otherwise the application won't close due to live threads.

Scheduling a task returns a `ScheduledFuture<T>` object that can be used to cancel the task at any time or get the result of the scheduled `Callable<T>`.

**Task:** Open the `tasks.ScheduledExecutorServiceTask` class and fill the missing parts.

## CountdownLatch

Suppose you want to write your own `Future<T>` class.
It should have a method `get` that returns the value or blocks until it's available.
It should also have a method `complete` to set the value to return in `get` and unblock the threads waiting on `get`.
How to make the waiting threads block and only wake up when the value is set?

`java.util.concurrent.CountDownLatch` is a tool for having threads wait until a set of operations complete.
When creating the latch, you must specify a **count** - the number of operations that must complete before the waiting threads can continue.
`await` method of the latch should be called in the threads that want to wait for the operations to complete - this will block the thread until the operations have completed.
After completing each operation, the `countDown` method of the latch should be called to reduce the count.
Once the count reaches zero, all the waiting threads are unblocked.

**Task:** Open the `tasks.CountdownLatchTask` class and fill the missing parts.

## wait/notifyAll

Suppose you want to write your own CountDownLatch class.
It should have the methods `countDown` and `await`.
Most importantly, `await` should efficiently block until the count reaches zero.

This task can be solved using the most basic synchronization tools java provides: `Object#wait` and `Object#notifyAll`.
The idea is not complicated: the threads calling `wait` on some object will block until another thread calls `notifyAll` on the same object.
Both `wait` and `notifyAll` must be called on the same object, but it doesn't really matter which object - it's only purpose is bring together the waiters and notifiers.
* `wait` - *Causes the current thread to wait until another thread invokes the notifyAll() method for this object.*
* `notifyAll` - *Wakes up all threads that are waiting on this object's monitor.*

This is how using wait/notifyAll should look like:
```
void waitForCondition() throws InterruptedException {
    synchronized (this) {
        while (/* condition we are waiting for is not right */) {
            this.wait();
        }
    }
}

void changeCondition() {
    synchronized (this) {
        /* change condition */
        this.notifyAll();
    }
}
```

While the idea of wait/notifyAll sounds simple, there are a few gotchas to this mechanism:
* A call to `wait` can sometimes randomly return (stop blocking) even before anyone has called `notifyAll` on the object (spurious wakeups).
  Due to this, calling `wait` should always be in a loop that checks if waiting should continue (checks some condition).
* When `notifyAll` is called on an object, only the threads that have called `wait` on the same object are unblocked.
  Before calling `wait` on an object the thread must enter a synchronized block with that object as the monitor.
* To call `wait` on an object, you must be in a `synchronized` block with that object as the monitor.
  Calling `wait` will release the monitor of that synchronized block and block the thread.
  Once the monitor is released, other threads can acquire the monitor.
  When the original thread wakes up again (after `notifyAll`), it will reacquire the monitor (and may need to wait on other threads before it can do so).
  This is a significant exception to how the synchronized keyword works.

**Task:** Open the `tasks.WaitNotifyTask` class and fill the missing parts.

## CopyOnWriteArrayList

As you may have noticed, ArrayLists don't like being changed while they are being iterator with a for-each loop.
Also, ArrayLists are not *thread-safe* - when using an ArrayList from multiple threads, access must be synchronized.
Unsynchronized use leads to exceptions such as `ConcurrentModificationException` and `NullPointerException` will be thrown seemingly randomly.

One way to use ArrayList from multiple threads is to use the `synchronized` keyword.
That works and guarantees that only one thread is accessing the list at any given time.
However, when threads are mostly reading the list and modifications are very rare, then the synchronization approach can be slow, because threads will be often blocked by other readers.

An alternative to ArrayList + `synchronized` is to use a special class called `CopyOnWriteArrayList`.
Each time the list is changed, the list creates a copy of its internal contents array and applies the changes to the copy (copy-on-write).
Once the copy is ready, the original contents array is replaced with the copy and the old array is discarded.

The `CopyOnWriteArrayList` is *thread-safe* - you don't need to use the `synchronized` keyword to use it from different theads.
Multiple threads can read and iterate the list concurrently.
Only one thread can change the list at a time (other calls to add/remove etc will block).
When the list is changed, any already running for-each loops will continue on the old contents array and won't see the changes.

Use CopyOnWriteArrayList when a list is used by multiple threads but changes are infrequent.

**Task:** Open the `tasks.COWTask` class and fill the missing parts.
