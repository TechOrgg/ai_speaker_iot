package com.choi.hai;

/**
 * @author ??? (dreamsalmon@gmail.com)
 *
 */
public enum  MsgEnum
{
    MSG_HOTWORD_LISTENING,
    MSG_HOTWORD_DETECT,
    MSG_SPEECH_RECOGNITION,
    MSG_SR_TIMEOUT,
    MSG_CONVERSATION_RESPONSE,
    MSG_ERROR,
    MSG_INFO;

    public static MsgEnum getMsgEnum(int i) {
        return MsgEnum.values()[i];
    }
}
