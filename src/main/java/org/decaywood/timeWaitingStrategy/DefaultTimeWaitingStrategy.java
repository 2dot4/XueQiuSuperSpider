package org.decaywood.timeWaitingStrategy;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author: decaywood
 * @date: 2015/11/24 16:21
 */

/**
 * 默认超时等待策略
 */
public class DefaultTimeWaitingStrategy implements TimeWaitingStrategy {

    private final long timeWaitingThreshold;
    private final long baseWaiting;
    private final int retryTime;
    private final double jitterFactor;

    public DefaultTimeWaitingStrategy() {
        this(10000, 500, 10, 0.3);
    }


    /**
     *
     * @param timeWaitingThreshold 超时等待阈值（最多等待阈值指定时间然后进入下一次请求尝试）
     * @param timeWaiting 起始等待时间
     * @param retryTime 重试次数（超过次数抛出超时异常）
     */
    public DefaultTimeWaitingStrategy(final long timeWaitingThreshold, long timeWaiting, int retryTime) {
        this(timeWaitingThreshold, timeWaiting, retryTime, 0.3);
    }

    public DefaultTimeWaitingStrategy(final long timeWaitingThreshold,
                                      long timeWaiting,
                                      int retryTime,
                                      double jitterFactor) {
        this.timeWaitingThreshold = timeWaitingThreshold;
        this.baseWaiting = timeWaiting;
        this.retryTime = retryTime;
        this.jitterFactor = Math.max(0d, jitterFactor);
    }


    @Override
    public void waiting(int loopTime) {
        try {
            long exponential = (long) (baseWaiting * Math.pow(2, loopTime));
            long capped = Math.min(exponential, timeWaitingThreshold);
            double minFactor = Math.max(0d, 1 - jitterFactor);
            double maxFactor = 1 + jitterFactor;
            long sleepTime = (long) (ThreadLocalRandom.current()
                    .nextDouble(minFactor, maxFactor) * capped);
            Thread.sleep(Math.max(100L, sleepTime));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int retryTimes() {
        return retryTime;
    }
}
