package io.github.anenasa.news;

import android.content.Context;
import android.os.StrictMode;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class YtDlp {
    private final String TAG = "YtDlp";
    private PyObject yt_dlp;
    static String version;

    /**
     * Initialize yt-dlp
     * @param context Context
     * @throws PyException yt-dlp failed to load
     */
    YtDlp(Context context) throws PyException, IOException {
        try {
            if(!YtDlp.isDownloaded(context)) {
                YtDlp.download(context);
            }
            Python py = Python.getInstance();
            String ytdl_filename = new File(context.getExternalFilesDir(null), "yt-dlp").toString();
            PyObject sys = py.getModule("sys");
            PyObject path = sys.get("path");
            path.callAttr("insert", 0, ytdl_filename);
            yt_dlp = py.getModule("yt_dlp");
            PyObject version_module = py.getModule("yt_dlp.version");
            version = version_module.get("__version__").toString();
        } catch (PyException | IOException e){
            new File(context.getExternalFilesDir(null), "yt-dlp").delete();
            throw e;
        }
    }

    /**
     * Check if yt-dlp is downloaded
     * @param context Context
     * @return true if yt-dlp is downloaded, false otherwise
     */
    public static boolean isDownloaded(Context context){
        File file = new File(context.getExternalFilesDir(null), "yt-dlp");
        return file.exists();
    }

    /**
     * Download yt-dlp binary to external storage
     * @param context Context
     * @return true if download is successful, false otherwise
     */
    public static void download(Context context) throws IOException {
        File file = new File(context.getExternalFilesDir(null), "yt-dlp.part");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        URL url = new URL("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();

        FileOutputStream fileOutput = new FileOutputStream(file);
        InputStream inputStream = urlConnection.getInputStream();

        byte[] buffer = new byte[1024];
        int bufferLength = 0;

        while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.close();
        file.renameTo(new File(context.getExternalFilesDir(null), "yt-dlp"));
    }

    /**
     * Extract video url
     * @param url url to extract
     * @param option python dictionary of options for yt-dlp
     * @return extracted video url
     * @throws PyException extraction failed
     * @see <a href="https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/YoutubeDL.py#L184">list of options</a>
     */
    public String extract(String url, PyObject option) throws PyException {
        PyObject ydl = yt_dlp.callAttr("YoutubeDL", option);
        PyObject info_dict = ydl.callAttr("extract_info", url, false);
        // url is in entries for playlist
        PyObject entries = info_dict.callAttr("get", "entries");
        if(entries != null){
            info_dict = entries.callAttr("__getitem__", 0);
        }
        PyObject videoUrl = info_dict.callAttr("get", "url");
        if(videoUrl != null) {
            return videoUrl.toString();
        }
        // url is in requested_formats for merging multiple formats
        PyObject requested_formats = info_dict.callAttr("get", "requested_formats");
        if(requested_formats != null){
            return requested_formats.callAttr("__getitem__", 0).callAttr("__getitem__", "url").toString() + "\n"
                    + requested_formats.callAttr("__getitem__", 1).callAttr("__getitem__", "url").toString();
        }
        throw new PyException("找不到影片網址");
    }
}
