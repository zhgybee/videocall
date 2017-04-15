package net.suowei.video;

import org.json.JSONException;
import org.json.JSONObject;

public interface VideoListener
{
    void addIceCandidate(JSONObject message) throws JSONException;

    void setRemoteDescription(JSONObject message) throws JSONException;

    void createAnswer();

    void createOffer();
}
