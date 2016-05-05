/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author guanli
 */
public class FoxMainCMD {

    private FoxDB oDB;             // cmd 使用加上 static
    public final int downThread = 9;  // 页面下载任务线程数
    public int leftThread = downThread;
  
    public void updateAllOneByOne() {
        List upList = oDB.getList("select id as id, name as name, url as url from book where isEnd is null or isEnd != 1");
        Iterator itr = upList.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap<String, String>) itr.next();
            Integer bookID = (Integer) item.get("id");
            String bookUrl = (String) item.get("url");
            String bookName = (String) item.get("name");

            System.out.println("★　更新: " + bookID + " : " + bookName);
            List<Map<String, Object>> lData = FoxDBHelper.getNewChapters(bookID, bookUrl, false, oDB);
            // 单线程循环更新页面
            Iterator<Map<String, Object>> itrz = lData.iterator();
            while (itrz.hasNext()) {
                HashMap<String, Object> nn = (HashMap<String, Object>) itrz.next();
                String nowURL = (String) nn.get("url");
                Integer nowpageid = (Integer) nn.get("id");
                String pageLen = FoxDBHelper.updatepage(FoxBookLib.getFullURL(bookUrl, nowURL), nowpageid, oDB);
                System.out.println("★　　新章节: " + nowpageid + " : " + bookName + " : " + nn.get("name") + "  字数: " + pageLen);
            }
        }
    }

    public static void main(String[] args) {  // 入口
        int argc = args.length;
        String helpMsg = "用法: java -jar FoxBook.jar [动作] [数据库路径]\n"
                + "说明: 注意命令行参数的顺序，主要是作者懒得解析哈\n"
                + "  -up\t\t更新该数据库\n"
                + "  -upDESC\t更新该数据库并快捷倒序\n"
                + "  -upASC\t更新该数据库并快捷顺序\n"
                + "  -upOBO\t依次更新所有书籍\n"
                + "  -mobi\t\t生成mobi\n"
                + "  -epub\t\t生成epub\n"
                + "  -txt\t\t生成txt\n"
                + "";
        //      + "  -s  [端口] HTTP服务器模式\n"

        String iDBPath = "FoxBook.db3";
        String iAction = args[0];
        if (argc == 2) {
            iDBPath = args[1];
        }

        FoxMainCMD foxcmd = new FoxMainCMD(iDBPath); // 初始化DB

        if (iAction.equalsIgnoreCase("-up")) {
            System.out.println("★　更新: " + iDBPath);
            foxcmd.doUpdateAll();
        } else if (iAction.equalsIgnoreCase("-upOBO")) {
            System.out.println("★　依次更新: " + iDBPath);
            foxcmd.updateAllOneByOne();
        } else if (iAction.equalsIgnoreCase("-upDESC")) {
            System.out.println("★　更新: " + iDBPath);
            long sTime = System.currentTimeMillis();
            foxcmd.doUpdateAll();
            System.out.println("★　已完成倒序并缩小数据库: " + foxcmd.doSortDBDesc() + " K   耗时(ms): " + (System.currentTimeMillis() - sTime));
        } else if (iAction.equalsIgnoreCase("-upASC")) {
            long sTime = System.currentTimeMillis();
            foxcmd.doUpdateAll();
            System.out.println("★　已完成顺序并缩小数据库: " + foxcmd.doSortDBAsc() + " K   耗时(ms): " + (System.currentTimeMillis() - sTime));
        } else if (iAction.equalsIgnoreCase("-mobi")) {
            System.out.println("★　转成mobi: " + iDBPath);
            foxcmd.toebook(1);
        } else if (iAction.equalsIgnoreCase("-epub")) {
            System.out.println("★　转成epub: " + iDBPath);
            foxcmd.toebook(2);
        } else if (iAction.equalsIgnoreCase("-txt")) {
            System.out.println("★　转成txt: " + iDBPath);
            foxcmd.toebook(9);
        } else {
            System.out.println(helpMsg);
        }

        foxcmd.done(); // 关闭DB
    }
    
    public FoxMainCMD(String iDBPath) {  // 初始化
        oDB = new FoxDB(iDBPath);
    }

    public void done() {
        oDB.closeDB();
    }
    
    // 更新
    public void doUpdateAll() {
        try {
            Thread uu = new Thread(new UpdateAllBook()); // 更新
            uu.start();
            uu.join();
        } catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
    }

    // 快捷倒排
    public double doSortDBDesc() {
        FoxDBHelper.regenID(2, oDB);
        FoxDBHelper.regenID(9, oDB);
        FoxDBHelper.simplifyAllDelList(oDB);
        return oDB.vacuumDB();
    }
    // 快捷顺排
    public double doSortDBAsc() {
        FoxDBHelper.regenID(1, oDB);
        FoxDBHelper.regenID(9, oDB);
        FoxDBHelper.simplifyAllDelList(oDB);
        return oDB.vacuumDB();
    }


    // 转为电子书，注意iSelect值与all2Ebook中的值一致
    public void toebook(int iSelect) {
        long sTime = System.currentTimeMillis();
        FoxDBHelper.all2Ebook(iSelect, 0, oDB);
        System.out.println("★　转换完毕，耗时(ms): " + (System.currentTimeMillis() - sTime));
    }
    

    public class UpdateAllBook implements Runnable { 

        /**
         * GUI菜单更新所有书籍
         */
        @Override
        public void run() {
            ThreadGroup grpFox = new ThreadGroup("fox"); // 更新线程组
            List upList = oDB.getList("select id as id, name as name, url as url from book where isEnd is null or isEnd != 1");

            Iterator itr = upList.iterator();
            Thread nowT;
            while (itr.hasNext()) {
                HashMap item = (HashMap<String, String>) itr.next();
                nowT = new Thread(grpFox, new UpdateBook((Integer) item.get("id"), (String) item.get("url"), (String) item.get("name"), true));
                nowT.start();
            }

            System.out.println("★　开始更新所有书籍，请耐心等待...");

            // 使用线程组来确定进度，由于使用循环，使用CPU可能比较频繁，所以加入sleep
            int lastLeftThreadCount = 0;
            int tmpThreadCount ;
            final int allThreadCount = upList.size();
            while ((tmpThreadCount = grpFox.activeCount()) > 0) {
                final int nowLeftThreadCount = tmpThreadCount;
                if (nowLeftThreadCount == lastLeftThreadCount) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        ex.toString();
                    }
                    continue;
                } else {
                    lastLeftThreadCount = nowLeftThreadCount;
                }

                System.out.println("★　　　剩余线程: " + nowLeftThreadCount + " / " + allThreadCount);
            }

            System.out.println("★　全部线程更新完毕，恭喜");
        }
    }

    public class UpdateBook implements Runnable { // 后台线程更新书

        private int bookID;
        private String bookName;
        private String bookUrl;
        private boolean bDownPage = true;
        private boolean bMultiThreadDownOneBook = false;

        UpdateBook(int inbookid, String inbookurl, String inbookname, boolean bDownPage) {
            this.bookID = inbookid;
            this.bookName = inbookname;
            this.bookUrl = inbookurl;
            this.bDownPage = bDownPage;
        }

        UpdateBook(int inbookid, String inbookurl, String inbookname, int bDownOneBook) {  // 多线程更新一本书
            this.bookID = inbookid;
            this.bookName = inbookname;
            this.bookUrl = inbookurl;
            this.bMultiThreadDownOneBook = true;
        }

        @Override
        public void run() {
            List<Map<String, Object>> lData = FoxDBHelper.getNewChapters(bookID, bookUrl, bMultiThreadDownOneBook, oDB) ;

            if (bDownPage) {
                int cTask = lData.size(); // 总任务数

                if (bMultiThreadDownOneBook) { // 当新章节数大于 25章就采用多任务下载模式
                    int nBaseCount = cTask / downThread; //每线程基础任务数
                    int nLeftCount = cTask % downThread; //剩余任务数
                    int aList[] = new int[downThread]; // 每个线程中的任务数

                    for (int i = 0; i < downThread; i++) {  // 分配任务数
                        if (i < nLeftCount) {
                            aList[i] = nBaseCount + 1;
                        } else {
                            aList[i] = nBaseCount;
                        }
                    }

                    List<Map<String, Object>> subList;
                    int startPoint = 0;
                    leftThread = downThread;
                    for (int i = 0; i < downThread; i++) {
                        if (aList[i] == 0) { // 这种情况出现在总任务比线程少的情况下
                            --leftThread;
                            continue;
                        }
                        subList = new ArrayList<Map<String, Object>>(aList[i]);
                        for (int n = startPoint; n < startPoint + aList[i]; n++) {
                            subList.add((HashMap<String, Object>) lData.get(n));
                        }
                        (new Thread(new FoxTaskDownPage(subList), "T" + i)).start();

                        startPoint += aList[i];
                    }
                } else {

                    // 单线程循环更新页面
                    Iterator<Map<String, Object>> itrz = lData.iterator();
                    while (itrz.hasNext()) {
                        HashMap<String, Object> nn = (HashMap<String, Object>) itrz.next();
                        String nowURL = (String) nn.get("url");
                        Integer nowpageid = (Integer) nn.get("id");
                        String pageLen = FoxDBHelper.updatepage(FoxBookLib.getFullURL(bookUrl, nowURL), nowpageid, oDB);
                        System.out.println("★　新章节: " + nowpageid + " : " + bookName + " : " + nn.get("name") + "  字数: " + pageLen);
                    }
                }
            }
        }
    }

    public class FoxTaskDownPage implements Runnable { // 多线程任务更新页面列表

        List<Map<String, Object>> taskList;

        public FoxTaskDownPage(List<Map<String, Object>> iTaskList) {
            this.taskList = iTaskList;
        }

        /**
         *  多线程任务更新页面列表
         */
        @Override
        public void run() {
            final String thName = Thread.currentThread().getName();
            Iterator<Map<String, Object>> itr = taskList.iterator();
            HashMap<String, Object> mm;
            int nowID;
            String nowURL;
            int locCount = 0;
            final int allCount = taskList.size();
            String pageLen = "";
            while (itr.hasNext()) {
                ++locCount;
                mm = (HashMap<String, Object>) itr.next();
                nowID = (Integer) mm.get("id");
                nowURL = (String) mm.get("url");

                pageLen = FoxDBHelper.updatepage(nowID, oDB);

                System.out.println("★　剩余线程:线程名:页面/页面数　" + leftThread + " : " + thName + " : " + locCount + " / " + allCount);

            }
            --leftThread;
            if (0 == leftThread) { // 所有线程更新完毕
                System.out.println("★　恭喜！理论上多线程更新完毕^_^");
            }
        }
    }
    
    
}
