/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import static com.linpinger.foxbook.FoxBookLib.getFullURL;
import java.awt.Color;
import java.io.File;
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

	private void msg(String inMsg) {
		msg.setText(inMsg);
//		tPage.addRow(new Object[]{inMsg});
	}

    public class UpdateAllBook implements Runnable { // GUI菜单更新所有书籍

        public void run() {
            List upList = oDB.getList("select id as id, name as name, url as url from book where isEnd is null or isEnd != 1");

            Iterator itr = upList.iterator();
            List<Thread> threadList = new ArrayList(30);
            Thread nowT;
            while (itr.hasNext()) {
                HashMap item = (HashMap<String, String>) itr.next();
                nowT = new Thread(new UpdateBook((Integer) item.get("id"), (String) item.get("url"), (String) item.get("name"), true));
                //       System.out.println("线程 " + nowT.getName() + " 更新:" + (String) item.get("name"));
                threadList.add(nowT);
                nowT.start();
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tPage.setRowCount(0);
                    msg("★　开始更新所有书籍，请耐心等待...");
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
                    msg.setForeground(Color.BLUE);
                    msg("★　全部线程更新完毕，恭喜");
                }
            });
            refreshBookList();
            System.out.println("全部线程完毕，恭喜");
        }
    }

    public class UpdateOneBook implements Runnable { // GUI菜单更新一本书籍

        private String nBookName;
        private String nURL;
        private int nBookID;
        private boolean bWritePage = true;

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
                    if (bWritePage) {
                        msg("★　开始更新本书: " + nBookName);
                    } else {
                        msg("★　开始更新本书目录: " + nBookName);
                    }
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
                    if (bWritePage) {
                        msg("★　本书更新完毕: " + nBookName);
                    } else {
                        msg("★　本书目录更新完毕: " + nBookName);
                    }
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
                    msg("★　开始不受控制滴多线程更新本书: " + nBookName);
                }
            });

            Thread nowUP = new Thread(new UpdateBook(nBookID, nURL, nBookName, 9));
            nowUP.start();
            try {
                nowUP.join();
            } catch (InterruptedException ex) {
				ex.toString();
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
                FoxBookDB.inserNewPages(lData, bookID, oDB); //写入数据库
            }
//            System.out.println("任务数:" + cTask);

            if (bDownPage) {
                // 获取新增章节
                lData = oDB.getList("select id as id, name as name, url as url from page where ( bookid=" + bookID + " ) and ( (content is null) or ( length(content) < 9 ) )");
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
        this.setLocationRelativeTo(null); // 屏幕居中显示
    }

    public void refreshBookList() {
        tBook.setRowCount(0);
        List rsdata = oDB.getList("select book.Name as name,count(page.id) as cc,book.ID as id,book.URL as url,book.isEnd as isend from Book left join page on book.id=page.bookid group by book.id order by book.DisOrder ;");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            tBook.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("url")});
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
        mBookDelete = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        mBook2Mobi = new javax.swing.JMenuItem();
        mBook2Epub = new javax.swing.JMenuItem();
        mBook2Txt = new javax.swing.JMenuItem();
        jPopupMenuPage = new javax.swing.JPopupMenu();
        mPageUpdateOne = new javax.swing.JMenuItem();
        mPageDeleteMulti = new javax.swing.JMenuItem();
        mPageDeleteMultiNotUpdate = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        mPages2Mobi = new javax.swing.JMenuItem();
        mPages2Epub = new javax.swing.JMenuItem();
        mPages2txt = new javax.swing.JMenuItem();
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
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        mAll2Mobi = new javax.swing.JMenuItem();
        mAll2Epub = new javax.swing.JMenuItem();
        mAll2Txt = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        mDBSortAsc = new javax.swing.JMenuItem();
        mDBSortDesc = new javax.swing.JMenuItem();
        mDBRegenPageIDs = new javax.swing.JMenuItem();
        mDBSimpAllDelList = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mDBVacuum = new javax.swing.JMenuItem();
        mDBSwich = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        mDBQuickA = new javax.swing.JMenuItem();
        msg = new javax.swing.JMenu();

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

        mBookDelete.setText("删除本书");
        mBookDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookDeleteActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBookDelete);
        jPopupMenuBook.add(jSeparator4);

        mBook2Mobi.setText("本书转为mobi");
        mBook2Mobi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBook2MobiActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBook2Mobi);

        mBook2Epub.setText("本书转为epub");
        mBook2Epub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBook2EpubActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBook2Epub);

        mBook2Txt.setText("本书转为txt");
        mBook2Txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBook2TxtActionPerformed(evt);
            }
        });
        jPopupMenuBook.add(mBook2Txt);

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
        jPopupMenuPage.add(jSeparator5);

        mPages2Mobi.setText("选中章节转为mobi");
        mPages2Mobi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPages2MobiActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPages2Mobi);

        mPages2Epub.setText("选中章节转为epub");
        mPages2Epub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPages2EpubActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPages2Epub);

        mPages2txt.setText("选中章节转为txt");
        mPages2txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPages2txtActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPages2txt);

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

        jMenuBar1.setFont(new java.awt.Font("宋体", 1, 12)); // NOI18N

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
        jMenu1.add(jSeparator6);

        mAll2Mobi.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.ALT_MASK));
        mAll2Mobi.setText("所有转为 Mobi");
        mAll2Mobi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mAll2MobiActionPerformed(evt);
            }
        });
        jMenu1.add(mAll2Mobi);

        mAll2Epub.setText("所有转为 Epub");
        mAll2Epub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mAll2EpubActionPerformed(evt);
            }
        });
        jMenu1.add(mAll2Epub);

        mAll2Txt.setText("所有转为 Txt");
        mAll2Txt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mAll2TxtActionPerformed(evt);
            }
        });
        jMenu1.add(mAll2Txt);

        jMenuBar1.add(jMenu1);

        jMenu2.setMnemonic('z');
        jMenu2.setText("数据库(Z)");

        mDBSortAsc.setText("按书籍页数顺序排列");
        mDBSortAsc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBSortAscActionPerformed(evt);
            }
        });
        jMenu2.add(mDBSortAsc);

        mDBSortDesc.setText("按书籍页数倒序排列");
        mDBSortDesc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBSortDescActionPerformed(evt);
            }
        });
        jMenu2.add(mDBSortDesc);

        mDBRegenPageIDs.setText("重新生成页面ID");
        mDBRegenPageIDs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBRegenPageIDsActionPerformed(evt);
            }
        });
        jMenu2.add(mDBRegenPageIDs);

        mDBSimpAllDelList.setText("精简所有DelList");
        mDBSimpAllDelList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBSimpAllDelListActionPerformed(evt);
            }
        });
        jMenu2.add(mDBSimpAllDelList);
        jMenu2.add(jSeparator1);

        mDBVacuum.setText("缩小数据库");
        mDBVacuum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBVacuumActionPerformed(evt);
            }
        });
        jMenu2.add(mDBVacuum);

        mDBSwich.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        mDBSwich.setMnemonic('s');
        mDBSwich.setText("切换数据库(S)");
        mDBSwich.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBSwichActionPerformed(evt);
            }
        });
        jMenu2.add(mDBSwich);
        jMenu2.add(jSeparator7);

        mDBQuickA.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.ALT_MASK));
        mDBQuickA.setText("快捷倒序缩小数据库");
        mDBQuickA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBQuickAActionPerformed(evt);
            }
        });
        jMenu2.add(mDBQuickA);

        jMenuBar1.add(jMenu2);

        msg.setForeground(new java.awt.Color(0, 0, 255));
        msg.setText("★　FoxBook Java Swing 版  作者: 爱尔兰之狐  Ver: 2014-09-17");
        msg.setToolTipText("★　哈哈我是消息栏");
        msg.setEnabled(false);
        msg.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jMenuBar1.add(msg);

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
        if (2 == evt.getClickCount()) {
            int nRow = uBook.getSelectedRow();
            msg("★　" + uBook.getValueAt(nRow, 0) + "　" + uBook.getValueAt(nRow, 3));
            tPage.setRowCount(0);
            List rsdata = oDB.getList("select name as name, charcount as cc, id as id, bookid as bid, url as url from page where bookid=" + uBook.getValueAt(nRow, 2));
            Iterator itr = rsdata.iterator();
            while (itr.hasNext()) {
                HashMap item = (HashMap) itr.next();
                tPage.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("bid"), item.get("url")});
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
            msg("★　查看: " + uPage.getValueAt(nRow, 0) + "　" + uPage.getValueAt(nRow, 4));
            List xx = oDB.getList("select name as name, content as cc from page where id=" + uPage.getValueAt(nRow, 2).toString());
            HashMap mm = (HashMap) xx.get(0);

            if (null == mm.get("cc")) {
                return;
            }
            Jpanel_ShowPage sp = new Jpanel_ShowPage(mm.get("name").toString() + "\n\n" + mm.get("cc").toString(), showContent);
            showContent.setContentPane(sp);
            showContent.setSize(sp.getPreferredSize());
            showContent.setLocationRelativeTo(null);
            showContent.setVisible(true);
        }
        if (java.awt.event.MouseEvent.BUTTON3 == evt.getButton()) {
            int nSel = uPage.getSelectedRowCount();
//            System.out.println("选中数量: " + nSel);
            if (nSel <= 1) { // 当选中1行时
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
            tPage.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("bname"), item.get("url")});
        }
        msg("★　章节总数: " + rsdata.size());
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
        msg.setForeground(Color.GREEN);
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
//            System.out.println("已删除章节: " + nPageName);
        }

        for (int i = nRow.length - 1; i >= 0; i--) { // 倒序删除 LV 中显示条目
            tPage.removeRow(nRow[i]);
        }

        System.out.println("已删除章节数: " + nRow.length);
        refreshBookList();
    }

    private void mPageDeleteMultiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageDeleteMultiActionPerformed
        // TODO add your handling code here:
        deleteSelectedPages(true);
    }//GEN-LAST:event_mPageDeleteMultiActionPerformed

    private void mPageDeleteMultiNotUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageDeleteMultiNotUpdateActionPerformed
        // TODO add your handling code here:
        deleteSelectedPages(false);
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
        msg("★　已重新生成页面ID");
    }//GEN-LAST:event_mDBRegenPageIDsActionPerformed

    private void mDBVacuumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBVacuumActionPerformed
        // TODO add your handling code here:
        tPage.setRowCount(0);
        double subSize = oDB.vacuumDB();
        msg("★　已缩小数据库: " + subSize + " K");
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

        JPanel_BookInfoEditor edtBI = new JPanel_BookInfoEditor(Integer.valueOf(nBookID), oDB, editBookInfo);
        editBookInfo.setContentPane(edtBI);
        editBookInfo.setSize(edtBI.getPreferredSize());
        editBookInfo.setLocationRelativeTo(null);
        editBookInfo.setVisible(true);
    }//GEN-LAST:event_mBookInfoEditorActionPerformed

    private void mBookNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookNewActionPerformed
        // TODO add your handling code here:
        JPanel_BookInfoEditor edtBI = new JPanel_BookInfoEditor(0, oDB, editBookInfo);
        editBookInfo.setContentPane(edtBI);
        editBookInfo.setSize(edtBI.getPreferredSize());
        editBookInfo.setLocationRelativeTo(null);
        editBookInfo.setVisible(true);
    }//GEN-LAST:event_mBookNewActionPerformed

    private void mRefreshBookListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mRefreshBookListActionPerformed
        // TODO add your handling code here:
        refreshBookList();
    }//GEN-LAST:event_mRefreshBookListActionPerformed

    private void mBookDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookDeleteActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        //       String nBookName = uBook.getValueAt(nRow, 0).toString();
        String nBookID = uBook.getValueAt(nRow, 2).toString();
        oDB.exec("Delete From Page where BookID = " + nBookID);
        oDB.exec("Delete From Book where ID = " + nBookID);
        refreshBookList();
    }//GEN-LAST:event_mBookDeleteActionPerformed

    private void mDBSwichActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBSwichActionPerformed
        // TODO add your handling code here:
        tPage.setRowCount(0);
        String nowDB = oDB.switchDB();
        refreshBookList();
        msg("★　切换到: " + nowDB);
    }//GEN-LAST:event_mDBSwichActionPerformed

    public class book2ebook implements Runnable {

        private int transType = 0; // 1:mobi 2:epub 9:txt
        private int transMode = 9; // 0:all pages 1:selected pages 2:one book
        private int bookid = 0;
        private String pageids = "";

        book2ebook(int inTransType, int inBookIDorMode) {
            this.transType = inTransType;
            if (inBookIDorMode == 0) {
                this.transMode = 0;
            } else {
                this.transMode = 2;
                this.bookid = inBookIDorMode;
            }
        }

        book2ebook(int inTransType, String inPageIDs) {
            this.transType = inTransType;
            this.transMode = 1;
            this.pageids = inPageIDs;
        }

        public void run() {
            long sTime = System.currentTimeMillis();
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
			if ( transType != 9 ) {
            if (transMode != 2 ) { // 0:all pages 1:selected pages 2:one book
                String sst = FoxBookDB.getSiteType(oDB);
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
                    FoxBookLib.all2Epub(data, oBookName, outDir + fBookName + ".mobi");
                    break;
                case 2:
                    FoxBookLib.all2Epub(data, oBookName, outDir + fBookName + ".epub");
                    break;
                case 9:
                    if (transMode == 2) {
                        FoxBookLib.all2txt(data, true);
                    } else {
                        FoxBookLib.all2txt(data, false);
                    }
                    break;
            }
            final long eTime = System.currentTimeMillis() - sTime;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    msg.setForeground(Color.BLUE);
                    msg("★　转换完毕，耗时(ms): " + eTime);
                }
            });
        }
    }
    private void mBook2TxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBook2TxtActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookID = uBook.getValueAt(nRow, 2).toString();

        tPage.setRowCount(0);
        msg("★　开始转换本书为 txt");
        new Thread(new book2ebook(9, Integer.valueOf(nBookID))).start();
    }//GEN-LAST:event_mBook2TxtActionPerformed

    private void mPages2txtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPages2txtActionPerformed
        // TODO add your handling code here:
        int[] nRow = uPage.getSelectedRows(); // 选中的所有行号

        StringBuilder ids = new StringBuilder(10240);
        int nMax = nRow.length - 1;
        for (int n = 0; n < nMax; n++) {  // 删除数据库，并更新dellist
            ids.append(uPage.getValueAt(nRow[n], 2)).append(", ");
        }
        ids.append(uPage.getValueAt(nRow[nMax], 2));
        //       System.out.println(ids.toString());

        tPage.setRowCount(0);
        msg("★　开始转换选定章节为 txt");
        new Thread(new book2ebook(9, ids.toString())).start();
    }//GEN-LAST:event_mPages2txtActionPerformed

    private void mBook2MobiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBook2MobiActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookID = uBook.getValueAt(nRow, 2).toString();

        tPage.setRowCount(0);
        msg("★　开始转换本书为 mobi");
        new Thread(new book2ebook(1, Integer.valueOf(nBookID))).start();
    }//GEN-LAST:event_mBook2MobiActionPerformed

    private void mPages2MobiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPages2MobiActionPerformed
        // TODO add your handling code here:
        int[] nRow = uPage.getSelectedRows(); // 选中的所有行号

        StringBuilder ids = new StringBuilder(10240);
        int nMax = nRow.length - 1;
        for (int n = 0; n < nMax; n++) {  // 删除数据库，并更新dellist
            ids.append(uPage.getValueAt(nRow[n], 2)).append(", ");
        }
        ids.append(uPage.getValueAt(nRow[nMax], 2));
        //       System.out.println(ids.toString());

        tPage.setRowCount(0);
        msg("★　开始转换选定章节为 mobi");
        new Thread(new book2ebook(1, ids.toString())).start();
    }//GEN-LAST:event_mPages2MobiActionPerformed

    private void mBook2EpubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBook2EpubActionPerformed
        // TODO add your handling code here:
        int nRow = uBook.getSelectedRow();
        String nBookID = uBook.getValueAt(nRow, 2).toString();

        tPage.setRowCount(0);
        msg("★　开始转换本书为 epub");
        new Thread(new book2ebook(2, Integer.valueOf(nBookID))).start();
    }//GEN-LAST:event_mBook2EpubActionPerformed

    private void mPages2EpubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPages2EpubActionPerformed
        // TODO add your handling code here:
        int[] nRow = uPage.getSelectedRows(); // 选中的所有行号

        StringBuilder ids = new StringBuilder(10240);
        int nMax = nRow.length - 1;
        for (int n = 0; n < nMax; n++) {  // 删除数据库，并更新dellist
            ids.append(uPage.getValueAt(nRow[n], 2)).append(", ");
        }
        ids.append(uPage.getValueAt(nRow[nMax], 2));
        tPage.setRowCount(0);
        msg("★　开始转换选定章节为 epub");
        new Thread(new book2ebook(2, ids.toString())).start();
    }//GEN-LAST:event_mPages2EpubActionPerformed

    private void mAll2MobiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mAll2MobiActionPerformed
        // TODO add your handling code here:
        tPage.setRowCount(0);
        msg.setForeground(Color.GREEN);
        msg("★　开始转换所有章节为 mobi");
        new Thread(new book2ebook(1, 0)).start();
    }//GEN-LAST:event_mAll2MobiActionPerformed

    private void mAll2EpubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mAll2EpubActionPerformed
        // TODO add your handling code here:
        tPage.setRowCount(0);
        msg("★　开始转换所有章节为 epub");
        new Thread(new book2ebook(2, 0)).start();
    }//GEN-LAST:event_mAll2EpubActionPerformed

    private void mAll2TxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mAll2TxtActionPerformed
        // TODO add your handling code here:
        tPage.setRowCount(0);
        msg("★　开始转换所有章节为 txt");
        new Thread(new book2ebook(9, 0)).start();
    }//GEN-LAST:event_mAll2TxtActionPerformed

    private void mDBQuickAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBQuickAActionPerformed
        // TODO add your handling code here:
        long sTime = System.currentTimeMillis();
        msg("★　开始倒序所有书籍...");
        FoxBookDB.regenID(2, oDB);
        refreshBookList();
        tPage.setRowCount(0);
        msg("★　开始重新生成pageID");
        FoxBookDB.regenID(9, oDB);
        msg("★　开始精简所有DelList");
        FoxBookDB.simplifyAllDelList(oDB);
        msg("★　开始缩小数据库...");
        double subSize = oDB.vacuumDB();
        long eTime = System.currentTimeMillis() - sTime;
        msg("★　已完成倒序并缩小数据库: " + subSize + " K   耗时(ms): " + eTime);
    }//GEN-LAST:event_mDBQuickAActionPerformed

    private void mDBSimpAllDelListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBSimpAllDelListActionPerformed
        msg("★　开始精简所有DelList");
        long sTime = System.currentTimeMillis();
        FoxBookDB.simplifyAllDelList(oDB);
        long eTime = System.currentTimeMillis() - sTime;
        msg("★　已精简所有DelList   耗时(ms): " + eTime);
    }//GEN-LAST:event_mDBSimpAllDelListActionPerformed

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
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPopupMenu jPopupMenuBook;
    private javax.swing.JPopupMenu jPopupMenuPage;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuItem mAll2Epub;
    private javax.swing.JMenuItem mAll2Mobi;
    private javax.swing.JMenuItem mAll2Txt;
    private javax.swing.JMenuItem mBook2Epub;
    private javax.swing.JMenuItem mBook2Mobi;
    private javax.swing.JMenuItem mBook2Txt;
    private javax.swing.JMenuItem mBookDelete;
    private javax.swing.JMenuItem mBookInfoEditor;
    private javax.swing.JMenuItem mBookMultiThreadUpdateOne;
    private javax.swing.JMenuItem mBookNew;
    private javax.swing.JMenuItem mBookShowAll;
    private javax.swing.JMenuItem mBookUpdateAll;
    private javax.swing.JMenuItem mBookUpdateOne;
    private javax.swing.JMenuItem mBookUpdateTocOne;
    private javax.swing.JMenuItem mDBQuickA;
    private javax.swing.JMenuItem mDBRegenPageIDs;
    private javax.swing.JMenuItem mDBSimpAllDelList;
    private javax.swing.JMenuItem mDBSortAsc;
    private javax.swing.JMenuItem mDBSortDesc;
    private javax.swing.JMenuItem mDBSwich;
    private javax.swing.JMenuItem mDBVacuum;
    private javax.swing.JMenuItem mPageDeleteMulti;
    private javax.swing.JMenuItem mPageDeleteMultiNotUpdate;
    private javax.swing.JMenuItem mPageUpdateOne;
    private javax.swing.JMenuItem mPages2Epub;
    private javax.swing.JMenuItem mPages2Mobi;
    private javax.swing.JMenuItem mPages2txt;
    private javax.swing.JMenuItem mRefreshBookList;
    private javax.swing.JMenu msg;
    private javax.swing.JDialog showContent;
    private javax.swing.JTable uBook;
    private javax.swing.JTable uPage;
    // End of variables declaration//GEN-END:variables
}
