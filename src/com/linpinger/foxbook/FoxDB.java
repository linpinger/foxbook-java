/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author guanli
 */
public class FoxDB {

    private String dbPath = "FoxBook.db3";
    private String dbPath1 = "FoxBook.db3";
    private String dbPath2 = "FoxBook.db3.old";
    private Connection conn;
    private boolean bFirstOpen = true;

    public FoxDB() {
        OpenDB();
    }
    
    public String getOneCell(String inSQL) {
        String retStr = "";
        try {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(inSQL);
            rs.next();
            retStr = rs.getObject(1).toString();
            rs.close();
            stat.close();
        } catch (SQLException e) {
            e.toString();
        }
        return retStr;
    }

    public List getList(String inSQL) {
        List retList = new ArrayList();
        try {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(inSQL);
            ResultSetMetaData md = rs.getMetaData();
            int nColum = md.getColumnCount();
            while (rs.next()) { //将查询到的数据打印出来
                Map item = new HashMap(nColum);
                for (int i = 1; i <= nColum; i++) {
                    item.put(md.getColumnName(i), rs.getObject(i));
                }
                retList.add(item);
            }
            rs.close();
            stat.close();
        } catch (SQLException ex) {
            Logger.getLogger(FoxDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        return retList;
    }

    public int exec(String inSQL) {
        int rrr = 0;
        try {
            conn.setAutoCommit(false);
            Statement stat = conn.createStatement();
            rrr = stat.executeUpdate(inSQL);
            stat.close();
            conn.commit(); //提交事务
        } catch (SQLException ex) {
            Logger.getLogger(FoxDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rrr;
    }
    
    public void execPreOne(String inSQL, String escapeStr ) {
        try {
            conn.setAutoCommit(false);
            PreparedStatement pstmt = conn.prepareStatement(inSQL);
            pstmt.setString(1, escapeStr);
            pstmt.executeUpdate();
            pstmt.close();
            conn.commit(); //提交事务
        } catch (Exception ex) {
            ex.toString();
        }
    }

    private void OpenDB() {
        if (!bFirstOpen) {
            closeDB();
        }
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(FoxDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void reOpenDB() {
        this.bFirstOpen = false;
        OpenDB();
    }
    
    public Connection getConnect() {  // 事务处理需要这个
        return this.conn;
    }

    public void switchDB() {
        if (this.dbPath == this.dbPath1) {
            this.dbPath = this.dbPath2;
        } else {
            this.dbPath = this.dbPath1;
        }
        reOpenDB();
    }
    
    public double vacuumDB() {
        long sizeBefore = new File(this.dbPath).length() ;
        reOpenDB();
        try {
            Statement stat = conn.createStatement();
            stat.executeUpdate("vacuum");
            stat.close();
        } catch (SQLException ex) {
            System.out.println(ex.toString());
        }
        long sizeAfter = new File(this.dbPath).length() ;
        return Math.floor((sizeBefore - sizeAfter) / 1024) ;
    }

    public void closeDB() {
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(FoxDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
