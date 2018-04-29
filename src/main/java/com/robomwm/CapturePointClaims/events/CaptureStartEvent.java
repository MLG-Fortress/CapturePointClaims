package com.robomwm.CapturePointClaims.events;

import com.robomwm.CapturePointClaims.point.CapturePoint;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by RoboMWM on 1/21/2017.
 */
public class CaptureStartEvent extends Event
{
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private CapturePoint point;

    public CaptureStartEvent(CapturePoint point)
    {
        this.point = point;
    }

    public CapturePoint getPoint()
    {
        return point;
    }
}
