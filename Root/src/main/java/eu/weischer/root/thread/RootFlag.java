package eu.weischer.root.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RootFlag {
    public enum WaitResult {flagSet, timeout, interrupted}
    private boolean state = false;
    private Lock flagLock = null;
    private Condition flagCondition = null;
    public boolean getFlag() {
        return state;
    }
    public WaitResult await() {
        if (state)
            return WaitResult.flagSet;
        synchronized (this) {
            if (flagLock == null)
                flagLock = new ReentrantLock();
            flagCondition = flagLock.newCondition();
            flagLock.lock();
        }
        try {
            while(! state)
                flagCondition.await();
            return WaitResult.flagSet;
        } catch (InterruptedException ex) {
            return WaitResult.interrupted;
        } finally {
            flagLock.unlock();
        }
    }
    public WaitResult await(long timeout) {
        if (state)
            return WaitResult.flagSet;
        long timeLimit = System.currentTimeMillis() + timeout;
        synchronized (this) {
            if (flagLock == null)
                flagLock = new ReentrantLock();
            flagCondition = flagLock.newCondition();
        }
        flagLock.lock();
        try {
            while(! state)  {
                long remaining = timeLimit - System.currentTimeMillis();
                if ((remaining <= 0) || (! flagCondition.await(timeout, TimeUnit.MILLISECONDS)))
                    return WaitResult.timeout;
            }
            return WaitResult.flagSet;
        } catch (InterruptedException ex) {
            return WaitResult.interrupted;
        } finally {
            flagLock.unlock();
        }
    }
    public synchronized void setFlag() {
        if (state)
            return;
        state = true;
        if (flagCondition == null)
            return;
        flagLock.lock();
        flagCondition.signal();
        flagLock.unlock();
    }
    public synchronized void clearFlag() {
        if(state) {
            state = false;
            flagCondition = null;
        }
    }
}
