package com.robomwm.CapturePointClaims.events;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
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
    private OfflinePlayer defender;
    private boolean defended;

    public CaptureFinishedEvent(OfflinePlayer attacker, OfflinePlayer defender, boolean defenderWon)
    {
        this.attacker = attacker;
        this.defender = defender;
        defended = defenderWon;
    }

    public OfflinePlayer getAttacker()
    {
        return attacker;
    }

    public OfflinePlayer getDefender()
    {
        return defender;
    }

    public boolean isDefended()
    {
        return defended;
    }
}
