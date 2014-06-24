package com.baidu.nlog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import java.net.Proxy;

import com.baidu.yuedu.util.LogUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
     * nlog
     * @description Native统计框架，负责存储和网络通信
     * @author 王集鹄(WangJihu,http://weibo.com/zswang),彭正山(PengZhengshan)
     * @see https://github.com/uxrp/nlog/wiki/design
     * @version 1.0
     * @copyright www.baidu.com
     */
    /**
     *  日志TAG
     */
    private static final String LOG_TAG = "NStorage";

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
    private static String rootDir = null; // init中初始化
    /**
     * 规则文件名
     */
    private static String ruleFilename = null; // init中初始化
    /**
     * 版本文件名
     */
    private static String versionFilename = null; // init中初始化

    /**
     * 缓存文件名模板 _nlog_[version]_[itemname].dat, itemname => [name].[md5(head)]
     */
    private static String cacheFileFormat = null; // ini中初始化

    /**
     * 缓存项
     */
    private static class CacheItem {
        public StringBuffer sb;
        public String name;
        public String head;
        public byte[] pass;
        public Boolean passiveSend = false;

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
        if (result != null)
            return result;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(String.format("%s,%s,%s", deviceId, name, secretKey)
                    .getBytes());
            baos.write(md.digest());
            md.update(String.format("%s,%s,%s", name, deviceId, secretKey)
                    .getBytes());
            baos.write(md.digest());
            md.update(String.format("%s,%s,%s", deviceId, secretKey, name)
                    .getBytes());
            baos.write(md.digest());
            result = baos.toByteArray();
            baos.close();
            passMap.put(name, result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
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
    private static Map<String, Object> runProtocolParameter(
            Object protocolParameter, Map<String, Object> map) {
        if (protocolParameter == null || (!(protocolParameter instanceof Map))) {
            return map;
        }
        Map<String, Object> parameter = (HashMap<String, Object>) protocolParameter;
        Map<String, Object> result = new HashMap<String, Object>();
        for (String key : map.keySet()) {
            if (parameter.containsKey(key)) { // 需要转换
                Object newKey = parameter.get(key);
                if (newKey != null) { // 为null时抛弃
                    result.put((String) newKey, map.get(key));
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

        ReportParamItem(String trackerName, Map<String, Object> fields,
                Map<String, Object> data) {
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
    public static void report(String trackerName, Map<String, Object> fields,
            Map<String, Object> data) {

        if (!initCompleted) {

            reportParamList.add(new ReportParamItem(trackerName, fields, data));
            return;
        }
        String postUrl = (String) fields.get("postUrl");
        if (fields.get("postUrl") == null) {
            // 发送被取消

            return;
        }
        Boolean syncSave = NLog.safeBoolean(data.get("syncSave"), false);
        Boolean passiveSend = NLog.safeBoolean(data.get("passiveSend"), false);
        // 同步
        Object parameter = fields.get("protocolParameter");
        // 转义和过滤
        Map<String, Object> headMap = runProtocolParameter(parameter, fields);
        Map<String, Object> lineMap = runProtocolParameter(parameter, data);

        String separator = "&";
        if (postUrl.indexOf("?") < 0) { // 处理url中存在“？”的情况
            separator = "?";
        }
        appendCache(trackerName, postUrl + separator + NLog.buildPost(headMap),
                NLog.buildPost(lineMap), syncSave, passiveSend);
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
     * @param syncSave 同步保存，等主进程关闭是调用
     * @param passiveSend 是否被动发送
     */
    private static void appendCache(String trackerName, String head,
            String line, Boolean syncSave, Boolean passiveSend) {
        synchronized (cacheItems) {
            String itemname = String.format("%s.%s", trackerName, getMD5(head));
            CacheItem item = cacheItems.get(itemname);
            if (item == null) {
                item = new CacheItem(itemname, head);
                cacheItems.put(itemname, item); // 加入缓存
            }
            if (passiveSend) { // 被动发送
                if (item.sb.length() <= 0) { // 空内容的情况，采用被动发送
                    item.passiveSend = passiveSend;
                }
            } else { // 主动发送
                item.passiveSend = passiveSend;
            }
            synchronized (item.sb) {
                item.sb.append(line + '\n');
            }
            if (syncSave) {
                saveFile(item);
                if (!passiveSend) { // 不主动发送
                    sendMessage_postFile(new PostItem(item.name, null));
                }
            } else {
                sendMessage_saveFile(item);
            }
        }
    }

    /** 
     * 判断Network是否连接成功(包括移动网络和wifi) 
     * @return 返回是否连接
     */
    public static boolean isNetworkConnected() {
        return checkWifiConnected() || checkNetConnected();
    }

    /**
     * 检查移动网络是否连接
     * @return 返回是否连接
     */
    public static boolean checkNetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) NLog
                .getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
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
        ConnectivityManager connectivityManager = (ConnectivityManager) NLog
                .getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
        if (!file.exists())
            return null;
        String result = filename
                .replaceFirst("\\.dat$",
                        "." + Long.toString(System.currentTimeMillis(), 36)
                                + ".locked");
        File locked = new File(result);
        while (!file.renameTo(locked)) {
            result = filename.replaceFirst("\\.dat$",
                    "." + Long.toString(System.currentTimeMillis(), 36)
                            + ".locked");
            locked = new File(result);
        }
        return result;
    }

    @SuppressLint("DefaultLocale")
    private static Boolean loadRule() {
        Boolean result = false;

        if (!isNetworkConnected()) {

            return result;
        }

        String ruleUrl = (String) NLog.get("ruleUrl");
        if (ruleUrl == null) {
            return result;
        }

        HttpURLConnection conn = null;
        ConnectivityManager conManager = (ConnectivityManager) NLog
                .getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = conManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = conManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        try {
            Proxy proxy = null;
            if (wifi != null && wifi.isAvailable()) {

            } else if (mobile != null && mobile.isAvailable()) {
                String apn = mobile.getExtraInfo().toLowerCase();

                if (apn.startsWith("cmwap") || apn.startsWith("uniwap")
                        || apn.startsWith("3gwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP,
                            new InetSocketAddress("10.0.0.172", 80));
                } else if (apn.startsWith("ctwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP,
                            new InetSocketAddress("10.0.0.200", 80));
                }
            } else { //@fixed in TV

            }

            URL url;
            url = new URL(ruleUrl);
            if (proxy == null) {
                conn = (HttpURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection(proxy);
            }
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);

            // POST方式
            conn.setDoOutput(false);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(true);
            conn.connect();

            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Integer len;
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            is.close();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                buffer = baos.toByteArray();
                FileOutputStream fos = new FileOutputStream(ruleFilename);
                fos.write(buffer);
                fos.close();
                NLog.updateRule(new String(buffer));
                result = true;
            }
            conn.disconnect();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            conn.disconnect();
            conn = null;
        } catch (Exception e) {
            e.printStackTrace();
        } catch (NoClassDefFoundError e) { //避免某些手机connect 方法出现java.lang.NoClassDefFoundError: libcore/io/GaiException
            LogUtil.e(LOG_TAG, e.getMessage(), e);
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

        Boolean result = false;
        if (NLog.safeBoolean(NLog.get("onlywifi"), false)
                && !checkWifiConnected()) {

            return result;
        } else if (!isNetworkConnected()) {

            return result;
        }

        String filename = item.locked == null ? String.format(cacheFileFormat,
                fileVersion, item.name) : item.locked;
        File file = new File(filename);
        if (!file.exists() || file.length() <= 0) {
            Log.w(LOG_TAG, String.format("postFile() - file '%s' not found.",
                    filename));
            return result;
        }

        byte[] pass = item.pass;
        int len;
        int size = 1024;
        int preLength = 0;
        byte[] buf = new byte[size];
        String postUrl = null;
        byte[] gzipBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            FileInputStream fis;
            fis = new FileInputStream(filename);
            Integer offset = 0;
            Boolean existsHead = false;
            Boolean processHead = false;
            while ((len = fis.read(buf, 0, size)) != -1) {
                int t = 0;
                for (int i = 0; i < len; i++) {
                    buf[i] = (byte) (buf[i] ^ offset % 256 ^ pass[offset
                            % pass.length]); // 解密
                    offset++;
                    if (!existsHead) {
                        if (buf[i] == '\n') {
                            t = i;
                            postUrl = new String(buf, 0, i);
                            existsHead = true;
                            processHead = true;
                        }
                    }
                }
                if (processHead) { // 需要处理头信息，即：第一行为postUrl和参数
                    preLength += len - t - 1;
                    gos.write(buf, t + 1, len - t - 1);
                    processHead = false;
                } else {
                    preLength += len;
                    gos.write(buf, 0, len);
                }
            }
            fis.close();
            fis = null;
            gos.flush();
            gos.finish();
            gos.close();
            gos = null;

            if (postUrl == null || preLength <= 0) {
                return result;
            }
            gzipBytes = baos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpURLConnection conn = null;
        ConnectivityManager conManager = (ConnectivityManager) NLog
                .getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = conManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = conManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        try {
            Proxy proxy = null;
            if (wifi != null && wifi.isAvailable()) {

            } else if (mobile != null && mobile.isAvailable()
                    && mobile.getExtraInfo() != null) {
                String apn = mobile.getExtraInfo().toLowerCase();

                if (apn.startsWith("cmwap") || apn.startsWith("uniwap")
                        || apn.startsWith("3gwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP,
                            new InetSocketAddress("10.0.0.172", 80));
                } else if (apn.startsWith("ctwap")) {
                    proxy = new Proxy(java.net.Proxy.Type.HTTP,
                            new InetSocketAddress("10.0.0.200", 80));
                }
            } else { //@fixed in TV

            }
            URL url;

            url = new URL(postUrl);
            if (proxy == null) {
                conn = (HttpURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection(proxy);
            }
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);

            conn.setDoOutput(true); // POST方式
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);











            conn.setRequestProperty("Content-Encoding", "gzip");
            conn.connect();

            String lockedname = item.locked;
            if (lockedname == null) { // 需要锁定文件
                lockedname = buildLocked(item.name);

            }
            File locked = new File(lockedname);
            if (!locked.exists()) {

                return result;
            }

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.write(gzipBytes);
            dos.flush();
            dos.close();

            /*
            // 读取返回
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            baos = null;
            is.close();
            */

            Integer rc = conn.getResponseCode();

            // 在输入流外包装一层BufferReader，提高读入效率 getInputStream返回的是字节流，将其转换成字符流
            if (rc == HttpURLConnection.HTTP_OK) {
                // 处理成功
                result = true;
                locked.delete();
            }
            conn.disconnect();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            conn = null;
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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
        String filename = String
                .format(cacheFileFormat, fileVersion, item.name);

        Boolean result = false;
        synchronized (item) {
            if (item.sb.length() <= 0) { // 无内容可写
                return result;
            }
            Integer sendMaxLength = NLog.getInteger("sendMaxLength");
            try {
                File file = new File(filename);
                int offset = 0;
                byte[] linesBuffer;
                if (file.exists()) {
                    offset = (int) file.length();
                }
                if (offset >= sendMaxLength * 1024) { // 文件超过范围，建立新文件
                    buildLocked(item.name); // 将之前的文件锁定
                    sendMessage_scanDir(true);
                    offset = 0;
                }
                if (offset <= 0) { // 文件不存在 // 头必须写
                    linesBuffer = (item.head + '\n' + item.sb.toString())
                            .toString().getBytes();
                } else {
                    linesBuffer = item.sb.toString().getBytes();
                }
                byte[] pass = item.pass;
                if (pass != null && pass.length > 0) { // 需要加密
                    for (int i = 0, len = linesBuffer.length; i < len; i++) {
                        int t = (int) (i + offset);
                        linesBuffer[i] = (byte) (linesBuffer[i] ^ t % 256 ^ pass[t
                                % pass.length]);
                    }
                }
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(filename, true);
					fos.write(linesBuffer);
					fos.flush();
					item.sb.delete(0, item.sb.length()); // 清空数据
					result = true;
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
            } catch (Exception e) {
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
                sb.append(Integer.toHexString(((int) b & 0xff) + 0x100)
                        .substring(1));
            }
            result = sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
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
     * 扫描目录
     */
    private static final byte MESSAGE_SCANDIR = 4;

    /**
     * 扫描目录
     * @param onlyLocked 只扫描锁定的文件
     */
    private static Boolean scanDir(Boolean onlyLocked) {
        Boolean result = false;
        lastScanTime = System.currentTimeMillis();
        lastScanExists = false;
        try {
            File file = new File(rootDir + File.separatorChar);
            File[] files = file.listFiles();
            if (files == null) {
                return result;
            }
            for (File subFile : files) {

                Matcher matcher = dataFilePattern.matcher(subFile.getName());
                if (!matcher.find()) { // 不符合nlog文件名
                    continue;
                }

                // 数据过期时间
                Integer storageExpires = NLog.getInteger("storageExpires");
                if (System.currentTimeMillis() - subFile.lastModified() >= storageExpires
                        * 24 * 60 * 60 * 1000) {

                    subFile.delete();
                    continue;
                }

                String version = matcher.group(1);
                String itemname = matcher.group(2); // 项名
                String extname = matcher.group(4); // 扩展名
                if (!fileVersion.equals(version)) { // 不兼容的版本

                    subFile.delete();
                    continue;
                }
                if (onlyLocked && !"locked".equals(extname)) {
                    continue;
                }
                // 开始发送文件
                if (sendMessage_postFile(new PostItem(itemname,
                        "locked".equals(extname) ? subFile.getAbsolutePath()
                                : null))) { // 发送成功
                    lastScanExists = true;
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {

        }
        return result;
    }

    /**
     * 处理存储的句柄
     */
    private static class StorageHandler extends Handler {
        StorageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SCANDIR:
                synchronized (messages) { // 清空消息
                    String msgName = String.format("%s", MESSAGE_SCANDIR);
                    messages.put(msgName, null);
                }
                scanDir((Boolean) msg.obj);
                break;
            case MESSAGE_INIT:

                try {
                    File file = new File(rootDir + File.separatorChar);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    File ruleFile = new File(ruleFilename);
                    if (ruleFile.exists()
                            && System.currentTimeMillis()
                                    - ruleFile.lastModified() >= NLog
                                    .getInteger("ruleExpires")
                                    * 24
                                    * 60
                                    * 60
                                    * 1000) {
                        // 存在并且没过期
                        FileInputStream fis = new FileInputStream(ruleFilename);
                        Integer len = fis.available();
                        byte[] buffer = new byte[len];
                        fis.read(buffer);
                        String ruleText = new String(buffer);
                        fis.close();
                        NLog.updateRule(ruleText);
                    } else {
                        loadRule();
                    }
                    String applicationVersion = NLog.getString(
                            "applicationVersion", "");
                    if (!"".equals(applicationVersion)) {
                        String versionText = "";
                        File versionFile = new File(versionFilename);
                        if (versionFile.exists()) {
                            FileInputStream fis = new FileInputStream(
                                    versionFilename);
                            Integer len = fis.available();
                            byte[] buffer = new byte[len];
                            fis.read(buffer);
                            versionText = new String(buffer);
                            fis.close();
                        }
                        if (!applicationVersion.equals(versionText)) {
                            FileOutputStream fos = new FileOutputStream(
                                    versionFilename);
                            fos.write(applicationVersion.getBytes());
                            fos.close();

                            NLog.updateVersion(applicationVersion, versionText);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case MESSAGE_SAVEFILE: {

                if (msg.obj == null) {
                    break;
                }

                try {
                    CacheItem cacheItem = (CacheItem) msg.obj;
                    synchronized (messages) { // 清空消息
                        String msgName = String.format("%s.%s", cacheItem.name,
                                MESSAGE_SAVEFILE);
                        messages.put(msgName, null);
                    }
                    saveFile(cacheItem); // 会清空 item.sb内容
                    if (!cacheItem.passiveSend) { // 非主动发送时处理
                        sendMessage_postFile(new PostItem(cacheItem.name, null));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case MESSAGE_POSTFILE: {
                if (msg.obj == null) {
                    break;
                }
                try {
                    PostItem postItem = (PostItem) msg.obj;

                    synchronized (messages) { // 清空消息
                        String msgName = String.format("%s.%s.%s",
                                postItem.name, MESSAGE_POSTFILE,
                                postItem.locked != null);
                        messages.put(msgName, null);
                    }
                    postFile(postItem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
     * 等待发送的文件
     */
    private static Pattern dataFilePattern = Pattern
            .compile("\\b_nlog(?:_(\\d+))?_(\\w+\\.[a-f0-9]{32})(?:\\.([a-z0-9]+))?\\.(locked|dat)$");

    // '_nlog_1_wenku.01234567890123456789012345678901.h0123456.locked'
    // '_nlog_1_wenku.01234567890123456789012345678901.dat'

    /**
     * 发送扫描目录的消息
     * @param onlyLocked 是否只扫描锁定的文件
     * @return 返回是否发送消息
     */
    private static Boolean sendMessage_scanDir(boolean onlyLocked) {
        Boolean result = false;
        synchronized (messages) { // 消息正在发送的途中
            String msgName = String.format("%s", MESSAGE_SCANDIR);
            Message m = messages.get(msgName);
            if (m == null) { // 是否有相同的消息在处理
                try {
                    m = storageHandler.obtainMessage(MESSAGE_SCANDIR, 0, 0,
                            onlyLocked);
                    storageHandler.sendMessageDelayed(m, 5000);
                    messages.put(msgName, m);
                } catch (Exception ex) {
                }
                result = true;

            } else {

            }
        }
        return result;
    }

    /**
     * 发送提交文件的消息
     * @param item 提交项，item{ name, locked }
     * @return 返回是否发送消息
     */
    private static Boolean sendMessage_postFile(PostItem item) {
        Boolean result = false;
        synchronized (messages) { // 消息正在发送的途中
            String msgName = String.format("%s.%s.%s", item.name,
                    MESSAGE_POSTFILE, item.locked != null);
            Message m = messages.get(msgName);
            if (m == null) { // 是否有相同的消息在处理
                try {
                    m = storageHandler.obtainMessage(MESSAGE_POSTFILE, 0, 0,
                            item);
                    storageHandler.sendMessageDelayed(m, 3000);
                    messages.put(msgName, m);
                } catch (Exception ex) {
                }
                result = true;
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
        synchronized (messages) { // 消息正在发送的途中
            String msgName = String
                    .format("%s.%s", item.name, MESSAGE_SAVEFILE);
            Message m = messages.get(msgName);
            if (m == null) { // 是否有相同的消息在处理
                try {
                    m = storageHandler.obtainMessage(MESSAGE_SAVEFILE, 0, 0,
                            item);
                    storageHandler.sendMessage(m);
                    messages.put(msgName, m);
                } catch (Exception ex) {
                }
                result = true;
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
        rootDir = NLog.getContext().getFilesDir() + File.separator
                + "_nlog_cache";

        ruleFilename = rootDir + File.separator + "rules.dat";
        versionFilename = rootDir + File.separator + "version.dat";
        cacheFileFormat = rootDir + File.separator + "_nlog_%s_%s.dat";

        initCompleted = true;

        TelephonyManager tm = (TelephonyManager) NLog.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        deviceId = tm.getDeviceId();

        HandlerThread handlerThread = new HandlerThread("NSTORAGE_HANDLER",
                Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        storageHandler = new StorageHandler(handlerThread.getLooper());
        try {
            Message msg = storageHandler.obtainMessage(MESSAGE_INIT);
            storageHandler.sendMessage(msg);
        } catch (Exception ex) {
        }

        for (ReportParamItem item : reportParamList) {
            report(item.trackerName, item.fields, item.data);
        }
        reportParamList.clear();
        reportParamList = null;

        // 是否自动发送
        Boolean autoSend = NLog.getBoolean("autoSend");
        if (autoSend) {
            sendTimer = new Timer();
            sendTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Long now = System.currentTimeMillis();
                    Integer sendInterval = NLog.getInteger("sendInterval");
                    Integer sendIntervalWifi = NLog
                            .getInteger("sendIntervalWifi");
                    if (!lastScanExists) { // 上次没扫描到文件，增加延迟
                        sendInterval += (int) (sendInterval * 1.5);
                        sendIntervalWifi += (int) (sendIntervalWifi * 1.5);
                    }
                    Boolean onlywifi = NLog.safeBoolean(NLog.get("onlywifi"),
                            false);
                    if (now - lastScanTime < Math.min(sendInterval,
                            sendIntervalWifi) * 1000) {
                        return;
                    }

                    // 无网络链接
                    if (!isNetworkConnected()) {
                        return;
                    } else if (checkWifiConnected()) {
                        if (now - lastScanTime < sendIntervalWifi * 1000) { // wifi
                            return;
                        }
                    } else {
                        if (onlywifi) { // 只在wifi下
                            return;
                        }
                        if (now - lastScanTime < sendInterval * 1000) { //wifi
                            return;
                        }
                    }
                    sendMessage_scanDir(false);
                }
            }, 60000, 60000);
        }
    }
}
