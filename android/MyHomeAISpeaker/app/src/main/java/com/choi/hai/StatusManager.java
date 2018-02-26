package com.choi.hai;

import android.os.Handler;
import android.os.Message;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 * 입력되는 값에 따라 HOTWORD DETECTION 또는  SPEECH RECOGNITION 상태를 결정한다.
 *
 */
public class StatusManager extends Thread
{
    private static final int ACTIVE_TIMEOUT = 5 * 1000;	// 10 sec

    public static final int EVENT_RECORD = 1;
    public static final int EVENT_PARTIAL_RESULT = 2;
    public static final int EVENT_RESULT = 3;
    public static final int EVENT_END_DETECT = 4;
    public static final int EVENT_INACTIVE = 5;
    public static final int EVENT_ERROR = 6;

    private static final int STATUS_START = 0;
    private static final int STATUS_LISTEN = 1;
    private static final int STATUS_SPEECH_RECOGNITION = 2;
    private static final int STATUS_FAILURE = 3;

    private int status = STATUS_START;
    private long timer;
    private boolean running = true;
    private boolean progress;

    private Handler handler = null;

    public StatusManager(Handler handler) {
        this.handler = handler;
    }

    /**
     *
     */
    public void listening()
    {
        sendMessage(MsgEnum.MSG_HOTWORD_LISTENING, "READY");

        status = STATUS_LISTEN;
    }

    /**
     *
     */
    public void detectedHotword()
    {
        sendMessage(MsgEnum.MSG_SPEECH_RECOGNITION, "");
        status = STATUS_SPEECH_RECOGNITION;
        timer = System.currentTimeMillis();
    }

    /**
     * @param eventType
     */
    public void speechRecognition(int eventType)
    {
        switch(eventType)
        {
            case EVENT_RECORD:
                break;

            case EVENT_PARTIAL_RESULT:
            case EVENT_RESULT:
                timer = System.currentTimeMillis();
                break;

            case EVENT_INACTIVE:
                listening();
                break;

            case EVENT_ERROR:
                status = STATUS_FAILURE;
                break;
        }
    }

    /**
     *
     */
    public void shutdown()
    {
        running = false;
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run()
    {
        sendMessage(MsgEnum.MSG_HOTWORD_LISTENING, "READY");

        while(running)
        {
            if ( progress == true ) {
                timer = System.currentTimeMillis();
            }
            else {
                if ( status == STATUS_SPEECH_RECOGNITION ) {
                    if ( System.currentTimeMillis() - timer >= ACTIVE_TIMEOUT ) {
                        sendMessage(MsgEnum.MSG_SR_TIMEOUT, "TIMEOUT");
                        listening();
                    }
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 메시지 전달
     *
     * @param what
     * @param obj
     */
    private void sendMessage(MsgEnum what, Object obj)
    {
        if (handler != null) {
            Message msg = handler.obtainMessage(what.ordinal(), obj);
            handler.sendMessage(msg);
        }
    }
}
