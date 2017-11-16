package cofproject.tau.android.cof;


import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Uri on 31/10/2017.
 */

public class ImageViewFragment extends Fragment
{
    private ImageView imageView;
    private Bitmap bitmap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.image_view_fragment, container, false);
        imageView = view.findViewById(R.id.new_photo_view);
        imageView.setImageBitmap(bitmap);
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        imageView.setImageBitmap(bitmap);
    }


    public void setImage(Uri uri)
    {
        InputStream stream = null;
        try
        {
            if (bitmap != null) { bitmap.recycle(); }
            if(uri != null)
            {
                // Store image
                stream = getActivity().getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(stream);
            }
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        finally
        {
            if (stream != null)
            {
                try { stream.close(); }
                catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    public void setImage(Bitmap bmp)
    {
        bitmap = bmp.copy(bmp.getConfig(), false);
        if(this.isResumed())
        {
            imageView.setImageBitmap(bitmap);
        }
    }


    public void setImage(byte[] bytesArray)
    {
            bitmap = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.length);
    }
}
