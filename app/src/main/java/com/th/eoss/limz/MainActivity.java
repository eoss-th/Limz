package com.th.eoss.limz;

import android.content.Intent;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.eoss.brain.MessageObject;
import com.eoss.brain.NodeEvent;
import com.eoss.brain.Session;
import com.eoss.brain.command.data.ImportRawDataFromWebCommandNode;
import com.eoss.brain.command.http.GetCommandNode;
import com.eoss.brain.command.line.BizWakeupCommandNode;
import com.eoss.brain.command.line.WakeupCommandNode;
import com.eoss.brain.net.Context;
import com.eoss.brain.net.ContextListener;
import com.eoss.brain.net.FileContext;
import com.eoss.brain.net.FileIndexSupportContext;
import com.eoss.brain.net.GAEStorageContext;
import com.eoss.brain.net.GAEWebIndexSupportContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecognitionListener, TextToSpeech.OnInitListener, ContextListener {

    Handler handler;

    Context bothoiContext;
    Session session;

    SpeechRecognizer speech;
    TextToSpeech textToSpeech;
    ConstraintLayout layout;
    TextView text;
    ToggleButton web;
    Button like;
    Button dislike;
    Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.th.eoss.limz.R.layout.activity_main);

        layout = (ConstraintLayout) findViewById(R.id.layout);
        text = (TextView) findViewById(R.id.text);
        web = (ToggleButton) findViewById(R.id.web);
        like = (Button) findViewById(R.id.like);
        dislike = (Button) findViewById(R.id.dislike);
        next = (Button) findViewById(R.id.next);

        if (handler==null) {
            handler = new Handler();
        }

        if (bothoiContext==null) {
            bothoiContext =
                    //new GAEWebIndexSupportContext(
                    new GAEStorageContext("minnie");
        }

        if (session==null) {
            session = new Session(bothoiContext);

            new Thread() {
                public void run() {
                    new WakeupCommandNode(session).execute(null);
                }
            }.start();
            //session.learning = true;
        }


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

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                process(getResources().getString(R.string.next));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        layout.setBackground(ContextCompat.getDrawable(this, R.drawable.muay_sleeping));

        if (textToSpeech==null) {
            textToSpeech = new TextToSpeech(this, this, "com.google.android.textToSpeech");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening();
        textToSpeech.stop();
        if(textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private Intent recognizerIntent() {

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getLanguage()+"_"+Locale.getDefault().getCountry());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().getCountry());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500);
        return recognizerIntent;
    }

    private void startListening() {

        if (speech==null) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(this);
        }
        speech.startListening(recognizerIntent());
        like.setEnabled(true);
        dislike.setEnabled(true);
        next.setEnabled(true);

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
        web.setChecked(false);
        like.setEnabled(false);
        dislike.setEnabled(false);
        next.setEnabled(false);

        if (speech!=null) {
            //speech.stopListening();
            //speech.cancel();
            speech.destroy();
            speech = null;
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        web.setChecked(true);
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

        text.setText(msg);

        final MessageObject messageObject = MessageObject.build(msg);

        new Thread() {
            @Override
            public void run() {
                final String response = session.parse(messageObject).replace(">", "แล้วไปที่");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        layout.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.muay_speaking));
                        text.setText(response);
                    }
                });

                final GetCommandNode getCommandNode = new GetCommandNode(session, null, "");
                if (getCommandNode.matched(MessageObject.build(messageObject, response))) {
                    getCommandNode.execute(MessageObject.build(messageObject, response));
                    speak("yes");
                } else {

                    String text = response;
                    if (text.contains("https://"))
                        text = response.replace("https://", "");
                    if (text.contains("http://"))
                        text = response.replace("http://", "");
                    if (text.contains("/"))
                        text = text.substring(0, text.indexOf("/"));

                    speak(text);
                }
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
                                                MainActivity.this, R.drawable.muay_listening));


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
                                                MainActivity.this, R.drawable.muay_speaking));


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

            handler.post(new Runnable() {
                @Override
                public void run() {
                    stopListening();
                    String webText = nodeEvent.messageObject.toString();
                    webText = webText.length()>20?webText.substring(0, 20)+"...":webText;
                    text.setText(webText);
                }
            });

            final String [] messages = nodeEvent.messageObject.toString().split(System.lineSeparator());
            if (messages.length>0) {

                final ImportRawDataFromWebCommandNode importRawDataFromWebCommandNode = new ImportRawDataFromWebCommandNode(session, null, getFilesDir());
                new Thread() {
                    public void run() {
                        importRawDataFromWebCommandNode.execute(MessageObject.build(messages[1]));
                    }
                }.start();

                speak(messages[0].substring(messages[0].indexOf(" ... ")+5).replace(".", "").trim());
            }
        }
    }
}
