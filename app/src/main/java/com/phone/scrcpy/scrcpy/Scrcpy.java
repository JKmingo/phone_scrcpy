package com.phone.scrcpy.scrcpy;

import static com.phone.scrcpy.model.ConfigParam.ENABLE_AUDIO;
import static com.phone.scrcpy.model.ConfigParam.PREFERENCE_KEY;
import static com.phone.scrcpy.model.ConfigParam.SPINNER_BITRATE;
import static com.phone.scrcpy.model.ConfigParam.SPINNER_RESOLUTION;
import static com.phone.scrcpy.model.ConfigParam.TURN_OFF_SCREEN;
import static com.phone.scrcpy.model.ConfigParam.USE_H265;
import static com.phone.scrcpy.model.ConfigParam.VIDEO_FPS;

import android.app.Service;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

import com.phone.scrcpy.R;
import com.phone.scrcpy.adb.Adb;
import com.phone.scrcpy.adb.BufferStream;
import com.phone.scrcpy.decoder.VideoDecoder;
import com.phone.scrcpy.helper.ViewTools;
import com.phone.scrcpy.model.UsbDevicesMap;
import com.phone.scrcpy.model.VideoPacket;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class Scrcpy extends Service implements IScrcpy {
    private static final int SAMPLE_RATE = 48000;
    private Surface surface;
    private int videoWidth;
    private int videoHeight;
    private VideoDecoder videoDecoder;
    private AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private IBinder mBinder = new MyServiceBinder();
    private AtomicBoolean LetServiceRunning = new AtomicBoolean(true);
    private ServiceCallbacks serviceCallbacks;
    private Adb adb;
    private AudioTrack audioTrack;
    private BufferStream videoScrcpy;
    private BufferStream audioScrcpy;
    private BufferStream controlScrcpy;
    private BufferStream shell;

    private long videoStartTime;
    private long audioStartTime;
    private boolean audioInited;
    private boolean isAudio;
    private boolean isTurnOffScreen;
    private ExecutorService executor = Executors.newFixedThreadPool(3);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void setServiceCallbacks(ServiceCallbacks callbacks) {
        this.serviceCallbacks = callbacks;
    }

    @Override
    public void start(Surface surface, String serverAdr) {
        this.surface = surface;
        setupScrcpy(serverAdr);
        startVideoStream();
        if (isAudio) {
            startAudioStream();
        }
    }

    @Override
    public void pause() {
        videoDecoder.stop();
        updateAvailable.set(false);
    }

    @Override
    public void resume() {
        videoDecoder.start();
        updateAvailable.set(true);
    }

    @Override
    public void stopService() {
        cleanupResources();
        stopSelf();
    }

    @Override
    public boolean touchevent(View view, MotionEvent touch_event, int displayW, int displayH) {
        executor.submit(() -> {
            int action = touch_event.getActionMasked(); // 获取动作类型（如 ACTION_DOWN, ACTION_POINTER_DOWN 等）
            int pointerIndex = touch_event.getActionIndex(); // 获取当前动作对应的 Pointer Index
            try {
                switch (action) {
                    case MotionEvent.ACTION_DOWN: // 第一个手指按下
                    case MotionEvent.ACTION_POINTER_DOWN: // 其他手指按下
                        int pointerId = touch_event.getPointerId(pointerIndex); // 根据 Pointer Index 获取 Pointer ID
                        float x = touch_event.getX(pointerIndex); // 当前触点的 X 坐标
                        float y = touch_event.getY(pointerIndex); // 当前触点的 Y 坐标
                        Point point1 = ViewTools.mapScreenToVideo(view.getWidth(), view.getHeight(), videoWidth, videoHeight, x, y);
                        controlScrcpy.write(ControlPacket.sendTouchEvent(action, pointerId, point1.x, point1.y, displayW, displayH));
                        break;

                    case MotionEvent.ACTION_MOVE: // 手指移动（所有触点的信息都可以获取）
                        int pointerCount = touch_event.getPointerCount(); // 获取当前触点数量
                        for (int i = 0; i < pointerCount; i++) {
                            int id = touch_event.getPointerId(i); // 获取每个触点的 Pointer ID
                            float moveX = touch_event.getX(i); // 获取当前触点的 X 坐标
                            float moveY = touch_event.getY(i); // 获取当前触点的 Y 坐标
                            Point point2 = ViewTools.mapScreenToVideo(view.getWidth(), view.getHeight(), videoWidth, videoHeight, moveX, moveY);
                            controlScrcpy.write(ControlPacket.sendTouchEvent(action, id, point2.x, point2.y, displayW, displayH));
                        }
                        break;

                    case MotionEvent.ACTION_UP: // 最后一个手指抬起
                    case MotionEvent.ACTION_POINTER_UP: // 其他手指抬起
                        int pointerUpId = touch_event.getPointerId(pointerIndex); // 获取抬起的 Pointer ID
                        float upX = touch_event.getX(pointerUpId); // 当前触点的 X 坐标
                        float upY = touch_event.getY(pointerUpId); // 当前触点的 Y 坐标
                        Point point3 = ViewTools.mapScreenToVideo(view.getWidth(), view.getHeight(), videoWidth, videoHeight, upX, upY);
                        controlScrcpy.write(ControlPacket.sendTouchEvent(action, pointerUpId, point3.x, point3.y, displayW, displayH));
                        break;
                }
            } catch (Exception e) {
                Log.e("scrcpy", "", e);
            }
        });
        return true;
    }

    @Override
    public void sendKeyevent(int keycode) {
        executor.submit(() -> {
            try {
                controlScrcpy.write(ControlPacket.sendKeyDown(keycode));
                Thread.sleep(50);
                controlScrcpy.write(ControlPacket.sendKeyUp(keycode));
            } catch (Exception e) {
                Log.e("scrcpy", "", e);
            }
        });
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopService();
        return super.onUnbind(intent);
    }

    private void setupScrcpy(String serverAdr) {
        Future<?> future = executor.submit(() -> {
            try {
                if (adb == null) {
                    adb = initializeAdb(serverAdr);
                }
                setupServer();
                setupBufferStreams();
                setupVideoAudio();
            } catch (Exception e) {
                Log.e("scrcpy", "Setup error", e);
            }
        });
        try {
            // 等待任务执行完成
            future.get(); // 会阻塞，直到 setupScrcpy 执行完成
        } catch (Exception e) {
            Log.e("TcpScrcpy", "Error during setupScrcpy execution", e);
        }
    }

    private Adb initializeAdb(String serverAdr) throws Exception {
        if (serverAdr.toUpperCase().endsWith("(USB)")) {
            UsbDevice usbDevice = UsbDevicesMap.get(serverAdr.replace("(USB)", ""));
            return new Adb(this, usbDevice);
        }
        int adbPort = 5555;
        String adbAddress = serverAdr.split(":")[0].trim();
        if (serverAdr.contains(":")) adbPort = Integer.parseInt(serverAdr.split(":")[1].trim());
        return new Adb(this, adbAddress, adbPort);
    }

    private void setupServer() throws Exception {
        String serverName = "/data/local/tmp/scrcpy_server.jar";
        if (adb.runAdbCmd("ls " + serverName).contains("No such file")) {
            adb.pushFile(this.getResources().openRawResource(R.raw.scrcpy_server), serverName, null);
        }

        int resolution = getSharedPreferences(PREFERENCE_KEY, 0).getInt(SPINNER_RESOLUTION, 1600);
        int bitrate = getSharedPreferences(PREFERENCE_KEY, 0).getInt(SPINNER_BITRATE, 4);
        int fps = getSharedPreferences(PREFERENCE_KEY, 0).getInt(VIDEO_FPS, 30);
        isAudio = getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(ENABLE_AUDIO, false);
        boolean isUseH265 = getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(USE_H265, false);
        isTurnOffScreen = getSharedPreferences(PREFERENCE_KEY, 0).getBoolean(TURN_OFF_SCREEN, false);

        String cmdStr = String.format("app_process -Djava.class.path=%s / com.genymobile.scrcpy.Server 3.1 "
                        + "max_size=%s video_bit_rate=%s max_fps=%s video_codec=%s audio=%s control=%s tunnel_forward=true " +
                        "display_id=0 show_touches=false stay_awake=false audio_codec=raw " +
                        "power_off_on_close=false send_frame_meta=true" + " \n",
                serverName, resolution, bitrate + "000000", fps, isUseH265 ? "h265" : "h264", isAudio, "true");
        shell = adb.getShell();
        shell.write(ByteBuffer.wrap(cmdStr.getBytes()));
        Thread.sleep(2000);
        String checkRes = adb.runAdbCmd("ps -ef | grep scrcpy | grep -v grep");
        System.out.println(checkRes);
    }

    private void setupBufferStreams() throws Exception {
        videoScrcpy = adb.localSocketForward("scrcpy");
        if (isAudio) {
            audioScrcpy = adb.localSocketForward("scrcpy");
        }
        controlScrcpy = adb.localSocketForward("scrcpy");
    }

    private void setupVideoAudio() throws Exception {
        if (videoScrcpy.readByte() != 0) {
            stopService();
            throw new RuntimeException("videoScrcpyForwardError");
        }
        videoScrcpy.readByteArray(64);
        ByteBuffer videoInfo = videoScrcpy.readByteArray(12);
        videoWidth = videoInfo.getInt(4);
        videoHeight = videoInfo.getInt(8);
        Log.i("scrcpy", "Video size:" + videoWidth + "x" + videoHeight);
        if (isAudio) {
            ByteBuffer meta = audioScrcpy.readByteArray(4);
            byte[] metaByte = new byte[meta.remaining()];
            meta.get(metaByte);
            String audioCodec = new String(metaByte, "UTF-8");
            Log.i("scrcpy", "audio codec: " + audioCodec);
        }
    }

    private void startVideoStream() {
        videoDecoder = new VideoDecoder();
        videoDecoder.start();
        updateAvailable.set(true);
        executor.submit(this::streamVideo);
    }

    private void streamVideo() {
        try {
            VideoPacket.Flag flag = VideoPacket.Flag.CONFIG;
            while (LetServiceRunning.get()) {
                byte[] data = getH264Data();
                if (data == null) {
                    continue;
                }
                if (!updateAvailable.get()) {
                    continue;
                }
                if (flag == VideoPacket.Flag.CONFIG) {
                    VideoPacket.StreamSettings streamSettings = VideoPacket.getStreamSettings(data);
                    flag = VideoPacket.Flag.FRAME;
                    videoDecoder.configure(surface, videoWidth, videoHeight, streamSettings.sps, streamSettings.pps);
                    if (serviceCallbacks != null) {
                        serviceCallbacks.loadNewRotation(videoWidth, videoHeight);
                    }
                } else {
                    videoDecoder.decodeSample(data, 0, data.length, 0, 0);
                    if (isTurnOffScreen) {
                        controlScrcpy.write(ControlPacket.screenOff());
                    }
                }
                if (serviceCallbacks != null) {
                    Pair<Integer, Integer> videoSize = videoDecoder.getVideoSize();
                    if (videoSize.first > 0 && videoSize.second > 0 && videoWidth != videoSize.first && videoHeight != videoSize.second) {
                        videoWidth = videoSize.first;
                        videoHeight = videoSize.second;
                        serviceCallbacks.loadNewRotation(videoWidth, videoHeight);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("scrcpy", "Video stream error", e);
        }
    }

    private byte[] getH264Data() throws Exception {
        if (videoScrcpy == null || videoScrcpy.isClosed()) return null;
        ByteBuffer buffer = videoScrcpy.readByteArray(12);
        videoStartTime = buffer.getLong();
        int dataLength = buffer.getInt(8);
        ByteBuffer nalData = videoScrcpy.readByteArray(dataLength);
        byte[] nalByteArray = new byte[nalData.remaining()];
        nalData.get(nalByteArray);
        return nalByteArray;
    }

    private void startAudioStream() {
        initAudioTrack();
        executor.submit(this::streamAudio);
    }

    private void streamAudio() {
        try {
            while (LetServiceRunning.get()) {
                ByteBuffer buffer = audioScrcpy.readByteArray(12);
                audioStartTime = buffer.getLong();
                int dataLength = buffer.getInt(8);
                ByteBuffer data = audioScrcpy.readByteArray(dataLength);
                if (!audioInited) {
                    if (videoStartTime <= 0) {
                        continue;
                    }
                    if (videoStartTime > audioStartTime) {
                        continue;
                    }
                }
                audioInited = true;
                byte[] dataByte = new byte[data.remaining()];
                data.get(dataByte);
                audioTrack.write(dataByte, 0, dataLength);
            }
        } catch (Exception e) {
            Log.e("scrcpy", "Audio stream error", e);
        }
    }

    private void initAudioTrack() {
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,      // 音频流类型
                SAMPLE_RATE,                    // 采样率
                AudioFormat.CHANNEL_OUT_STEREO, // 声道配置
                AudioFormat.ENCODING_PCM_16BIT, // 编码格式
                bufferSize,                     // 缓冲区大小
                AudioTrack.MODE_STREAM          // 流模式
        );
        audioTrack.play();
    }


    private void cleanupResources() {
        LetServiceRunning.set(false);
        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        closeStream(videoScrcpy);
        closeStream(audioScrcpy);
        closeStream(controlScrcpy);
        closeStream(shell);
        if (adb != null) adb.close();
        if (executor != null) executor.shutdown();
    }

    private void closeStream(BufferStream stream) {
        if (stream != null) {
            try {
                stream.flush();
                stream.close();
            } catch (Exception e) {
                Log.e("scrcpy", "Stream close error", e);
            }
        }
    }

    public class MyServiceBinder extends Binder {
        public Scrcpy getService() {
            return Scrcpy.this;
        }
    }
}
