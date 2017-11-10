package com.th.eoss.limz;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.eoss.brain.MessageObject;
import com.eoss.brain.NodeEvent;
import com.eoss.brain.Session;
import com.eoss.brain.command.line.WakeupCommandNode;
import com.eoss.brain.net.Context;
import com.eoss.brain.net.ContextListener;
import com.eoss.brain.net.GAEStorageContext;
import com.eoss.brain.net.GAEWebIndexSupportContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecognitionListener, TextToSpeech.OnInitListener, ContextListener {

    Handler handler;

    LocationManager locationManager;
    LocationListener locationListener;
    Location lastLocation;

    Context bothoiContext;
    Session session;

    SpeechRecognizer speech;
    TextToSpeech textToSpeech;
    ConstraintLayout layout;

    ToggleButton gps;
    Button like;
    Button dislike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.th.eoss.limz.R.layout.activity_main);

        locationManager = (LocationManager) this.getSystemService(Activity.LOCATION_SERVICE);

        layout = (ConstraintLayout) findViewById(R.id.layout);
        gps = (ToggleButton) findViewById(R.id.gps);
        like = (Button) findViewById(R.id.like);
        dislike = (Button) findViewById(R.id.dislike);

        if (handler == null) {
            handler = new Handler();
        }

        gps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

                Log.d("Button try", "" + checked);
                Log.d("Button status", "" + gps.isChecked());
                if (checked) {
                    enableGPS();
                } else {
                    disableGPS();
                    loadPrivateContext();
                }
            }
        });


        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                process(getResources().getString(R.string.yes));
            }
        });

        dislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                process(getResources().getString(R.string.no));
            }
        });

    }

    private void loadPublicContext(Location location) {
        Log.d("FETCHGPS", "" + location);
    }

    private void loadPrivateContext() {

        Log.d("Context", "Private");
        bothoiContext = new GAEStorageContext("minnie");
        session = new Session(bothoiContext);
        session.learning = true;

        new Thread() {
            public void run() {
                new WakeupCommandNode(session).execute(null);
            }
        }.start();
    }

    private void enableGPS() {
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Your GPS is disabled!", Toast.LENGTH_LONG);
            gps.setChecked(false);
        } else {
            Toast.makeText(this, "Start explore!", Toast.LENGTH_LONG);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private void disableGPS() {
        if (locationListener!=null)
            locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        layout.setBackground(ContextCompat.getDrawable(this, R.drawable.muay_sleeping));

        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(this, this, "com.google.android.textToSpeech");
        }


        if (gps.isChecked()) {
            enableGPS();
        } else {
            loadPrivateContext();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        disableGPS();
        stopListening();
        textToSpeech.stop();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private Intent recognizerIntent() {

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().getCountry());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500);

        return recognizerIntent;
    }

    private void startListening() {

        if (gps.isChecked()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

            } else {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation!=null && isBetterLocation(lastKnownLocation, lastLocation)) {
                    Log.d("FECT", "" + isBetterLocation(lastKnownLocation, lastLocation));
                    loadPublicContext(lastKnownLocation);
                    lastLocation = lastKnownLocation;
                }
            }
        }

        if (speech==null) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(this);
        }
        speech.startListening(recognizerIntent());
        like.setEnabled(true);
        dislike.setEnabled(true);

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                layout.setBackground(ContextCompat.getDrawable(
                        MainActivity.this, R.drawable.muay_listening));
            }
        });
    }

    private void stopListening() {

        layout.setBackground(ContextCompat.getDrawable(this, R.drawable.muay_smiling));
        like.setEnabled(false);
        dislike.setEnabled(false);

        if (speech!=null) {
            //speech.stopListening();
            //speech.cancel();
            speech.destroy();
            speech = null;
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "R.string.error_audio_error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "R.string.error_client";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "R.string.error_permission";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "R.string.error_network";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "R.string.error_timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "R.string.error_no_match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "R.string.error_busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "R.string.error_server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "R.string.error_timeout";
                break;
            default:
                message = "R.string.error_understand";
                break;
        }
        stopListening();
        startListening();
    }

    @Override
    public void onResults(Bundle bundle) {

        ArrayList<String> matches = bundle
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        List<String> list = new ArrayList<>();
        for (String text:matches) {
            list.add(text);
        }

        String voice = list.get(0);

        process(voice);

    }

    private void process(String msg) {

        stopListening();

        final MessageObject messageObject = MessageObject.build(msg);

        new Thread() {
            @Override
            public void run() {
                final String response = session.parse(messageObject);
                speak(response);
            }
        }.start();
    }

    @Override
    public void onPartialResults(Bundle bundle) {
    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(new Locale("th"));

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                boolean talking;

                @Override
                public void onStart(final String s) {
                    talking = true;
                    new Thread() {
                        public void run() {
                            while (talking) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        layout.setBackground(ContextCompat.getDrawable(
                                                MainActivity.this, R.drawable.muay_speaking));


                                    }
                                });
                                if (!talking) break;
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        layout.setBackground(ContextCompat.getDrawable(
                                                MainActivity.this, R.drawable.muay_listening));


                                    }
                                });
                            }
                        }
                    }.start();
                }

                @Override
                public void onDone(final String s) {
                    talking = false;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            startListening();
                        }
                    });
                }

                @Override
                public void onError(String s) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            startListening();
                        }
                    });
                }
            });

            speak(getResources().getString(R.string.hello));

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    @Override
    public void callback(final NodeEvent nodeEvent) {
        if (nodeEvent.event==NodeEvent.Event.LateReply) {

        }
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        /*
        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        */
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

}
