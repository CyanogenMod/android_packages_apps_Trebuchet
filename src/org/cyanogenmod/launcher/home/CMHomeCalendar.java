package org.cyanogenmod.launcher.home;

/**
 * Created by Schoen on 7/24/15.
 */
public class CMHomeCalendar extends CMHomeCard {

    long startTime;
    long endTime;
    String title;
    String location;

    public CMHomeCalendar(
            long startTime,
            long endTime,
            String title,
            String location){

        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.location = location;

    }

}
