/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import static com.linpinger.foxbook.FoxBookLib.getFullURL;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author guanli
 */
public class FoxMainFrame extends javax.swing.JFrame {
    //	private final int SITE_EASOU = 11 ;

    private final int SITE_ZSSQ = 12;
    private final int SITE_KUAIDU = 13;
    public final int downThread = 9;  // 页面下载任务线程数
    public int leftThread = downThread;
    
    public class UpdateAllBook implements Runnable { // GUI菜单更新所有书籍

        public void run() {
            List upList = oDB.getList("select id as id, name as name, url as url from book where isEnd is null or isEnd != 1");

            Iterator itr = upList.iterator();
            List<Thread> threadList = new ArrayList(30);
            Thread nowT;
            while (itr.hasNext()) {
                HashMap item = (HashMap<String, String>) itr.next();
                nowT = new Thread(new UpdateBook((Integer) item.get("id"), (String) item.get("url"), (String) item.get("name"), true));
                System.out.println("线程 " + nowT.getName() + " 更新:" + (String) item.get("name"));
                threadList.add(nowT);
                nowT.start();
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tPage.setRowCount(0);
                    tPage.addRow(new Object[]{"★　开始更新所有书籍"});
                }
            });
            System.out.println("等待诸多线程...");

            Iterator itrT = threadList.iterator();
            while (itrT.hasNext()) {
                nowT = (Thread) itrT.next();
                try {
                    nowT.join();
                } catch (Exception ex) {
                    System.out.println("等待线程错误: " + ex.toString());
                }
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tPage.addRow(new Object[]{"★　全部线程完毕，恭喜"});
                }
            });         
            refreshBookList();
            System.out.println("全部线程完毕，恭喜");
            
        }
    }

    public class UpdateOneBook implements Runnable { // GUI菜单更新一本书籍
        private String nBookName ;
        private String nURL;
        private int nBookID;
        private boolean bWritePage = true ;
        
        public UpdateOneBook(int BookID, String BookURL, String BookName, boolean bUpdatePage) {
            this.nBookID = BookID;
            this.nBookName = BookName;
            this.nURL = BookURL;
            this.bWritePage = bUpdatePage;
        }

        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tPage.setRowCount(0);
                    if ( bWritePage )
                        tPage.addRow(new Object[]{"★　开始更新本书: " + nBookName});
                    else
                        tPage.addRow(new Object[]{"★　开始更新本书目录: " + nBookName});
                }
            });

            Thread nowUP = new Thread(new UpdateBook(nBookID, nURL, nBookName, bWritePage));
            nowUP.start();
            try {
                nowUP.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(FoxMainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if ( bWritePage )
                        tPage.addRow(new Object[]{"★　本书更新完毕: " + nBookName});
                    else
                        tPage.addRow(new Object[]{"★　本书目录更新完毕: " + nBookName});
                }
            });
            refreshBookList();
        }
    }

    public class MultiThreadUpdateOneBook implements Runnable { // 多线程更新一本书

        private String nBookName;
        private String nURL;
        private int nBookID;

        public MultiThreadUpdateOneBook(int BookID, String BookURL, String BookName) {
            this.nBookID = BookID;
            this.nBookName = BookName;
            this.nURL = BookURL;
        }

        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tPage.setRowCount(0);
                    tPage.addRow(new Object[]{"★　开始不受控制滴多线程更新本书: " + nBookName});
                }
            });

            Thread nowUP = new Thread(new UpdateBook(nBookID, nURL, nBookName, 9));
            nowUP.start();
            try {
                nowUP.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(FoxMainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
/*
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tPage.addRow(new Object[]{"★　本书不受控制滴更新完毕: " + nBookName});
                }
            });
*/
            refreshBookList();
        }
    }
    
    public class FoxTaskDownPage implements Runnable { // 多线程任务更新页面列表

        List<Map<String, Object>> taskList;

        public FoxTaskDownPage(List<Map<String, Object>> iTaskList) {
            this.taskList = iTaskList;
        }

        public void run() {
            String thName = Thread.currentThread().getName();
            Iterator<Map<String, Object>> itr = taskList.iterator();
            HashMap<String, Object> mm;
            int nowID;
            String nowURL;
            int locCount = 0;
            int allCount = taskList.size();
            String pageLen = "";
            while (itr.hasNext()) {
                ++locCount;
                mm = (HashMap<String, Object>) itr.next();
                nowID = (Integer) mm.get("id");
                nowURL = (String) mm.get("url");

                pageLen = FoxBookLib.updatepage(nowID, oDB);

                //               msg.obj = leftThread + ":" + thName + ":" + locCount + " / " + allCount;
                final Object data[] = new Object[]{mm.get("name"), pageLen, mm.get("id"), thName, mm.get("url")};
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        tPage.addRow(data);
                    }
                });
                
            }
            --leftThread;
            if (0 == leftThread) { // 所有线程更新完毕
//                msg.obj = "已更新完所有空白章节>25";
            }
        }
    }

    public class UpdateBook implements Runnable { // 后台线程更新书

        private int bookID;
        private String bookName;
        private String bookUrl;
        private boolean bDownPage = true;
        private boolean bMultiThreadDownOneBook = false ;

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
            String existList = FoxBookDB.getPageListStr(Integer.valueOf(bookID), oDB);

            int site_type = 0;
            if (bookUrl.indexOf("zhuishushenqi.com") > -1) {
                site_type = SITE_ZSSQ;
            }
            if (bookUrl.indexOf(".qreader.") > -1) {
                site_type = SITE_KUAIDU;
            }

            String html = "";
            List<Map<String, Object>> lData;
            switch (site_type) {
                case SITE_KUAIDU:
                    if ( (existList.length() > 3) && ( ! bMultiThreadDownOneBook ) ) {
                        lData = site_qreader.qreader_GetIndex(bookUrl, 55, 1); // 更新模式  最后55章
                    } else {
                        lData = site_qreader.qreader_GetIndex(bookUrl, 0, 1); // 更新模式
                    }
                    break;
                case SITE_ZSSQ:
                    html = FoxBookLib.downhtml(bookUrl, "utf-8"); // 下载json
                    if ( (existList.length() > 3) && ( ! bMultiThreadDownOneBook ) ) {
                        lData = site_zssq.json2PageList(html, 55, 1); // 更新模式  最后55章
                    } else {
                        lData = site_zssq.json2PageList(html, 0, 1); // 更新模式
                    }
                    break;
                default:
                    html = FoxBookLib.downhtml(bookUrl); // 下载url
                    if ( (existList.length() > 3) && ( ! bMultiThreadDownOneBook ) ) {
                        lData = FoxBookLib.tocHref(html, 55); // 分析获取 list 最后55章
                    } else {
                        lData = FoxBookLib.tocHref(html, 0); // 分析获取 list 所有章节
                    }
            }

            // 比较，得到新章节
            lData = FoxBookLib.compare2GetNewPages(lData, existList);
            if ( lData.size() > 0 ) { // 有新章节才写入数据库
                FoxBookDB.inserNewPages(lData, bookID, oDB); //写入数据库
            }
//            System.out.println("任务数:" + cTask);

            if (bDownPage) {
                // 获取新增章节
                lData = oDB.getList("select id as id, name as name, url as url from page where ( bookid=" + bookID + " ) and ( (content is null) or ( length(content) < 9 ) )");
                int cTask = lData.size(); // 总任务数
                
                if ( bMultiThreadDownOneBook ) { // 当新章节数大于 25章就采用多任务下载模式
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

                        pageLen = FoxBookLib.updatepage(getFullURL(bookUrl, nowURL), nowpageid, oDB);

                        final Object data[] = new Object[]{nn.get("name"), pageLen, nn.get("id"), bookName, nn.get("url")};
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                tPage.addRow(data);
                            }
                        });
                        
                    }
                }
            } else {
//                tPage.setRowCount(0); // 填充uPage
                Iterator itr = lData.iterator();
                while (itr.hasNext()) {
                    HashMap item = (HashMap) itr.next();
                    final Object data[] = new Object[]{item.get("name"), "0", item.get("id"), bookName, item.get("url")};

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            tPage.addRow(data);
                        }
                    });
                
                }
            }
        }
    }

    /**
     * Creates new form FoxMainFrame
     */
    public FoxMainFrame() {
        FoxInit();
        initComponents();

        // 设置宽度
        TableColumnModel tcmL = uBook.getTableHeader().getColumnModel();
        tcmL.getColumn(0).setPreferredWidth(200);
        tcmL.getColumn(2).setPreferredWidth(40);
        TableColumnModel tcmR = uPage.getTableHeader().getColumnModel();
        tcmR.getColumn(0).setPreferredWidth(300);
        tcmR.getColumn(2).setPreferredWidth(40);
        tcmR.getColumn(3).setPreferredWidth(150);
    }

    public void refreshBookList() {
        tBook.setRowCount(0);
        List rsdata = oDB.getList("select book.Name as name,count(page.id) as cc,book.ID as id,book.URL as url,book.isEnd as isend from Book left join page on book.id=page.bookid group by book.id order by book.DisOrder ;");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            tBook.addRow(new Object[]{ item.get("name"), item.get("cc"), item.get("id"), item.get("url") }) ;
        }
    }

    private void FoxInit() {
        tBook = new javax.swing.table.DefaultTableModel(null, new String[]{
            "Name", "Count", "ID", "URL"
        }) {
            boolean[] canEdit = new boolean[]{
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        tPage = new javax.swing.table.DefaultTableModel(null, new String[]{
            "Name", "Count", "ID", "bID/Name", "URL"
        }) {
            boolean[] canEdit = new boolean[]{
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        oDB = new FoxDB();
        refreshBookList();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        showContent = new javax.swing.JDialog();
        jPopupMenuBook = new javax.swing.JPopupMenu();
        mBookUpdateOne = new javax.swing.JMenuItem();
        mBookUpdateTocOne = new javax.swing.JMenuItem();
        mBookMultiThreadUpdateOne = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        mBookInfoEditor = new javax.swing.JMenuItem();
        jPopupMenuPage = new javax.swing.JPopupMenu();
        mPageUpdateOne = new javax.swing.JMenuItem();
        mPageDeleteMulti = new javax.swing.JMenuItem();
        mPageDeleteMultiNotUpdate = new javax.swing.JMenuItem();
        editBookInfo = new javax.swing.JDialog();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        uBook = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        uPage = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mBookNew = new javax.swing.JMenuItem();
        mRefreshBookList = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mBookShowAll = new javax.swing.JMenuItem();
        mBookUpdateAll = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        mDBSortDesc = new javax.swing.JMenuItem();
        mDBSortAsc = new javax.swing.JMenuItem();
        mDBRegenPageIDs = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mDBVacuum = new javax.swing.JMenuItem();

        showContent.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        showContent.setTitle("呵呵");

        javax.swing.GroupLayout showContentLayout = new javax.swing.GroupLayout(showContent.getContentPane());
        showContent.getContentPane().setLayout(showContentLayout);
        showContentLayout.setHorizontalGroup(
            showContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        showContentLayout.setVerticalGroup(
            showContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        mBookUpdateOne.setMnemonic('g');
        mBookUpdateOne.setText("更新本书(G)");
        mBookUpdateOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookUpdateOneActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBookUpdateOne);

        mBookUpdateTocOne.setMnemonic('t');
        mBookUpdateTocOne.setText("更新本书目录(T)");
        mBookUpdateTocOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookUpdateTocOneActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBookUpdateTocOne);

        mBookMultiThreadUpdateOne.setText("多线程更新本书");
        mBookMultiThreadUpdateOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookMultiThreadUpdateOneActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBookMultiThreadUpdateOne);
        jPopupMenuBook.add(jSeparator3);

        mBookInfoEditor.setMnemonic('e');
        mBookInfoEditor.setText("编辑本书信息(E)");
        mBookInfoEditor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookInfoEditorActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBookInfoEditor);

        mPageUpdateOne.setMnemonic('g');
        mPageUpdateOne.setText("更新本章(G)");
        mPageUpdateOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPageUpdateOneActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPageUpdateOne);

        mPageDeleteMulti.setMnemonic('d');
        mPageDeleteMulti.setText("删除选择的章节(D)");
        mPageDeleteMulti.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPageDeleteMultiActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPageDeleteMulti);

        mPageDeleteMultiNotUpdate.setMnemonic('b');
        mPageDeleteMultiNotUpdate.setText("删除选择的章节并不写入删除列表(B)");
        mPageDeleteMultiNotUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPageDeleteMultiNotUpdateActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPageDeleteMultiNotUpdate);

        editBookInfo.setTitle("编辑信息");
        editBookInfo.setLocationByPlatform(true);

        javax.swing.GroupLayout editBookInfoLayout = new javax.swing.GroupLayout(editBookInfo.getContentPane());
        editBookInfo.getContentPane().setLayout(editBookInfoLayout);
        editBookInfoLayout.setHorizontalGroup(
            editBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        editBookInfoLayout.setVerticalGroup(
            editBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Hello");
        setBounds(new java.awt.Rectangle(0, 0, 0, 0));
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jSplitPane1.setDividerLocation(250);

        uBook.setModel(tBook);
        uBook.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        uBook.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                uBookMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(uBook);
        uBook.getAccessibleContext().setAccessibleName("");

        jSplitPane1.setLeftComponent(jScrollPane3);

        uPage.setModel(tPage);
        uPage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                uPageMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(uPage);

        jSplitPane1.setRightComponent(jScrollPane1);

        jMenu1.setMnemonic('B');
        jMenu1.setText("书籍(B)");

        mBookNew.setMnemonic('n');
        mBookNew.setText("新增书籍(N)");
        mBookNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookNewActionPerformed(evt);
            }
        });
        jMenu1.add(mBookNew);

        mRefreshBookList.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mRefreshBookList.setText("刷新书籍列表");
        mRefreshBookList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mRefreshBookListActionPerformed(evt);
            }
        });
        jMenu1.add(mRefreshBookList);
        jMenu1.add(jSeparator2);

        mBookShowAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        mBookShowAll.setText("显示所有");
        mBookShowAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookShowAllActionPerformed(evt);
            }
        });
        jMenu1.add(mBookShowAll);

        mBookUpdateAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.ALT_MASK));
        mBookUpdateAll.setText("更新所有");
        mBookUpdateAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookUpdateAllActionPerformed(evt);
            }
        });
        jMenu1.add(mBookUpdateAll);

        jMenuBar1.add(jMenu1);

        jMenu2.setMnemonic('x');
        jMenu2.setText("页面(X)");
        jMenuBar1.add(jMenu2);

        jMenu3.setMnemonic('z');
        jMenu3.setText("数据库(Z)");

        mDBSortDesc.setText("按书籍页数倒序排列");
        mDBSortDesc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBSortDescActionPerformed(evt);
            }
        });
        jMenu3.add(mDBSortDesc);

        mDBSortAsc.setText("按书籍页数顺序排列");
        mDBSortAsc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBSortAscActionPerformed(evt);
            }
        });
        jMenu3.add(mDBSortAsc);

        mDBRegenPageIDs.setText("重新生成页面ID");
        mDBRegenPageIDs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBRegenPageIDsActionPerformed(evt);
            }
        });
        jMenu3.add(mDBRegenPageIDs);
        jMenu3.add(jSeparator1);

        mDBVacuum.setText("缩小数据库");
        mDBVacuum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBVacuumActionPerformed(evt);
            }
        });
        jMenu3.add(mDBVacuum);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 804, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        oDB.closeDB();
    }//GEN-LAST:event_formWindowClosing

    private void uBookMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uBookMouseClicked
        // TODO add your handling code here:
        if (2 == evt.getClickCount()) {
            int nRow = uBook.getSelectedRow();
//            System.out.println(jTable3.getValueAt(nRow, 0));
            tPage.setRowCount(0);
            List rsdata = oDB.getList("select name as name, charcount as cc, id as id, bookid as bid, url as url from page where bookid=" + uBook.getValueAt(nRow, 2));
            Iterator itr = rsdata.iterator();
            while (itr.hasNext()) {
                HashMap item = (HashMap) itr.next();
                tPage.addRow(new Object[]{ item.get("name"), item.get("cc"), item.get("id"), item.get("bid"), item.get("url") }) ;
            }
        }
        if (java.awt.event.MouseEvent.BUTTON3 == evt.getButton()) {
            int nRow = uBook.rowAtPoint(evt.getPoint());
            uBook.setRowSelectionInterval(nRow, nRow);
            jPopupMenuBook.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_uBookMouseClicked

    private void uPageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uPageMouseClicked
        // TODO add your handling code here:
        if (2 == evt.getClickCount()) {
            int nRow = uPage.getSelectedRow();
            if (Integer.valueOf(uPage.getValueAt(nRow, 2).toString()) < 1) {
                return;
            }
            List xx = oDB.getList("select name as name, content as cc from page where id=" + uPage.getValueAt(nRow, 2).toString());
            HashMap mm = (HashMap) xx.get(0);

            if (null == mm.get("cc")) {
                return;
            }
            showPage sp = new showPage(mm.get("name").toString() + "\n\n" + mm.get("cc").toString());
            showContent.setContentPane(sp);
            showContent.setSize(sp.getPreferredSize());
            showContent.setVisible(true);
        }
        if (java.awt.event.MouseEvent.BUTTON3 == evt.getButton()) {
            int nSel = uPage.getSelectedRowCount();
//            System.out.println("选中数量: " + nSel);
            if ( nSel <= 1 ) { // 当选中1行时
                int nRow = uPage.rowAtPoint(evt.getPoint());
                uPage.setRowSelectionInterval(nRow, nRow);
            }
            jPopupMenuPage.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_uPageMouseClicked

    private void mBookShowAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookShowAllActionPerformed
        // TODO add your handling code here:
        tPage.setRowCount(0);
        List rsdata = oDB.getList("select page.name as name, page.CharCount as cc, page.ID as id, book.name as bname, page.url as url from book,Page where book.id=page.bookid order by page.bookid,page.ID");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            tPage.addRow(new Object[]{ item.get("name"), item.get("cc"), item.get("id"), item.get("bname"), item.get("url") }) ;
        }
    }//GEN-LAST:event_mBookShowAllActionPerformed

    private void mBookUpdateOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookUpdateOneActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookName = uBook.getValueAt(nRow, 0).toString();
        String nBookID = uBook.getValueAt(nRow, 2).toString();
        String nURL = uBook.getValueAt(nRow, 3).toString();
        
        new Thread(new UpdateOneBook(Integer.valueOf(nBookID), nURL, nBookName, true)).start();

    }//GEN-LAST:event_mBookUpdateOneActionPerformed

    private void mPageUpdateOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageUpdateOneActionPerformed
        // TODO add your handling code here:
        int nRow = uPage.getSelectedRow();
        String nPageName = uPage.getValueAt(nRow, 0).toString();
        String nPageID = uPage.getValueAt(nRow, 2).toString();
        String nPageURL = uPage.getValueAt(nRow, 4).toString();

        FoxBookLib.updatepage(Integer.valueOf(nPageID), oDB);
        System.out.println("更新: " + nPageName + " : " + nPageURL);
    }//GEN-LAST:event_mPageUpdateOneActionPerformed

    private void mBookUpdateAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookUpdateAllActionPerformed
        // TODO add your handling code here:
        new Thread(new UpdateAllBook()).start();
    }//GEN-LAST:event_mBookUpdateAllActionPerformed

    public void deleteSelectedPages(boolean bUpdateDelList) { // 删除选定章节
        int[] nRow = uPage.getSelectedRows(); // 选中的所有行号

        String nPageName;
        Integer nPageID;
        for (int n = 0; n < nRow.length; n++) {  // 删除数据库，并更新dellist
            nPageName = uPage.getValueAt(nRow[n], 0).toString();
            nPageID = Integer.valueOf(uPage.getValueAt(nRow[n], 2).toString());
            FoxBookDB.deletePage(nPageID, bUpdateDelList, oDB);
            System.out.println("已删除章节: " + nPageName);
        }

        for (int i = nRow.length - 1; i >= 0; i--) { // 倒序删除 LV 中显示条目
            tPage.removeRow(nRow[i]);
        }

        System.out.println("已删除章节数: " + nRow.length);
        refreshBookList();
    }
    
    private void mPageDeleteMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageDeleteMultiActionPerformed
        // TODO add your handling code here:
        deleteSelectedPages(true) ;
    }//GEN-LAST:event_mPageDeleteMultiActionPerformed

    private void mPageDeleteMultiNotUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageDeleteMultiNotUpdateActionPerformed
        // TODO add your handling code here:
        deleteSelectedPages(false) ;
    }//GEN-LAST:event_mPageDeleteMultiNotUpdateActionPerformed

    private void mDBSortDescActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBSortDescActionPerformed
        // TODO add your handling code here:
        FoxBookDB.regenID(2, oDB);
        refreshBookList();
    }//GEN-LAST:event_mDBSortDescActionPerformed

    private void mDBSortAscActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBSortAscActionPerformed
        // TODO add your handling code here:
        FoxBookDB.regenID(1, oDB);
        refreshBookList();
    }//GEN-LAST:event_mDBSortAscActionPerformed

    private void mDBRegenPageIDsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBRegenPageIDsActionPerformed
        // TODO add your handling code here:
        FoxBookDB.regenID(9, oDB);
        tPage.setRowCount(0); // 清空uPage
        refreshBookList();
        tPage.addRow(new Object[]{"★已重新生成页面ID"});
    }//GEN-LAST:event_mDBRegenPageIDsActionPerformed

    private void mDBVacuumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBVacuumActionPerformed
        // TODO add your handling code here:
        FoxBookDB.vacuumDB(oDB);
        System.out.println("已缩小数据库");
        tPage.addRow(new Object[]{"★已缩小数据库"});
    }//GEN-LAST:event_mDBVacuumActionPerformed

    private void mBookUpdateTocOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookUpdateTocOneActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookName = uBook.getValueAt(nRow, 0).toString();
        String nBookID = uBook.getValueAt(nRow, 2).toString();
        String nURL = uBook.getValueAt(nRow, 3).toString();
        
        new Thread(new UpdateOneBook(Integer.valueOf(nBookID), nURL, nBookName, false)).start();

    }//GEN-LAST:event_mBookUpdateTocOneActionPerformed

    private void mBookMultiThreadUpdateOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookMultiThreadUpdateOneActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookName = uBook.getValueAt(nRow, 0).toString();
        String nBookID = uBook.getValueAt(nRow, 2).toString();
        String nURL = uBook.getValueAt(nRow, 3).toString();
        
        new Thread(new MultiThreadUpdateOneBook(Integer.valueOf(nBookID), nURL, nBookName)).start();
    }//GEN-LAST:event_mBookMultiThreadUpdateOneActionPerformed

    private void mBookInfoEditorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookInfoEditorActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookName = uBook.getValueAt(nRow, 0).toString();
        String nBookID = uBook.getValueAt(nRow, 2).toString();

        bookInfoEditor edtBI = new bookInfoEditor(Integer.valueOf(nBookID), oDB, editBookInfo);
        editBookInfo.setContentPane(edtBI);
        editBookInfo.setSize(edtBI.getPreferredSize());
        editBookInfo.setVisible(true);
    }//GEN-LAST:event_mBookInfoEditorActionPerformed

    private void mBookNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookNewActionPerformed
        // TODO add your handling code here:
        bookInfoEditor edtBI = new bookInfoEditor(0, oDB, editBookInfo);
        editBookInfo.setContentPane(edtBI);
        editBookInfo.setSize(edtBI.getPreferredSize());
        editBookInfo.setVisible(true);
    }//GEN-LAST:event_mBookNewActionPerformed

    private void mRefreshBookListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mRefreshBookListActionPerformed
        // TODO add your handling code here:
        refreshBookList();
    }//GEN-LAST:event_mRefreshBookListActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        /*
         private static Set<String> NIMBUS_PRIMARY_COLORS = new HashSet<String>(Arrays.asList(
         "text", "control", "nimbusBase", "nimbusOrange", "nimbusGreen", "nimbusRed", "nimbusInfoBlue",
         "nimbusAlertYellow", "nimbusFocus", "nimbusSelectedText", "nimbusSelectionBackground",
         "nimbusDisabledText", "nimbusLightBackground", "info"));
         private static Set<String> NIMBUS_SECONDARY_COLORS = new HashSet<String>(Arrays.asList(
         "textForeground", "textBackground", "background",
         "nimbusBlueGrey", "nimbusBorder", "nimbusSelection", "infoText", "menuText", "menu", "scrollbar",
         "controlText", "controlHighlight", "controlLHighlight", "controlShadow", "controlDkShadow", "textHighlight",
         "textHighlightText", "textInactiveText", "desktop", "activeCaption", "inactiveCaption"));
         private static String[] NIMBUS_COMPONENTS = new String[]{
         "ArrowButton", "Button", "ToggleButton", "RadioButton", "CheckBox", "ColorChooser", "ComboBox",
         "\"ComboBox.scrollPane\"", "FileChooser", "InternalFrameTitlePane", "InternalFrame", "DesktopIcon",
         "DesktopPane", "Label", "List", "MenuBar", "MenuItem", "RadioButtonMenuItem", "CheckBoxMenuItem", "Menu",
         "PopupMenu", "PopupMenuSeparator", "OptionPane", "Panel", "ProgressBar", "Separator", "ScrollBar",
         "ScrollPane", "Viewport", "Slider", "Spinner", "SplitPane", "TabbedPane", "Table", "TableHeader",
         "\"Table.editor\"", "\"Tree.cellEditor\"", "TextField", "FormattedTextField", "PasswordField", "TextArea",
         "TextPane", "EditorPane", "ToolBar", "ToolBarSeparator", "ToolTip", "Tree", "RootPane"};
         */
        /*
         javax.swing.UIManager.put("nimbusBase", new Color(160, 222, 181));
         javax.swing.UIManager.put("nimbusBlueGrey", new Color(160, 222, 181));
         javax.swing.UIManager.put("control", new Color(160, 222, 181));
         javax.swing.UIManager.put("textText", Color.WHITE);
         */
        javax.swing.UIManager.put("nimbusBlueGrey", new Color(179, 219, 179));        //控件色
        javax.swing.UIManager.put("nimbusLightBackground", new Color(228, 242, 228)); // 文本背景色
        javax.swing.UIManager.put("control", new Color(228, 242, 228));               // 控件背景色
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                /* 
                 Metal
                 Nimbus
                 CDE/Motif
                 Windows
                 Windows Classic
                 */
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FoxMainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FoxMainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FoxMainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FoxMainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FoxMainFrame().setVisible(true);
            }
        });
    }
    private FoxDB oDB;
    private javax.swing.table.DefaultTableModel tBook;
    private javax.swing.table.DefaultTableModel tPage;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog editBookInfo;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPopupMenu jPopupMenuBook;
    private javax.swing.JPopupMenu jPopupMenuPage;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuItem mBookInfoEditor;
    private javax.swing.JMenuItem mBookMultiThreadUpdateOne;
    private javax.swing.JMenuItem mBookNew;
    private javax.swing.JMenuItem mBookShowAll;
    private javax.swing.JMenuItem mBookUpdateAll;
    private javax.swing.JMenuItem mBookUpdateOne;
    private javax.swing.JMenuItem mBookUpdateTocOne;
    private javax.swing.JMenuItem mDBRegenPageIDs;
    private javax.swing.JMenuItem mDBSortAsc;
    private javax.swing.JMenuItem mDBSortDesc;
    private javax.swing.JMenuItem mDBVacuum;
    private javax.swing.JMenuItem mPageDeleteMulti;
    private javax.swing.JMenuItem mPageDeleteMultiNotUpdate;
    private javax.swing.JMenuItem mPageUpdateOne;
    private javax.swing.JMenuItem mRefreshBookList;
    private javax.swing.JDialog showContent;
    private javax.swing.JTable uBook;
    private javax.swing.JTable uPage;
    // End of variables declaration//GEN-END:variables
}
