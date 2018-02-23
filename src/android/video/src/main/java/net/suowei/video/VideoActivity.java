package net.suowei.video;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private GLSurfaceView videoView;

    private VideoClient client;

    private PeerConnection peerConnection;

    private MediaConstraints peerConstraints = new MediaConstraints();

    private VideoRenderer.Callbacks localRenderer = null;

    private VideoRenderer.Callbacks remoteRenderer = null;

    private PeerConnectionFactory peerConnectionFactory;

    private VideoTrack videoTrack;

    private AudioTrack audioTrack;

    private VideoSource videoSource;

    private AudioSource audioSource;

    private int REQUEST_CODE_ASK_PERMISSIONS = 1981;

    private Typeface iconTypeFace;

    private View connectingLayout;

    private View connectedLayout;

    private Chronometer timer;

    private AudioManager audioManager;

    private VideoCapturerAndroid viewcapturer;

    private ImageView calleeicon;

    private User caller;

    private User callee;

    private LocalBroadcastManager localBroadcastManager;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        iconTypeFace = Typeface.createFromAsset(getAssets(),"fonts/iconfont.ttf");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

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
        Point screensize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screensize);
        int screenwidth = screensize.x;
        int screenheight = screensize.y;

        this.audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        this.audioManager.setSpeakerphoneOn(true);

        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        peerConnectionFactory = new PeerConnectionFactory();

        MediaConstraints mediaconstraints = new MediaConstraints();
        mediaconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", String.valueOf(screenwidth)));
        mediaconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", String.valueOf(screenheight)));
        mediaconstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", String.valueOf(30)));
        mediaconstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", String.valueOf(30)));

        viewcapturer = VideoCapturerAndroid.create(CameraEnumerationAndroid.getNameOfFrontFacingDevice(), null);

        videoSource = peerConnectionFactory.createVideoSource(viewcapturer, mediaconstraints);
        videoTrack = peerConnectionFactory.createVideoTrack("ARDAMSV0", videoSource);
        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        audioTrack = peerConnectionFactory.createAudioTrack("ARDAMSA0", audioSource);

        peerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        peerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        videoView = new GLSurfaceView(this);
        setContentView(videoView);
        VideoRendererGui.setView(videoView, null);

        try
        {
            remoteRenderer = VideoRendererGui.createGuiRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
            localRenderer = VideoRendererGui.createGuiRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        videoTrack.addRenderer(new VideoRenderer(localRenderer));

        Intent intent = getIntent();
        caller = (User)intent.getSerializableExtra("caller");
        if(caller.id != null && !caller.id.equals(""))
        {
            String url = intent.getStringExtra("url");
            String iceserver = intent.getStringExtra("iceserver");

            LinkedList<PeerConnection.IceServer> iceservers = new LinkedList<PeerConnection.IceServer>();

			if(iceserver != null)
			{
				String[] servers = iceserver.split(",");
				for(int i = 0 ; i < servers.length ; i++)
				{
					String server = servers[i];
					String[] serveritems = server.split("|");
					if(serveritems.length == 3)
					{
						String iceurl = serveritems[0];
						String icename = serveritems[1];
						String icepassword = serveritems[2];
						iceservers.add(new PeerConnection.IceServer(iceurl, icename, icepassword));
					}
					else if(serveritems.length == 1)
					{
						String iceurl = serveritems[0];
						iceservers.add(new PeerConnection.IceServer(iceurl));
					}
				}
			}


            peerConnection = peerConnectionFactory.createPeerConnection(iceservers, peerConstraints, new PeerObserver());

            MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("ARDAMS");
            mediaStream.addTrack(videoTrack);
            mediaStream.addTrack(audioTrack);
            peerConnection.addStream(mediaStream);

            callee = (User)intent.getSerializableExtra("callee");

            client = new VideoClient(url, VideoActivity.this, VideoActivity.this);
            client.caller = caller.id;
            client.connect();

            if(callee.id != null && !callee.id.equals(""))
            {
                client.callee = callee.id;
                client.call();
            }
            else
            {
                connecting();
            }
        }
        else
        {
            VideoActivity.this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    VideoActivity.this.broadcast("105", "");
                    Toast.makeText(getApplicationContext(), "呼叫人未设置", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void connecting()
    {
        startBellService();
        if(this.connectingLayout == null)
        {
            this.connectingLayout = createConnectingLayout();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            addContentView(this.connectingLayout, params);
        }
    }

    public void connected()
    {
        stopBellService();
        if(this.connectingLayout != null)
        {
            this.connectingLayout.setVisibility(View.GONE);
        }
        if(this.connectedLayout == null)
        {
            this.connectedLayout = createConnectedLayout();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            addContentView(this.connectedLayout, params);
        }

        timer = (Chronometer)this.connectedLayout.findViewById(R.id.timer);
        timer.start();
    }


    public View createConnectingLayout()
    {
        View view = View.inflate(this, R.layout.connecting, null);
        ImageView cancelbutton = (ImageView)view.findViewById(R.id.cancel);
        cancelbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                VideoActivity.this.broadcast("102", "");
                VideoActivity.this.finish();
            }
        });

        this.calleeicon = (ImageView)view.findViewById(R.id.calleeicon);

        if(VideoActivity.this.callee.icon != null && !VideoActivity.this.callee.icon.equals(""))
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    Bitmap bitmap = AppUtils.getBitmap(VideoActivity.this.callee.icon);

                    Message message = new Message();
                    message.obj = bitmap;
                    loadCalleeIcon.sendMessage(message);
                }
            }).start();
        }

        TextView calleename = (TextView)view.findViewById(R.id.calleename);
        calleename.setText(this.callee.name);

        return view;
    }

    public View createConnectedLayout()
    {
        View view = View.inflate(this, R.layout.connected, null);
        TextView speakerbutton = (TextView)view.findViewById(R.id.speaker);
        speakerbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if(!VideoActivity.this.audioManager.isSpeakerphoneOn())
                {
                    VideoActivity.this.audioManager.setSpeakerphoneOn(true);
                    VideoActivity.this.audioManager.setMode(AudioManager.MODE_NORMAL);
                }
                else
                {
                    VideoActivity.this.audioManager.setSpeakerphoneOn(false);
                    VideoActivity.this.audioManager.setMode(AudioManager.MODE_IN_CALL);
                }
            }
        });
        TextView camerabutton = (TextView)view.findViewById(R.id.camera);
        camerabutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                VideoActivity.this.viewcapturer.switchCamera(null);
            }
        });
        ImageView hangupbutton = (ImageView)view.findViewById(R.id.hangup);
        hangupbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                VideoActivity.this.broadcast("103", String.valueOf(VideoActivity.this.timer.getText()));
                VideoActivity.this.finish();
            }
        });

        speakerbutton.setTypeface(iconTypeFace);
        camerabutton.setTypeface(iconTypeFace);


        return view;
    }

    private Handler loadCalleeIcon = new Handler()
    {
        public void handleMessage(Message message)
        {
            Bitmap bitmap = (Bitmap)message.obj;
            VideoActivity.this.calleeicon.setImageBitmap(bitmap);
        }
    };

    public void startBellService()
    {
        Intent intent = new Intent(this, BellService.class);
        this.startService(intent);
    }

    public void stopBellService()
    {
        Intent intent = new Intent(this, BellService.class);
        this.stopService(intent);
    }

    public void broadcast(String code, String message)
    {
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent();
        intent.putExtra("code", code);
        intent.putExtra("message", message);
        intent.setAction(PluginBroadcastReceiver.BROADCASTCODE);
        localBroadcastManager.sendBroadcast(intent);
    }

    public void onPause()
    {
        stopBellService();
        if(videoView != null)
        {
            videoView.onPause();
        }
        if(videoSource != null)
        {
            videoSource.stop();
        }
        if(client != null)
        {
            client.onPause();
        }
        super.onPause();
    }

    public void onResume()
    {
        startBellService();
        if(videoView != null)
        {
            videoView.onResume();
        }
        if(videoSource != null)
        {
            videoSource.restart();
        }
        if(client != null)
        {
            client.onResume();
        }
        super.onResume();
    }

    public void onDestroy()
    {
        stopBellService();
        if(peerConnection != null)
        {
            peerConnection.dispose();
        }

        if(videoSource != null)
        {
            videoSource.dispose();
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
                VideoActivity.this.finish();
                VideoActivity.this.broadcast("104", "");
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
                VideoActivity.this.broadcast("101", "");
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
                VideoActivity.this.finish();
                VideoActivity.this.broadcast("106", "");
                Toast.makeText(getApplicationContext(), "连接超时", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addIceCandidate(JSONObject message) throws JSONException
    {
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
        JSONObject description = message.getJSONObject("description");
        peerConnection.setRemoteDescription(new PeerSdpObserver(), new SessionDescription(SessionDescription.Type.fromCanonicalForm(description.getString("type")), description.getString("sdp")));
    }

    public void createAnswer()
    {
        peerConnection.createAnswer(new PeerSdpObserver(), peerConstraints);
    }

    public void createOffer()
    {
        peerConnection.createOffer(new PeerSdpObserver(), peerConstraints);
    }


    private class PeerSdpObserver implements SdpObserver
    {
        public void onCreateSuccess(SessionDescription session)
        {
            try
            {
                peerConnection.setLocalDescription(this, session);
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
        }

        public void onCreateFailure(String s)
        {
        }

        public void onSetFailure(String s)
        {
        }
    }

    private class PeerObserver implements PeerConnection.Observer
    {

        public void onSignalingChange(PeerConnection.SignalingState signalingState)
        {
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
            VideoActivity.this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    connected();
                }
            });
            mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRenderer));
            VideoRendererGui.update(remoteRenderer, 0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRendererGui.update(localRenderer, 3, 3, 25, 25, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
        }

        public void onRemoveStream(MediaStream mediaStream)
        {
            VideoRendererGui.update(localRenderer, 0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            peerConnection.close();
        }

        public void onDataChannel(DataChannel dataChannel)
        {
        }

        public void onRenegotiationNeeded()
        {
        }
    }

}
