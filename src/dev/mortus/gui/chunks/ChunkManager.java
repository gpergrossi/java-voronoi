package dev.mortus.gui.chunks;

import java.awt.Point;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dev.mortus.util.data.StochasticPriorityQueue;

public abstract class ChunkManager<T extends Chunk<T>> {

	protected final Comparator<T> OLDEST_CHUNK_FIRST = new Comparator<T>() {
		public int compare(T o1, T o2) {
			return (int) (o1.lastSeen - o2.lastSeen);
		}
	};
	
	protected final Comparator<T> CLOSEST_CHUNK_FIRST = new Comparator<T>() {
		public int compare(T o1, T o2) {
			Point pt1 = new Point(o1.chunkX, o1.chunkY);
			Point pt2 = new Point(o2.chunkX, o2.chunkY);
			double dist1 = pt1.distanceSq(center);
			double dist2 = pt2.distanceSq(center);
			return (int) (dist1 - dist2);
		}
	};
	
	protected final Comparator<T> FARTHEST_CHUNK_FIRST = new Comparator<T>() {
		public int compare(T o1, T o2) {
			Point pt1 = new Point(o1.chunkX, o1.chunkY);
			Point pt2 = new Point(o2.chunkX, o2.chunkY);
			double dist1 = pt1.distanceSq(center);
			double dist2 = pt2.distanceSq(center);
			return (int) (dist2 - dist1);
		}
	};

	protected Thread[] workers;
	protected Object workAvailable = new Object();	// Notification object to tell worker threads of work
	protected boolean workersRunning = false;			// Running condition for quick kill of worker threads
	protected long currentViewIteration;				// Used to keep track of how long a chunk has been out of view
	protected Point center = new Point(0,0);
	
	
	protected ChunkLoader<T> loader;
	protected double chunkSize;
	
	protected final Queue<T> loadingQueue;		// Queue of chunks to be loaded (Use lock)
	protected Lock loadingQueueLock = new ReentrantLock(true);
	
	protected final Queue<T> unloadingQueue;	// Queue of chunks to be unloaded (Use lock)
	protected Lock unloadingQueueLock = new ReentrantLock(true);
	
	protected final Queue<T> loadedChunks;		// List of chunk sorted by age
	protected Lock loadedChunksLock = new ReentrantLock(true);	
	
	public ChunkManager(ChunkLoader<T> loader) {
		this(loader, 4, 40);
	}
	
	public ChunkManager(ChunkLoader<T> loader, int numWorkers) {
		this(loader, numWorkers, 40);
	}
	
	public ChunkManager(ChunkLoader<T> loader, int numWorkers, int initialQueueSize) {
		this.loader = loader;
		this.chunkSize = loader.getChunkSize();
		loadingQueue = new StochasticPriorityQueue<T>(initialQueueSize, CLOSEST_CHUNK_FIRST);
		unloadingQueue = new StochasticPriorityQueue<T>(initialQueueSize, FARTHEST_CHUNK_FIRST);
		loadedChunks = new StochasticPriorityQueue<T>(initialQueueSize, OLDEST_CHUNK_FIRST);
		workers = new Thread[numWorkers];
		for (int i = 0; i < numWorkers; i++) {
			workers[i] = new Thread(new WorkerTask<T>(this));
			workers[i].setName("ChunkManager worker thread #"+(i+1)+"/"+numWorkers+"");
			workers[i].setPriority(Thread.MIN_PRIORITY);
		}
		currentViewIteration = 0;
		this.loader.setManager(this);
	}
	
	/**
	 * Start all worker threads
	 */
	public void start() {
		startWorkers();
	}
	
	/**
	 * Stop all worker threads
	 */
	public void stop() {
		stopWorkers();
	}

	public ChunkLoader<T> getLoader() {
		return loader;
	}
	
	/**
	 * @return How many chunks are loaded?
	 */
	public int getNumLoaded() {
		acquire("loadedChunks", loadedChunksLock);
		int num = loadedChunks.size();
		release("loadedChunks", loadedChunksLock);
		return num;
	}
	
	/**
	 * Returns the chunk object for position x, y. Does not load it. See: loadChunk(), touch()
	 */
	public T getChunk(double x, double y) {
		Point p = getChunkCoordinate(x, y);
		return loader.getChunk(p.x, p.y);
	}
	
	/**
	 * Queues the chunk for loading. See: getChunk(), unload(), touch()
	 */
	public void load(T chunk) {
		queueLoad(chunk);
		touch(chunk);
	}
	
	/**
	 * Queues the chunk for unloading. See: getChunk(), loadChunk()
	 */
	public void unload(T chunk) {
		queueUnload(chunk);
		touch(chunk);
	}
	
	/**
	 * Should touch all chunks that should continue to load or stay loaded. see touch()
	 * update the 'center' variable for determining load and unload priority. center represents
	 * the center of the visible area of chunks. chunks in the loading queue are more likely
	 * to load first if they are closer, while chunks in the unloading queue are more likely
	 * to unload first if they are farther.
	 */
	public abstract void touchAll();
	
	/**
	 * Call once per update() to keep chunks "relevant". Relevant chunks will load and stay loaded
	 * until they have gone some number of iterations without being touched. See: ChunkLoader.getMaxAge()
	 */
	public void touch(T chunk) {
		chunk.lastSeen = currentViewIteration;
		if (!chunk.isLoaded()) queueLoad(chunk);
	}
	
	/**
	 * Increases the age of all chunks and manages the loading and unloading queues
	 */
	public void update() {
		// For unloading
		currentViewIteration++;
		
		// Touch currently in use chunks
		touchAll();
		
		// Unload old chunks
		acquire("loadedChunks", loadedChunksLock);
		long maxAge = loader.getMaxChunkAge();
		T chunk = loadedChunks.peek();
		while (chunk != null && (currentViewIteration - chunk.lastSeen) > maxAge) {
			queueUnload(chunk);
			loadedChunks.remove(chunk);
			chunk = loadedChunks.peek();
		}
		release("loadedChunks", loadedChunksLock);
		
		// Stop loading off-screen chunks
		acquire("loadingQueue", loadingQueueLock);
		Iterator<T> iterator = loadingQueue.iterator();
		while (iterator.hasNext()) {
			chunk = iterator.next();
			if ((currentViewIteration - chunk.lastSeen) > 0) {
				chunk.loading = false;
				iterator.remove();
			}
		}
		release("loadingQueue", loadingQueueLock);
	}

	protected Point getChunkCoordinate(double x, double y) {
		Point coord = new Point();
		coord.x = (int) Math.floor(x / chunkSize);
		coord.y = (int) Math.floor(y / chunkSize);
		return coord;
	}
	
	protected void queueLoad(T chunk) {
		if (!chunk.canLoad()) return;
		
		acquire("unloadingQueue/"+chunk, unloadingQueueLock, chunk.lock);
		
		if (unloadingQueue.remove(chunk)) {
			chunk.unloading = false;
		}
		
		release("unloadingQueue/"+chunk, unloadingQueueLock, chunk.lock);
		acquire("loadingQueue/"+chunk, loadingQueueLock, chunk.lock);
		
		if (!loadingQueue.contains(chunk) && chunk.canLoad()) {
			chunk.loading = true;
			loadingQueue.offer(chunk);
			debug("Added "+chunk+" to load queue");
			synchronized (workAvailable) {
				workAvailable.notify();
			}
		}

		release("loadingQueue/"+chunk, loadingQueueLock, chunk.lock);
	}
	
	protected void queueUnload(T chunk) {
		if (!chunk.canUnload()) return;
		
		acquire("loadingQueue/"+chunk, loadingQueueLock, chunk.lock);
		
		if (loadingQueue.remove(chunk)) {
			chunk.loading = false;
		}

		release("loadingQueue/"+chunk, loadingQueueLock, chunk.lock);
		acquire("unloadingQueue/"+chunk, unloadingQueueLock, chunk.lock);
		
		if (!unloadingQueue.contains(chunk) && chunk.canUnload()) {
			chunk.unloading = true;
			unloadingQueue.offer(chunk);
			debug("Added "+chunk+" to unload queue");
			synchronized (workAvailable) {
				workAvailable.notify();
			}
		}

		release("unloadingQueue/"+chunk, unloadingQueueLock, chunk.lock);
	}
	
	private void addLoaded(T chunk) {
		acquire("loadedChunks", loadedChunksLock);
		loadedChunks.offer(chunk);
		release("loadedChunks", loadedChunksLock);
	}
	
	private static void debug(String string) {
		//System.out.println("[ChunkManager] "+string);
	}
	
	protected static void acquire(String resource, Lock... locks) {
		boolean allAcquired = false;
		debug(Thread.currentThread().getName()+" acquiring "+resource+" ("+locks.length+" locks)");
		int sleepTime = 50;
		while (!allAcquired) {
			int i;
			allAcquired = true;
			for (i = 0; i < locks.length; i++) {
				try {
					boolean acquired = locks[i].tryLock(50, TimeUnit.MILLISECONDS);
					if (!acquired) {
						allAcquired = false;
						break;
					}
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!allAcquired) {
				Lock failed = locks[i];
				for (i--; i >= 0; i--) {
					locks[i].unlock();
				}
				debug(Thread.currentThread().getName()+" acquire failed. ("+failed+") Sleeping...");
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				sleepTime *= 2;
			} else {
				sleepTime = 50;
			}
		}
	}
	
	protected static void release(String resource, Lock... locks) {
		debug(Thread.currentThread().getName()+" releasing "+resource+" ("+locks.length+" locks)");
		for (int i = locks.length-1; i >= 0; i--) {
			locks[i].unlock();
		}
	}
	
	private void startWorkers() {
		debug("Starting worker threads...");
		workersRunning = true;
		for (int i = 0; i < workers.length; i++) {
			workers[i].start();
			debug(workers[i].getName()+" has been started.");
		}
		debug("Started.");
	}
	
	private void stopWorkers() {
		debug("Stopping worker threads...");
		workersRunning = false;
		synchronized (workAvailable) {
			workAvailable.notifyAll();
		}
		for (int i = 0; i < workers.length; i++) {
			boolean joined = false;
			while (!joined) {
				try {
					workers[i].join();
					debug(workers[i].getName()+" has been stopped.");
					joined = true;
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		debug("Stopped.");
	}
	
	private static class WorkerTask<T extends Chunk<T>> implements Runnable {
		
		ChunkManager<T> manager;
		Job<T> myJob;
		
		public WorkerTask(ChunkManager<T> manager) {
			this.manager = manager;
			myJob = new Job<T>(manager);
		}
		
		public void run() {
			while (manager.workersRunning) {
			    doWork();
			}
		}
		
		private void debug(String msg) {
			ChunkManager.debug(msg);
		}
		
		private void doWork() {
			while (!getJob(myJob).isAssigned()) {
				try {
					debug(Thread.currentThread().getName()+" is waiting for work.");
					synchronized (manager.workAvailable) {
						manager.workAvailable.wait();
					}
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				if (!manager.workersRunning) return;
			}
			myJob.complete();
		}

		private Job<T> getJob(Job<T> job) {			
			// Look for unloading jobs first
			debug(Thread.currentThread().getName()+" locking unloading queue");
			acquire("unloadingQueue", manager.unloadingQueueLock);
			job.chunk = manager.unloadingQueue.poll();
			if (job.chunk != null) {
				acquire("chunk.lock", job.chunk.lock);
				job.type = Job.Type.UNLOAD;
				debug(Thread.currentThread().getName()+" locking "+job.chunk);
				release("unloadingQueue", manager.unloadingQueueLock);
				debug(Thread.currentThread().getName()+" unlocked unloading queue");
				return job;
			}
			release("unloadingQueue", manager.unloadingQueueLock);
			debug(Thread.currentThread().getName()+" unlocked unloading queue");
			
			// Look for loading jobs
			debug(Thread.currentThread().getName()+" locking loading queue");
			acquire("loadingQueue", manager.loadingQueueLock);
			job.chunk = manager.loadingQueue.poll();
			if (job.chunk != null) {
				debug(Thread.currentThread().getName()+" locking "+job.chunk);
				acquire("chunk.lock", job.chunk.lock);
				job.type = Job.Type.LOAD;
				release("loadingQueue", manager.loadingQueueLock);
				debug(Thread.currentThread().getName()+" unlocked loading queue");
				return job;
			}
			release("loadingQueue", manager.loadingQueueLock);
			debug(Thread.currentThread().getName()+" unlocked loading queue");

			job.type = Job.Type.UNASSIGNED;
			
			return job;
		}
	}
	
	/**
	 * Job class that represents a chunk related job returned 
	 * by the WorkerTask getJob method and used by Tasks.
	 */
	private static class Job<T extends Chunk<T>> {
		ChunkManager<T> manager;
		
		enum Type { UNASSIGNED, LOAD, UNLOAD };
		Type type = Type.UNASSIGNED;
		T chunk = null;
		
		public Job(ChunkManager<T> manager) {
			this.manager = manager;
		}
		
		public boolean isAssigned() {
			return type != Type.UNASSIGNED;
		}
		
		private void debug(String msg) {
			ChunkManager.debug(msg);
		}
		
		public void complete() {
			if (this.chunk == null) {
				debug("Error: Job does not refer to a chunk");
				return;
			}
			
			// Do job
			switch (type) {
				case LOAD:
					chunk.internalLoad();
					manager.addLoaded(chunk);
					debug(chunk+" loaded.");
					break;
				case UNLOAD:
					chunk.internalUnload();
					debug(chunk+" unloaded.");
					break;
				default: 
					debug("Error: Job type is UNASSIGNED");
					return;
			}

			debug(Thread.currentThread().getName()+" unlocking "+chunk);
			release(chunk.toString(), chunk.lock);
			
			// Clear job info
			this.type = Type.UNASSIGNED;
			this.chunk = null;
		}
	}
	
}
