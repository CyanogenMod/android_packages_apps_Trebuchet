package org.cyanogenmod.launcher.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.launcher3.R;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.List;

/**
 * Created by Schoen on 7/25/15.
 */
public class CMHomeAdapter extends RecyclerView.Adapter<CMHomeAdapter.ViewHolder>{

    Context mContext;
    List<CMHomeCard> mCards;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        //contact
        LinearLayout contactCard;
        ImageView contactImage1;
        ImageView contactImage2;
        ImageView contactImage3;
        ImageView contactImage4;

        //calendar card
        LinearLayout calendarCard;
        LinearLayout eventContainer;

        //news card
        LinearLayout newsCard;
        ImageView newsImage;
        TextView newsTitle;
        TextView sourceAndTime;


        ViewHolder(View cardView, int cardType){
            super(cardView);

            switch(cardType){
                case 0:
                    contactCard = (LinearLayout)itemView.findViewById(R.id.contact_card);
                    contactImage1 = (ImageView)itemView.findViewById(R.id.contact_image_one);
                    contactImage2 = (ImageView)itemView.findViewById(R.id.contact_image_two);
                    contactImage3 = (ImageView)itemView.findViewById(R.id.contact_image_three);
                    contactImage4 = (ImageView)itemView.findViewById(R.id.contact_image_four);
                    break;
                case 1:
                    calendarCard = (LinearLayout)itemView.findViewById(R.id.calendar_card);
                    eventContainer = (LinearLayout)itemView.findViewById(R.id.event_container);
                    break;
                case 2:
                    newsCard = (LinearLayout)itemView.findViewById(R.id.news_card);
                    newsImage = (ImageView)itemView.findViewById(R.id.news_image);
                    newsTitle = (TextView)itemView.findViewById(R.id.news_title);
                    sourceAndTime = (TextView)itemView.findViewById(R.id.news_source_time);
                    break;
                case 3:
                    newsCard = (LinearLayout)itemView.findViewById(R.id.news_card);
                    newsImage = (ImageView)itemView.findViewById(R.id.news_image);
                    newsTitle = (TextView)itemView.findViewById(R.id.news_title);
                    sourceAndTime = (TextView)itemView.findViewById(R.id.news_source_time);
                    break;
            }
        }

    }

    CMHomeAdapter(Context context, List<CMHomeCard> cards){
        mContext = context;
        mCards = cards;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView){
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemViewType(int position) {

        Log.w("HAX", "get item view type");
        //default type
        int viewType = 3;

        if(position == 0){
            //contact card
            viewType = 0;
        }
        if(position == 1){
            //calendar card
            viewType = 1;
        }
        if(position == 2){
            viewType = 2;
        }

        return viewType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int cardType){
        View v = null;

        switch(cardType){
            case 0:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.contact_card, viewGroup, false);
                Log.w("HAX","feature first");
                break;
            case 1:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.calendar_card, viewGroup, false);
                Log.w("HAX","feature");
                break;
            case 2:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.news_card_first, viewGroup, false);
                Log.w("HAX","item first");
                break;
            case 3:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.news_card, viewGroup, false);
                Log.w("HAX","item");
                break;
        }

        ViewHolder vh = new ViewHolder(v, cardType);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder vh,int i){

        if(i == 0){
            setupContact(vh.contactImage1);
            setupContact(vh.contactImage2);
            setupContact(vh.contactImage3);
            setupContact(vh.contactImage4);
        }

        if(i == 1){
            int numEvents = getUpcomingEventCount();
            getEventData();

            for(int e = 0; e < numEvents; e++){
                createEventEntry(vh.eventContainer, e);
            }

        }

        if(i > 1){
            createNewsCard(vh, i);
        }


        Log.w("HAX","we binded");
    }

    @Override
    public int getItemCount(){
        return mCards.size();
    }

    private void setupContact(View view){
        ImageView iv = (ImageView)view;
        iv.setImageResource(R.drawable.persona2);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //launch the appropriate contact card
            }
        });
    }

    private int getUpcomingEventCount(){
        //get the upcoming calendar events here
        //right now I am just returning an int representing the count available that maxes at 3

        return 2;
    }

    private void getEventData(){
        //doesn't do anything yet
    }

    private void createEventEntry(View view, int eventNum){
        LinearLayout ll = (LinearLayout)view;
        View v = LayoutInflater.from(mContext).inflate(R.layout.calendar_event_item,ll,false);
        TextView startTime = (TextView)v.findViewById(R.id.start_time);
        TextView endTime = (TextView)v.findViewById(R.id.end_time);
        TextView title = (TextView)v.findViewById(R.id.event_title);
        TextView location = (TextView)v.findViewById(R.id.event_location);
        startTime.setText("12:00");
        endTime.setText("to 1:00");
        title.setText("Stand Up");
        location.setText("Conference Room");

        ll.addView(v);
    }

    private void createNewsCard(ViewHolder vh, int i){
        new DownloadImageTask(vh.newsImage)
                .execute("http://slidell-independent.com/wp-content/uploads/2013/01/wsne.jpg");//need to get the url out of the spoof data

        vh.newsTitle.setText("This is a temp title");
        vh.sourceAndTime.setText("This is a temp source and time");

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage){
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls){
            String urldisplay = urls[0];
            Bitmap image = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                image = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return image;
        }
        protected  void onPostExecute(Bitmap result){
            bmImage.setImageBitmap(result);
        }
    }

}
