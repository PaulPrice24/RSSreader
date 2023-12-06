package com.paulprice.rssreader;

import static android.content.ContentValues.TAG;
import static android.widget.Toast.LENGTH_SHORT;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.HttpsURLConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class RssReader extends AppCompatActivity {

    private ListView myRss;
    private ArrayList<String> titles;
    private ArrayList<String> links;
    private ArrayList<String> imageUrls;
    private ArrayList<String> descriptions;

    private ArrayList<String> published;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rss_reader);

        // Initialize UI components
        myRss = findViewById(R.id.myRss);
        titles = new ArrayList<>();
        links = new ArrayList<>();
        imageUrls = new ArrayList<>();
        descriptions = new ArrayList<>();
        published = new ArrayList<>();

        // Set item click listener to open the link in a browser
        myRss.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openLinkInBrowser(position);
            }
        });

        // Start background task to fetch and process RSS feed
        new ProcessInBackground().execute();
    }

    // Open the selected link in a browser
    private void openLinkInBrowser(int position) {
        Uri uri = Uri.parse(links.get(position));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    // Simplify network connection to handle HTTPS
    private InputStream getInputStream(URL url) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            return connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Background task to fetch and process RSS feed
    private class ProcessInBackground extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;
        private String value;

        private int number;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Show progress dialog while fetching data
            progressDialog = new ProgressDialog(RssReader.this);
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference(currentFirebaseUser.getUid());

                CountDownLatch latch = new CountDownLatch(1);

                // Add ValueEventListener to retrieve the value
                myRef.child("Link").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // This method is called once with the initial value and again whenever
                        // data at this location is updated.
                        value = dataSnapshot.getValue(String.class);
                        if (value != null) {
                            // Handle the retrieved value (value) here
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle any errors that may occur.
                        Toast.makeText(RssReader.this, "Error retrieving value", Toast.LENGTH_SHORT).show();
                        latch.countDown();
                    }
                });

                myRef.child("Items").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Get the value from the dataSnapshot
                        if (dataSnapshot.exists()) {
                            // dataSnapshot.getValue() returns an Object, so you need to cast it to the appropriate type
                            number = dataSnapshot.getValue(Integer.class);

                        } else {
                            // Handle the case where "Items" doesn't exist in the database
                            Toast.makeText(RssReader.this, "No data found", Toast.LENGTH_SHORT).show();
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle errors, if any
                        Toast.makeText(RssReader.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        latch.countDown();
                    }
                });

                latch.await();

                // URL for the RSS feed
                URL url = new URL(value);

                // XML parsing setup
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(getInputStream(url), "UTF_8");

                // Variables for parsing
                boolean insideItem = false;
                int eventType = xpp.getEventType();

                String imageUrl = null;

                int maxItems = number;
                int itemsCount = 0;

                // Loop through XML elements
                while (eventType != XmlPullParser.END_DOCUMENT && itemsCount <= maxItems) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equalsIgnoreCase("item")) {
                            insideItem = true;
                            itemsCount++;
                        } else if (xpp.getName().equalsIgnoreCase("title")) {
                            if (insideItem) {
                                titles.add(xpp.nextText());
                            }
                        } else if (xpp.getName().equalsIgnoreCase("description")) {
                            if (insideItem) {
                                descriptions.add(xpp.nextText());
                            }
                        } else if (xpp.getName().equalsIgnoreCase("link")) {
                            if (insideItem) {
                                links.add(xpp.nextText());
                            }
                        } else if (xpp.getName().equalsIgnoreCase("pubDate")) {
                            if (insideItem) {
                                published.add(xpp.nextText());
                            }
                        } else if (xpp.getName().equalsIgnoreCase("media:content")) {
                            // Check if the current tag is media:content
                            imageUrl = xpp.getAttributeValue(null, "url");
                            if (insideItem && imageUrl != null) {
                                // Add the image URL to the list
                                imageUrls.add(imageUrl);
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")) {
                        insideItem = false;
                    }
                    eventType = xpp.next();
                }

            } catch (XmlPullParserException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // Update UI with the fetched titles
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(RssReader.this, R.layout.list_item_layout, R.id.titleTextView, titles) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    // Get references to the ImageView and TextView
                    ImageView imageView = view.findViewById(R.id.imageView);
                    TextView titleTextView = view.findViewById(R.id.titleTextView);
                    TextView descriptionTextView = view.findViewById(R.id.descriptionTextView);
                    TextView publishedTextView = view.findViewById(R.id.publishedTextView);

                    // Set title
                    titleTextView.setText(titles.get(position));

                    // Set description
                    descriptionTextView.setText(descriptions.get(position));

                    // Set published date and time
                    publishedTextView.setText(published.get(position));

                    // Log image URL
                    String imageUrl = imageUrls.get(position);
                    Log.d("ImageUrlDebug", "Image URL at position: " + imageUrl);

                    // Load image using Picasso with error handling
                    Picasso.get().load(imageUrl).placeholder(R.drawable.img).error(R.drawable.img).into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            // Image loaded successfully
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            // Retry the image loading
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Picasso.get().load(imageUrl).placeholder(R.drawable.img).error(R.drawable.img).into(imageView);
                            }, 2000);
                        }
                    });

                    return view;
                }
            };

            myRss.setAdapter(adapter);

            // Dismiss the progress dialog
            progressDialog.dismiss();
        }
    }

    public void Subscribe(View view) {
        Intent i = new Intent(getApplicationContext(), Subscribe.class);
        startActivity(i);
    }
}