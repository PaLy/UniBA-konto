package sk.pluk64.unibakonto.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.charset.Charset;

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

    static byte[] paramsArray2PostData(String[] postParams) {
        StringBuilder stringParams = new StringBuilder();
        for (int i = 0; i < postParams.length; i += 2) {
            stringParams.append(postParams[i]).append("=").append(postParams[i + 1]);
            if (i + 2 < postParams.length) {
                stringParams.append("&");
            }
        }
//        System.out.println(Arrays.toString(postParams));
//        System.out.println(stringParams.toString());
        return stringParams.toString().getBytes(Charset.forName("UTF-8"));
    }

    public static class ConnectionFailedException extends Exception {
    }
}