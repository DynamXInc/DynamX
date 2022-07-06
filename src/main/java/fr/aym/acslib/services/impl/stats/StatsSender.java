package fr.aym.acslib.services.impl.stats;

import com.google.gson.JsonObject;
import fr.aym.acslib.services.impl.stats.core.LoadingFrame;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class StatsSender
{
    private static LoadingFrame FRAME;

    /**
     * @param gpu An info provider for gpu, product name and user id
     * @return A StatsSheet with current user and hardware information
     */
    public static StatsSheet provideStats(SystemInfoProvider gpu, boolean showFrame) {
        if(showFrame)
            FRAME = new LoadingFrame("Envoi du crash report...", null);
        return new StatsSheet(gpu.getUserId(), System.currentTimeMillis()/1000, gpu);
    }

    /**
     * @param to The url that will receive the POST data
     * @param data The StatsSheet to send
     * @throws IOException
     */
    public static void reportStats(URL to, @Nullable String credentials, StatsSheet data) throws IOException {
        if(FRAME != null)
        {
            FRAME.status.setText("Envoi au serveur en cours...");
        }
        try {
            JsonObject rqJson = data.toJson();
            StringBuilder rqString = new StringBuilder();
            if(rqJson.has("FileContent"))
            {
                String crashContent = rqJson.get("FileContent").getAsString();

                rqString.append("FILE_NAME="+rqJson.get("FileName").getAsString()+"&");
                rqString.append("FILE="+crashContent+"&");

                rqJson.remove("FileName");
                rqJson.remove("FileContent");
            }
            rqString.append("DT="+rqJson.toString().replaceAll("=", ":_:"));
            sendPostRq(to, credentials, rqString.toString().getBytes(Charset.forName("UTF-8")));
        } catch (UnknownHostException e) {
            System.err.println("[StatsBot] Cannot connect to target url ! (UnknownHost)");
        }
        finally
        {
            if(FRAME != null)
            {
                FRAME.setVisible(false);
                FRAME.dispose();
                FRAME = null;
            }
        }
    }

    public static void sendPostRq(URL requestUrl, @Nullable String credentials, byte[] postData) throws IOException
    {
        // Creating the HTTP Connection
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();

        // Adding some user agents
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        if(credentials != null)
            connection.addRequestProperty("Authorization", "Basic "+credentials);//c3Z6OnN0YWJkOTg=//YXJlZDptZHBzaGl0eA==");

        // Setting post enabled if needed
        connection.setRequestMethod("POST");

        // Writing the post data if needed
        if(postData != null) {
            connection.setDoOutput(true);

            OutputStream output = connection.getOutputStream();
            output.write(postData);

        }

        // Creating the buffered reader
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // Reading the response
        String response = "";
        String currentLine;

        while((currentLine = br.readLine()) != null)
            response += currentLine;

        System.out.println("[StatsBot] Request result: " + response);
    }
}
