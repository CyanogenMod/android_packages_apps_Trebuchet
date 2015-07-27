package org.cyanogenmod.launcher.home;

/**
 * Created by Schoen on 7/24/15.
 */
public class CMHomeNews extends CMHomeCard {

    String imageURL;
    String title;
    String source;
    long time;
    String url;

    public CMHomeNews(
            String imageURL,
            String title,
            String source,
            long time,
            String url){

        this.imageURL = imageURL;
        this.title = title;
        this.source = source;
        this.time = time;
        this.url = url;

    }

}
