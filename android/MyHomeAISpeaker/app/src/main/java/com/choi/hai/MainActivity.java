package com.choi.hai;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.io.IOAgent;
import com.naver.speech.clientapi.SpeechConfig;
import com.naver.speech.clientapi.SpeechRecognitionException;
import com.naver.speech.clientapi.SpeechRecognitionListener;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.naver.speech.clientapi.SpeechRecognizer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ai.kitt.snowboy.AppResCopy;
import ai.kitt.snowboy.Constants;
import ai.kitt.snowboy.RecordingThread;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public class MainActivity extends AppCompatActivity implements SpeechRecognitionListener
{
    class ButtonHandler
    {
        private static final int DOUBLE_CLICK = 150;    // 500 msec
        private long pressLeftTime;
        private long pressRightTime;

        public ButtonHandler()
        {
            pressLeftTime = System.currentTimeMillis();
            pressRightTime = System.currentTimeMillis();
        }

        public void pressLeft()
        {
            pressLeftTime = System.currentTimeMillis();
            process(true);
        }

        public  void pressRight()
        {
            pressRightTime = System.currentTimeMillis();
            process(false);
        }

        private void process(boolean left)
        {
            boolean flagBreak = false;

            if ( left ) {
                if ( pressLeftTime - pressRightTime <= DOUBLE_CLICK ) {
                    flagBreak = true;
                }
                else {
                    volumeDown();
                    if ( musicPlayer == null || musicPlayer.isPlaying() == false )
                        recordingThread.testSound();
                }
            }
            else {
                if ( pressRightTime - pressLeftTime <= DOUBLE_CLICK ) {
                    flagBreak = true;
                }
                else {
                    volumeUp();
                    if ( musicPlayer == null || musicPlayer.isPlaying() == false )
                       recordingThread.testSound();
                }
            }

            if ( flagBreak ) {
                if ( musicPlayer != null && musicPlayer.isPlaying() )
                    musicPlayer.stop();
                else {
                    // 음성인식
                    recordingThread.stopRecording();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    ioAgent.showFade(true);
                    launchVoiceRecognition();
                }
            }
        }
    }

    private SpeechRecognizer nhnRecognizer;
    private RecordingThread recordingThread;

    private StatusManager statusManager;
    private IOAgent ioAgent;

    private MediaPlayer player = new MediaPlayer();
    private MediaPlayer musicPlayer;

    private Gpio leftButton;
    private Gpio rightButton;
    private ButtonHandler buttonHandler = new ButtonHandler();

    /*
     *
     */
    private Handler recogHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);

            switch (message) {
                case MSG_HOTWORD_LISTENING:
                    ioAgent.showFade(false);
                    recordingThread.startRecording();
                    break;

                case MSG_SPEECH_RECOGNITION:
                    ioAgent.showFade(true);
                    launchVoiceRecognition();
                    break;

                case MSG_SR_TIMEOUT:
                    nhnRecognizer.stop();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    recordingThread.startRecording();
                    break;

                case MSG_HOTWORD_DETECT:
                    recordingThread.stopRecording();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    statusManager.detectedHotword();
                    break;

                case MSG_CONVERSATION_RESPONSE:
                    try {
                        final String vfile = msg.obj.toString();
                        player.reset();
                        player.setOnCompletionListener (new MediaPlayer.OnCompletionListener() {
                            public void onCompletion (MediaPlayer mp) {
                                File f = new File(vfile);
                                f.delete();
                            }
                        });

                        player.setDataSource(vfile);
                        player.prepare();
                        player.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case MSG_ERROR:
                    System.out.println("@.@ ERROR : " + msg.obj);
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private AiServiceClient  serviceClient;
    private NaverTTS naverTTS;
    private ServiceThread serviceThread;

    private class ServiceThread extends Thread
    {
        public Handler serviceHandler;

        public void run()
        {
            Looper.prepare();

            serviceHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if ( msg.what == 0405 ) {
                        callService(msg.obj.toString());
                    }
                }
            };

            Looper.loop();
        }

        /**
         *
         * @param say
         */
        private void callService(String say)
        {
            try
            {
                ioAgent.showProgress(false);

                JSONObject payload = new JSONObject();
                JSONObject req = new JSONObject();
                req.put("text", say);
                payload.put("request", req);

                JSONObject rs = new JSONObject(serviceClient.say(payload.toString()).toString());
                String code = rs.getString("returnCode");
                if ( "OK".equals(code) ) {
                    JSONArray ja = rs.getJSONArray("response");

                    for(int i = 0; i < ja.length(); i++) {
                        JSONObject obj  = ja.getJSONObject(i);
                        String action = obj.getString("action");

                        if ( "speak".equals(action) ) {
                            final String voiceFile = naverTTS.tts(obj.getString("data"));
                            if ( voiceFile != null ) {
                                try {
                                    player.reset();
                                    player.setOnCompletionListener (new MediaPlayer.OnCompletionListener() {
                                        public void onCompletion (MediaPlayer mp) {
                                            File f = new File(voiceFile);
                                            f.delete();
                                        }
                                    });

                                    player.setDataSource(voiceFile);
                                    player.prepare();
                                    player.start();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            ioAgent.showProgress(true);
                        }
                        else  if ( "playMusic".equals(action) ) {
                            try {
                                JSONObject data = obj.getJSONObject("data");

                                // Authdata
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Authorization","Basic " + data.getString("authToken"));

                                musicPlayer.reset();
                                musicPlayer.setOnCompletionListener (new MediaPlayer.OnCompletionListener() {
                                    public void onCompletion (MediaPlayer mp) {
                                        musicPlayer.stop();
                                    }
                                });

                                Uri uri = Uri.parse(data.getString("songUrl"));

                                try {
                                    musicPlayer.setDataSource(MainActivity.this, uri, headers);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                musicPlayer.prepareAsync();
                                musicPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    public void onPrepared(MediaPlayer mp) {
                                        ioAgent.showProgress(true);
                                        mp.start();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                ioAgent.showProgress(true);
                            }
                        }
                        else if ( "task".equals(action) ) {
                            ioAgent.showProgress(true);

                            if ( "BREAK".equals(obj.getString("data")) ) {
                                if ( musicPlayer.isPlaying() ) {
                                    musicPlayer.stop();
                                    musicPlayer.reset();
                                }
                            }

                            ioAgent.showProgress(true);
                        }
                    }
                }
                else {
                    ioAgent.showProgress(true);
                    System.out.println("@.@ $$$$$$$$$$$$$$$$$$$$$$$  SERVICE ERROR ==> " + rs.getString("errorString"));
                }
            }catch (Exception e) {
                e.printStackTrace();
            }finally{
                //ioAgent.showProgress(true);
            }
        }
    }

    private int maxVolume;
    private int minVolume;
    private int curVolume;

    /**
     *
     */
    private void prepairVolumn()
    {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        minVolume = 1;
    }

    /**
     *
     */
    private void volumeUp()
    {
        if ( curVolume + 1 < maxVolume ) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ++curVolume, 0);
        }
    }

    /**
     *
     */
    private void volumeDown()
    {
        if ( curVolume - 1 > minVolume ) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, --curVolume, 0);
        }
    }

    /**
     *
     */
    private void launchVoiceRecognition()
    {
        try {
            nhnRecognizer.recognize(new SpeechConfig(
                    SpeechConfig.LanguageType.KOREAN.KOREAN,
                    SpeechConfig.EndPointDetectType.AUTO));
        } catch (SpeechRecognitionException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppResCopy.copyResFromAssetsToSD(this);

        try {
            prepairVolumn();

            // 서버 서비스
            serviceClient = new AiServiceClient(AiSpeakerEnv.GRPC_SERVER, AiSpeakerEnv.GRPC_PORT);

            // I2C
            PeripheralManagerService manager = new PeripheralManagerService();
            ioAgent = new IOAgent(manager);

            recordingThread = new RecordingThread(recogHandler);

            nhnRecognizer = new SpeechRecognizer(this, AiSpeakerEnv.CLIENT_ID);
            nhnRecognizer.setSpeechRecognitionListener(this);

            statusManager = new StatusManager(recogHandler);
            statusManager.start();

            PeripheralManagerService service = new PeripheralManagerService();

            leftButton = service.openGpio("BCM17");
            leftButton.setDirection(Gpio.DIRECTION_IN);
            leftButton.setEdgeTriggerType(Gpio.EDGE_FALLING);
            leftButton.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    System.out.println("GPIO changed, LEFT pressed");
                    buttonHandler.pressLeft();
                    return true;
                }
            });

            rightButton = service.openGpio("BCM27");
            rightButton.setDirection(Gpio.DIRECTION_IN);
            rightButton.setEdgeTriggerType(Gpio.EDGE_FALLING);
            rightButton.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    System.out.println("GPIO changed, RIGHT pressed");
                    buttonHandler.pressRight();
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        naverTTS = new NaverTTS();
        serviceThread = new ServiceThread();
        serviceThread.start();

        /*
        MUSIC
         */
        musicPlayer = new MediaPlayer();
        musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();
        nhnRecognizer.initialize();
        ioAgent.showStartup();

        try {
            musicPlayer.setDataSource(Constants.DEFAULT_WORK_SPACE+"start.wav");
            musicPlayer.prepare();
            musicPlayer.start();
        } catch (IOException e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        nhnRecognizer.release();
        recordingThread.stopRecording();

        try {
            leftButton.close();
            rightButton.close();
        } catch (IOException e) {
        } finally {
            leftButton = null;
            rightButton = null;
        }

        try {
            serviceClient.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }
    @Override
    public void onInactive() {
        System.out.println("@.@ onInactive");
        statusManager.speechRecognition(StatusManager.EVENT_INACTIVE);
    }

    @Override
    public void onReady() {
        System.out.println("@.@ onReady");
    }

    @Override
    public void onRecord(short[] speech) {
        System.out.println("@.@ onRecord " + speech.length);
        statusManager.speechRecognition(StatusManager.EVENT_RECORD);
    }

    @Override
    public void onPartialResult(String partialResult) {
        System.out.println("@.@ Partial Result!! (" + partialResult + ")");
        statusManager.speechRecognition(StatusManager.EVENT_PARTIAL_RESULT);
    }

    @Override
    public void onEndPointDetected() {
        System.out.println("@.@ onEndPointDetected");
        statusManager.speechRecognition(StatusManager.EVENT_END_DETECT);
    }

    @Override
    public void onResult(SpeechRecognitionResult finalResult)
    {
        String text = finalResult.getResults().get(0);
        System.out.println("@.@ Final Result!! (" + text + ")");
        if ( text.length() > 0 ) {
            statusManager.speechRecognition(StatusManager.EVENT_RESULT);

            /*
             * 인식된 사용자 입력을 서버로 전달한다.
             */
            Message msg = serviceThread.serviceHandler.obtainMessage(0405, text);
            serviceThread.serviceHandler.sendMessage(msg);
        }
    }

    @Override
    public void onError(int errorCode) {
        System.out.println("@.@ onError : " + errorCode);
        statusManager.speechRecognition(StatusManager.EVENT_ERROR);
    }

    @Override
    public void onEndPointDetectTypeSelected(SpeechConfig.EndPointDetectType epdType) {
        System.out.println("@.@ onEndPointDetectTypeSelected : " + epdType);
    }
}
