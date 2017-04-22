package net.suowei.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUtils
{

    public static int toPixel(Context context, int dip)
    {
        final float scale = context.getResources().getDisplayMetrics().density;
        return new Float(dip * scale + 0.5f).intValue();
    }

    public static Drawable getDrawable(String url)
    {
        Drawable drawable = null;
        try
        {
            drawable = Drawable.createFromStream(new URL(url).openStream(), "image.jpg");
        }
        catch (IOException e)
        {
            Log.e(AppUtils.class.getName(), e.toString());
        }
        return drawable ;
    }

    public static Bitmap getBitmap(String url)
    {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputstream = null;
        try
        {
            URL imageurl = new URL(url);
            connection = (HttpURLConnection)imageurl.openConnection();
            connection.setConnectTimeout(6000);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.connect();
            inputstream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputstream);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(inputstream != null)
                {
                    inputstream.close();
                }
                if(connection != null)
                {
                    connection.disconnect();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return bitmap;
    }
}
