package com.io;

import android.support.annotation.NonNull;

import com.choi.hai.AiSpeakerEnv;
import com.google.android.things.pio.PeripheralManagerService;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 * PCA9685에 연결된 LED를 동작 모드에 따라 제어한다.
 */
public class IOAgent
{
    class RGBPin {
        public int redPin;
        public int greenPin;
        public int bluePin;
    }

    /*
	 * RGB fade
	 */
    class Fade extends Thread {
        private boolean running;

        public void run()
        {
            running = true;
            long delay = 0;

            int seedValue = (int) (Math.random() % 255);

            while (running) {
                if (System.currentTimeMillis() - delay >= 15) {
                    float x;
                    int r, g, b;

                    x = (float) seedValue;
                    r = (int) (127 * (Math.sin(x / 180 * Math.PI) + 1));
                    g = (int) (127 * (Math.sin(x / 180 * Math.PI + 3 / 2 * Math.PI) + 1));
                    b = (int) (127 * (Math.sin(x / 180 * Math.PI + 0.5 * Math.PI) + 1));

                    try {
                        setPwm(sensLed.redPin, map(r, 255, 4095));
                        setPwm(sensLed.greenPin, map(g, 255, 4095));
                        setPwm(sensLed.bluePin, map(b, 255, 4095));
                    }catch(Exception ig) {}

                    seedValue = (seedValue + 1) % 360;

                    delay = System.currentTimeMillis();
                } else {
                    try {sleep(5);} catch (InterruptedException e) {}
                }
            }

            try {
                setPwm(sensLed.redPin, 0);
                setPwm(sensLed.greenPin, 0);
                setPwm(sensLed.bluePin, 0);
            } catch (Exception ig) {
                ig.printStackTrace();
            }
        }
    }

    /*
	 * Knight Rider
	 */
    class KnightRider extends Thread {
        private boolean running;

        public void run() {
            running = true;
            long delay = 0;
            int direction = 1;
            int channel = 0;

            try {
                while (running) {
                    if (System.currentTimeMillis() - delay >= 100) {
                        for (int i = 0; i < progressLed.length; i++)
                            setPwm(progressLed[i].redPin, 0);

                        setPwm(progressLed[channel].redPin, 2500);

                        if (direction == 1) {
                            if (channel > 0)
                                setPwm(progressLed[channel - 1].redPin, 1000);
                            if (channel > 1)
                                setPwm(progressLed[channel - 2].redPin, 80);

                            channel++;

                            if (channel >= progressLed.length - 1) {
                                direction = -1;
                                channel = progressLed.length - 1;
                            }
                        } else {
                            if (channel < progressLed.length - 1)
                                setPwm(progressLed[channel + 1].redPin, 1000);
                            if (channel < progressLed.length - 2)
                                setPwm(progressLed[channel + 2].redPin, 80);

                            channel--;

                            if (channel <= 0) {
                                direction = 1;
                                channel = 0;
                            }
                        }

                        delay = System.currentTimeMillis();
                    } else {
                        sleep(10);
                    }
                }

                for (int i = 0; i < progressLed.length; i++)
                    setPwm(progressLed[i].redPin, 0);

            } catch (Exception ig) {
                ig.printStackTrace();
            }
        }
    }

    /*
	 * LEDBarPlayer
	 */
    class LEDBarPlayer extends Thread {
        private boolean running;
        private LEDBar ledBar;

        public LEDBarPlayer(LEDBar led) {
            ledBar = led;
        }

        public void run() {
            running = true;

            try {
                do {
                    short[] data = ledBar.currentLedValue();

                    for (int i = 0; i < data.length; i++)
                        setPwm(progressLed[i].redPin, data[i]);

                    Thread.sleep(ledBar.getIntervalTime());
                } while (running && ledBar.moveNext());

                for (int i = 0; i < progressLed.length; i++)
                    setPwm(progressLed[i].redPin, 0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static final byte PCA9685_ADDRESS = 0x40;

    private PCA9685 pca9685;

    private RGBPin sensLed;
    private RGBPin[] progressLed;

    private KnightRider rider = null;
    private Fade fade = null;
    private LEDBarPlayer player = null;
    private LEDBarPlayer progress = null;

    public IOAgent(@NonNull PeripheralManagerService manager) {
        try {
            pca9685 = new PCA9685(PCA9685_ADDRESS, manager);
            pca9685.reset();
        } catch (Exception x) {
            x.printStackTrace();
        }

		/*
		  RGB LED
		 */
        String[] rgb = AiSpeakerEnv.RGB_LED.split(",");
        sensLed = new RGBPin();
        sensLed.redPin = toInt(rgb[0]);
        sensLed.greenPin = toInt(rgb[1]);
        sensLed.bluePin = toInt(rgb[2]);

		/*
         PROGRESS LED - 8개
		 */
        String[] leds = AiSpeakerEnv.PROGRESS_LED.split(",");
        progressLed = new RGBPin[leds.length];
        for (int i = 0; i < progressLed.length; i++) {
            progressLed[i] = new RGBPin();
            progressLed[i].redPin = toInt(leds[i]);
        }
    }

    /**
     * 문자열을 int 기본타입으로 변환한다.
     * @param val
     * @return int 값
     */
    private int toInt(String val) { // throws Exception
        int v = 0;

        try {
            v = Integer.parseInt(val);
        } catch (Exception e) {
            //throw e;
        }

        return v;
    }

    /**
     * @param wake
     */
    public void setWakeup(boolean wake)
    {
        if (wake) {
            float x;
            int r, g, b;

            x = (float) (Math.random() % 255);
            r = (int) (127 * (Math.sin(x / 180 * Math.PI) + 1));
            g = (int) (127 * (Math.sin(x / 180 * Math.PI + 3 / 2 * Math.PI) + 1));
            b = (int) (127 * (Math.sin(x / 180 * Math.PI + 0.5 * Math.PI) + 1));
            try {
                setPwm(sensLed.redPin, map(r, 255, 4095));
                setPwm(sensLed.greenPin, map(g, 255, 4095));
                setPwm(sensLed.bluePin, map(b, 255, 4095));
            } catch (Exception ex) {
            }
        } else {
            try {
                setPwm(sensLed.redPin, 0);
                setPwm(sensLed.greenPin, 0);
                setPwm(sensLed.bluePin, 0);
            } catch (Exception ex) {
            }

        }
    }

    /**
     * @param rgbValue
     */
    public void changeRGB(final String rgbValue) {
        System.out.println("@.@ CHANGE LED = " + rgbValue);

        try {
            setPwm(sensLed.redPin, map(Integer.parseInt(rgbValue.substring(0, 2), 16), 255, 4095));
            setPwm(sensLed.greenPin, map(Integer.parseInt(rgbValue.substring(2, 4), 16), 255, 4095));
            setPwm(sensLed.bluePin, map(Integer.parseInt(rgbValue.substring(4), 16), 255, 4095));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param complete
     */
    public void showProgress(boolean complete)
    {
//        if ( complete == false ) {
//            if ( rider == null ) {
//                rider = new KnightRider();
//                rider.start();
//            }
//        }
//        else {
//            if ( rider != null ) {
//                rider.running = false;
//                rider = null;
//            }
//        }

        if ( complete == false ) {
            if ( progress == null ) {
                progress = new LEDBarPlayer(LedDisplayData.cycle);
                progress.start();
            }
        }
        else {
            if ( progress != null ) {
                progress.running = false;
                progress = null;
            }
        }
    }

    /**
     *
     * @param start
     */
    public void showFade(boolean start)
    {
        if ( start ) {
            if ( fade == null ) {
                fade = new Fade();
                fade.start();
            }
        }
        else {
            if ( fade != null ) {
                fade.running = false;
                fade = null;
            }
        }
    }

    /**
     *
     */
    public void showStartup()
    {
        player = new LEDBarPlayer(LedDisplayData.startUp);
        player.start();
    }

    /**
     *
     */
    public void stopLED()
    {
        if ( player != null ) {
            player.running = false;
            player = null;
        }
    }

    /**
     *
     */
    public void showWait()
    {
        stopLED();
        player = new LEDBarPlayer(LedDisplayData.waitResponse);
        player.start();
    }

	/**
	 * @param mv
	 * @param max
	 * @param mapMax
	 * @return
	 */
	private int map(int mv, int max, int mapMax)
	{
		return (mapMax * mv) / max;
	}
	
    /**
     * @param channel
     * @param value
     * @throws Exception
     */
    public synchronized void setPwm(int channel, int value) throws Exception
    {
        if ( value == 0 )
            pca9685.setAlwaysOff(channel);
        else if ( value > 4095 )
            pca9685.setAlwaysOn(channel);
        else
            pca9685.setPwm(channel, 0, value);
    }
}
