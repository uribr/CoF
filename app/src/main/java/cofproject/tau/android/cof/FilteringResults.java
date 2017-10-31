package cofproject.tau.android.cof;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class FilteringResults extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //TODO - display results in the imageView view.
        setContentView(R.layout.activity_filtering_results);

    }

    private void goBackToMainActivity()
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    /**
     *
     * @param view
     */
    public void onDiscardResults(View view)
    {
        //TODO - make sure to delete/release the image.
        goBackToMainActivity();
    }

    /**
     *
     * @param view
     */
    public void onSaveResults(View view)
    {
        //TODO - save image.
        goBackToMainActivity();
    }


}

