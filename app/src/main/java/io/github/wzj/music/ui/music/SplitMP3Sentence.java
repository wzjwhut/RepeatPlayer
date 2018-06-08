package io.github.wzj.music.ui.music;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by senshan_wang on 2016/3/31.
 */

public class SplitMP3Sentence {

    private final static Logger logger = LogManager.getLogger("wzj");

    private final String srcPath;
    private MediaCodec mediaDecode;
    private MediaExtractor mediaExtractor;
    private OnCompleteListener onCompleteListener;
    private OnProgressListener onProgressListener;
    private Thread extractThread;
    private Thread decodeThread;
    private ArrayList<Integer> amptitudes;
    private int seconds;
    private ArrayList<SentenceBreakPoint> breakpoints = new ArrayList<SentenceBreakPoint>();
    private ArrayList<Integer> pointsList = new ArrayList<>();

    /**
     * 转码完成回调接口
     */
    public interface OnCompleteListener {
        void completed();
    }
    /**
     * 转码进度监听器
     */
    public interface OnProgressListener {
        void progress(int percent);
    }

    private Handler handler = new Handler();

    public static class SentenceBreakPoint{
        public int pos;
        public int dur;
    }

    public SplitMP3Sentence(String filepath){
        this.srcPath = filepath;
    }

    public ArrayList<Integer> getPointsList() {
        return pointsList;
    }

    public void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final OnCompleteListener listener = onCompleteListener;
                try {
                    do {
                        if (srcPath == null) {
                            logger.error("srcPath is null");
                            break;
                        }
                        File file = new File(srcPath);
                        if (!file.isFile()) {
                            logger.error("not file");
                            break;
                        }
                        /** 每20毫秒计算一次幅度值 */

                        try {
                            initMediaDecode();
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        logger.info("length in seconds: {}", seconds);


                        amptitudes = new ArrayList<Integer>(seconds*50);
                        extractThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                extract();
                            }
                        });
                        extractThread.start();

                        decodeThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                drain();
                            }
                        });
                        decodeThread.start();

                    } while (false);
                }finally {
                    if(extractThread != null){
                        try {
                            extractThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        extractThread = null;
                    }

                    if(decodeThread != null){
                        try {
                            decodeThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        decodeThread = null;
                    }
                    release();
                }
                logger.info("complete");


                final int throshold = (int) (Short.MAX_VALUE* 0.025 );
                if(amptitudes != null && amptitudes.size()>0) {
                    /** 保存语句断点, 单位为毫秒. 振幅为每20毫秒计算一次
                     *  断句的规则:
                     *  超过200这毫秒的停顿, 认为是一个疑似断点, 但可能是一个假断点
                     */

                    /** 出现疑似静音的位置 */
                    int silenceBeginIndex = 0;
                    /** 静音的数量 */
                    int silenceCount = 0;

                    /** 非静音段的数量 */
                    int voiceCount = 0;

                    /** 如果为true, 表示当前处于正在处理静音的模式 */
                    boolean silenceMode = false;

                    {
                        SentenceBreakPoint point = new SentenceBreakPoint();
                        point.pos = 0;
                        point.dur = 0;
                        breakpoints.add(point);
                    }

                    int curIndex = 0;
                    for (Integer am : amptitudes) {
                        if(am<throshold){
                            //没有声音,可能是个断点.
                            if(silenceMode){
                                silenceCount++;
                            }else{
                                //切换到静音处理模式.
                                silenceMode = true;
                                silenceCount = 1;
                                silenceBeginIndex = curIndex;
                            }
                        }else{
                            //有声音
                            if(silenceMode){
                                //退出静音模式
                                silenceMode = false;
                                if(silenceCount<=2){
                                    /** 太短, 很可能不是一个断点 */
                                }else{
                                    /** 很可能是一个断点 */
                                    SentenceBreakPoint point = new SentenceBreakPoint();
                                    point.pos = silenceBeginIndex * 20;
                                    point.dur = silenceCount * 20;
                                    breakpoints.add(point);
                                    logger.info("breaks {}", breakpoints.size());
                                }
                            }else{

                            }
                        }
                        curIndex++;
                    }
                }
                logger.info("break point count: {}", breakpoints.size());
                pointsList = new ArrayList<Integer>();

                int voiceLength = 0;
                ArrayList<SentenceBreakPoint> temp = new ArrayList<>();
                //ArrayList<Break> result = new ArrayList<>();
                for(int i=1; i<breakpoints.size(); i++){
                    SentenceBreakPoint last = breakpoints.get(i-1);
                    SentenceBreakPoint cur = breakpoints.get(i);
                    int len = cur.pos - last.pos - last.dur;
                    voiceLength += len;
                    if(voiceLength < 3000){
                        continue;
                    }else if(voiceLength < 8000){
                        temp.add(cur);
                    }else{//>=10000
                        temp.add(cur);
                        SentenceBreakPoint max = temp.get(0);
                        int fitIndex = 0;
                        for(int j=0; j<temp.size(); j++){
                            SentenceBreakPoint b = temp.get(j);
                            if(max.dur<b.dur){
                                max = b;
                                fitIndex = j;
                            }
                        }

                        if(voiceLength < 15000 && max.dur<=60){
                            continue;
                        }

                        pointsList.add(max.pos + (max.dur /2));
                        logger.info("points: {}", pointsList.size());
                        voiceLength = 0;
                        i = i - ( temp.size() - fitIndex ) + 1;
                        temp.clear();
                    }
                }


//                int voiceLength = breakpoints.size()==0?0:breakpoints.get(0).pos;
//                for(int i=0; i<breakpoints.size()-1; i++){
//                    SentenceBreakPoint cur = breakpoints.get(i);
//                    SentenceBreakPoint next = breakpoints.get(i+1);
//                    int thizVoiceLength = next.pos - cur.pos - cur.dur;
//
//                    if((voiceLength>=5000 && cur.dur >=350) || (voiceLength>=8000)){
//                        /** 语音太长了, 当前就是一个断点 */
//                        pointsList.add(cur.pos + (cur.dur /2));
////                        logger.info("[Break]voiceLength: {}, thisVoidelen: {}, cur pos: {}, cur dur: {}, nex pos: {}, next dur: {}",
////                                voiceLength, thizVoiceLength, cur.positionMS, cur.durationMS, next.positionMS, next.durationMS);
//                        voiceLength = thizVoiceLength;
//                    }else{
//                        voiceLength += thizVoiceLength;
////                        logger.info("[None] voiceLength: {}, thisVoidelen: {}, cur pos: {}, cur dur: {}, nex pos: {}, next dur: {}",
////                                voiceLength, thizVoiceLength, cur.positionMS, cur.durationMS, next.positionMS, next.durationMS);
//                    }
//                }

//                for(Integer point : pointsList){
//                    logger.info("point: {}", point);
//                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null){
                            listener.completed();
                        }
                    }
                });

            }
        }).start();
    }

    private int sampleRate;
    private int channelCount;
    /**
     * 初始化解码器
     */
    private void initMediaDecode() throws Exception {
        mediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
        mediaExtractor.setDataSource(srcPath);//媒体文件的位置
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            long duration = format.getLong(MediaFormat.KEY_DURATION);
            logger.error("duration us: {}", duration);
            seconds = (int) (duration/(1000000) + 1);
//            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
//            metadataRetriever.setDataSource(file.getAbsolutePath());
//            String keyDuration = mediaExtractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            logger.info("sample rate: {}, channel count: {}", sampleRate, channelCount);

            if (mime.startsWith("audio")) {//获取音频轨道
//                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                mediaExtractor.selectTrack(i);//选择此音频轨道
                mediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                mediaDecode.configure(format, null, null, 0);
                break;
            }
        }

        if (mediaDecode == null) {
            throw new RuntimeException("get audio track failed");
        }
        mediaDecode.start();//启动MediaCodec ，等待传入数据
    }



    private int totalSampleCount = 0;


    private void extract(){
        int inputIndex;
        ByteBuffer[] decodeInputBuffers = mediaDecode.getInputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        while(true){
            inputIndex = mediaDecode.dequeueInputBuffer(-1);//获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
            if (inputIndex < 0) {
                Log.e("wzj", "dequeue input buffer faild");
                return;
            }
            ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];//拿到inputBuffer
            inputBuffer.clear();//清空之前传入inputBuffer内的数据
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);//MediaExtractor读取数据到inputBuffer中
            if (sampleSize < 0) {//小于0 代表所有数据已读取完成
                Log.e("wzj", "readSampleData completed");
                break;
            } else {
                mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);//通知MediaDecode解码刚刚传入的数据
                mediaExtractor.advance();//MediaExtractor移动到下一取样处
            }
        }
        inputIndex = mediaDecode.dequeueInputBuffer(-1);
        mediaDecode.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);//通知MediaDecode解码刚刚传入的数据
    }

    private void drain() {
        //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        ByteBuffer[] decodeOutputBuffers = mediaDecode.getOutputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
        byte[] pcm = null;
        short sample;
        int sampleCount = 0;
        long sum = 0;
        int max = 0;
        /** 每20毫秒中的采样数*/
        final int samplesPer20MS = sampleRate/50*channelCount;
        final short[] tempBuffer = new short[samplesPer20MS];

        while(true) {
            int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 100000);
            if(outputIndex<0){
                Log.e("wzj", "dequeueOutputBuffer continue, " + outputIndex + ", sample size: " + this.totalSampleCount);
                continue;
            }
            if(decodeBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM ){
                Log.e("wzj", "end of stream, " + outputIndex + ", sample size: " + this.totalSampleCount);
                break;
            }
            ByteBuffer outputBuffer = decodeOutputBuffers[outputIndex];//拿到用于存放PCM数据的Buffer
            totalSampleCount += decodeBufferInfo.size >> 1;
            if(pcm == null || pcm.length < decodeBufferInfo.size){
                pcm = new byte[decodeBufferInfo.size];
            }
            outputBuffer.get(pcm);//将Buffer内的数据取出到字节数组中
            outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
            mediaDecode.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据

            for(int i=0; i<decodeBufferInfo.size; i+=2){
                sample = (short)((pcm[i] & 0xff) | ( (pcm[i+1]&0xff)<<8));
                sampleCount++;
                sum += Math.abs(sample);
                max = Math.max(max, Math.abs(sample));
                if(sampleCount == samplesPer20MS){
                    int average = (int)(sum/samplesPer20MS);
                    //average = (average + max)/2;
                    amptitudes.add(average);
                    //amptitudes.add(v);
                    sum = 0;
                    max = 0;
                    sampleCount = 0;
                }
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {

        if (mediaDecode != null) {
            mediaDecode.stop();
            mediaDecode.release();
            mediaDecode = null;
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }

        if (onCompleteListener != null) {
            onCompleteListener = null;
        }

        if (onProgressListener != null) {
            onProgressListener = null;
        }
    }


    /**
     * 设置转码完成监听器
     *
     * @param onCompleteListener
     */
    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

}

