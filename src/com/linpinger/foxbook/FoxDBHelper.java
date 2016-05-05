/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author guanli
 */
public class FoxDBHelper {
    private static final int SITE_QIDIAN_MOBILE = 16;
    // private static final int SITE_EASOU = 11 ;
    private static final int SITE_ZSSQ = 12;
    private static final int SITE_KUAIDU = 13;

    public static void importQidianTxt(String txtPath, FoxDB oDB) {
        File ttt = new File(txtPath);
        
        String sQidianid = ttt.getName().replace(".txt", ""); // 文件名
        String sQidianURL = site_qidian.qidian_getIndexURL_Desk(Integer.valueOf(sQidianid)); // URL
        String sBookName = sQidianid;
        try {  // 第一行书名
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath), "GBK"));
            sBookName =  br.readLine() ;
            br.close();
        } catch (Exception e) {
            e.toString();
        }
        
        oDB.execPreOne("insert into book (Name, QidianID, URL) values(?, \"" + sQidianid + "\", \"" + sQidianURL +  "\")", sBookName); // 新增书籍
        String sBookid = oDB.getOneCell("select id from book where qidianid=" + sQidianid); // 获取id
        
        String txtContent = site_qidian.qidian_getTextFromPageJS(FoxBookLib.readText(txtPath, "GBK")) + "\r\n<end>\r\n" ;
        
        String sql = "insert into page(bookid,name,content,CharCount) values(" + sBookid + ",?,?,?);";
        
        Connection con = oDB.getConnect();

        try {
            con.setAutoCommit(false); //设置手工提交事务模式
            PreparedStatement pstmt = con.prepareStatement(sql);

            Matcher mat = Pattern.compile("(?mi)^([^\\r\\n]+)[\\r\\n]{1,2}更新时间.*$[\\r\\n]{2,4}([^\\a]+?)(?=(^([^\\r\\n]+)[\\r\\n]{1,2}更新时间)|^<end>$)").matcher(txtContent);
            while (mat.find()) {
                
                pstmt.setString(1, mat.group(1));
                pstmt.setString(2, mat.group(2).replace("\n\n", "\n"));
                pstmt.setString(3, String.valueOf(mat.group(2).length()));
                pstmt.executeUpdate();
            }

            pstmt.close();
            con.commit(); //提交事务
        } catch (SQLException ex) {
            ex.toString();
        }
        
    }
        //"select book.ID from Book left join page on book.id=page.bookid group by book.id order by count(page.id),book.isEnd,book.ID"
    public static void regenID(int sortmode, FoxDB oDB) { // 重新排序bookid ,pageid
        String sSQL = "";
        switch (sortmode) {
            case 1: // 书籍页数顺序
                sSQL = "select book.ID as id from Book left join page on book.id=page.bookid group by book.id order by count(page.id),book.isEnd,book.ID";
                break;
            case 2: // 书籍页数倒序
                sSQL = "select book.ID as id from Book left join page on book.id=page.bookid group by book.id order by count(page.id) desc,book.isEnd,book.ID";
                break;
            case 9:  // 根据bookid重新生成pageid
                sSQL = "select id as id from page order by bookid,id";
                break;
        }
        // 获取id列表到数组中
        ArrayList<HashMap<String, Object>> idList = (ArrayList<HashMap<String, Object>>)oDB.getList(sSQL);
        int nRow = idList.size();
        if ( nRow == 0 ) {
            return;
        }
        int[] ids = new int[nRow];
        for (int i = 0; i<nRow; i++ ) {
            ids[i] = Integer.valueOf(idList.get(i).get("id").toString()) ;
        }

        // 最大ID
        int nStartID;
        if (9 == sortmode) {
            nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from page"));
        } else {
            nStartID = 5 + Integer.valueOf(oDB.getOneCell("select max(id) from book"));
        }
        int nStartID1 = nStartID;
        int nStartID2 = nStartID;


        Connection conn = oDB.getConnect();
        try {
            conn.setAutoCommit(false);
            Statement stat = conn.createStatement();

            for (int i = 0; i < nRow; i++) {
                ++nStartID1;
                if (9 == sortmode) {
                    stat.executeUpdate("update page set id=" + nStartID1 + " where id=" + ids[i]);
                } else {
                    stat.executeUpdate("update page set bookid=" + nStartID1 + " where bookid=" + ids[i]);
                    stat.executeUpdate("update book set id=" + nStartID1 + " where id=" + ids[i]);
                }
            }
            
            stat.close();
            conn.commit(); //提交事务
        } catch (SQLException ex) {
            ex.toString();
        }

        try {
            conn.setAutoCommit(false);
            Statement stat = conn.createStatement();

            for (int i = 1; i <= nRow; i++) {
                ++nStartID2;
                if (9 == sortmode) {
                    stat.executeUpdate("update page set id=" + i + " where id=" + nStartID2);
                } else {
                    stat.executeUpdate("update page set bookid=" + i + " where bookid=" + nStartID2);
                    stat.executeUpdate("update book set id=" + i + " where id=" + nStartID2);
                }
            }
            stat.close();
            conn.commit(); //提交事务
        } catch (SQLException ex) {
            ex.toString();
        }

        if (9 != sortmode) {
            oDB.exec("update Book set Disorder=ID");
        }
    }
    
    public static void deletePage(int pageid, boolean bUpdateDelList, FoxDB oDB) { // 删除单章节
       if (bUpdateDelList) { // 修改 DelURL
            ArrayList<HashMap<String,String>> xx = (ArrayList<HashMap<String,String>>)oDB.getList("select book.DelURL as old, page.bookid as bid, page.url as url, page.name as name from book,page where page.id=" + pageid + " and book.id = page.bookid");
            String newDelStr = xx.get(0).get("old").replace("\n\n", "\n") + xx.get(0).get("url") + "|" + xx.get(0).get("name") + "\n" ;
            String sql = "update book set DelURL = ? where id =" + String.valueOf(xx.get(0).get("bid"));
            oDB.execPreOne(sql, newDelStr);
        }
        oDB.exec("Delete From Page where ID = " + pageid);
    }

    public static synchronized void setPageContent(int pageid, String text, FoxDB oDB) { // 修改指定章节的内容
        String aNow = (new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(new java.util.Date());
        String sql = "update page set Content = ? , CharCount=" + text.length() + " , Mark='text', DownTime='" + aNow + "' where id = " + pageid;
        oDB.execPreOne(sql, text);
    }

    public static synchronized void inserNewPages(List<Map<String, Object>> data, int bookid, FoxDB oDB) { // 新增章节到数据库
        Connection con = oDB.getConnect();
        String sql = "insert into page(bookid,url,name) values(?,?,?);";
        String sBookID = String.valueOf(bookid);
        try {
            con.setAutoCommit(false); //设置手工提交事务模式
            PreparedStatement pstmt = con.prepareStatement(sql);

            Iterator<Map<String, Object>> itr = data.iterator();
            HashMap<String, Object> mm;
            while (itr.hasNext()) {
                mm = (HashMap<String, Object>) itr.next();
                pstmt.setString(1, sBookID);
                pstmt.setString(2, (String) mm.get("url"));
                pstmt.setString(3, (String) mm.get("name"));
                pstmt.executeUpdate();
            }

            pstmt.close();
            con.commit(); //提交事务
        } catch (SQLException ex) {
            ex.toString();
        }
    }
    
    public static String getPageListStr(int bookid, FoxDB oDB) { // 获取 url,name 列表
	return getPageListStr_Del(bookid, oDB) + getPageListStr_notDel(bookid, oDB);
    }

    public static String getPageListStr_Del(int bookid, FoxDB oDB) { // 获取 已删除 url,name 列表
	return oDB.getOneCell("select DelURL from book where id = " + bookid).replace("\n\n", "\n");
    }

    public static String getPageListStr_notDel(int bookid, FoxDB oDB) { // 获取 未删除url,name列表
	return getPageListStr_notDel("where bookid = " + String.valueOf(bookid), oDB);
    }

    private static String getPageListStr_notDel(String sqlWhereStr, FoxDB oDB) { // 私有: 获取 未删除url,name列表
	String addDelList = "";
        List rsdata = oDB.getList("select url as url, name as name from page " + sqlWhereStr);
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            addDelList += item.get("url").toString() + "|" + item.get("name").toString() + "\n";
        }
	return addDelList;
    }
    public static String getSiteType(FoxDB oDB) {
        String sitetype = "unknown";
        String urls = oDB.getOneCell("select URL from book where ( isEnd isnull or isEnd < 1 )");
        Matcher mat = Pattern.compile("(?i)http[s]?://[0-9a-z]*[\\.]?([^\\.]+)\\.(com|net|org|se|me|cc|cn|net\\.cn|com\\.cn|org\\.cn|com\\.tw)/").matcher(urls);
        while (mat.find()) {
            sitetype = mat.group(1);
        }
        return sitetype;
    }
    
    public static void simplifyAllDelList(FoxDB oDB) {
        List xx = oDB.getList("select ID as id, DelURL as du from book where length(DelURL) > 200");
        Iterator itr = xx.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            oDB.execPreOne("update Book set DelURL=? where ID = " + item.get("id").toString(), FoxBookLib.simplifyDelList(item.get("du").toString()));
        }
     }
    
    
     public static List<Map<String, Object>> getNewChapters(int bookID, String bookUrl, boolean bMultiThreadDownOneBook, FoxDB oDB) {

        String existList = FoxDBHelper.getPageListStr(Integer.valueOf(bookID), oDB);

        int site_type = 0;
        if (bookUrl.indexOf("zhuishushenqi.com") > -1) {
            site_type = SITE_ZSSQ;
        }
        if (bookUrl.indexOf(".qreader.") > -1) {
            site_type = SITE_KUAIDU;
        }
        if (bookUrl.indexOf("3g.if.qidian.com") > -1) {
            site_type = SITE_QIDIAN_MOBILE;
        }

        String html = "";
        List<Map<String, Object>> lData;
        switch (site_type) {
            case SITE_KUAIDU:
                if ((existList.length() > 3) && (!bMultiThreadDownOneBook)) {
                    lData = site_qreader.qreader_GetIndex(bookUrl, 55, 1); // 更新模式  最后55章
                } else {
                    lData = site_qreader.qreader_GetIndex(bookUrl, 0, 1); // 更新模式
                }
                break;
            case SITE_ZSSQ:
                html = FoxBookLib.downhtml(bookUrl, "utf-8"); // 下载json
                if ((existList.length() > 3) && (!bMultiThreadDownOneBook)) {
                    lData = site_zssq.json2PageList(html, 55, 1); // 更新模式  最后55章
                } else {
                    lData = site_zssq.json2PageList(html, 0, 1); // 更新模式
                }
                break;
            case SITE_QIDIAN_MOBILE:
                html = FoxBookLib.downhtml(bookUrl, "utf-8"); // 下载json
                if ((existList.length() > 3) && (!bMultiThreadDownOneBook)) {
                    lData = site_qidian.json2PageList(html); // 更新模式  最后55章
                } else {
                    lData = site_qidian.json2PageList(html); // 更新模式
                }
                break;
            default:
                html = FoxBookLib.downhtml(bookUrl); // 下载url
                if ((existList.length() > 3) && (!bMultiThreadDownOneBook)) {
                    lData = FoxBookLib.tocHref(html, 55); // 分析获取 list 最后55章
                } else {
                    lData = FoxBookLib.tocHref(html, 0); // 分析获取 list 所有章节
                }
        }

        // 比较，得到新章节
        lData = FoxBookLib.compare2GetNewPages(lData, existList);
        if (lData.size() > 0) { // 有新章节才写入数据库
            FoxDBHelper.inserNewPages(lData, bookID, oDB); //写入数据库
            lData = oDB.getList("select id as id, name as name, url as url from page where ( bookid=" + bookID + " ) and ( (content is null) or ( length(content) < 9 ) )"); // 获取新增章节
        }

        return lData;
    }
    
    public static String updatepage(int pageid, FoxDB oDB) {
        ArrayList<Map<String, String>> xx = (ArrayList<Map<String, String>>) oDB.getList("select book.url as bu,page.url as pu from book,page where page.id=" + String.valueOf(pageid) + " and  book.id in (select bookid from page where id=" + String.valueOf(pageid) + ")");
        String fullPageURL = FoxBookLib.getFullURL(xx.get(0).get("bu"), xx.get(0).get("pu"));		// 获取bookurl, pageurl 合成得到url

        return updatepage(fullPageURL, pageid, oDB);
    }

    public static String updatepage(String pageFullURL, int pageid, FoxDB oDB) {
        String text = "";
        String html = "";
        int site_type = 0; // 特殊页面处理 

        if (pageFullURL.contains(".qidian.com")) {
            site_type = 99;
        }
        if (pageFullURL.contains("files.qidian.com")) {  // 起点手机站直接用txt地址好了
            site_type = 16;
        }
        if (pageFullURL.contains(".qreader.")) {
            site_type = 13;
        }
        if (pageFullURL.contains("zhuishushenqi.com")) {
            site_type = 12;
        } // 这个得放在qidian后面，因为有时候zssq地址会包含起点的url

        switch (site_type) {
            case 12:
                String json = FoxBookLib.downhtml(pageFullURL, "utf-8"); // 下载json
                text = site_zssq.json2Text(json);
                break;
            case 13:
                text = site_qreader.qreader_GetContent(pageFullURL);
                break;
            case 16:
                html = FoxBookLib.downhtml(pageFullURL, "GBK"); // 下载json
                text = site_qidian.qidian_getTextFromPageJS(html);
                break;
            case 99:
//              String nURL = site_qidian.qidian_toPageURL_FromPageInfoURL(pageFullURL);
		String nURL = site_qidian.qidian_toTxtURL_FromPageContent(FoxBookLib.downhtml(pageFullURL)) ; // 2015-11-17: 起点地址变动，只能下载网页后再获取txt地址
                html = FoxBookLib.downhtml(nURL);
                text = site_qidian.qidian_getTextFromPageJS(html);
                break;
            default:
                html = FoxBookLib.downhtml(pageFullURL); // 下载url
                text = FoxBookLib.pagetext(html);   	// 分析得到text
        }

        if (pageid > 0) { // 当pageid小于0时不写入数据库，主要用于在线查看
            FoxDBHelper.setPageContent(pageid, text, oDB); // 写入数据库
            return String.valueOf(text.length());
        } else {
            return text;
        }
    }

    // data包含的hashmap中需包含三个必要key: bookname,title,content
    public static void all2txt(ArrayList<HashMap<String, String>> data, boolean bOneBook) { // 所有书籍转为txt
        // select b.name as bookname, p.name as title, p.content as content from book as b, page as p where b.id = p.bookid and b.id=1 order by p.bookid,p.id
        String txtPath = "foxbook.txt";
        File saveDir = new File("c:/etc") ;
        if ( saveDir.exists() && saveDir.isDirectory() )
            txtPath = "c:/etc/foxbook.txt";
        
        StringBuilder txt = new StringBuilder(512000) ;
        Iterator<HashMap<String, String>> itr = data.iterator();
        HashMap<String, String> mm ;
        
        if (bOneBook) {
            txt.append(data.get(0).get("bookname")).append("\n\n");
            while (itr.hasNext()) {
                mm = itr.next();
                txt.append(mm.get("title")).append("\n\n").append(mm.get("content")).append("\n\n\n");
            }
        } else {
            while (itr.hasNext()) {
                mm = itr.next();
                txt.append("●").append(mm.get("bookname")).append("●").append(mm.get("title")).append("\n\n").append(mm.get("content")).append("\n\n\n");
            }
        }

        try {
            BufferedWriter bw1 = new BufferedWriter(new FileWriter(txtPath, false));
            bw1.write(txt.toString());
            bw1.flush();
            bw1.close();
        } catch (IOException e) {
            e.toString();
        }
    }
    
    public static void all2Epub(ArrayList<HashMap<String, String>> data, String inBookName, String inSavePath) {
        Iterator<HashMap<String, String>> itr = data.iterator();
        HashMap<String, String> mm;

        FoxEpub oEpub = new FoxEpub(inBookName, inSavePath);
        while (itr.hasNext()) {
            mm = itr.next();
            oEpub.AddChapter(mm.get("title"), mm.get("content"), -1);
         }
        oEpub.SaveTo();
    }

    public static void all2Ebook(int transType, String pageids, FoxDB oDB) {
        all2Ebook(transType, 1, 0, pageids, oDB);
    }

    public static void all2Ebook(int transType, int inBookIDorMode, FoxDB oDB) {
        int intransMode = 9;
        int inBookID = 0;
        if (inBookIDorMode == 0) {
            intransMode = 0;
        } else {
            intransMode = 2;
            inBookID = inBookIDorMode;
        }
        all2Ebook(transType, intransMode, inBookID, "", oDB);
    }

    private static void all2Ebook(int transType, int transMode, int bookid, String pageids, FoxDB oDB) {
        // transType = 1:mobi 2:epub 9:txt
        // transMode = 0:all pages 1:selected pages 2:one book
        String sql = "";
        switch (transMode) {
            case 0:
                sql = "select b.name as bookname, p.name as title, p.content as content from book as b, page as p where b.id = p.bookid order by p.bookid,p.id";
                break;
            case 2:
                sql = "select b.name as bookname, p.name as title, p.content as content from book as b, page as p where b.id = p.bookid and b.id=" + bookid + " order by p.bookid,p.id";
                break;
            case 1:
                sql = "select b.name as bookname, p.name as title, p.content as content from book as b, page as p where b.id = p.bookid and p.id in (" + pageids + ") order by p.bookid,p.id";
                break;
        }
        ArrayList<HashMap<String, String>> data = (ArrayList<HashMap<String, String>>) oDB.getList(sql);
        String bookname = data.get(0).get("bookname");
        String outDir = "./";
        File saveDir = new File("c:/etc");
        if (saveDir.exists() && saveDir.isDirectory()) {
            outDir = "c:/etc/";
        }
        String oBookName = bookname;
        String fBookName = bookname;
        if (transType != 9) {
            if (transMode != 2) { // 0:all pages 1:selected pages 2:one book
                String sst = FoxDBHelper.getSiteType(oDB);
                oBookName = bookname + "_" + sst;
                fBookName = "all_" + sst;

                //处理章节名 内容
                String preBName = "";
                String nowBName = "";
                HashMap<String, String> mm;
                int cData = data.size();
                for (int i = 0; i < cData; i++) {
                    mm = data.get(i);
                    nowBName = mm.get("bookname");
                    mm.put("content", "\n　　" + mm.get("content").replace("\n", "<br/>\n　　"));
                    if (!preBName.equals(nowBName)) { // 书名和上一条的不同，修改本条
                        mm.put("title", "●" + nowBName + "●" + mm.get("title"));
                        data.set(i, mm);
                        preBName = nowBName;
                    }
                }
            } else { // 处理txt -> html
                HashMap<String, String> mm;
                int cData = data.size();
                for (int i = 0; i < cData; i++) {
                    mm = data.get(i);
                    mm.put("content", "\n　　" + mm.get("content").replace("\n", "<br/>\n　　"));
                    data.set(i, mm);
                }
            }
        }

        switch (transType) { // 1:mobi 2:epub 9:txt
            case 1:
                FoxDBHelper.all2Epub(data, oBookName, outDir + fBookName + ".mobi");
                break;
            case 2:
                FoxDBHelper.all2Epub(data, oBookName, outDir + fBookName + ".epub");
                break;
            case 9:
                if (transMode == 2) {
                    FoxDBHelper.all2txt(data, true);
                } else {
                    FoxDBHelper.all2txt(data, false);
                }
                break;
        }
    }
}
