package com.baidu.nlog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.net.Proxy;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.util.Log;

@SuppressLint({ "HandlerLeak", "DefaultLocale" })
public class NStorage {
    /**
     *  日志TAG
     */
    private static String LOGTAG = (new Object() {
        public String getClassName() {
            String clazzName = this.getClass().getName();
            return clazzName.substring(0, clazzName.lastIndexOf('$'));
        }
    }).getClassName();

    /**
     * 设备id
     */
    private static String deviceId = "";
    
    /**
     * 建立链接超时,单位：毫秒
     */
    private static final int connTimeout = 40 * 1000; // 40秒
    /**
     * 读取链接超时,单位：毫秒
     */
    private static final int readTimeout = 60 * 1000; //60秒 获取数据超时
    /*
     * 存储文件版本 // 用来处理文件版本改变
     */
    public static final String fileVersion = "0";
    
    /**
     * 保存缓存文件的目录
     */
    private static String rootDir = Environment.getExternalStorageDirectory() + File.separator + "_nlog_cache";
    /**
     * 规则文件名
     */
    private static String ruleFilename = rootDir + File.separator + "urle.dat";
    
    /**
     * 缓存文件名模板 _nlog_[version]_[itemname].dat, itemname => [name].[md5(head)]
     */
    private static final String cacheFileFormat = rootDir + File.separator + "_nlog_%s_%s.dat";
        
    /**
     * 缓存项
     */
    private static class CacheItem {
        public StringBuffer sb;
        public String name;
        public String head;
        public byte[] pass;
        /**
         * 构造
         * @param name 项名 
         * @param head 首行
         * @param sb 字符缓存
         */
        CacheItem(String name, String head) {
            this.sb = new StringBuffer();
            this.head = head;
            this.name = name;

            this.pass = buildPass(name);
        }
    }
    
    /**
     * 发送项
     */
    private static class PostItem {
        public String name;
        public byte[] pass;
        public String locked;
        /**
         * 构造
         * @param name 项名
         * @param locked 锁定文件名
         */
        PostItem(String name, String locked) {
            this.name = name;
            this.locked = locked;
            this.pass = buildPass(name);
        }
    }
    
    /**
     * 文件密钥，可在实际上线是修改成自己的
     */
    private static String secretKey = "5D97EEF8-3127-4859-2222-82E6C8FABD8A";
    
    /**
     * 密钥串缓存，为了提升速度
     */
    private static Map<String, byte[]> passMap = new HashMap<String, byte[]>(); 
    
    /**
     * 生成密钥，如果规则修改需要升级fileVersion
     * @param name 项名
     * @return 返回密钥串
     */
    private static byte[] buildPass(String name) {
        byte[] result = passMap.get(name);
        if (result != null) return result;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(String.format("%s,%s,%s", deviceId, name, secretKey).getBytes());
            baos.write(md.digest());
            md.update(String.format("%s,%s,%s", name, deviceId, secretKey).getBytes());
            baos.write(md.digest());
            md.update(String.format("%s,%s,%s", deviceId, secretKey, name).getBytes());
            baos.write(md.digest());
            result = baos.toByteArray(); 
            baos.close();
            passMap.put(name, result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 缓存日志
     */
    private static Map<String, CacheItem> cacheItems = new HashMap<String, CacheItem>();
    
    /**
     * 处理字段名缩写，如果缩写为null则将其过滤
     * @param protocolParameter 字段名字典
     * @param map 参数集合
     * @return 返回处理后的字典
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> runProtocolParameter(Object protocolParameter, Map<String, Object> map) {
        if (protocolParameter == null || (!(protocolParameter instanceof Map))) {
            return map;
        }
        Map<String, Object> parameter = (HashMap<String, Object>)protocolParameter;
        Map<String, Object> result = new HashMap<String, Object>();
        for (String key : map.keySet()) {
            if (parameter.containsKey(key)) { // 需要转换
                Object newKey = parameter.get(key);
                if (newKey != null) { // 为null时抛弃
                    result.put((String)newKey, map.get(key));
                }
            } else {
                result.put(key, map.get(key));
            }
        }
        return result;
    }

    
    private static class ReportParamItem {
        public String trackerName;
        public Map<String, Object> fields;
        public Map<String, Object> data;
        ReportParamItem(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
            this.trackerName = trackerName;
            this.fields = fields;
            this.data = data;
        }
    }
    private static ArrayList<ReportParamItem> reportParamList = new ArrayList<ReportParamItem>();
    
    /**
     * 上报数据
     * @param trackerName 追踪器名称
     * @param fields 公共字段
     * @param data 上报数据
     */
    public static void report(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
        /* debug start */
        Log.d(LOGTAG, String.format("report(%s, %s) postUrl='%s'", fields, data, fields.get("postUrl")));
        /* debug end */
        if (!initCompleted) {
            reportParamList.add(new ReportParamItem(trackerName, fields, data));
            return;
        }
        String postUrl = (String)fields.get("postUrl");
        if (fields.get("postUrl") == null) {
            // 发送被取消
            return;
        }
        Object parameter = fields.get("protocolParameter");
        // 转义和过滤
        Map<String, Object> headMap = runProtocolParameter(parameter, fields);
        Map<String, Object> lineMap = runProtocolParameter(parameter, data);

        String separator = "&";
        if (postUrl.indexOf("?") < 0) { // 处理url中存在“？”的情况
            separator = "?";
        }
        appendCache(trackerName, postUrl + separator + NLog.buildPost(headMap), NLog.buildPost(lineMap));
    }
    
    /**
     * 处理消息
     */
    private static Map<String, Message> messages = new HashMap<String, Message>();
    
    /**
     * 将数据放到缓存中
     * @param trackerName 追踪器名称
     * @param head 首行数据，公用数据
     * @param line 每行数据
     */
    private static void appendCache(String trackerName, String head, String line) {
        /* debug start */
        Log.d(LOGTAG, String.format("appendCache('%s', '%s', '%s')", trackerName, head, line));
        /* debug end */

        synchronized(cacheItems) {
            String itemname = String.format("%s.%s", trackerName, getMD5(head));
            CacheItem item = cacheItems.get(itemname);
            if (item == null) {
                item = new CacheItem(itemname, head);
                cacheItems.put(itemname, item); // 加入缓存
            }
            synchronized(item.sb) {
                item.sb.append(line + '\n');
            }
            sendMessage_saveFile(item);
        }
    }
    /** 
     * 判断Network是否连接成功(包括移动网络和wifi) 
     * @return 返回是否连接
     */
    public static boolean isNetworkConnected(){ 
        return checkWifiConnected() || checkNetConnected(); 
    }
    
    /**
     * 检查移动网络是否连接
     * @return 返回是否连接
     */
    public static boolean checkNetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager)NLog.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager
                .getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * 检查wifi是否连接
     * @return 返回是否连接
     */
    public static boolean checkWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager)NLog.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wiFiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wiFiNetworkInfo != null) {
            return wiFiNetworkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * 将文件锁定
     * @param itemname 项名
     * @return 返回锁定后的文件名
     */
    private static String buildLocked(String itemname) {
        String filename = String.format(cacheFileFormat, fileVersion, itemname);
        File file = new File(filename);
        if (!file.exists()) return null;
        String result = filename.replaceFirst("\\.dat$", "." + Long.toString(System.currentTimeMillis(), 36) + ".locked");
        File locked = new File(result);
        while (!file.renameTo(locked)) {
            result = filename.replaceFirst("\\.dat$", "." + Long.toString(System.currentTimeMillis(), 36) + ".locked");
            locked = new File(result);
        }
        return result;
    }

    @SuppressLint("DefaultLocale")
    private static Boolean loadRule() {
        String ruleUrl = (String)NLog.get("ruleUrl");
        Boolean result = false;
        if (ruleUrl == null) {
            return result;
        }
        
        /* TODO 网络状态 */
        HttpURLConnection conn = null;
        ConnectivityManager conManager = (ConnectivityManager) NLog.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        try {
            Proxy proxy = null;
            if (wifi != null && wifi.isAvailable()) {
                Log.d(LOGTAG, "WIFI is available");
            } else if (mobile != null && mobile.isAvailable()) {
                String apn = mobile.getExtraInfo().toLowerCase();
                Log.d(LOGTAG, "apn = " + apn);
                if (apn.startsWith("cmwap") || apn.startsWith("uniwap") || apn.startsWith("3gwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("10.0.0.172", 80));
                } else if (apn.startsWith("ctwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("10.0.0.200", 80));
                }
            } else { //@fixed in TV
                Log.d(LOGTAG, "getConnection:not wifi and mobile");
            }
            
            URL url;
            url = new URL(ruleUrl);
            if (proxy == null) {
                conn = (HttpURLConnection)url.openConnection();
            } else {
                conn = (HttpURLConnection)url.openConnection(proxy);
            }
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);
            
            // POST方式
            conn.setDoOutput(false);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(true);
            conn.connect();
            
            BufferedReader bufr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String inputString;
            while ((inputString = bufr.readLine()) != null) {
                sb.append(inputString);
            }
            bufr.close();
            conn.disconnect();
            
            int length = conn.getContentLength();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK && length != 0) {
                JSONObject json = new JSONObject(sb.toString());
                FileOutputStream fos = new FileOutputStream(ruleFilename);
                fos.write(sb.toString().getBytes());
                fos.close();
                NLog.updateRule(json);
                result = true;
                Log.d(LOGTAG, String.format("rule load success!\n===body===\n%s", sb));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            conn.disconnect();
            conn = null;
        }       
        return result;
    }
    /**
     * 发送文件
     * @param item 缓存项
     * @param lockedname 锁定文件名
     * @return 是否发送成功
     */
    @SuppressLint("DefaultLocale")
    private static Boolean postFile(PostItem item) {
        /* debug start */
        Log.d(LOGTAG, String.format("postFile('%s', '%s')", item.name, item.locked));
        /* debug end */
        
        Boolean result = false;
        if (NLog.safeBoolean(NLog.get("onlywifi"), false) && !checkWifiConnected()) {
            Log.d(LOGTAG, String.format("postFile() - Without a wifi connection. onlywifi = true"));
            return result;
        } else if (!isNetworkConnected()) {
            Log.d(LOGTAG, String.format("postFile() - Without a network connection."));
            return result;
        }
        

        String filename = String.format(cacheFileFormat, fileVersion, item.name);
        File file = new File(filename);
        if (!file.exists() || file.length() <= 0) {
            Log.d(LOGTAG, String.format("postFile() - file '%s' not found.", filename));
            return result;
        }
        
        byte[] pass = item.pass;
        int len;
        int size = 1024;
        byte[] buf = new byte[size];
        String postUrl = null;
        try {
            FileInputStream fis;
            fis = new FileInputStream(filename);
            len = fis.read(buf);
            for (int i = 0; i < len; i++) {
                buf[i] = (byte)(buf[i] ^ i % 256 ^ pass[i % pass.length]); 
                if (buf[i] == '\n') {
                    postUrl = new String(buf, 0, i);
                    break;
                }
            }
            fis.close();

            Log.d(LOGTAG, String.format("postFile() - postUrl = %s.", postUrl));
            if (postUrl == null) {
                return result;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* TODO 网络状态 */
        HttpURLConnection conn = null;
        ConnectivityManager conManager = (ConnectivityManager) NLog.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        try {
            Proxy proxy = null;
            if (wifi != null && wifi.isAvailable()) {
                Log.d(LOGTAG, "WIFI is available");
            } else if (mobile != null && mobile.isAvailable()) {
                String apn = mobile.getExtraInfo().toLowerCase();
                Log.d(LOGTAG, "apn = " + apn);
                if (apn.startsWith("cmwap") || apn.startsWith("uniwap") || apn.startsWith("3gwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("10.0.0.172", 80));
                } else if (apn.startsWith("ctwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("10.0.0.200", 80));
                }
            } else { //@fixed in TV
                Log.d(LOGTAG, "getConnection:not wifi and mobile");
            }
            URL url;

            url = new URL(postUrl);
            if (proxy == null) {
                conn = (HttpURLConnection)url.openConnection();
            } else {
                conn = (HttpURLConnection)url.openConnection(proxy);
            }
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);
            
            // POST方式
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "gzip");
            
            conn.connect();
            GZIPOutputStream gos = new GZIPOutputStream(conn.getOutputStream());
            String lockedname = item.locked;
            if (lockedname == null) { // 需要锁定文件
                lockedname = buildLocked(item.name);
            }
            File locked = new File(lockedname);
            
            FileInputStream fis = new FileInputStream(lockedname);
            int offset = 0;
            Boolean isHead = false;
            while ((len = fis.read(buf, 0, size)) != -1) {
                int t = 0;
                for (int i = 0; i < len; i++) {
                    buf[i] = (byte)(buf[i] ^ offset % 256 ^ pass[offset % pass.length]); 
                    offset++;
                    if (!isHead) {
                        if (buf[i] == '\n') {
                            t = i;
                            isHead = true;
                        }
                    }
                }
                gos.write(buf, t + 1, len - t - 1);
            }
            gos.close();
            gos = null;
            fis.close();
            fis = null;
            
            // 在输入流外包装一层BufferReader，提高读入效率 getInputStream返回的是字节流，将其转换成字符流
            //*
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
                result = true;
                locked.delete();
                Log.d(LOGTAG, "post success!");
                // 处理成功
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            conn.disconnect();
            conn = null;
        }
        return result;
    }
    /**
     * 将缓存项保存为文件，如果之前存在文件则追加写入
     * @param item
     * @return
     */
    public static Boolean saveFile(CacheItem item) {
        if (item == null) {
            return false;
        }
        String filename = String.format(cacheFileFormat, fileVersion, item.name);
        /* debug start */
        Log.d(LOGTAG, String.format("saveFile() filename : %s", filename));
        /* debug end */
        
        Integer sendMaxLength = NLog.safeInteger(NLog.get("sendMaxLength"), 200);
        Boolean result = false;
        synchronized(item) {
            try {
                File file = new File(filename);
                int offset = 0;
                byte[] linesBuffer;
                if (file.exists()) {
                    offset = (int)file.length();
                }
                if (offset >= sendMaxLength * 1024) { // 文件超过范围，建立新文件
                    buildLocked(item.name); // 将之前的文件锁定
                    offset = 0;
                }
                if (offset <= 0) { // 文件不存在 // 头必须写
                    linesBuffer = (item.head + '\n' + item.sb.toString()).toString().getBytes();
                } else {
                    linesBuffer = item.sb.toString().getBytes();
                }
                byte[] pass = item.pass;
                if (pass != null && pass.length > 0) { // 需要加密
                    for (int i = 0, len = linesBuffer.length; i < len; i++) {
                        int t = (int) (i + offset);
                        linesBuffer[i] = (byte)(linesBuffer[i] ^ t % 256 ^ pass[t % pass.length]); 
                    }
                }
                @SuppressWarnings("resource")
                FileOutputStream fos = new FileOutputStream(filename, true);
                fos.write(linesBuffer);
                fos.flush();
                item.sb.delete(0, item.sb.length()); // 清空数据
                result = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
        return result;
    }
        
    /**
     * 获取md5字符串
     * @param text 文本
     * @return 返回小写md5序列号
     */
    public static String getMD5(String text) {
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte b : md.digest()) {
                sb.append(Integer.toHexString(((int)b & 0xff) + 0x100).substring(1));
            }
            result = sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * 处理存储的句柄
     */
    private static StorageHandler storageHandler;
    
    /**
     * 初始化目录消息
     */
    private static final byte MESSAGE_INIT = 1;
    
    /**
     * 保存为文件的消息
     * @param obj item
     */
    private static final byte MESSAGE_SAVEFILE = 2;
    
    /**
     * 上报文件
     * @param obj item
     */
    private static final byte MESSAGE_POSTFILE = 3;
    
    /**
     * 处理存储的句柄
     */
    private static class StorageHandler extends Handler {
        StorageHandler(Looper looper) {
            super(looper);
        }
        
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_INIT:
                    /* debug start */
                    Log.d(LOGTAG, String.format("case MESSAGE_INIT"));
                    /* debug end */
                    File file = new File(rootDir + File.separatorChar);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    File ruleFile = new File(ruleFilename);
                    if (ruleFile.exists() && 
                            System.currentTimeMillis() - ruleFile.lastModified() >= (Integer)NLog.get("ruleExpires") * 24 * 60 * 60 * 1000) {
                        // 存在并且没过期
                        try {
                            FileInputStream fis = new FileInputStream(ruleFilename);
                            Integer len = fis.available();
                            byte[] buffer = new byte[len];
                            fis.read(buffer);
                            String ruleText = new String(buffer);
                            fis.close();
                            /* debug start */
                            Log.d(LOGTAG, String.format("read '%s'\n===body====\n%s", ruleFilename, ruleText));
                            /* debug end */
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        loadRule();
                    }
                    break;
                case MESSAGE_SAVEFILE: {
                    /* debug start */
                    Log.d(LOGTAG, String.format("case MESSAGE_SAVEFILE"));
                    /* debug end */
                    
                    if (msg.obj == null) {
                        break;
                    }
                    
                    CacheItem cacheItem = (CacheItem)msg.obj;
                    synchronized(messages) { // 清空消息
                        String msgName = String.format("%s.%s", cacheItem.name, MESSAGE_SAVEFILE);
                        messages.put(msgName, null);
                    }
                    saveFile(cacheItem); // 会清空 item.sb内容
                    sendMessage_postFile(new PostItem(cacheItem.name, null));
                    break;
                }
                case MESSAGE_POSTFILE: {
                    /* debug start */
                    Log.d(LOGTAG, String.format("case MESSAGE_POSTFILE"));
                    /* debug end */
                    
                    PostItem postItem = (PostItem)msg.obj;
                    synchronized(messages) { // 清空消息
                        String msgName = String.format("%s.%s.%s", postItem.name, MESSAGE_POSTFILE, postItem.locked != null);
                        messages.put(msgName, null);
                    }
                    postFile(postItem);
                    break;
                }
            }
        }

    }
    /**
     * 定时发送日志
     */
    private static Timer sendTimer = null;
    
    /**
     * 发送提交文件的消息
     * @param item 提交项，item{ name, locked }
     * @return 返回是否发送消息
     */
    private static Boolean sendMessage_postFile(PostItem item) {
        Boolean result = false;
        synchronized(messages) { // 消息正在发送的途中
            String msgName = String.format("%s.%s.%s", item.name, MESSAGE_POSTFILE, item.locked != null);
            Message m = messages.get(msgName);
            if (m == null) { // 是否有相同的消息在处理
                m = storageHandler.obtainMessage(MESSAGE_POSTFILE, 0, 0, item);
                storageHandler.sendMessageDelayed(m, 5000);
                messages.put(msgName, m);
                result = true;
                Log.d(LOGTAG, String.format("MESSAGE_POSTFILE '%s' message send", msgName));
            } else {
                Log.d(LOGTAG, String.format("MESSAGE_POSTFILE message sending..."));
            }
        }
        return result;
    }
    
    /**
     * 发送保存文件的消息
     * @param item 提交项，item{ name }
     * @return 返回是否发送消息
     */
    private static Boolean sendMessage_saveFile(CacheItem item) {
        Boolean result = false;
        synchronized(messages) { // 消息正在发送的途中
            String msgName = String.format("%s.%s", item.name, MESSAGE_SAVEFILE);
            Message m = messages.get(msgName);
            if (m == null) { // 是否有相同的消息在处理
                m = storageHandler.obtainMessage(MESSAGE_SAVEFILE, 0, 0, item);
                storageHandler.sendMessageDelayed(m, 1000);
                messages.put(msgName, m);
                result = true;
                Log.d(LOGTAG, String.format("MESSAGE_SAVEFILE '%s' message send", msgName));
            } else {
                Log.d(LOGTAG, String.format("MESSAGE_SAVEFILE message sending..."));
            }
        }
        return result;
    }

    /**
     * 是否已经初始化
     */
    private static Boolean initCompleted = false;
    public static Boolean getInitCompleted() {
        return initCompleted;
    }
    
    /**
     * 最近一次扫描目录的时间
     */
    private static Long lastScanTime = 0l;
    /**
     * 最近一次扫描是否存在未发送的文件
     */
    private static Boolean lastScanExists = false;
    /**
     * 初始化
     * @throws  
     */
    public static void init() {
        if (initCompleted) {
            Log.w(NLog.class.getName(), "init() Can't repeat initialization.");
            return;
        }
        initCompleted = true;
        
        TelephonyManager tm = (TelephonyManager)NLog.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        deviceId = tm.getDeviceId();
        
        HandlerThread handlerThread = new HandlerThread("NSTORAGE_HANDLER",
                Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        storageHandler = new StorageHandler(handlerThread.getLooper());    
        Message msg = storageHandler.obtainMessage(MESSAGE_INIT);
        storageHandler.sendMessageDelayed(msg, 100);
        
        for (ReportParamItem item : reportParamList) {
            report(item.trackerName, item.fields, item.data);
        }
        reportParamList.clear();
        reportParamList = null;
        
        sendTimer = new Timer();
        sendTimer.schedule(new TimerTask() {
            /**
             * 等待发送的文件
             */
            private Pattern dataFilePattern = Pattern.compile("\\b_nlog(?:_(\\d+))?_(\\w+\\.[a-f0-9]{32})(?:\\.([a-z0-9]+))?\\.(locked|dat)$");
            // '_nlog_1_wenku.01234567890123456789012345678901.h0123456.locked'
            // '_nlog_1_wenku.01234567890123456789012345678901.dat'
            
            @Override
            public void run() {
                Long now = System.currentTimeMillis();
                Integer sendInterval = NLog.safeInteger(NLog.get("sendInterval"), 120);
                Integer sendIntervalWifi = NLog.safeInteger(NLog.get("sendIntervalWifi"), 60);
                if (!lastScanExists) { // 上次没扫描到文件，增加延迟
                    sendInterval += (int)(sendInterval * 1.5);
                    sendIntervalWifi += (int)(sendIntervalWifi * 1.5);
                }
                Boolean onlywifi = NLog.safeBoolean(NLog.get("onlywifi"), false);
                if (now - lastScanTime < Math.min(sendInterval, sendIntervalWifi)) {
                    return;
                }
                
                // 无网络链接
                if (!isNetworkConnected()) {
                    return;
                } else if (checkWifiConnected()) {
                    if (now - lastScanTime < sendIntervalWifi) { // wifi
                        return;
                    }
                } else {
                    if (onlywifi) { // 只在wifi下
                        return;
                    }
                    if (now - lastScanTime < sendInterval) { //wifi
                        return;
                    }
                }
                lastScanTime = now;
                lastScanExists = false;
                Integer storageExpires = NLog.safeInteger(NLog.get("storageExpires"), 10);

                File file = new File(rootDir + File.separatorChar);
                for (File subFile : file.listFiles()) {
                    /* debug start */
                    Log.d(LOGTAG, String.format("file : %s(%sbyte).", subFile.getName(), subFile.length()));
                    /* debug end */
                    
                    Matcher matcher = dataFilePattern.matcher(subFile.getName());
                    if (!matcher.find()) { // 不符合nlog文件名
                        continue;
                    }
                    
                    // 数据过期时间
                    if (System.currentTimeMillis() - subFile.lastModified() >= storageExpires * 24 * 60 * 60 * 1000) {
                        /* debug start */
                        Log.d(LOGTAG, String.format("del file : %s(%sbyte).", subFile.getName(), subFile.length()));
                        /* debug end */
                        subFile.delete();
                        continue;
                    }
                    
                    String version = matcher.group(1);
                    String itemname = matcher.group(2); // 项名
                    String extname = matcher.group(4); // 扩展名
                    if (!fileVersion.equals(version)) { // 不兼容的版本
                        /* debug start */
                        Log.d(LOGTAG, String.format("del file : %s(%sbyte).", subFile.getName(), subFile.length()));
                        /* debug end */
                        subFile.delete();
                        continue;
                    }
                    // 开始发送文件
                    if (sendMessage_postFile(new PostItem(itemname, "locked".equals(extname) ? subFile.getName() : null))) { // 发送成功
                        lastScanExists = true;
                        return;
                    }
                }
            }
        }, 100, 30000);

    }
}
