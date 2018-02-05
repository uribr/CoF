package cofproject.tau.android.cof;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InfoActivity extends AppCompatActivity {

    private static final String TAG = "InfoActivity";
    private StringBuilder mStringBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        setTitle("About");

        mStringBuilder = new StringBuilder();
        getInfoText();

        TextView textView = findViewById(R.id.infoTextView);
        textView.setText(Html.fromHtml(mStringBuilder.toString()));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

    }



    private void getInfoText() {
        BufferedReader reader = null;
        String line;

        try {
            reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.info)));
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "getInfoText: no info file found", e);
        }

        try {
            if (reader != null) {
                while ((line = reader.readLine())!=null) {
                    mStringBuilder.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Error reading file!",Toast.LENGTH_LONG).show();
            e.printStackTrace();
            Log.e(TAG, "getInfoText: IOException", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getInfoText: IOException", e);
            }
        }
    }
}
