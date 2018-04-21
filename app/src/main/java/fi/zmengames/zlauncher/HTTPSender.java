package fi.zmengames.zlauncher;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HTTPSender extends AsyncTask<String, Void, String> {
    private static final String TAG = HTTPSender.class.getSimpleName();
    @Override
    protected String doInBackground(String... params) {

        String data = "";

        HttpsURLConnection httpURLConnection = null;
        try {

            httpURLConnection = (HttpsURLConnection) new URL(params[0]).openConnection();
            httpURLConnection.setRequestMethod("POST");

            httpURLConnection.setDoOutput(true);

     //       DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
     //       wr.writeBytes("PostData=" + params[0]);
     //       wr.flush();
     //       wr.close();
            InputStream in;
            int status = httpURLConnection.getResponseCode();
            if(status != 200)
                in = httpURLConnection.getErrorStream();
            else
                in = httpURLConnection.getInputStream();

            InputStreamReader inputStreamReader = new InputStreamReader(in);

            int inputStreamData = inputStreamReader.read();
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                data += current;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return data;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.e(TAG, result); // this is expecting a response code to be sent from your server upon receiving the POST data
    }
}