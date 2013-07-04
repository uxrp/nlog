import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPOutputStream;

public class NLogPoster {

     private static void log(String message) {
          System.out.println(message);
     }

     private static String join(String separator, String... items) {
     	StringBuffer sb = new StringBuffer();
     	for (String item : items) {
     		sb.append(separator);
     	    sb.append(item);
        }
        if (sb.length() > 0) sb.delete(0, separator.length());
        return sb.toString();
     }

     public static void main(String args[]) {
    	URL url;
		try {
			url = new URL("http://localhost:2013/command/?command=nlog-post");

			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(4000);
			
			// POST方式
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "gzip");
			
			conn.connect();
			
			GZIPOutputStream gos = new GZIPOutputStream(conn.getOutputStream());
			gos.write(join("",
				"ht=event&ec=button&ea=click&el=save&t=" + System.currentTimeMillis() + "\n",
				"ht=event&ec=button&ea=click&el=save&t=" + System.currentTimeMillis() + "\n",
				"ht=event&ec=button&ea=click&el=load&t=" + System.currentTimeMillis() + "\n",
				"ht=appview\n"
			).getBytes());
			
			gos.close();
			gos = null;

			StringBuffer sb = new StringBuffer();
			BufferedReader bufr = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String inputString;
			while ((inputString = bufr.readLine()) != null) {
				sb.append(inputString);
			}
			bufr.close(); // 流关闭，会释放连接资源
			bufr = null;

			conn.disconnect();

			int length = conn.getContentLength();
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK && length != 0) {
				log(String.format("return =>\n %s", sb));
			} else {
				log(String.format("error"));
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
     }
}
