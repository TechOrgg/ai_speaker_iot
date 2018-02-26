package ai.kitt.snowboy;

import android.os.Environment;

public class Constants
{
    public static final String ASSETS_RES_DIR = "snowboy";
    public static final String DEFAULT_WORK_SPACE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/snowboy/";

    public static final String ACTIVE_UMDL = "alexa.umdl";

    public static final String ACTIVE_RES = "common.res";

    public static final int SAMPLE_RATE = 16000;
}
