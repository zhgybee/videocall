package net.suowei.video;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class VideoActivity extends Activity implements VideoListener, ConnectListener
{

    private GLSurfaceView videoview;

    private VideoClient client;

    private PeerConnection peerConnection;

    private MediaConstraints peerConstraints = new MediaConstraints();

    private VideoRenderer.Callbacks localrenderer = null;

    private VideoRenderer.Callbacks remoterenderer = null;

    private PeerConnectionFactory peerConnectionFactory;

    private VideoTrack videotrack;

    private AudioTrack audiotrack;

    private VideoSource videosource;

    private AudioSource audiosource;

    private int REQUEST_CODE_ASK_PERMISSIONS = 1981;

    private Typeface iconTypeFace;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        iconTypeFace = Typeface.createFromAsset(getAssets(),"fonts/iconfont.ttf");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.video);

        Set<String> permissions = new HashSet<String>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);

        Set<String> usepermissions = new HashSet<String>();
        for(String permission : permissions)
        {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                usepermissions.add(permission);
            }
        }

        if(usepermissions.size() == 0)
        {
            initialise();
        }
        else
        {
            boolean isactivate = false;
            for(String permission : usepermissions)
            {
                isactivate = isactivate || ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
            }

            if (isactivate)
            {
                Toast.makeText(this, "打开失败，必须同意权限请求才可使用本程序。", Toast.LENGTH_SHORT).show();
            }
            else
            {
                ActivityCompat.requestPermissions(this, usepermissions.toArray(new String[usepermissions.size()]), REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if(requestCode == REQUEST_CODE_ASK_PERMISSIONS)
        {
            boolean agree = true;
            for(int result : grantResults)
            {
                agree = (result == PackageManager.PERMISSION_GRANTED) && agree;
            }

            if (agree)
            {
                initialise();
            }
            else
            {
                Toast.makeText(this, "打开失败，必须同意权限请求才可使用本程序。", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void initialise()
    {
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        peerConnectionFactory = new PeerConnectionFactory();

        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        MediaConstraints videoconstraints = new MediaConstraints();
        videoconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", String.valueOf(point.x)));
        videoconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", String.valueOf(point.y)));
        videoconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", String.valueOf(30)));
        videoconstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", String.valueOf(30)));

        VideoCapturerAndroid viewcapturer = VideoCapturerAndroid.create(CameraEnumerationAndroid.getNameOfFrontFacingDevice(), null);
        videosource = peerConnectionFactory.createVideoSource(viewcapturer, videoconstraints);
        videotrack = peerConnectionFactory.createVideoTrack("ARDAMSV0", videosource);
        audiosource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        audiotrack = peerConnectionFactory.createAudioTrack("ARDAMSA0", audiosource);

        peerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        peerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        videoview = new GLSurfaceView(this);
        setContentView(videoview);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int width = size.x;

        TextView setupbutton = new TextView(this);
        setupbutton.setText(R.string.icon_setup);
        setupbutton.setTextSize(50);
        setupbutton.setTextColor(0x44ffffff);
        setupbutton.setTypeface(iconTypeFace);
        setupbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                VideoActivity.this.finish();
            }
        });

        int space = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        setupbutton.measure(space, space);
        setupbutton.setX(width - setupbutton.getMeasuredWidth() - 20);
        setupbutton.setY(20);
        addContentView(setupbutton, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT ,FrameLayout.LayoutParams.WRAP_CONTENT));
        VideoRendererGui.setView(videoview, null);

        try
        {
            remoterenderer = VideoRendererGui.createGuiRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
            localrenderer = VideoRendererGui.createGuiRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        videotrack.addRenderer(new VideoRenderer(localrenderer));


        Intent intent = getIntent();
        String caller = intent.getStringExtra("caller");
        if(caller != null && !caller.equals(""))
        {
            LinkedList<PeerConnection.IceServer> iceservers = new LinkedList<PeerConnection.IceServer>();
            iceservers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

            peerConnection = peerConnectionFactory.createPeerConnection(iceservers, peerConstraints, new PeerObserver());

            MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("ARDAMS");
            mediaStream.addTrack(videotrack);
            mediaStream.addTrack(audiotrack);
            peerConnection.addStream(mediaStream);

            String callee = intent.getStringExtra("callee");

            client = new VideoClient(VideoActivity.this, VideoActivity.this);
            client.caller = caller;
            client.connect();

            if(callee != null && !callee.equals(""))
            {
                client.callee = callee;
                client.call();
            }
        }
        else
        {
            VideoActivity.this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(getApplicationContext(), "呼叫人未设置", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void onPause()
    {
        Log.e("测试消息", "onPause");
        if(videoview != null)
        {
            videoview.onPause();
        }
        if(videosource != null)
        {
            videosource.stop();
        }
        if(client != null)
        {
            client.onPause();
        }
        super.onPause();
    }

    public void onResume()
    {
        Log.e("测试消息", "onResume");
        if(videoview != null)
        {
            videoview.onResume();
        }
        if(videosource != null)
        {
            videosource.restart();
        }
        if(client != null)
        {
            client.onResume();
        }
        super.onResume();
    }

    public void onDestroy()
    {
        Log.e("测试消息", "onDestroy");
        if(peerConnection != null)
        {
            peerConnection.dispose();
        }

        if(videosource != null)
        {
            videosource.dispose();
        }

        if(peerConnectionFactory != null)
        {
            peerConnectionFactory.dispose();
        }

        if(client != null)
        {
            client.onDestroy();
        }
        super.onDestroy();
    }



    public void error()
    {
        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), "连接失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void connect()
    {
        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void timeout()
    {
        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(), "连接超时", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addIceCandidate(JSONObject message) throws JSONException
    {
        Log.e("测试消息", "设置本地的远程iceserver");
        if(!message.isNull("candidate"))
        {
            JSONObject candidate = message.getJSONObject("candidate");
            if(candidate != null)
            {
                peerConnection.addIceCandidate(new IceCandidate(candidate.getString("sdpMid"), candidate.getInt("sdpMLineIndex"), candidate.getString("candidate")));
            }
        }
    }

    public void setRemoteDescription(JSONObject message) throws JSONException
    {
        Log.e("测试消息", "设置本地的远程webrtc描述");
        JSONObject description = message.getJSONObject("description");
        peerConnection.setRemoteDescription(new PeerSdpObserver(), new SessionDescription(SessionDescription.Type.fromCanonicalForm(description.getString("type")), description.getString("sdp")));
    }

    public void createAnswer()
    {
        Log.e("测试消息", "创建answer");
        peerConnection.createAnswer(new PeerSdpObserver(), peerConstraints);
    }


    public void createOffer()
    {
        Log.e("测试消息", "创建offer");
        peerConnection.createOffer(new PeerSdpObserver(), peerConstraints);
    }

    private class PeerSdpObserver implements SdpObserver
    {
        public void onCreateSuccess(SessionDescription session)
        {
            try
            {
                Log.e("测试消息", "设置本地的webrtc描述");
                peerConnection.setLocalDescription(this, session);
                Log.e("测试消息", "发送创建的offer或answer");
                JSONObject message = new JSONObject();
                message.put("event", session.type.canonicalForm());
                JSONObject description = new JSONObject();
                description.put("type", session.type.canonicalForm());
                description.put("sdp", session.description);
                message.put("description", description);
                message.put("caller", VideoActivity.this.client.caller);
                message.put("callee", VideoActivity.this.client.callee);
                VideoActivity.this.client.send("message", message);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }

        public void onSetSuccess()
        {
            Log.e("1111111111111", "onSetSuccess");
        }

        public void onCreateFailure(String s)
        {
            Log.e("1111111111111", "onCreateFailure");
        }

        public void onSetFailure(String s)
        {
            Log.e("1111111111111", "onSetFailure");
        }
    }

    private class PeerObserver implements PeerConnection.Observer
    {

        public void onSignalingChange(PeerConnection.SignalingState signalingState)
        {
            Log.e("1111111111111", "onSignalingChange");
        }

        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState)
        {
        }

        public void onIceConnectionReceivingChange(boolean b)
        {
        }

        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState)
        {
        }

        public void onIceCandidate(IceCandidate icecandidate)
        {
            Log.e("测试消息", "发送iceserver");
            try
            {
                JSONObject message = new JSONObject();
                message.put("event", "candidate");
                JSONObject candidate = new JSONObject();
                candidate.put("sdpMid", icecandidate.sdpMid);
                candidate.put("sdpMLineIndex", icecandidate.sdpMLineIndex);
                candidate.put("candidate", icecandidate.sdp);
                message.put("candidate", candidate);
                message.put("caller", VideoActivity.this.client.caller);
                message.put("callee", VideoActivity.this.client.callee);
                VideoActivity.this.client.send("message", message);
            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }
        }

        public void onAddStream(MediaStream mediaStream)
        {
            Log.e("测试消息", "开始加载远程视频流");
            mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoterenderer));
            VideoRendererGui.update(remoterenderer, 0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);

            VideoRendererGui.update(localrenderer, 3, 3, 25, 25, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
        }

        public void onRemoveStream(MediaStream mediaStream)
        {
            Log.e("1111111111111", "onRemoveStream");
            peerConnection.close();
        }

        public void onDataChannel(DataChannel dataChannel)
        {
            Log.e("1111111111111", "onDataChannel");
        }

        public void onRenegotiationNeeded()
        {
            Log.e("1111111111111", "onRenegotiationNeeded");
        }
    }

}
