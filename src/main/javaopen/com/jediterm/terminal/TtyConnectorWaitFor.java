package com.jediterm.terminal;

import com.google.common.base.Predicate;
import org.slf4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TtyConnectorWaitFor {
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TtyConnectorWaitFor.class);

  private final Future<?> myWaitForThreadFuture;
  private final BlockingQueue<Predicate<Integer>> myTerminationCallback = new ArrayBlockingQueue<Predicate<Integer>>(1);

  public void detach() {
    myWaitForThreadFuture.cancel(true);
  }


  public TtyConnectorWaitFor(final TtyConnector ttyConnector, final ExecutorService executor) {
    myWaitForThreadFuture = executor.submit(new Runnable() {
      @Override
      public void run() {
        int exitCode = 0;
        try {
          while (true) {
            try {
              exitCode = ttyConnector.waitFor();
              break;
            }
            catch (InterruptedException e) {
              LOG.error(e.getMessage());
            }
          }
        }
        finally {
          try {
            if (!myWaitForThreadFuture.isCancelled()) {
              myTerminationCallback.take().apply(exitCode);
            }
          }
          catch (InterruptedException e) {
            LOG.error(e.getMessage());
          }
        }
      }
    });
  }

  public void setTerminationCallback(Predicate<Integer> r) {
    myTerminationCallback.offer(r);
  }
}
