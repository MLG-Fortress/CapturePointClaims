package com.robomwm.CapturePointClaims.events;

import com.robomwm.CapturePointClaims.CapturePoint;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created on 7/7/2017.
 *
 * @author RoboMWM
 */
public class CaptureFinishedEvent extends Event
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

    private OfflinePlayer attacker;
    private CapturePoint point;

    public CaptureFinishedEvent(CapturePoint point, OfflinePlayer attacker)
    {
        this.attacker = attacker;
        this.point = point;
    }

    public OfflinePlayer getAttacker()
    {
        return attacker;
    }

    public CapturePoint getPoint()
    {
        return point;
    }
}
