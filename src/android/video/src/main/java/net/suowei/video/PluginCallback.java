package net.suowei.video;

import org.json.JSONObject;

/**
 * Created by randy on 2017-4-21.
 */

public interface PluginCallback
{
    //100:启动成功
    //101:连接服务器成功
    //102:呼叫中时取消
    //103:通话中时挂断
    //104:未连接到服务器
    //105:呼叫人id为空
    //106:连接服务器超时
    void callback(JSONObject message);
}
