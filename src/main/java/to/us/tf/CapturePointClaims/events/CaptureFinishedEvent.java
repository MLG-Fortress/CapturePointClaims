package to.us.tf.CapturePointClaims.events;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
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

    private Clan attackingClan;
    private Clan defendingClan;
    private boolean defended;

    public CaptureFinishedEvent(Clan attackers, Clan defenders, boolean defendersWon)
    {
        attackingClan = attackers;
        defendingClan = defenders;
        defended = defendersWon;
    }

    public Clan getAttackingClan()
    {
        return attackingClan;
    }

    public Clan getDefendingClan()
    {
        return defendingClan;
    }

    public boolean isDefended()
    {
        return defended;
    }
}
