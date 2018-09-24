package com.robomwm.CapturePointClaims.point;

import com.robomwm.CapturePointClaims.CapturePointClaims;
import com.robomwm.CapturePointClaims.region.Region;
import com.robomwm.CapturePointClaims.events.CaptureFinishedEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

/**
 * Contains information about a point in the process of being captured
 * Automatically manages and performs end-game (time runs out or point being captured)
 */
public class CapturePoint
{
    //If successfully defended, how long the post prevents future captures from occurring
    private long lockoutTime;
    //captureProgress (decrements to 0)
    private int captureProgress;
    //Maximum amount of time to capture point, in ticks
    private int ticksToEndGame = 18000; //15 minutes
    //Used to determine end game, and when point should be unlocked
    private Long timeCaptured = 0L;
    //Determines who won
    private boolean defended = true;
    private Region region;
    private OfflinePlayer defender; //store defender in case of capture

    //Capturing a new point
    public CapturePoint(Region region)
    {
        this.region = region;
        this.captureProgress = region.getHealth();
        this.defender = region.getOwner();

        //set capture time based on amount of fuel
        ticksToEndGame -= Math.min(region.getFuel(), 12000);
        //consume fuel
        region.addFuel(-12000);

        if (defender == null)
        {
            lockoutTime = TimeUnit.MINUTES.toMillis(30);
            return;
        }

        //set lockout time (used to prevent successive capture attempts if point is successfully defended)

        final long ninetyDaysInMilliseconds = TimeUnit.DAYS.toMillis(90);
        
        //Get time since capture
        long time = Math.min(ninetyDaysInMilliseconds, System.currentTimeMillis() - region.getTimeCaptured());
        //Divide by 90 days (max time) to get percentage
        time = time / ninetyDaysInMilliseconds;
        //Multiply by max lockout time (3 days)
        time = time * TimeUnit.HOURS.toMillis(72);
    }

    public OfflinePlayer getDefender()
    {
        return defender;
    }

    public boolean isDefended()
    {
        return defended;
    }

    public boolean isEnded()
    {
        return this.timeCaptured > 0L;
    }

    private long getTimeCaptured()
    {
        return this.timeCaptured;
    }

    public double getCaptureProgress()
    {
        return (double)this.captureProgress / (double)region.getHealth();
    }

    public int getTicksToEndGame()
    {
        return this.ticksToEndGame;
    }

    public int getSecondsToEndGame()
    {
        return this.ticksToEndGame / 20;
    }

    public Region getRegion()
    {
        return this.region;
    }

    public boolean isLockExpired()
    {
        //return getTimeCaptured() != 0L && this.getTimeCaptured() < (System.currentTimeMillis() - 86400000); //1 day
        return getExpirationTimeRemaining() != null && getExpirationTimeRemaining() <= 0;
    }

    /**
     * @return Time remaining until lockout expires, in seconds
     */
    public Long getExpirationTimeRemaining()
    {
        if (!isEnded())
            return null;
        return TimeUnit.MILLISECONDS.toSeconds((this.getTimeCaptured() + lockoutTime) - System.currentTimeMillis());
    }

    /**
     * @return 0.0 - 1.0, decreasing to 0
     * TODO: add checks????
     */
    public Double getExpirationTimeAsPercentage()
    {
        if (!isEnded())
            return null;
        final double timeRemainingInMilliseconds = (this.getTimeCaptured() + lockoutTime) - System.currentTimeMillis();
        return timeRemainingInMilliseconds / lockoutTime;
    }


    public void tick(int ticksToTick)
    {
        if (isEnded())
            return;
        this.ticksToEndGame = this.ticksToEndGame - ticksToTick;
    }

    public int decrementCaptureProgress(int captureProgress)
    {
        if (isEnded())
            return this.captureProgress;
        this.captureProgress -= captureProgress;
        return this.captureProgress;
    }

    Boolean checkOrEndGame(CapturePointClaims instance, Player player)
    {
        if (this.isEnded())
            return this.defended;
        if (this.ticksToEndGame <= 0)
            this.defended = true;
        else if (captureProgress <= 0)
            this.defended = false;
        else
            return null;

        //"Game over"
        this.timeCaptured = System.currentTimeMillis();

        if (!this.defended && player != null)
        {
            region.changeOwner(player, instance);
            region.setTimeCaptured(System.currentTimeMillis());
            captureProgress = 100;
            lockoutTime = 0;
        }

        region.setHealth(captureProgress);
        region.getRegionManager().saveRegion(region);

        instance.getServer().getPluginManager().callEvent(new CaptureFinishedEvent(this, player));

        return this.defended;
    }
}
