package com.bng.scheduler;
/*
 * File   : $Source: /usr/local/cvs/opencms/src/org/opencms/scheduler/CmsSchedulerThreadPool.java,v $
 * Date   : $Date: 2009-06-09 10:02:56 $
 * Version: $Revision: 1.18 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * 
 * This library is based to some extend on code from the 
 * OpenSymphony Quartz project. Original copyright notice:
 * 
 * Copyright James House (c) 2001-2005
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple thread pool used for the Quartz scheduler in OpenCms.<p>
 * 
 * @author Alexander Kandzior 
 * @author James House
 * @author Juergen Donnerstag
 *
 * @version $Revision: 1.18 $ 
 * 
 * @since 6.0.0 
 */
public class MyThreadPool implements ThreadPool {

    /** The log object for this class. */
	private final Logger LOG = LoggerFactory.getLogger(getClass());
    private int currentThreadCount;

    private boolean inheritGroup;

    private boolean inheritLoader;

    private int initialThreadCount;

	private boolean isShutdown;

    private boolean makeThreadsDaemons;

    private int maxThreadCount;

    private Runnable nextRunnable;

    private Object nextRunnableLock;

    private ThreadGroup threadGroup;

    private String threadNamePrefix;

    private int threadPriority;

    private CmsSchedulerThread[] workers;

    /**
     * Create a new <code>CmsSchedulerThreadPool</code> with default values.
     * 
     * This will create a pool with 0 initial and 10 maximum threads running 
     * in normal priority.<p>
     * 
     * @see #CmsSchedulerThreadPool(int, int, int)
     */
    public MyThreadPool() {

        this(0, 10, Thread.NORM_PRIORITY);
    }

    /**
     * Create a new <code>CmsSchedulerThreadPool</code> with the specified number
     * of threads that have the given priority.
     * 
     * The OpenCms scheduler thread pool will initially start with provided number of
     * active scheduler threads.
     * When a thread is requested by the scheduler, and no "free" threads are available,
     * a new thread will be added to the pool and used for execution. The pool 
     * will be allowed to grow until it has reached the configured number 
     * of maximum threads.<p>
     * 
     * @param initialThreadCount the initial number of threads for the pool
     * @param maxThreadCount maximum number of threads the pool is allowed to grow
     * @param threadPriority the thread priority for the scheduler threads
     * 
     * @see java.lang.Thread
     */
    public MyThreadPool(int initialThreadCount, int maxThreadCount, int threadPriority) {

        inheritGroup = true;
        inheritLoader = true;
        nextRunnableLock = new Object();
        threadNamePrefix = "OpenCms: Scheduler Thread ";
        makeThreadsDaemons = true;
        this.initialThreadCount = initialThreadCount;
        currentThreadCount = 0;
        this.maxThreadCount = maxThreadCount;
        this.threadPriority = threadPriority;
    }

    /**
     * @see org.quartz.spi.ThreadPool
     * 
     * @return if the pool should be blocked for available threads
     */
    public int blockForAvailableThreads() {

        // Waiting will be done in runInThread so we always return 1
        return 1;
    }

    /**
     * @see org.quartz.spi.ThreadPool#getPoolSize()
     */
    public int getPoolSize() {

        return currentThreadCount;
    }

    /**
     * Returns the thread priority of the threads in the scheduler pool.<p>
     * 
     * @return the thread priority of the threads in the scheduler pool 
     */
    public int getThreadPriority() {

        return threadPriority;
    }

    /**
     * @see org.quartz.spi.ThreadPool#initialize()
     */
    public void initialize() throws SchedulerConfigException {

        if ((maxThreadCount <= 0) || (maxThreadCount > 200)) {
            throw new SchedulerConfigException("");
        }
        if ((initialThreadCount < 0) || (initialThreadCount > maxThreadCount)) {
            throw new SchedulerConfigException("");
        }
        if ((threadPriority <= 0) || (threadPriority > 9)) {
            throw new SchedulerConfigException("");
        }

        if (inheritGroup) {
            threadGroup = Thread.currentThread().getThreadGroup();
        } else {
            // follow the threadGroup tree to the root thread group
            threadGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parent = threadGroup;
            while (!parent.getName().equals("main")) {
                threadGroup = parent;
                parent = threadGroup.getParent();
            }
            threadGroup = new ThreadGroup(parent, this.getClass().getName());
        }

        if (inheritLoader) {
            LOG.debug(
                Thread.currentThread().getName());
        }

        // create the worker threads and start them
        workers = new CmsSchedulerThread[maxThreadCount];
        for (int i = 0; i < initialThreadCount; ++i) {
            growThreadPool();
        }
    }

    /**
     * Run the given <code>Runnable</code> object in the next available
     * <code>Thread</code>.<p>
     * 
     * If while waiting the thread pool is asked to
     * shut down, the Runnable is executed immediately within a new additional
     * thread.<p>
     * 
     * @param runnable the <code>Runnable</code> to run
     * @return true if the <code>Runnable</code> was run
     */
    public boolean runInThread(Runnable runnable) {

        if (runnable == null) {
            return false;
        }

        if (isShutdown) {
            LOG.debug("");
            return false;
        }

        boolean hasNextRunnable;
        synchronized (nextRunnableLock) {
            // must synchronize here to avoid potential double checked locking
            hasNextRunnable = (nextRunnable != null);
        }

        if (hasNextRunnable || (currentThreadCount == 0)) {
            // try to grow the thread pool since other runnables are already waiting
            growThreadPool();
        }

        synchronized (nextRunnableLock) {

            // wait until a worker thread has taken the previous Runnable
            // or until the thread pool is asked to shutdown
            while ((nextRunnable != null) && !isShutdown) {
                try {
                    nextRunnableLock.wait(1000);
                } catch (InterruptedException e) {
                    // can be ignores
                }
            }

            // during normal operation, not shutdown, set the nextRunnable
            // and notify the worker threads waiting (getNextRunnable())
            if (!isShutdown) {
                nextRunnable = runnable;
                nextRunnableLock.notifyAll();
            }
        }

        // if the thread pool is going down, execute the Runnable
        // within a new additional worker thread (no thread from the pool)
        // note: the synchronized section should be as short (time) as
        // possible as starting a new thread is not a quick action
        if (isShutdown) {
            CmsSchedulerThread thread = new CmsSchedulerThread(
                this,
                threadGroup,
                threadNamePrefix + "(final)",
                threadPriority,
                false,
                runnable);
            thread.start();
        }

        return true;
    }

    /**
     * Terminate any worker threads in this thread group.<p>
     * 
     * Jobs currently in progress will be allowed to complete.<p>
     */
    public void shutdown() {

        shutdown(true);
    }

    /**
     * Terminate all threads in this thread group.<p>
     * 
     * @param waitForJobsToComplete if true,, all current jobs will be allowed to complete
     */
    public void shutdown(boolean waitForJobsToComplete) {

        isShutdown = true;

        // signal each scheduler thread to shut down
        for (int i = 0; i < currentThreadCount; i++) {
            if (workers[i] != null) {
                workers[i].shutdown();
            }
        }

        // give waiting (wait(1000)) worker threads a chance to shut down
        // active worker threads will shut down after finishing their
        // current job
        synchronized (nextRunnableLock) {
            nextRunnableLock.notifyAll();
        }

        if (waitForJobsToComplete) {
            // wait until all worker threads are shut down
            int alive = currentThreadCount;
            while (alive > 0) {
                alive = 0;
                for (int i = 0; i < currentThreadCount; i++) {
                    if (workers[i].isAlive()) {
                        try {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("");
                            }

                            // note: with waiting infinite - join(0) - the application 
                            // may appear to 'hang' 
                            // waiting for a finite time however requires an additional loop (alive)
                            alive++;
                            workers[i].join(200);
                        } catch (InterruptedException e) {
                            // can be ignored
                        }
                    }
                }
            }

            int activeCount = threadGroup.activeCount();
            if ((activeCount > 0) && LOG.isInfoEnabled()) {
                LOG.info("");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("");
            }
        }
    }

    /**
     * Dequeue the next pending <code>Runnable</code>.<p>
     * 
     * @return the next pending <code>Runnable</code>
     * @throws InterruptedException if something goes wrong
     */
    protected Runnable getNextRunnable() throws InterruptedException {

        Runnable toRun = null;

        // Wait for new Runnable (see runInThread()) and notify runInThread()
        // in case the next Runnable is already waiting.
        synchronized (nextRunnableLock) {
            if (nextRunnable == null) {
                nextRunnableLock.wait(1000);
            }

            if (nextRunnable != null) {
                toRun = nextRunnable;
                nextRunnable = null;
                nextRunnableLock.notifyAll();
            }
        }

        return toRun;
    }

    /**
     * Grows the thread pool by one new thread if the maximum pool size 
     * has not been reached.<p>
     */
    private void growThreadPool() {
    	
        if (currentThreadCount < maxThreadCount) {
            // if maximum number is not reached grow the thread pool
            synchronized (nextRunnableLock) {
                workers[currentThreadCount] = new CmsSchedulerThread(this, threadGroup, threadNamePrefix
                    + currentThreadCount, threadPriority, makeThreadsDaemons);
                workers[currentThreadCount].start();
                if (inheritLoader) {
                    workers[currentThreadCount].setContextClassLoader(Thread.currentThread().getContextClassLoader());
                }
                // increas the current size
                currentThreadCount++;
//                System.out.println("growing thread. current count: "+ currentThreadCount);
                // notify the waiting threads
                nextRunnableLock.notifyAll();
            }
        }
    }
    
    protected void reduceThreadPool(CmsSchedulerThread thread){
    	 if (currentThreadCount > initialThreadCount) {
             // if minimum number is not reached reduce the thread pool
             synchronized (nextRunnableLock) {
            	 int i=0;
                 for(;i<currentThreadCount;i++){
                	 if(workers[i].equals(thread)){
                		 while(i<currentThreadCount-1){
                			 workers[i] = workers[i+1]; 
                			 i++;
                		 }
                		 break;
                	 }
                 }
                 workers[i]=null;
                 // decrease the current size
                 currentThreadCount--;
//                 System.out.println("thread reduced. count : "+currentThreadCount);
                 // notify the waiting threads
                 nextRunnableLock.notifyAll();
             }
         }
    }

	@Override
	public void setInstanceId(String schedInstId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInstanceName(String schedName) {
		// TODO Auto-generated method stub
		
	}
    public int getInitialThreadCount() {
		return initialThreadCount;
	}

	public void setInitialThreadCount(int m_initialThreadCount) {
		this.initialThreadCount = m_initialThreadCount;
	}

	public int getMaxThreadCount() {
		return maxThreadCount;
	}

	public void setMaxThreadCount(int m_maxThreadCount) {
		this.maxThreadCount = m_maxThreadCount;
	}
	
	
	
	
	/**
	 * A worker thread for the OpenCms scheduler.<p>
	 * 
	 * @author Alexander Kandzior 
	 *  
	 * @version $Revision: 1.15 $ 
	 * 
	 * @since 6.0.0 
	 */
	public class CmsSchedulerThread extends Thread {

	    /** The log object for this class. */
		private final Logger LOG = LoggerFactory.getLogger(getClass());

	    /** The scheduler thread pool this thread belongs to. */
	    private MyThreadPool m_pool;

	    /** A flag that signals the thread to terminate. */
	    private boolean m_run;

	    /** A runnable class. */
	    private Runnable m_runnable;

	    /**
	     * Create a scheduler thread that runs continuosly,
	     * waiting for new runnables to be provided by the scheduler thread pool.<p>
	     * 
	     * @param pool the pool to use
	     * @param threadGroup the thread group to use
	     * @param threadName the name for the thread
	     * @param prio the priority of the thread
	     * @param isDaemon controls if this should be a deamon thread or not
	     */
	    CmsSchedulerThread(
	        MyThreadPool pool,
	        ThreadGroup threadGroup,
	        String threadName,
	        int prio,
	        boolean isDaemon) {

	        this(pool, threadGroup, threadName, prio, isDaemon, null);
	    }

	    /**
	     * Create a scheduler thread that runs the specified runnable exactly once.<p>
	     * 
	     * @param pool the pool to use
	     * @param threadGroup the thread group to use
	     * @param threadName the name for the thread
	     * @param prio the priority of the thread
	     * @param isDaemon controls if this should be a deamon thread or not
	     * @param runnable the runnable to run
	     */
	    CmsSchedulerThread(
	        MyThreadPool pool,
	        ThreadGroup threadGroup,
	        String threadName,
	        int prio,
	        boolean isDaemon,
	        Runnable runnable) {

	        super(threadGroup, threadName);
	        m_run = true;
	        m_pool = pool;
	        m_runnable = runnable;
	        setPriority(prio);
	        setDaemon(isDaemon);
	    }

	    /**
	     * Loop, executing targets as they are received.<p>
	     */
	    @Override
	    public void run() {

	        boolean runOnce = (m_runnable != null);

	        while (m_run) {
	            setPriority(m_pool.getThreadPriority());
	            try {
	                if (m_runnable == null) {
	                    m_runnable = m_pool.getNextRunnable();
	                }

	                if (m_runnable != null) {
	                    m_runnable.run();
	                }
	            } catch (InterruptedException e) {
	                LOG.error("");
	            } catch (Throwable t) {
	                LOG.error("");
	            } finally {
	                if (runOnce) {
	                    m_run = false;
	                }
	                m_runnable = null;
	                
	                reduceThreadPool(this);
	                
	            }
	        }
	        if (LOG.isDebugEnabled()) {
	            LOG.debug("");
	        }
	    }

	    /**
	     * Signal the thread that it should terminate.<p>
	     */
	    void shutdown() {

	        m_run = false;
	    }
	}
	
}