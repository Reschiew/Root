package eu.weischer.root.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import eu.weischer.root.application.App;
import eu.weischer.root.application.Logger;

public abstract class RootChannel {
    private static final Logger.LogAdapter log = Logger.getLogAdapter("RootRequest");

    public static class SingleResponseHandler<T> implements Consumer<SingleRequestBlock<T>> {
        private ExecutorService responseExecutor;
        private Consumer<SingleRequestBlock<T>> responseConsumer;

        public SingleResponseHandler(ExecutorService responseExecutor, Consumer<SingleRequestBlock<T>> responseConsumer) {
            this.responseExecutor = responseExecutor;
            this.responseConsumer = responseConsumer;
        }

        public ExecutorService getResponseExecutor() {
            return responseExecutor;
        }
        public Consumer<SingleRequestBlock<T>> getResponseConsumer() {
            return responseConsumer;
        }

        @Override
        public void accept(SingleRequestBlock<T> singleRequestBlock) {
            responseExecutor.submit(App.getConsumer(responseConsumer, singleRequestBlock));
        }
    }
    public static class MultiResponseHandler<T> {
        private ExecutorService responseExecutor;
        private BiConsumer<MultiRequestBlock<T>, T> responseConsumer;

        public MultiResponseHandler(ExecutorService responseExecutor, BiConsumer<MultiRequestBlock<T>, T> responseConsumer) {
            this.responseExecutor = responseExecutor;
            this.responseConsumer = responseConsumer;
        }

        public ExecutorService getResponseExecutor() {
            return responseExecutor;
        }
        public BiConsumer<MultiRequestBlock<T>, T> getResponseConsumer() {
            return responseConsumer;
        }
    }
    public static class SingleResponseBlock<T>{
        public enum ResultStatus {
            successful,
            executionError,
            timeout,
            interrupted,
            cancelled
        }
        private ResultStatus resultStatus = ResultStatus.successful;
        private Exception exception = null;
        private T responseObject = null;

        private SingleResponseBlock(ResultStatus resultStatus, Exception exception, T responseObject) {
            this.resultStatus = resultStatus;
            this.exception = exception;
            this.responseObject = responseObject;
        }

        public T getResponseObject() {
            return responseObject;
        }
        public ResultStatus getResultStatus() {
            return resultStatus;
        }
        public Exception getException() {
            return exception;
        }
        public boolean isSuccessful() {
            return resultStatus == ResultStatus.successful;
        }
        public boolean isCancelled() {
            return resultStatus == ResultStatus.cancelled;
        }
        public boolean isTimedOut() {
            return resultStatus == ResultStatus.timeout;
        }
        public boolean isInterrupted() {
            return resultStatus == ResultStatus.interrupted;
        }
        public boolean hasExecutionError() {
            return exception != null;
        }
    }
    protected static class DoubleLinkedNode {
        protected DoubleLinkedNode predecessor;
        protected DoubleLinkedNode successor;
        protected DoubleLinkedNode() {
            predecessor = this;
            successor = this;
        }
    }
    protected class RequestBlock extends DoubleLinkedNode {
        protected State state = State.created;
        protected Object tag = null;

        protected RequestBlock(){};
        protected RequestBlock(Object tag) {
            this.tag = tag;
        }

        public void setTag(Object tag) {
            if (state == State.created)
                this.tag = tag;
        }
        public State getState() {
            return state;
        }
        public Object getTag() {
            return tag;
        }
        public RootChannel getRootChannel() {
            return RootChannel.this;
        }
        public boolean isMultiRequest() {
            return false;
        }
        public void queue() {
            synchronized(RootChannel.this) {
                if ((state == State.created) && (! RootChannel.this.channelDown)) {
                    state = State.queued;
                    log.v("request block queued");
                    addNode(this);
                }
            }
        }
        public void cancel() {
            synchronized(RootChannel.this) {
                if (state != State.finished) {
                    removeNode(this);
                    log.v("request block cancelled");
                }
                state = State.finished;
            }
        }
    }
    public class SingleRequestBlock<T> extends RequestBlock {
        private Consumer<SingleRequestBlock<T>> responseHandler = null;
        private SingleResponseBlock<T> responseBlock = null;
        private RootFlag rootFlag = null;
        private RootTimer.OneShotTimer timeoutRequest = null;

        protected SingleRequestBlock(){};
        protected SingleRequestBlock(Consumer<SingleRequestBlock<T>> responseHandler) {
            super();
            this.responseHandler = responseHandler;
        }
        protected SingleRequestBlock(Consumer<SingleRequestBlock<T>> responseHandler, Object tag) {
            super(tag);
            this.responseHandler = responseHandler;
        }

        public void setResponseHandler(SingleResponseHandler<T> responseHandler) {
            if (state == State.created)
                this.responseHandler = responseHandler;
        }
        public SingleResponseBlock<T> getResponseBlock() {
            return responseBlock;
        }
        public T getResponseObject() {
            return responseBlock == null ? null : responseBlock.responseObject;
        }
        public void cancel (boolean response) {
            if (response)
                respond(SingleResponseBlock.ResultStatus.cancelled, null, null);
            super.cancel();
        }
        @Override
        public void cancel() {
            cancel(false);
        }
        protected void respond(SingleResponseBlock.ResultStatus resultStatus, Exception exception, T responseObject){
            synchronized(RootChannel.this) {
                if (state == State.queued) {
                    responseBlock = new SingleResponseBlock(resultStatus, exception, responseObject);
                    if (responseHandler != null)
                        responseHandler.accept(this);
                    if (rootFlag != null)
                        rootFlag.setFlag();
                    if (timeoutRequest != null) {
                        timeoutRequest.cancel(false);
                        timeoutRequest = null;
                    }
                }
                cancel();
            }
        }
        public void waitFor() {
            waitFor(-1);
        }
        public void waitFor(long timeOut) {
            try {
                if (state == State.finished) {
                    if (responseBlock == null)
                        responseBlock = new SingleResponseBlock<>(SingleResponseBlock.ResultStatus.executionError, new NullPointerException(), null);
                    return;
                }
                synchronized (RootChannel.this) {
                    rootFlag = new RootFlag();
                }
                RootFlag.WaitResult  waitResult = null;
                if (timeOut >= 0)
                    waitResult = rootFlag.await(timeOut);
                else
                    waitResult = rootFlag.await();
                if (waitResult == RootFlag.WaitResult.interrupted) {
                    responseBlock = new SingleResponseBlock<>(SingleResponseBlock.ResultStatus.interrupted, new InterruptedException(), null);
                    return;
                }
                if (waitResult == RootFlag.WaitResult.timeout) {
                    responseBlock = new SingleResponseBlock<>(SingleResponseBlock.ResultStatus.timeout, new TimeoutException(), null);
                    return;
                }
            } catch (Exception  ex) {
                responseBlock = new SingleResponseBlock<>(SingleResponseBlock.ResultStatus.executionError, ex, null);
                return;
            }
            if (state == State.finished)
                if (responseBlock == null)
                    responseBlock = new SingleResponseBlock<>(SingleResponseBlock.ResultStatus.executionError, new NullPointerException(), null);
        }
        @Override
        public void queue() {
            synchronized(RootChannel.this) {
                if (state == State.created) {
                    super.queue();
                    if (timeoutRequest != null) {
                        timeoutRequest.queue();
                    }
                }
            }
        }
        public void queue (long timeout) {
            if (state == State.created) {
                timeoutRequest = RootTimer.getRootTimer().getOneShotTimer(timeout, (requestBlock)->{
                    synchronized (RootChannel.this) {
                        SingleRequestBlock<T> singleRequestBlock = (SingleRequestBlock<T>) requestBlock.getTag();
                        if (singleRequestBlock.state != State.finished)
                            singleRequestBlock.respond(SingleResponseBlock.ResultStatus.timeout, new TimeoutException(), null);
                    }
                },this);
                queue();
            }
        }
        public void queueWait(long timeout) {
            queue(timeout);
            waitFor();
        }
        public void queueWait() {
            queue();
            waitFor();
        }
    }
    public class MultiRequestBlock<T> extends RequestBlock {
        private MultiResponseHandler<T> responseHandler = null;

        protected MultiRequestBlock(){};
        protected MultiRequestBlock(MultiResponseHandler<T> responseHandler) {
            super();
            this.responseHandler = responseHandler;
        }
        protected MultiRequestBlock(MultiResponseHandler<T> responseHandler, Object tag) {
            super(tag);
            this.responseHandler = responseHandler;
        }

        public void setResponseHandler(MultiResponseHandler<T> responseHandler) {
            if (state == State.created)
                this.responseHandler = responseHandler;
        }
        @Override
        public boolean isMultiRequest() {
            return true;
        }
        protected void respond(T responseObject){
            synchronized(RootChannel.this) {
                if (state == State.queued) {
                    if (responseHandler != null) {
                        BiConsumer<MultiRequestBlock<T>, T> biConsumer = responseHandler.getResponseConsumer();
                        responseHandler.getResponseExecutor().submit(App.getBiConsumer(biConsumer, this, responseObject));
                    }
                }
            }
        }
    }

    public synchronized void shutDown() {
        if (channelDown)
            return;
        log.v("Shutdown");
        channelDown = true;
        DoubleLinkedNode node = null;
        DoubleLinkedNode previousNode =null;
        while(true) {
            synchronized (this) {
                node = listHead.successor;
            }
            if ((node == listHead) || (node == previousNode))
                return;
            previousNode = node;
            try {
                RequestBlock requestBlock = (RequestBlock) node;
                requestBlock.cancel();
            } catch (Exception ex) {
                try {
                    removeNode(node);
                } catch (Exception e) {
                }
            }
        }
    }

    protected enum State { created, queued, finished }

    private DoubleLinkedNode listHead = new DoubleLinkedNode();
    private long listSize = 0;
    private boolean channelDown = false;

    private void addNode (DoubleLinkedNode node)  {
        try {
            node.successor = listHead;
            node.predecessor = listHead.predecessor;
            node.predecessor.successor = node;
            listHead.predecessor = node;
            listSize++;
        } catch (Exception ex) {
            log.e(ex, "Exception during addNode");
        }
    }
    private void removeNode (DoubleLinkedNode node)  {
        try {
            if (node.predecessor != node) {
                node.successor.predecessor = node.predecessor;
                node.predecessor.successor = node.successor;
                node.predecessor = node;
                node.successor = node;
                --listSize;
            }
        } catch (Exception ex) {
            log.e(ex, "Exception during addNode");
        }
    }
}


