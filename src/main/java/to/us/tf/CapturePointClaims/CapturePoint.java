package to.us.tf.CapturePointClaims;

import net.sacredlabyrinth.phaed.simpleclans.Clan;

import java.util.concurrent.TimeUnit;

public class CapturePoint
{
    /**
     * Contains information about a point in the process of being captured
     * Automatically manages and performs end-game (times runs out or point being captured)
     */
    //capturingClan
    private Clan attackingClan;
    //defendingClan (owning clan to notify, generally. Null if unclaimed)
    private Clan owningClan;
    //captureProgress (decrements to 0)
    private int captureProgress = 100;
    //Maximum amount of time to capture point, in ticks
    private int ticksToEndGame = 6000; //5 minutes
    //Used to determine end game, and when point should be unlocked
    private Long timeCaptured = 0L;
    //Determines who won
    private boolean defended = true;
    Region region;

    //Capturing a new point
    public CapturePoint(Clan attackingClan, Clan owningClan, Region region)
    {
        this.attackingClan = attackingClan;
        this.owningClan = owningClan;
        this.region = region;
    }

    public Clan getAttackingClan()
    {
        return this.attackingClan;
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

    public int getCaptureProgress()
    {
        return this.captureProgress;
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

    public boolean isExpired()
    {
        return getTimeCaptured() != 0L && this.getTimeCaptured() < (System.currentTimeMillis() - 86400000);
    }

    /**
     * @return Seconds remaining
     */
    public Long getExpirationTimeRemaining()
    {
        if (!isExpired())
            return null;
        return (((this.getTimeCaptured() + TimeUnit.DAYS.toMillis(1L)) - System.currentTimeMillis()) / 1000);
    }

    /**
     * @return 0.0 - 1.0, increasing to 1.0
     */
    public Double getExpirationTimeAsPercentage()
    {
        if (!isExpired())
            return null;
        return (TimeUnit.DAYS.toSeconds(1L) - Double.valueOf(getExpirationTimeRemaining())) / TimeUnit.DAYS.toSeconds(1L);
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

    public Boolean checkOrEndGame(CapturePointClaims instance)
    {
        if (this.isEnded())
            return this.defended;
        if (this.ticksToEndGame <= 0)
            this.defended = true;
        else if (this.getCaptureProgress() <= 0)
            this.defended = false;
        else
            return null;
        //TODO: fire event
        this.timeCaptured = System.currentTimeMillis();
        if (!this.defended)
            region.changeOwner(attackingClan, instance);
        return this.defended;
    }

}
