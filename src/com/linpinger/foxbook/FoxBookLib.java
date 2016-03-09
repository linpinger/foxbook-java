package com.linpinger.foxbook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class FoxBookLib {

    public static List compare2GetNewPages(List<Map<String, Object>> xx, String existList) {
        existList = existList.toLowerCase();
        int xxSize = xx.size();
        if (existList.contains("起止=")) { // 根据 起止 过滤一下 xx
            Matcher mat = Pattern.compile("(?i)起止=([0-9-]+),([0-9-]+)").matcher(existList);
            int qz_1 = 0;
            int qz_2 = 0;
            while (mat.find()) {
                qz_1 = Integer.valueOf(mat.group(1));
                qz_2 = Integer.valueOf(mat.group(2));
            }
	
            ArrayList<Map<String, Object>> nXX = new ArrayList<Map<String, Object>>(30);
            // 下面的初始值及判断顺序最好不要随便变动
            int sIdx = 0;
            int eIdx = xxSize;
            int leftIdx = 0;
            if (qz_2 > 0) {
                sIdx = qz_2;
                leftIdx = eIdx - sIdx;
            }
            if (qz_1 < 0) {
                eIdx = eIdx + qz_1;
                leftIdx = leftIdx + qz_1;
            }
            if (leftIdx > 0) {
                int nSIdx = 0;
                for (int i = 0; i < leftIdx; i++) {
                    nSIdx = sIdx + i;
                    nXX.add(xx.get(nSIdx));
                }
                xx = nXX;
            } else { // 章节数量为负
                if (55 == xxSize) {
                    String jj[] = existList.split("\n");
                    if (jj.length > 2) { // 截取已删除记录中第一条之后的记录，如果新章节>55可能会悲剧
                        String sToBeComp = jj[jj.length - 2];
                        ArrayList<Map<String, Object>> nX2 = new ArrayList<Map<String, Object>>(30);
                        Iterator itr = xx.iterator();
                        String nowurl = "";
                        boolean bFillArray = false;
                        while (itr.hasNext()) {
                            HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                            nowurl = mm.get("url").toString().toLowerCase();
                            if (sToBeComp.contains(nowurl)) {
                                bFillArray = true;
                                nX2.add(mm);
                            } else {
                                if (bFillArray) {
                                    nX2.add(mm);
                                }
                            }
                        }
                        xx = nX2;
                    } else {
                        System.out.println("error: jj < 2 : " + jj.length);
                    }
                } else {  // 下面放的代码是没有新章节的处理方法
                    return new ArrayList<HashMap<String, Object>>();
                }
            }
        }
	
        // 比较得到新章节
        String nowURL;
        ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>();
        Iterator<Map<String, Object>> itr = xx.iterator();
        while (itr.hasNext()) {
            HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
            nowURL = (String) mm.get("url");
            if (!existList.contains(nowURL.toLowerCase() + "|")) { // 新章节
                newPages.add(mm);
            }
        }
        return newPages;
    }

    public static String simplifyDelList(String DelList) { // 精简 DelList
        int qi = 0;
        int zhi = 0;
        if (DelList.contains("起止=")) {
            Matcher mat = Pattern.compile("(?i)起止=([0-9\\-]+),([0-9\\-]+)").matcher(DelList);
            while (mat.find()) {
                qi = Integer.valueOf(mat.group(1));
                zhi = Integer.valueOf(mat.group(2));
            }
        }
        DelList = DelList.replace("\r", "").replace("\n\n", "\n");
        String[] xx = DelList.split("\n");
        if (xx.length < 15) {
            return DelList;
        }
        int MaxLineCount = xx.length - 9;

        StringBuffer newList = new StringBuffer(1024);
        for (int i = 0; i < 9; i++) {
            newList.append(xx[MaxLineCount + i]).append("\n");
        }
        if (zhi > 0) {
            return "起止=" + qi + "," + String.valueOf(zhi + MaxLineCount - 1) + "\n" + newList.toString();
        } else {
            return "起止=" + qi + "," + String.valueOf(zhi + MaxLineCount) + "\n" + newList.toString();
        }
    }


    public static List<Map<String, Object>> tocHref(String html, int lastNpage) {
        if (html.length() < 100) { //网页木有下载下来
            return new ArrayList<Map<String, Object>>(1);
        }
        List<Map<String, Object>> ldata = new ArrayList<Map<String, Object>>(100);
        Map<String, Object> item;
        int nowurllen = 0;
        HashMap<Integer, Integer> lencount = new HashMap<Integer, Integer>();

        // 有些变态网站没有body标签，而java没找到 body 时， 会遍历整个网页，速度很慢
        if (html.matches("(?smi).*<body.*")) {
            html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // 获取正文
        } else {
            html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // 获取正文
        }

        if (html.contains("http://read.qidian.com/BookReader/")) { // 处理起点目录
            html = html.replaceAll("(?smi).*<div id=\"content\">(.*)<div class=\"book_opt\">.*", "$1"); // 获取列表

            html = html.replace("\n", " ");
            html = html.replace("<a", "\n<a");
            html = html.replaceAll("(?i)<a href.*?/Book/.*?>.*?</a>", ""); // 分卷阅读
            html = html.replaceAll("(?i)<a href.*?/BookReader/vol.*?>.*?</a>", ""); // 分卷阅读
            html = html.replaceAll("(?i)<a.*?href.*?/vipreader.qidian.com/.*?>", ""); // vip
            html = html.replaceAll("(?smi)<span[^>]*>", ""); // 起点<a></a>之间有span标签
            html = html.replace("</span>", "");
        }

        // 获取链接 并存入结构中
        Matcher mat = Pattern.compile(
                "(?smi)href *= *[\"']?([^>\"']+)[^>]*> *([^<]+)<")
                .matcher(html);
        while (mat.find()) {
            if (2 == mat.groupCount()) {
                if (((String) mat.group(1)).contains("javascript:")) {
                    continue; // 过滤js链接
                }
                item = new HashMap<String, Object>();
                item.put("url", mat.group(1));
                item.put("name", mat.group(2));
                nowurllen = mat.group(1).length();
                item.put("len", nowurllen);
                ldata.add(item);

                if (null == lencount.get(nowurllen)) {
                    lencount.put(nowurllen, 1);
                } else {
                    lencount.put(nowurllen, 1 + lencount.get(nowurllen));
                }
            }
        }

        // 遍历hashmap lencount 获取最多的url长度
        int maxurllencount = 0;
        int maxurllen = 0;
        Iterator iter = lencount.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            if (maxurllencount < (Integer) val) {
                maxurllencount = (Integer) val;
                maxurllen = (Integer) key;
            }
        }
//        System.out.println("MaxURLLen:" + maxurllen);

        int minLen = maxurllen - 2; // 最小长度值，这个值可以调节
        int maxLen = maxurllen + 2; // 最大长度值，这个值可以调节

        int ldataSize = ldata.size();
        int halfLink = (int) (ldataSize / 2);

        int startDelRowNum = -9;      // 开始删除的行
        int endDelRowNum = 9 + ldataSize;  // 结束删除的行
        // 只找链接的一半，前半是找开始行，后半是找结束行
        // 找开始
        Integer nowLen = 0;
        Integer nextLen = 0;
        for (int nowIdx = 0; nowIdx < halfLink; nowIdx++) {
            nowLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx))).get("len"));
            if ((nowLen > maxLen) || (nowLen < minLen)) {
                startDelRowNum = nowIdx;
            } else {
                nextLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx + 1))).get("len"));
                if ((nextLen - nowLen > 1) || (nextLen - nowLen < 0)) {
                    startDelRowNum = nowIdx;
                }
            }
        }
        // 找结束 nextLen means PrevLen here
        for (int nowIdx = ldataSize - 1; nowIdx > halfLink; nowIdx--) {
            nowLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx))).get("len"));
            if ((nowLen > maxLen) || (nowLen < minLen)) {
                endDelRowNum = nowIdx;
            } else {
                nextLen = (Integer) (((HashMap<String, Object>) (ldata.get(nowIdx - 1))).get("len"));
                if ((nowLen - nextLen > 1) || (nowLen - nextLen < 0)) {
                    endDelRowNum = nowIdx;
                }
            }
        }
//        System.out.println("startDelRowNum:" + startDelRowNum + " - endDelRowNum:" + endDelRowNum + " ldataSize:" + ldataSize);

        // 倒着删元素
        if (endDelRowNum < ldataSize) {
            for (int nowIdx = ldataSize - 1; nowIdx >= endDelRowNum; nowIdx--) {
                ldata.remove(nowIdx);
            }
        }
        if (startDelRowNum >= 0) {
            for (int nowIdx = startDelRowNum; nowIdx >= 0; nowIdx--) {
                ldata.remove(nowIdx);
            }
        }

        return getLastNPage(ldata, lastNpage); // 截取后一部分
    }

    public static List<Map<String, Object>> getSearchEngineHref(String html, String KeyWord) { // String KeyWord = "三界血歌" ;
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
        Map<String, Object> item;

        html = html.replace("\t", "");
        html = html.replace("\r", "");
        html = html.replace("\n", "");
        html = html.replaceAll("(?i)<!--[^>]+-->", "");
        html = html.replace("<em>", "");
        html = html.replace("</em>", "");
        html = html.replace("<b>", "");
        html = html.replace("</b>", "");
        html = html.replace("<strong>", "");
        html = html.replace("</strong>", "");
	
        // 获取链接 并存入结构中
        Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[\"']?[^>]*> *([^<]+)<").matcher(html);
        while (mat.find()) {
            if (2 == mat.groupCount()) {
                if (mat.group(1).length() < 5) {
                    continue;
                }
                if (!mat.group(1).startsWith("http")) {
                    continue;
                }
                if (mat.group(1).contains("www.sogou.com/web")) {
                    continue;
                }
                if (!mat.group(2).contains(KeyWord)) {
                    continue;
                }

                item = new HashMap<String, Object>();
                item.put("url", mat.group(1));
                item.put("name", mat.group(2));
                data.add(item);
            }
        }

        return data;
    }

    // 取倒数几个元素，被上面这个调用
    private static List<Map<String, Object>> getLastNPage(List<Map<String, Object>> inArrayList, int lastNpage) {
        int aSize = inArrayList.size();
        if (aSize <= lastNpage || lastNpage <= 0) {
            return inArrayList;
        }
        List<Map<String, Object>> outList = new ArrayList<Map<String, Object>>(100);
        for (int nowIdx = aSize - lastNpage; nowIdx < aSize; nowIdx++) {
            outList.add((HashMap<String, Object>) (inArrayList.get(nowIdx)));
        }
        return outList;
    }

    // 将page页的html转换为文本，通用规则
    public static String pagetext(String html) {
        // 规律 novel 应该是由<div>包裹着的最长的行
        // 有些变态网站没有body标签，而java没找到<body时，replaceAll会遍历整个html，速度很慢
        if (html.matches("(?smi).*<body.*")) {
            html = html.replaceAll("(?smi).*<body[^>]*>(.*)</body>.*", "$1"); // 获取正文
        } else {
            html = html.replaceAll("(?smi).*?</head>(.*)", "$1"); // 获取正文
        }

        // MinTag
        html = html.replaceAll("(?smi)<script[^>]*>.*?</script>", ""); // 脚本
        html = html.replaceAll("(?smi)<!--[^>]+-->", ""); // 注释 少见
        html = html.replaceAll("(?smi)<iframe[^>]*>.*?</iframe>", ""); // 框架
        // 相当少见
        html = html.replaceAll("(?smi)<h[1-9]?[^>]*>.*?</h[1-9]?>", ""); // 标题
        // 相当少见
        html = html.replaceAll("(?smi)<meta[^>]*>", ""); // 标题 相当少见

        // 2选1,正文链接有文字，目前没见到这么变态的，所以删吧
        html = html.replaceAll("(?smi)<a [^>]+>.*?</a>", ""); // 删除链接及中间内容
        // html = html.replaceAll("(?smi)<a[^>]*>", "<a>"); // 替换链接为<a>

        // 将html代码缩短,便于区分正文与广告内容，可以按需添加
        html = html.replaceAll("(?smi)<div[^>]*>", "<div>");
        html = html.replaceAll("(?smi)<font[^>]*>", "<font>");
        html = html.replaceAll("(?smi)<table[^>]*>", "<table>");
        html = html.replaceAll("(?smi)<td[^>]*>", "<td>");
        html = html.replaceAll("(?smi)<ul[^>]*>", "<ul>");
        html = html.replaceAll("(?smi)<dl[^>]*>", "<dl>");
        html = html.replaceAll("(?smi)<span[^>]*>", "<span>");

        html = html.toLowerCase();
        html = html.replace("\r", "");
        html = html.replace("\n", "");
        html = html.replace("\t", "");
        html = html.replace("</div>", "</div>\n");
        html = html.replace("<div></div>", "");
        html = html.replace("<li></li>", "");
        html = html.replace("  ", "");

        // getMaxLine -> ll[nMaxLine]
        String[] ll = html.split("\n");
        int nMaxLine = 0;
        int nMaxCount = 0;
        int tmpCount = 0;
        for (int i = 0; i < ll.length; i++) {
            tmpCount = ll[i].length();
            if (tmpCount > nMaxCount) {
                nMaxLine = i;
                nMaxCount = tmpCount;
            }
        }
        html = ll[nMaxLine];

        // html2txt
        html = html.replace("\t", "");
        html = html.replace("\r", "");
        html = html.replace("\n", "");
        html = html.replace("&nbsp;", "");
        html = html.replace("　　", "");
        html = html.replace("<br>", "\n");
        html = html.replace("</br>", "\n");
        html = html.replace("<br/>", "\n");
        html = html.replace("<br />", "\n");
        html = html.replace("<p>", "\n");
        html = html.replace("</p>", "\n");
        html = html.replace("<div>", "\n");
        html = html.replace("</div>", "\n");
        html = html.replace("\n\n", "\n");

        // 处理正文中的<img标签，可以将代码放在这里，典型例子:无错

        // 特殊网站处理可以放在这里
        // 144书院的这个会导致下面正则将正文也删掉了，已使用正则修复
        html = html.replaceAll("(?smi)<span[^>]*>.*?</span>", ""); // 删除<span>里面是混淆字符， 针对 纵横中文混淆字符，以及大家读结尾标签，一般都没有span标签
        html = html.replaceAll("(?smi)<[^<>]+>", ""); // 这是最后一步，调试时可先注释: 删除 html标签,改进型，防止正文有不成对的<
        html = html.replaceAll("(?smi)^\n*", "");

        return html;
    }

    public static String getFullURL(String sbaseurl, String suburl) { // 获取完整路径
        String allURL = "";
        if (sbaseurl.contains("zhuishushenqi.com")) {     // 对于这种非正常合成URL需处理一下 http://xxx.com/fskd/http://wwe.comvs.fs
            if (suburl.contains("zhuishushenqi.com")) {
                return suburl;
            } else {
                return site_zssq.getUrlPage(suburl);
            }
        }
        try {
            allURL = (new URL(new URL(sbaseurl), suburl)).toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return allURL;
    }

    //获取网络文件，转存到outPath中，outPath需要带文件后缀名
    public static void saveHTTPFile(String inURL, String outPath) {
        File toFile = new File(outPath);
        if (toFile.exists()) {
            toFile.delete();
        }
        try {
//			toFile.createNewFile();
            FileOutputStream outImgStream = new FileOutputStream(toFile);
            outImgStream.write(downHTTP(inURL, "GET"));
            outImgStream.close();
        } catch (Exception e) {
            e.toString();
        }
    }

    public static String downhtml(String inURL) {
        return downhtml(inURL, "");
    }

    public static String downhtml(String inURL, String pageCharSet) {
        return downhtml(inURL, pageCharSet, "GET");
    }

    public static String downhtml(String inURL, String pageCharSet, String PostData) {
        byte[] buf = downHTTP(inURL, PostData);
        if (buf == null) {
            return "";
        }
        try {
            String html = "";
            if (pageCharSet == "") {
                html = new String(buf, "gbk");
                if (html.matches("(?smi).*<meta[^>]*charset=[\"]?(utf8|utf-8)[\"]?.*")) { // 探测编码
                    html = new String(buf, "utf-8");
                }
            } else {
                html = new String(buf, pageCharSet);
            }
            return html;
        } catch (Exception e) { // 错误 是神马
            return "";
        }
    }

    public static byte[] downHTTP(String inURL, String PostData) {
        byte[] buf = null;
        try {
            URL url = new URL(inURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if ("GET" != PostData) {
                System.out.println("I am Posting ...");
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
//				conn.setInstanceFollowRedirects(true);
            }

            if (inURL.contains(".13xs.")) {
                conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/3.26"); // 2015-10-27: qqxs使用加速宝，带Java的头会被和谐
            } else {
                conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/3.26 Java/1.6.0_55"); // Android自带头部和IE8头部会导致yahoo搜索结果链接为追踪链接
            }
            conn.setRequestProperty("Accept", "*/*");
            if (!inURL.contains("files.qidian.com")) { // 2015-4-16: qidian txt 使用cdn加速，如果头里有gzip就会返回错误的gz数据
                conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            } else {
                conn.setRequestProperty("Accept-Encoding", "*"); // Android 会自动加上gzip，真坑，使用*覆盖之，起点CDN就能正确处理了
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);    // 读取超时15s
            conn.connect();

            if ("GET" != PostData) {
                conn.getOutputStream().write(PostData.getBytes("UTF-8"));
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
            }

            // 判断返回的是否是gzip数据
            boolean bGZDate = false;
            Map<String, List<String>> rh = conn.getHeaderFields();
            List<String> ce = rh.get("Content-Encoding");
            if (null == ce) { // 不是gzip数据
                bGZDate = false;
            } else {
                bGZDate = true;
            }

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len = 0;

            if (bGZDate) { // Gzip
                InputStream in = conn.getInputStream();
                GZIPInputStream gzin = new GZIPInputStream(in);
                while ((len = gzin.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                gzin.close();
                in.close();
            } else {
                InputStream in = conn.getInputStream();
                while ((len = in.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                in.close();
            }

            buf = outStream.toByteArray();
            outStream.close();
        } catch (Exception e) { // 错误 是神马
            e.toString();
        }
        return buf;
    }

    // { 通用文本读取，写入
    // 优先使用这个读取文本，快点，变量大小可以调整一下以达到最好的速度
    public static String readText(String filePath, String inFileEnCoding) {
        // 为了线程安全，可以替换StringBuilder 为 StringBuffer
        StringBuilder retStr = new StringBuilder(174080);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), inFileEnCoding));

            char[] chars = new char[4096]; // 这个大小不影响读取速度
            int length = 0;
            while ((length = br.read(chars)) > 0) {
                retStr.append(chars, 0, length);
            }
            /*
             // 下面这个效率稍低，但可以控制换行符
             String line = null;
             while ((line = br.readLine()) != null) {
             retStr.append(line).append("\n");
             }
             */
            br.close();
        } catch (IOException e) {
            e.toString();
        }
        return retStr.toString();
    }

    // 写入指定编码，速度快点
    public static void writeText(String iStr, String filePath, String oFileEncoding) {
        boolean bAppend = false;
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, bAppend), oFileEncoding));
            bw.write(iStr);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.toString();
        }
    }
	
	/*
	// 这个用在定长切割大文本上，已经在importQidianTxt中使用
    public static String readTextAndSplit(String filePath, String inFileEnCoding) {
        StringBuilder retStr = new StringBuilder(174080);
        StringBuilder chunkStr = new StringBuilder(65536);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), inFileEnCoding));
            String line = null;
            int chunkLen = 0;
            while ((line = br.readLine()) != null) {
                chunkLen = chunkStr.length();
                if ( chunkLen > 3000 && ( line.length() == 0 || chunkLen > 6000 || line.startsWith("第") || line.contains("卷") || line.contains("章") || line.contains("节") ) ) {
                    retStr.append(chunkStr).append("\n##################################################\n\n");
                    chunkStr = new StringBuilder(65536);
                }
                chunkStr.append(line).append("\n");
            }
            if ( chunkStr.length() > 0 )
                retStr.append(chunkStr).append("\n#####LAST###########\n\n");
            br.close();
        } catch (IOException e) {
            e.toString();
        }
        return retStr.toString();
    }
	 */	
	
    // } 通用文本读取，写入
    
} // 类结束
