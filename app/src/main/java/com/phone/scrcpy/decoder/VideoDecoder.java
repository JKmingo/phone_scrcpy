package com.phone.scrcpy.decoder;

import static com.phone.scrcpy.model.ConfigParam.PREFERENCE_KEY;
import static com.phone.scrcpy.model.ConfigParam.USE_H265;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Surface;

import com.phone.scrcpy.MyApp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoDecoder {
    private MediaCodec mCodec;
    private Worker mWorker;
    private AtomicBoolean mIsConfigured = new AtomicBoolean(false);
    private int videoWidth;
    private int videoHeight;

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        if (mWorker != null) {
            mWorker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }

    public void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
        if (mWorker != null) {
            mWorker.configure(surface, width, height, csd0, csd1);
        }
    }


    public void start() {
        if (mWorker == null) {
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
            mIsConfigured.set(false);
            mCodec.stop();
        }
    }

    private class Worker extends Thread {

        private AtomicBoolean mIsRunning = new AtomicBoolean(false);

        Worker() {
        }

        private void setRunning(boolean isRunning) {
            mIsRunning.set(isRunning);
        }

        private void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
            if (mIsConfigured.get()) {
                mIsConfigured.set(false);
                mCodec.stop();

            }
            boolean isUseH265 = MyApp.getAppContext().getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(USE_H265, false);
            String codecMime = isUseH265 ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
//            codecMime = MediaFormat.MIMETYPE_VIDEO_AVC;
            MediaFormat format = MediaFormat.createVideoFormat(codecMime, width, height);
            if (csd0 != null && csd1 != null) {
                format.setByteBuffer("csd-0", csd0);
                format.setByteBuffer("csd-1", csd1);
            }
            try {
                mCodec = MediaCodec.createDecoderByType(codecMime);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create codec", e);
            }
            mCodec.configure(format, surface, null, 0);
            mCodec.start();
            mIsConfigured.set(true);
        }


        @SuppressWarnings("deprecation")
        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            if (mIsConfigured.get() && mIsRunning.get()) {
                int index = mCodec.dequeueInputBuffer(-1);
                if (index >= 0) {
                    ByteBuffer buffer;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer = mCodec.getInputBuffers()[index];
                    } else {
                        buffer = mCodec.getInputBuffer(index);
                    }
                    if (buffer != null) {
                        buffer.clear();
                        buffer.put(data, offset, size);
                        mCodec.queueInputBuffer(index, 0, size, 0, flags);
                        try {
                            MediaFormat outputFormat = mCodec.getOutputFormat();
                            videoWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
                            videoHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        } catch (Exception e) {
                            videoWidth = 720;
                            videoHeight = 1600;
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mIsRunning.get()) {
                    if (mIsConfigured.get()) {
                        int index = mCodec.dequeueOutputBuffer(info, 0);
                        if (index >= 0) {
                            // setting true is telling system to render frame onto Surface
                            mCodec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    } else {
                        // just waiting to be configured, then decode and render
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } catch (IllegalStateException e) {
            }

        }
    }

    public Pair<Integer, Integer> getVideoSize() {
        return new Pair<>(videoWidth, videoHeight);
    }
}