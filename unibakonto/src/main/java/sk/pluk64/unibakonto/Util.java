package sk.pluk64.unibakonto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

public class Util {
    public static String connInput2String(URLConnection conn) throws IOException, ConnectionFailedException {
        String contentType = conn.getContentType();
        if (contentType == null) {
            throw new ConnectionFailedException();
        } else {
            int charsetBegining = contentType.indexOf("charset=");
            String charsetName = charsetBegining != -1 ?
                    contentType.substring(charsetBegining + "charset=".length()) :
                    "utf-8";

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), charsetName)
            );

            StringBuilder sb = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                sb.append(line);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            return sb.toString();
        }
    }

    static byte[] paramsMap2PostData(Map<String, String> params) {
        String[] paramsArray = new String[params.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramsArray[i] = entry.getKey();
            paramsArray[i + 1] = entry.getValue();
            i += 2;
        }
        return paramsArray2PostData(paramsArray);
    }

    static byte[] paramsArray2PostData(String[] postParams) {
        for (int i = 0; i < postParams.length; i++) {
            try {
                postParams[i] = URLEncoder.encode(postParams[i], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        StringBuilder stringParams = new StringBuilder();
        for (int i = 0; i < postParams.length; i += 2) {
            stringParams.append(postParams[i]).append("=").append(postParams[i + 1]);
            if (i + 2 < postParams.length) {
                stringParams.append("&");
            }
        }
        return stringParams.toString().getBytes(Charset.forName("UTF-8"));
    }

    public static class ConnectionFailedException extends Exception {
    }
}