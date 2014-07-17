/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author guanli
 */
public class FoxBookDB {
    
    public static synchronized void setPageContent(int pageid, String text, FoxDB oDB) { // 修改指定章节的内容
        String aNow = (new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(new java.util.Date());

        Connection con = oDB.getConnect();
        String sql = "update page set Content = ? , CharCount=" + text.length() + " , Mark=\"text\", DownTime=\"" + aNow + "\" where id = " + pageid;
        try {
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, text);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException ex) {
            ex.toString();
        }
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
                pstmt.execute();
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
    
}
