package net.suowei.video;

import android.util.Log;

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

    public VideoClient(ConnectListener connectListener, VideoListener videoListener)
    {
        this.connectListener = connectListener;
        this.videoListener = videoListener;
    }

    public void connect()
    {
        Log.e("测试消息", "链接服务器");
        try
        {
            this.socket = IO.socket("http://192.168.1.104:3000");

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
                    Log.e("lianjie", "链接成功");
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
                    Log.e("测试消息", "接收到远程消息");
                    try
                    {
                        JSONObject message = (JSONObject) args[0];
                        String event = message.getString("event");
                        String caller = message.getString("caller");
                        VideoClient.this.callee = caller;
                        if(event.equals("candidate"))
                        {
                            Log.e("测试消息", "candidate消息，开始设置ice服务");
                            VideoClient.this.videoListener.addIceCandidate(message);
                        }
                        else
                        {
                            VideoClient.this.videoListener.setRemoteDescription(message);
                            if(event.equals("offer"))
                            {
                                Log.e("测试消息", "请求连接消息，开始创建应答");
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
