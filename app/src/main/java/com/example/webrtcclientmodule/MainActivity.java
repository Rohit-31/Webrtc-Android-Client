package com.example.webrtcclientmodule;

import static org.webrtc.VideoFrameDrawer.TAG;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.controls.ControlsProviderService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    JSONObject answerObject;
    public PeerConnectionFactory mPeerConnectionFactory;
    org.webrtc.SurfaceViewRenderer localView, remoteView;

    public PeerConnection mPeerConnection;
    public DataChannel mDataChannel;
    Button button, button2;
    LinearLayout mLinearLayout;
    ImageButton recent,home,back;
    Socket clientSocket;
    public PrintWriter mPrintWriter;
    public BufferedReader mBufferedReader;
    Thread mThread;
    EglBase.Context eglBaseContext;
    private ArraySet<String> listOfStunServers = new ArraySet<>();
    private Intent mediaProjectionResultIntent;
    private int mResultCode;
    private int mMediaProjectionPermissionResultCode;
    SurfaceTextureHelper surfaceTextureHelper;
    VideoSource mVideoSource;
    VideoTrack localVideoTrack;
    MediaStream localMediaStream;
    GestureDetector mGestureDetector;
    VideoCapturer mVideoCapturer;

    private int CAPTURE_PERMISSION_REQUEST_CODE=9990;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        button = findViewById(R.id.button);
        recent = findViewById(R.id.b1);
        home = findViewById(R.id.b2);
        back = findViewById(R.id.b3);
        mLinearLayout=findViewById(R.id.llyt1);

        requestPermissions(new String[]{Manifest.permission.CAMERA},420);
        recent.setOnClickListener(view -> {
            JSONObject jsonObject=new JSONObject();
            try {
                jsonObject.put("operation","recent");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dataSend(jsonObject.toString());
        });

        home.setOnClickListener(view -> {
            JSONObject jsonObject=new JSONObject();
            try {
                jsonObject.put("operation","home");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dataSend(jsonObject.toString());
        });

        back.setOnClickListener(view -> {
            JSONObject jsonObject=new JSONObject();
            try {
                jsonObject.put("operation","back");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dataSend(jsonObject.toString());
        });


        eglBaseContext=EglBase.create().getEglBaseContext();
        initializePeerConnectionFactory();
        createPeerConnection();

//        localView=findViewById(R.id.localView);
        remoteView=findViewById(R.id.remoteView);
        initSurfaceView(remoteView);

        mGestureDetector=new GestureDetector(this,new SwipeListener(){
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float x1=e1.getX();
                float y1=e1.getY();

                float x2=e2.getX();
                float y2=e2.getY();

                float velocity=Math.abs(velocityX-velocityY);

                JSONObject mObject=new JSONObject();
                try {
                    mObject.put("operation","fling");
                    mObject.put("x1",x1);
                    mObject.put("x2",x2);
                    mObject.put("y1",y1);
                    mObject.put("y2",y2);
                    mObject.put("velocity",velocity);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e(ContentValues.TAG, "onFling: Fling detected " );
                Log.e(ContentValues.TAG, "onFling: x1= "+x1 );
                Log.e(ContentValues.TAG, "onFling: x2= "+ x2);
                Log.e(ContentValues.TAG, "onFling: y1= "+y1 );
                Log.e(ContentValues.TAG, "onFling: y2= "+y2 );
                Log.e(ContentValues.TAG, "onFling: velocity= "+velocity);
//            if(currentState== PeerConnection.IceConnectionState.CONNECTED){
                DataChannel.Init init = new DataChannel.Init();
                DataChannel dataChannel = mPeerConnection.createDataChannel("dc001", init);
                ByteBuffer mBuffer = stringToByteBuffer(mObject.toString());
                DataChannel.Buffer mBuffer2 = new DataChannel.Buffer(mBuffer, false);
                dataChannel.send(mBuffer2);

//            }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                float x1=e.getX();
                float y1=e.getY();
                JSONObject mObject=new JSONObject();
                try {
                    mObject.put("operation", "tap");
                    mObject.put("x1", x1);
                    mObject.put("y1", y1);
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                DataChannel.Init init = new DataChannel.Init();
                DataChannel dataChannel = mPeerConnection.createDataChannel("dc001", init);
                ByteBuffer mBuffer = stringToByteBuffer(mObject.toString());
                DataChannel.Buffer mBuffer2 = new DataChannel.Buffer(mBuffer, false);
                dataChannel.send(mBuffer2);

                return true;

            }
        });

        remoteView.setOnTouchListener(this);
        Log.e(TAG, "onCreate: Some log");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLinearLayout.setVisibility(View.GONE);
                mThread = new Thread(new ConnectToServer());
                mThread.start();
            }
        });

        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mPeerConnection.addStream(localMediaStream);
//                endSession();
                finishAndRemoveTask();
            }
        });

    }

    public void dataSend(String mObject){
        DataChannel.Init init = new DataChannel.Init();
        DataChannel dataChannel = mPeerConnection.createDataChannel("dc001", init);
        ByteBuffer mBuffer = stringToByteBuffer(mObject.toString());
        DataChannel.Buffer mBuffer2 = new DataChannel.Buffer(mBuffer, false);
        dataChannel.send(mBuffer2);
    }

    class SwipeListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return super.onSingleTapUp(e);
        }
    }


    public void initSurfaceView(org.webrtc.SurfaceViewRenderer surfaceViewRenderer) {
        surfaceViewRenderer.init(eglBaseContext, null);
        surfaceViewRenderer.setEnableHardwareScaler(true);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private class ConnectToServer implements Runnable {
        public String SERVER_IP = "192.168.131.69";
        public static final int SERVER_PORT = 8080;

        @Override
        public void run() {
            try {
                Log.e(TAG, "run: Connecting to server");
                clientSocket = new Socket(SERVER_IP, SERVER_PORT);
                Log.e(TAG, "run: Connected to server" + clientSocket.getRemoteSocketAddress());
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
                mBufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                mThread = new Thread(new ReadMessagesFromServer());
                mThread.start();

                MediaProjectionManager manager= (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(manager.createScreenCaptureIntent(),CAPTURE_PERMISSION_REQUEST_CODE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private class ReadMessagesFromServer implements Runnable {

        @Override
        public void run() {
            Log.e(TAG, "run: Read Message Thread started");
            while (true) {
                try {
                    final String messageFromServer = mBufferedReader.readLine();
                    if (messageFromServer != null) {
                        Log.e(TAG, "run: SERVER: " + messageFromServer);
                        JSONObject mObject = new JSONObject(messageFromServer);
                        Log.e(TAG, "run: " + mObject.getString("operation"));
                        Log.e(TAG, "run: " + mObject.getString("type"));
                        Log.e(TAG, "run: " + mObject.getString("description"));

                        String operation = mObject.getString("operation");
                        if (operation.equalsIgnoreCase("OFFER_SDP")) {
                            SessionDescription remoteSDP = new SessionDescription(SessionDescription.Type.OFFER, mObject.getString("description"));
                            setRemoteSessionDescription(remoteSDP);
                        } else if (operation.equalsIgnoreCase("IceCandidate")) {
                            IceCandidate mIceCandidate = new IceCandidate(
                                    mObject.getString("sdpMid"),
                                    mObject.getInt("sdpMLineIndex"),
                                    mObject.getString("sdp")
                            );
                            mPeerConnection.addIceCandidate(mIceCandidate);
                            Log.e(TAG, "run: ICE CANDIDATE ADDED ");

                        }
                    } else {
                        Log.e(TAG, "run: Message is null");
                        mThread = new Thread(new ConnectToServer());
                        mThread.start();
                        return;
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "run: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private class SendMessageToServer implements Runnable {
        private Object message;

        public SendMessageToServer(Object message) {
            this.message = message;
        }

        @Override
        public void run() {
            mPrintWriter.println(message);
            mPrintWriter.flush();
            Log.e(TAG, "run: Message sent to server: " + message);
        }
    }


    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE) {
            mediaProjectionResultIntent = data;
            mMediaProjectionPermissionResultCode = resultCode;
            createPeerConnection();
            setUpVideoData();
        }
    }

    private void setUpVideoData() {
//        mVideoCapturer = new ScreenCapturerAndroid(mediaProjectionResultIntent, new MediaProjection.Callback() {
//            @Override
//            public void onStop() {
//                super.onStop();
//                Log.e(ControlsProviderService.TAG, "onStop: Video capturer stopped");
//            }
//        });

        Camera2Enumerator camera2Enumerator=new Camera2Enumerator(getApplicationContext());
        String[] deviceNames= camera2Enumerator.getDeviceNames();
        for (String deviceName:
                deviceNames) {
            Log.e(ControlsProviderService.TAG, "setUpVideoData: Camera Device Name = "+deviceName );
        }
        mVideoCapturer=new Camera2Capturer(MainActivity.this, "0", new CameraVideoCapturer.CameraEventsHandler() {
            @Override
            public void onCameraError(String s) {
                Log.e(ControlsProviderService.TAG, "onCameraError: Camera error callback "+s );
            }

            @Override
            public void onCameraDisconnected() {
                Log.e(ControlsProviderService.TAG, "onCameraDisconnected: Camera Disconnected" );
            }

            @Override
            public void onCameraFreezed(String s) {
                Log.e(ControlsProviderService.TAG, "onCameraFreezed: Camera Frozen "+s);
            }

            @Override
            public void onCameraOpening(String s) {
                Log.e(ControlsProviderService.TAG, "onCameraOpening: "+s);
            }

            @Override
            public void onFirstFrameAvailable() {
                Log.e(ControlsProviderService.TAG, "onFirstFrameAvailable: ");
            }

            @Override
            public void onCameraClosed() {
                Log.e(ControlsProviderService.TAG, "onCameraClosed: ");
            }
        });

        EglBase.Context eglBaseContext = EglBase.create().getEglBaseContext();
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        localMediaStream= mPeerConnectionFactory.createLocalMediaStream("Rohit_Client_Video_Stream");
        mVideoSource = mPeerConnectionFactory.createVideoSource(mVideoCapturer.isScreencast());
        DisplayMetrics metrics = new DisplayMetrics();
        MainActivity.this.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        mVideoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), mVideoSource.getCapturerObserver());
        if (mVideoCapturer != null) {
            mVideoCapturer.startCapture(metrics.widthPixels, metrics.heightPixels-340, 90);
        }

        localVideoTrack = mPeerConnectionFactory.createVideoTrack("ARDAMSv0", mVideoSource);
        localVideoTrack.setEnabled(true);

        localMediaStream.addTrack(localVideoTrack);

//        localView = findViewById(R.id.localView);
//        localView.setMirror(true);
//        initSurfaceView(localView);
//        localVideoTrack.addSink(localView);

    }

    public void createPeerConnection() {
        List<PeerConnection.IceServer> mIceServers = new ArrayList<>();
//        for (String stunServer:
//                listOfStunServers) {
//            mIceServers.add(createIceServers(stunServer));
//        }
        PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302");
        iceServerBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
        PeerConnection.IceServer iceServer = iceServerBuilder.createIceServer();
        mIceServers.add(iceServer);
        PeerConnection.IceServer.Builder iceServerBuilder2 = PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302");
        iceServerBuilder2.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
        PeerConnection.IceServer iceServer2 = iceServerBuilder2.createIceServer();
        mIceServers.add(iceServer2);

//        mIceServers.add(iceServer);

        MediaConstraints constraints = new MediaConstraints();
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
//        AudioSource audioSource=mPeerConnectionFactory.createAudioSource(new MediaConstraints());
//        VideoSource localVideoSource = mPeerConnectionFactory.createVideoSource(true);
//        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
//        startActivityForResult(manager.createScreenCaptureIntent(), 909);
//        VideoCapturer mVideoCapturer = new ScreenCapturerAndroid(mediaProjectionResultIntent, new MediaProjection.Callback() {
//            @Override
//            public void onStop() {
//                super.onStop();
//            }
//        });

//        mVideoCapturer.startCapture(320,240,60);
//        AudioTrack localAudioTrack=mPeerConnectionFactory.createAudioTrack("_audioTrack",audioSource);
//        VideoTrack localVideoTrack = mPeerConnectionFactory.createVideoTrack("_videoTrack", localVideoSource);

        PeerConnection.Observer mObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.e(TAG, "onSignalingChange: " + signalingState.name());
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.e(TAG, "onIceConnectionChange: " + iceConnectionState.name());
                if(iceConnectionState== PeerConnection.IceConnectionState.DISCONNECTED){
                    createAnswer();
                }else if(iceConnectionState== PeerConnection.IceConnectionState.CONNECTED){
                    JSONObject jsonObject=new JSONObject();
                    DisplayMetrics metrics = new DisplayMetrics();
                    MainActivity.this.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

                    try {
                        jsonObject.put("operation","metrics");
//                        jsonObject.put("width",);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    sendToDataChannel();
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.e(TAG, "onIceConnectionReceivingChange: " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.e(TAG, "onIceGatheringChange: " + iceGatheringState.name());
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.e(TAG, "onIceCandidate: SDP =  " + iceCandidate.sdp);
                Log.e(TAG, "onIceCandidate: Server URL =  " + iceCandidate.serverUrl);
                Log.e(TAG, "onIceCandidate: SDP Mid =  " + iceCandidate.sdpMid);
                Log.e(TAG, "onIceCandidate: Adapter Type =  " + iceCandidate.adapterType);
                Log.e(TAG, "onIceCandidate: sdpMLineIndex =  " + iceCandidate.sdpMLineIndex);
//                mPeerConnection.addIceCandidate(iceCandidate);
                JSONObject mObject = new JSONObject();
                try {
                    mObject.put("operation", "IceCandidate");
                    mObject.put("sdp", iceCandidate.sdp);
                    mObject.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                    mObject.put("sdpMid", iceCandidate.sdpMid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mThread = new Thread(new SendMessageToServer(mObject.toString()));
                mThread.start();
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.e(TAG, "onIceCandidatesRemoved: Remaining ICE candidates= " + Arrays.toString(iceCandidates));
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
//                mediaStream.videoTracks.get(0).addSink(remoteView);
                Log.e(TAG, "onAddStream: " + mediaStream.getId() + "  size -> "+mediaStream.videoTracks.size());
//                VideoSink remote_view = null;
                VideoTrack mVideoTrack=mediaStream.videoTracks.get(0);
                mVideoTrack.setEnabled(true);
                mVideoTrack.addSink(remoteView);
//                Intent mIntent=new Intent(MainActivity.this,ScreenShareActivity.class);
//                mIntent.putExtra("videoTrack", (Parcelable) mVideoTrack);
//                startActivity(mIntent);
                mPeerConnection.addStream(localMediaStream);
//                button.setVisibility(View.GONE);
//                button2.setVisibility(View.GONE);
                createAnswer();

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.e(TAG, "onRemoveStream: " + mediaStream.getId());
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.e(TAG, "onDataChannel: " + dataChannel.label());
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
                        Log.e(TAG, "onBufferedAmountChange: " + l);
                    }

                    @Override
                    public void onStateChange() {
                        Log.e(TAG, "onStateChange: "+dataChannel.state());
                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        String msg = byteBufferToString(buffer.data);
                        Log.e(TAG, "onMessage: Received a new message =>" + msg);
                    }
                });
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.e(TAG, "onRenegotiationNeeded: ");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                mPeerConnection.addTrack(localVideoTrack);
                Log.e(TAG, "onAddTrack: " + rtpReceiver.id() + " " + Arrays.toString(mediaStreams));
            }
        };
        PeerConnection.RTCConfiguration mRtcConfiguration = new PeerConnection.RTCConfiguration(mIceServers);
        mPeerConnection = mPeerConnectionFactory.createPeerConnection(mRtcConfiguration, mObserver);
        Log.e(TAG, "createPeerConnection: DONE 2");

//        mPeerConnection.addTrack(localAudioTrack);
    }

    private PeerConnection.IceServer createIceServers(String stunServer) {
        PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder(stunServer);
        iceServerBuilder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
        return iceServerBuilder.createIceServer();

    }


    public void sendToDataChannel(String m) {
        ByteBuffer data = stringToByteBuffer(m);
        DataChannel.Init init = new DataChannel.Init();
        mDataChannel = mPeerConnection.createDataChannel("DataChannel", init);
        DataChannel.Buffer mBuffer = new DataChannel.Buffer(data, false);
        mDataChannel.send(mBuffer);
    }

    public void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions
                        .builder(getApplicationContext())
                        .setEnableInternalTracer(true)
                        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableNetworkMonitor = true;
//        options.disableEncryption=true;
        mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBaseContext))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .setOptions(options)
                .createPeerConnectionFactory();

    }

    private void endSession(){

        localMediaStream.dispose();
        if (mPeerConnection != null) {
            mPeerConnection.dispose();
        }

        /*if (mVideoCapturer != null) {
            mVideoCapturer.dispose();
        }

        if (mVideoSource != null) {
            mVideoSource.dispose();
        }

        if (mPeerConnectionFactory != null) {
            mPeerConnectionFactory.dispose();
        }

        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();

//        if (mediaProjection != null) {
//            mediaProjection.stop();
//        }

//        localView.release();
        remoteView.release();*/

    }
    private void setLocalSessionDescription(SessionDescription sessionDescription) {
//        RtpSender
        mPeerConnection.setLocalDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.e(TAG, "onCreateSuccess: Local Answer SDP created successfully");
            }

            @Override
            public void onSetSuccess() {
                Log.e(TAG, "onSetSuccess: Local Answer SDP set successfully");
                answerObject = new JSONObject();
                try {
                    answerObject.put("operation", "ANSWER_SDP");
                    answerObject.put("type", sessionDescription.type);
                    answerObject.put("description", sessionDescription.description);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mThread = new Thread(new SendMessageToServer(answerObject.toString()));
                mThread.start();

            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure: Local Answer SDP creation failure");

            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onCreateFailure: Local Answer SDP set failure");

            }
        }, sessionDescription);

    }

    private void setRemoteSessionDescription(SessionDescription sdp) {
        mPeerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.e(TAG, "onCreateSuccess: Remote SDP created successfully");
            }

            @Override
            public void onSetSuccess() {
                Log.e(TAG, "onSetSuccess: Remote SDP set successfully");
                createAnswer();
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure: Remote SDP creation failure");
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetFailure: Remote SDP set failure ");
                setRemoteSessionDescription(sdp);
            }
        }, sdp);

    }

    public void createAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        mPeerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                setLocalSessionDescription(sessionDescription);
                Log.e(TAG, "onCreateSuccess: Answer created successfully");
//                 answerObject=new JSONObject();
//                try {
//                    answerObject.put("type",sessionDescription.type);
//                    answerObject.put("description",sessionDescription.description);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                mThread=new Thread(new SendMessageToServer(answerObject.toString()));
//                mThread.start();

            }

            @Override
            public void onSetSuccess() {
                Log.e(TAG, "onSetSuccess: Local Answer SDP set successfully");
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onSetSuccess: Local Answer SDP creation failure");

            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetSuccess: Local Answer SDP set failure");

            }
        }, constraints);
    }


    public static ByteBuffer stringToByteBuffer(String msg) {
        return ByteBuffer.wrap(msg.getBytes(Charset.defaultCharset()));
    }

    public static String byteBufferToString(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return new String(bytes, Charset.defaultCharset());
    }
}