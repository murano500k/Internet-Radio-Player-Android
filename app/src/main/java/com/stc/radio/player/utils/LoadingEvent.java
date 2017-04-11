package com.stc.radio.player.utils;

/**
 * Created by artem on 3/28/17.
 */

public final class LoadingEvent {
    private int percent;
    private boolean isLoading;

    @Override
    public String toString() {
        return "LoadingEvent{" +
                "percent=" + percent +
                ", isLoading=" + isLoading +
                '}';
    }

    public LoadingEvent(final int percent, final boolean isLoading) {
        this.percent = percent;
        this.isLoading = isLoading;
    }

    public int getPercent() {
        return percent;
    }

    public boolean isLoading() {
        return isLoading;
    }

}
