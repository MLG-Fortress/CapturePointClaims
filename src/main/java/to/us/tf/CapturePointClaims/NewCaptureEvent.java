package to.us.tf.CapturePointClaims;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by RoboMWM on 1/21/2017.
 */
public class NewCaptureEvent extends Event
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

    private RegionCoordinates region;

    public NewCaptureEvent(RegionCoordinates region)
    {

    }
}
