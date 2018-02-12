package to.us.tf.CapturePointClaims;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import to.us.tf.CapturePointClaims.events.CaptureFinishedEvent;

import java.util.concurrent.TimeUnit;

public class CapturePoint
{
    /**
     * Contains information about a point in the process of being captured
     * Automatically manages and performs end-game (time runs out or point being captured)
     */
    private final long LOCK_TIME = TimeUnit.MINUTES.toMillis(30L); //point locks for 30 minutes
    private final long LOCK_TIME_SECONDS = TimeUnit.MINUTES.toMillis(30L) / 1000L;

    //defendingClan (owning clan to notify, generally. Null if unclaimed)
    private Clan owningClan;
    //captureProgress (decrements to 0)
    private int captureProgress;
    //Maximum amount of time to capture point, in ticks
    private int ticksToEndGame = 12000; //10 minutes
    //Used to determine end game, and when point should be unlocked
    private Long timeCaptured = 0L;
    //Determines who won
    private boolean defended = true;
    private Region region;

    //Capturing a new point
    public CapturePoint(Clan attackingClan, Clan owningClan, Region region)
    {
        this.owningClan = owningClan;
        this.region = region;
        this.captureProgress = region.getHealth();
    }

    public Clan getOwningClan()
    {
        return this.owningClan;
    }

    public String getOwningClanColorTag()
    {
        if (getOwningClan() == null)
            return "Wilderness";
        return getOwningClan().getColorTag();
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
     * @return Seconds remaining
     */
    public Long getExpirationTimeRemaining()
    {
        if (!isEnded())
            return null;
        //return (((this.getTimeCaptured() + TimeUnit.DAYS.toMillis(1L)) - System.currentTimeMillis()) / 1000); //1 day
        return (((this.getTimeCaptured() + LOCK_TIME) - System.currentTimeMillis()) / 1000);
    }

    /**
     * @return 0.0 - 1.0, increasing to 1.0
     * TODO: add checks????
     */
    public Double getExpirationTimeAsPercentage()
    {
        if (!isEnded())
            return null;
        return (LOCK_TIME_SECONDS - Double.valueOf(getExpirationTimeRemaining())) / LOCK_TIME_SECONDS;
    }


    public void setTicksToEndGame(int ticksToTick)
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

    public Boolean checkOrEndGame(CapturePointClaims instance, Clan clan)
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
        if (!this.defended)
        {
            region.changeOwner(clan, instance);
            captureProgress = 100;
            region.setCaptureTime(15);
        }

        region.setHealth(captureProgress);
        region.getRegionManager().saveRegion(region);

        instance.getServer().getPluginManager().callEvent(new CaptureFinishedEvent(clan, owningClan, defended));

        return this.defended;
    }

}
