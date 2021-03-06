package com.lkd.webrtcmodel.constant;

/**
 * WebRTC定义常量
 */
public class WebRTC {
    /**
     * 回声消除
     */
    public static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    /**
     * 自动增益
     */
    public static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    /**
     * 高音过滤
     */
    public static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    /**
     * 噪音处理
     */
    public static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    
    public static final String VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    
    public static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    
    public static final String DISABLE_WEBRTC_AGC_FIELDTRIAL = "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    
    public static final int FONT_FACTING = 0 ;
    
    public static final int BACK_FACING = 1 ;
    /**
     *  音频ID
     */
    public static final String AUDIO_TRACK_ID = "AUDIO";
    /**
     * 视频ID
     */
    public static final String VIDEO_TRACK_ID = "VIDEO";
}
