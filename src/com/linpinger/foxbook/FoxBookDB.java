/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
public class FoxBookDB {
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
        
        String txtContent = FoxBookLib.fileRead(txtPath, "GBK").replace("　　", "").replace("<a href=http://www.qidian.com>起点中文网 www.qidian.com 欢迎广大书友光临阅读，最新、最快、最火的连载作品尽在起点原创！</a>", "").replace("<a>手机用户请到m.qidian.com阅读。</a>", "") + "\r\n<end>\r\n" ;
        
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
        Matcher mat = Pattern.compile("(?i)http[s]?://[0-9a-z]*[\\.]?([^\\.]+)\\.(com|net|org|se|me|cc|cn|net\\.cn|com\\.cn|org\\.cn)/").matcher(urls);
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
    
}
