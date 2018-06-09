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
import java.util.LinkedList;
import java.util.List;

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

    private int seconds;
    private List<SilencePoint> silencePoints = new LinkedList<SilencePoint>();
    private List<Integer> splitPointsList = new LinkedList<>();

    /** 静音的振幅阈值 */
    final int VAD_THROLSHOLD = (int) (Short.MAX_VALUE* 0.025 );
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

    public static class SilencePoint {
        /** 静音开始的位置, 以毫秒为单位 */
        public final int posMS;
        /** 静音持续的时间, 以毫秒为单位 */
        public final int durMS;
        public SilencePoint(int p, int d){
            this.posMS = p;
            this.durMS = d;
        }
    }

    public SplitMP3Sentence(String filepath){
        this.srcPath = filepath;
        /** 方便算法处理 */
        silencePoints.add(new SilencePoint(0, 0));
    }

    public List<Integer> getSplitPointsList() {
        return splitPointsList;
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
                logger.info("silence point count: {}", silencePoints.size());

                /** silencePoints中的数据仅仅是可能的停顿点，需要对这些停顿点再次处理, 拆分出长度合适的语句  */
                int voiceLength = 0;
                //ArrayList<SilencePoint> temp = new ArrayList<>();

                SilencePoint maxSilentPoint = null;
                int maxSilenceIndex = 1;

                /** 第一个点，就是语音起始点0秒的时候, 长度也是0; 从第二个点开始计算每段的语音长度 */
                for(int i = 1; i< silencePoints.size(); i++){
                    SilencePoint last = silencePoints.get(i-1);
                    SilencePoint cur = silencePoints.get(i);

                    /** 当前停顿点与上一个停顿点之间的语音长度 */
                    int len = cur.posMS - last.posMS - last.durMS;
                    /** 语音已经累计的长度 */
                    voiceLength += len;
                    if(voiceLength < 3000){

                        /** 3秒的语音太短了, 继续找一下停顿点 */
                        continue;
                    }else if(voiceLength < 8000){
                        if(maxSilentPoint == null){
                            maxSilenceIndex = i;
                            maxSilentPoint = cur;
                        }
                        /**3秒到8秒之间的停顿点，先记录下来. 等语句累计到一定程度, 再找出最合适的点 */
                        if(maxSilentPoint.durMS < cur.durMS){
                            maxSilentPoint = cur;
                            maxSilenceIndex = i;
                        }
                    }else{ //超过了8秒, 理论上可以找出合适的停顿点了
                        if(maxSilentPoint == null){
                            maxSilenceIndex = i;
                            maxSilentPoint = cur;
                        }

                        if(voiceLength < 15000 && maxSilentPoint.durMS <=60){
                            /** 最长停顿点还是很短, 继续找. 如果达到了15秒, 语句太长, 则不再找了 */
                            continue;
                        }

                        splitPointsList.add(maxSilentPoint.posMS + (maxSilentPoint.durMS /2));
                        //logger.info("points: {}", splitPointsList.size());
                        voiceLength = 0;
                        maxSilentPoint = null;
                        i = maxSilenceIndex;
                    }
                }
                for(Integer pos : splitPointsList){
                    logger.info("split point: {}", pos);
                }

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
        ByteBuffer[] decodeOutputBuffers = mediaDecode.getOutputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();

        /** 每20毫秒中的采样数*/
        final int sampleCountPer20MS = sampleRate/50*channelCount;

        /** 临时存放解码出的pcm数据 */
        byte[] pcmBytes = null;
        /** 累计到20MS时, 计算振幅的平均值 */
        int sampleCount = 0;
        /** 累计振幅 */
        long samplesSum = 0;

        /** 出现静音的位置, 以20MS为单位 */
        int silence20msPos = 0;
        /** 已经处理了的数据包的个数, 每一个为20MS */
        int packet20msNumber = 0;

        /** 语音持续的长度, 以20MS为单位 */
        int voiceLength = 0;

        /** 静音持续的长度 */
        int silenceLength = 0;

        /** 上一次的音频是否为静音, 用于判断静音与非静音状态的转换 */
        boolean isSilenceMode = false;

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
            if(pcmBytes == null || pcmBytes.length < decodeBufferInfo.size){
                pcmBytes = new byte[decodeBufferInfo.size];
            }
            outputBuffer.get(pcmBytes);//将Buffer内的数据取出到字节数组中
            outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
            mediaDecode.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据

            /** 每20毫秒计算一次平均振幅 */
            for(int i=0; i<decodeBufferInfo.size; i+=2){
                sampleCount++;
                samplesSum += Math.abs((short)((pcmBytes[i] & 0xff) | ( (pcmBytes[i+1]&0xff)<<8)));
                if(sampleCount == sampleCountPer20MS){
                    int average = (int)(samplesSum /sampleCountPer20MS);
                    samplesSum = 0;
                    sampleCount = 0;

                    /** 静音检测 */
                    if(average < VAD_THROLSHOLD){
                        /** 声音太小, 认为是静音 */
                        if(isSilenceMode){
                            /** 持续静音 */
                            silenceLength++;
                        }else{
                            /** 由语音模式转换为静音模式 */
                            isSilenceMode = true;
                            silenceLength = 1;
                            silence20msPos = packet20msNumber;
                        }
                    }else{/** 有声音 */
                        if(isSilenceMode){
                            /** 由静音模式转换为语音 */
                            isSilenceMode = false;
                            if(silenceLength<=2){
                                /** 太短, 不认为是一个断点 */
                            }else{
                                /** 很可能是一个断点 */
                                this.silencePoints.add(new SilencePoint(silence20msPos*20, silenceLength*20));
                            }
                        }
                    }
                    packet20msNumber++;
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

