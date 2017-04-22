package net.suowei.video;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.net.URISyntaxException;

public class VideoClient implements Serializable
{
    public String caller;

    public String callee;

    public Socket socket;

    public ConnectListener connectListener;

    public VideoListener videoListener;

    public String url;

    public VideoClient(String url, ConnectListener connectListener, VideoListener videoListener)
    {
        this.connectListener = connectListener;
        this.videoListener = videoListener;
        this.url = url;
    }

    public void connect()
    {
        try
        {
            this.socket = IO.socket(url);

            this.socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener()
            {
                public void call(Object... args)
                {
                    VideoClient.this.connectListener.error();
                    VideoClient.this.socket.disconnect();
                    VideoClient.this.socket.off();
                }
            });

            this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener()
            {
                public void call(Object... args)
                {
                    JSONObject message = new JSONObject();
                    try
                    {
                        message.put("name", VideoClient.this.caller);
                        VideoClient.this.socket.emit("login", message);
                    }
                    catch(JSONException e)
                    {
                        e.printStackTrace();
                    }
                    VideoClient.this.connectListener.connect();
                }
            });

            this.socket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener()
            {
                public void call(Object... args)
                {
                    VideoClient.this.connectListener.timeout();
                }
            });


            this.socket.on("message", new Emitter.Listener()
            {
                @Override
                public void call(Object... args)
                {
                    try
                    {
                        JSONObject message = (JSONObject) args[0];
                        String event = message.getString("event");
                        String caller = message.getString("caller");
                        VideoClient.this.callee = caller;
                        if(event.equals("candidate"))
                        {
                            VideoClient.this.videoListener.addIceCandidate(message);
                        }
                        else
                        {
                            VideoClient.this.videoListener.setRemoteDescription(message);
                            if(event.equals("offer"))
                            {
                                VideoClient.this.videoListener.createAnswer();
                            }
                        }
                    }
                    catch(JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            this.socket.connect();
        }
        catch(URISyntaxException e)
        {
            e.printStackTrace();
        }
    }

    public void send(String type, Object message)
    {
        this.socket.emit(type, message);
    }

    public void call()
    {
        this.videoListener.createOffer();
    }


    public void onPause()
    {
    }

    public void onResume()
    {
    }

    public void onDestroy()
    {
        socket.disconnect();
        socket.off();
        socket.close();
    }
}
