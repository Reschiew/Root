package eu.weischer.root.thread;

import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import eu.weischer.root.application.App;
import eu.weischer.root.application.Logger;

public class RootTimer extends RootChannel {
    private static final Logger.LogAdapter log = Logger.getLogAdapter("RootTimer");
    private static long defaultWait =  60000;
    private static RootTimer rootTimer = null;
    public static RootTimer getRootTimer() {
        if (rootTimer == null)
            rootTimer = new RootTimer();
        return rootTimer;
    }

    public final class TimerResponse {
        public TimerResponse(long time) {
            this.time = time;
        }
        public long time;
    }
    private class TimerAttributes implements Comparable {
        private TimerAttributes(long nextTimestamp, RequestBlock requestBlock) {
            this.nextTimestamp = nextTimestamp;
            this.requestBlock = requestBlock;
        }
        protected long nextTimestamp;
        protected RequestBlock requestBlock;
        protected long id = RootTimer.this.getId();

        @Override
        public int compareTo(Object compareObject) {
            TimerAttributes timerAttributes = (TimerAttributes)compareObject;
            if(id == timerAttributes.id)
                return 0;
            if(this.nextTimestamp < timerAttributes.nextTimestamp)
                return -1;
            if(this.nextTimestamp > timerAttributes.nextTimestamp)
                return 1;
            return this.id > timerAttributes.id ? 1 : -1;
        }
        @Override
        public boolean equals(Object object) {
            if ((object == null) || (object.getClass() != this.getClass())){
                return false;
            }
            final RootTimer other = (RootTimer) object;
            return id == other.getId();
        }
        @Override
        public int hashCode() {
            return (int)(id & 0xFFFFFFFF);
        }
    }
    private class PeriodicAttributes extends TimerAttributes {
        private PeriodicAttributes(long nextTimestamp, RequestBlock requestBlock, long period, long limit) {
            super(nextTimestamp, requestBlock);
            this.period = period;
            this.limit = limit;
        }
        private long period;
        private long limit;
    }
    public final class OneShotTimer extends SingleRequestBlock<TimerResponse> {
        public OneShotTimer(long duration, Consumer<SingleRequestBlock<TimerResponse>> responseHandler, Object tag) {
            super(responseHandler, tag);
            timerAttributes = new TimerAttributes(System.currentTimeMillis() + duration, this);
        }
        @Override
        public void queue() {
            synchronized(RootTimer.this) {
                if (ready) {
                    super.queue();
                    if (state == State.queued) {
                        log.v("queue oneshottimer");
                        treeSet.add(timerAttributes);
                        synchFlag.setFlag();
                    }
                }
            }
        }
        @Override
        public void cancel(boolean response) {
            synchronized (RootTimer.this) {
                if (state == State.queued) {
                    log.v("cancel oneshottimer");
                    synchFlag.setFlag();
                    treeSet.remove(timerAttributes);
                }
                super.cancel(response);
            }
        }

        private TimerAttributes timerAttributes;
    }
    public final class PeriodicTimer extends MultiRequestBlock<TimerResponse> {
        public PeriodicTimer(long period, long offset, long limit, MultiResponseHandler<TimerResponse> responseHandler, Object tag) {
            super(responseHandler, tag);
            long nextTimestamp;
            if (offset < 0)
                nextTimestamp = -offset;
            else {
                nextTimestamp = System.currentTimeMillis();
                nextTimestamp -= nextTimestamp % period;
                nextTimestamp += offset;
                if (nextTimestamp < System.currentTimeMillis())
                    nextTimestamp += period;
            }
            if (limit > 0)
                limit =  System.currentTimeMillis() + limit;
            if (limit < 0 )
                limit = -limit;
            if (limit == 0)
                limit = Long.MAX_VALUE;
            periodicAttributes = new PeriodicAttributes(nextTimestamp,  this, period, limit);
            if (nextTimestamp > limit)
                cancel();
        }
        @Override
        public void queue() {
            synchronized(RootTimer.this) {
                if (ready) {
                    super.queue();
                    if (state == State.queued) {
                        log.v("queue periodic timer period=" + periodicAttributes.period);
                        treeSet.add(periodicAttributes);
                        synchFlag.setFlag();
                    }
                }
            }
        }
        @Override
        public void cancel() {
            synchronized (RootTimer.this) {
                if (state != State.finished) {
                    log.v("cancel periodic timer");
                    synchFlag.setFlag();
                    treeSet.remove(periodicAttributes);
                }
                super.cancel();
            }
        }

        private PeriodicAttributes periodicAttributes;
    }

    private long currentId = 0;
    private RootFlag synchFlag = new RootFlag();
    private TreeSet<TimerAttributes> treeSet = new TreeSet<>();
    private boolean ready = false;
    private ExecutorService executorService;

    public RootTimer() {
        executorService = App.getThreadPoolExecutor();
        init();
    }
    public OneShotTimer getOneShotTimer(long duration, Consumer<SingleRequestBlock<TimerResponse>> responseHandler, Object tag) {
        OneShotTimer result = new OneShotTimer(duration, responseHandler, tag);
        return result;
    }
    public OneShotTimer queueOneShotTimer(long duration, Consumer<SingleRequestBlock<TimerResponse>> responseHandler, Object tag) {
        OneShotTimer result = new OneShotTimer(duration, responseHandler, tag);
        result.queue();
        return result;
    }
    public void waitOneShotTimer(long duration) {
        OneShotTimer result = new OneShotTimer(duration, null, null);
        result.queueWait();
    }
    public PeriodicTimer getPeriodicTimer(long period, long offset, long limit, MultiResponseHandler<TimerResponse> responseHandler, Object tag) {
        PeriodicTimer result = new PeriodicTimer(period, offset, limit, responseHandler, tag);
        return result;
    }
    public PeriodicTimer queuePeriodicTimer(long period, long offset, long limit, MultiResponseHandler<TimerResponse> responseHandler, Object tag) {
        PeriodicTimer result = new PeriodicTimer(period, offset, limit, responseHandler, tag);
        result.queue();
        return result;
    }

    private synchronized long getId() {
        return ++currentId;
    }
    private void init() {
        ready = true;
        executorService.submit(() -> {
            try {
                while (true) {
                    RootFlag.WaitResult waitResult = synchFlag.await(getTimeToWait());
                    if (waitResult == RootFlag.WaitResult.interrupted)
                        break;
                }
            } catch (Exception ex) {
                log.e(ex, "Exception during timer execution");
            }
        });
    }
    private synchronized long getTimeToWait() {
        long result = defaultWait;
        synchFlag.clearFlag();
        long  currentTime = System.currentTimeMillis();
        while ((! treeSet.isEmpty()) && (treeSet.first().nextTimestamp <= currentTime)) {
            TimerAttributes timerAttributes = treeSet.pollFirst();
            if (timerAttributes.requestBlock.isMultiRequest()) {
                PeriodicAttributes periodicAttributes = (PeriodicAttributes) timerAttributes;
                ((PeriodicTimer) periodicAttributes.requestBlock).respond(new TimerResponse(periodicAttributes.nextTimestamp));
                log.v("Periodic timer signalled period=" + periodicAttributes.period);
                periodicAttributes.nextTimestamp += periodicAttributes.period;
                if (periodicAttributes.nextTimestamp > periodicAttributes.limit)
                    periodicAttributes.requestBlock.cancel();
                else
                    treeSet.add(periodicAttributes);
            } else {
                ((OneShotTimer) timerAttributes.requestBlock).respond(SingleResponseBlock.ResultStatus.successful,
                        null, new TimerResponse(timerAttributes.nextTimestamp));
                log.v("Oneshottimer signalled");
            }
        }
        if (! treeSet.isEmpty())
            result = treeSet.first().nextTimestamp - currentTime;
        return result;
    }
}
