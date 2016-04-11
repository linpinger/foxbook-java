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
/*
// FoxInterfaceMSG.java
package com.linpinger.foxbook;
public interface FoxInterfaceMSG {
    public void msg(String iStr) ;
    public void msg(String iStr, int msgType);
}
*/
// public class FoxMainCMD implements FoxInterfaceMSG {
public class FoxMainCMD {

    private FoxDB oDB;             // cmd 使用加上 static
    private final int SITE_QIDIAN_MOBILE = 16;
    // private final int SITE_EASOU = 11 ;
    private final int SITE_ZSSQ = 12;
    private final int SITE_KUAIDU = 13;
    public final int downThread = 9;  // 页面下载任务线程数
    public int leftThread = downThread;

/*
    @Override
    public void msg(String iStr) {
        System.out.println(iStr);
    }

    @Override
    public void msg(String iStr, int msgType) {
        switch (msgType) {
            case 1:
                System.out.println("  " + iStr);
                break;
            case 2:
                System.out.println("    " + iStr);
                break;
            default:
                System.out.println(iStr);
                break;
        }
    }
*/
    public static void main(String[] args) {  // 入口
        int argc = args.length;
        String helpMsg = "用法: java -jar FoxBook.jar [动作] [数据库路径]\n"
                + "说明: 注意命令行参数的顺序，主要是作者懒得解析哈\n"
                + "  -up\t\t更新该数据库\n"
                + "  -upDESC\t更新该数据库并快捷倒序\n"
                + "  -upASC\t更新该数据库并快捷顺序\n"
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
    

    public class UpdateAllBook implements Runnable { // GUI菜单更新所有书籍

        public void run() {
            ThreadGroup grpFox = new ThreadGroup("fox"); // 更新线程组
            List upList = oDB.getList("select id as id, name as name, url as url from book where isEnd is null or isEnd != 1");

            Iterator itr = upList.iterator();
//            List<Thread> threadList = new ArrayList(30);
            Thread nowT;
            while (itr.hasNext()) {
                HashMap item = (HashMap<String, String>) itr.next();
                nowT = new Thread(grpFox, new UpdateBook((Integer) item.get("id"), (String) item.get("url"), (String) item.get("name"), true));
//              threadList.add(nowT);
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
//                System.out.println("★　总更新线程数量: " + allThreadCount + "  剩余线程数量: " + nowLeftThreadCount);
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

            if (bDownPage) {
                int cTask = lData.size(); // 总任务数
//            System.out.println("任务数:" + cTask);

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
                    String nowURL = "";
                    Integer nowpageid = 0;
                    int nowCount = 0;
                    String pageLen;
//                tPage.setRowCount(0); // 填充uPage
                    while (itrz.hasNext()) {
                        HashMap<String, Object> nn = (HashMap<String, Object>) itrz.next();
                        nowURL = (String) nn.get("url");
                        nowpageid = (Integer) nn.get("id");

                        ++nowCount;

                        pageLen = FoxDBHelper.updatepage(FoxBookLib.getFullURL(bookUrl, nowURL), nowpageid, oDB);

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

//                final Object data[] = new Object[]{mm.get("name"), pageLen, mm.get("id"), thName, mm.get("url")};
                System.out.println("★　剩余线程:线程名:页面/页面数　" + leftThread + " : " + thName + " : " + locCount + " / " + allCount);
                // todo tPage.addRow(data);

            }
            --leftThread;
            if (0 == leftThread) { // 所有线程更新完毕
                System.out.println("★　恭喜！理论上多线程更新完毕^_^");
            }
        }
    }
    
    
}
