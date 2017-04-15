package net.suowei.video;

/**
 * Created by randy on 2017-4-10.
 */

public interface ConnectListener
{
    void error();

    void connect();

    void timeout();
}
