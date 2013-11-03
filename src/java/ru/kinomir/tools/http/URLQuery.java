/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kinomir.tools.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 *
 * @author Антон
 */
public class URLQuery {

    public static String excutePost(String targetURL, String requstContent) {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Content-Length", Integer.toString(requstContent.getBytes("CP1251").length));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(requstContent.getBytes("CP1251"));
            wr.flush();
            wr.close();

            //Get Response	
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String excutePost(String targetURL, Map<String, String> requestParams) {
        URL url;
        HttpURLConnection connection = null;
        try {
            StringBuilder requstContent = new StringBuilder();
            for (String paramName : requestParams.keySet()) {
                requstContent.append(paramName).append("=").append(requestParams.get(paramName)).append("&");
            }
            if (requstContent.length() > 0) {
                requstContent.deleteCharAt(requstContent.length() - 1);
            }
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Content-Length", Integer.toString(requstContent.toString().getBytes("UTF8").length));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(requstContent.toString().getBytes("UTF-8"));
            wr.flush();
            wr.close();

            //Get Response	
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
