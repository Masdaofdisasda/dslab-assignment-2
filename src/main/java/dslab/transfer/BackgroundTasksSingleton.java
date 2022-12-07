package dslab.transfer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// the thread pool of this class should handle all background tasks, such as forwarding messages
public class BackgroundTasksSingleton {
    private static volatile BackgroundTasksSingleton INSTANCE;

    private final ExecutorService backgroundTasksThreadPool;

    private BackgroundTasksSingleton() {
        // assign whatever number of threads to forwardingThreadPool
        this.backgroundTasksThreadPool = Executors.newFixedThreadPool(10);
    }

    public static synchronized BackgroundTasksSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BackgroundTasksSingleton();
        }

        return INSTANCE;
    }

    public synchronized void submit(Runnable task) {
        this.backgroundTasksThreadPool.submit(task);
    }

    public synchronized void shutdown() {
        this.backgroundTasksThreadPool.shutdown();
    }
}
