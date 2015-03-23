/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.linpinger.foxbook;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

public class FoxMainFrame extends javax.swing.JFrame {
    //	private final int SITE_EASOU = 11 ;

    private final int SITE_ZSSQ = 12;
    private final int SITE_KUAIDU = 13;
    public final int downThread = 9;  // 页面下载任务线程数
    public int leftThread = downThread;

    private void msg(String inMsg) {
        msg.setText(inMsg);
//	tPage.addRow(new Object[]{inMsg});
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
                ex.toString();
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

                pageLen = FoxBookLib.updatepage(nowID, oDB);

                final Object data[] = new Object[]{mm.get("name"), pageLen, mm.get("id"), thName, mm.get("url")};
                final int xcount = locCount;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        msg("★　剩余线程:线程名:页面/页面数　" + leftThread + " : " + thName + " : " + xcount + " / " + allCount);
                        tPage.addRow(data);
                    }
                });

            }
            --leftThread;
            if (0 == leftThread) { // 所有线程更新完毕
//                msg.obj = "已更新完所有空白章节>25";
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
//                        tPage.setRowCount(0);
                        msg("★　恭喜！理论上多线程更新完毕^_^");
                    }
                });
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

                        pageLen = FoxBookLib.updatepage(FoxBookLib.getFullURL(bookUrl, nowURL), nowpageid, oDB);

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
        this.setLocationRelativeTo(null); // 屏幕居中显示

        // 设置LV列宽度
        TableColumnModel tcmL = uBook.getTableHeader().getColumnModel();
        tcmL.getColumn(0).setPreferredWidth(200);
        tcmL.getColumn(2).setPreferredWidth(40);
        
        TableColumnModel tcmR = uPage.getTableHeader().getColumnModel();
        tcmR.getColumn(0).setPreferredWidth(300);
        tcmR.getColumn(2).setPreferredWidth(40);
        tcmR.getColumn(3).setPreferredWidth(150);
 
 
        // 设置宽度
        TableColumnModel tcmX = uList.getTableHeader().getColumnModel();
        tcmX.getColumn(0).setPreferredWidth(60);
        tcmX.getColumn(1).setPreferredWidth(400);
        tcmX.getColumn(2).setPreferredWidth(350);
        


        // { 显示内容
        jdShowContent.setSize(jdShowContent.getPreferredSize());
        jdShowContent.setLocationRelativeTo(null);

        // ESC 退出子窗口
        jdShowContent.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jdShowContent.dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // SPACE 翻页
        jdShowContent.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JScrollBar ss = jScrollPane2.getVerticalScrollBar();
                ss.setValue(ss.getValue() + ss.getBlockIncrement(JScrollBar.VERTICAL) - 25);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        // } 显示内容
        
        // { 编辑书籍信息
        jdEditBookInfo.setSize(jdEditBookInfo.getPreferredSize());
        jdEditBookInfo.setLocationRelativeTo(null);
        // ESC 退出子窗口
        jdEditBookInfo.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jdEditBookInfo.dispose();
 //               FoxDialogEditBookInfoSaveInfo(); // 退出保存到数据库
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // } 编辑书籍信息
        
        // { 搜索书籍
        jdSearchBook.setSize(jdSearchBook.getPreferredSize());
        jdSearchBook.setLocationRelativeTo(null);
        jdSearchBook.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jdSearchBook.dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        // } 搜索书籍
        
        // { 编辑章节信息
        jdEditPageInfo.setSize(jdEditPageInfo.getPreferredSize());
        jdEditPageInfo.setLocationRelativeTo(null);
        // ESC 退出子窗口
        jdEditPageInfo.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jdEditPageInfo.dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        // } 编辑章节信息
        
        // { 查找替换内容
        jdFindnReplace.setSize(jdFindnReplace.getPreferredSize());
        jdFindnReplace.setLocationRelativeTo(null);
        // ESC 退出子窗口
        jdFindnReplace.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jdFindnReplace.dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        // } 查找替换内容
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
        
        tList = new javax.swing.table.DefaultTableModel(null, new String[]{
            "类型", "URL", "标题"
        }) {
            boolean[] canEdit = new boolean[]{
                false, false, false
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
        mPageEditInfo = new javax.swing.JMenuItem();
        mPageDeleteMulti = new javax.swing.JMenuItem();
        mPageDeleteMultiNotUpdate = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        mPages2Mobi = new javax.swing.JMenuItem();
        mPages2Epub = new javax.swing.JMenuItem();
        mPages2txt = new javax.swing.JMenuItem();
        chooseTxt = new javax.swing.JFileChooser();
        jdEditBookInfo = new javax.swing.JDialog();
        jPanel1 = new javax.swing.JPanel();
        uBookID = new javax.swing.JLabel();
        uBookName = new javax.swing.JTextField();
        uBookSearch = new javax.swing.JButton();
        uQidianID = new javax.swing.JTextField();
        uQidianIDProc = new javax.swing.JButton();
        uBookURL = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        uDelListScrool = new javax.swing.JScrollPane();
        uDelList = new javax.swing.JTextArea();
        jdSearchBook = new javax.swing.JDialog();
        uSearchType = new javax.swing.JComboBox();
        uSearchString = new javax.swing.JComboBox();
        uSearchBookURL = new javax.swing.JTextField();
        uSearchIt = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        uList = new javax.swing.JTable();
        jdShowContent = new javax.swing.JDialog();
        jScrollPane2 = new javax.swing.JScrollPane();
        uPageContent = new javax.swing.JTextPane();
        jdEditPageInfo = new javax.swing.JDialog();
        jPanel2 = new javax.swing.JPanel();
        jlPID = new javax.swing.JLabel();
        jtfBID = new javax.swing.JTextField();
        jtfPName = new javax.swing.JTextField();
        jtfCharCount = new javax.swing.JTextField();
        jtfPURL = new javax.swing.JTextField();
        jbLeanPage = new javax.swing.JButton();
        jbSavePage = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jtaPageContent = new javax.swing.JTextArea();
        jdFindnReplace = new javax.swing.JDialog();
        jScrollPane6 = new javax.swing.JScrollPane();
        jtaReplaceSrc = new javax.swing.JTextArea();
        jScrollPane7 = new javax.swing.JScrollPane();
        jtaReplaceTar = new javax.swing.JTextArea();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        jButton3 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        uBook = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        uPage = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mBookNew = new javax.swing.JMenuItem();
        mRefreshBookList = new javax.swing.JMenuItem();
        mBookImportFromQidianTxt = new javax.swing.JMenuItem();
        mPageFindnReplace = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mBookShowAll = new javax.swing.JMenuItem();
        mBookShowSize = new javax.swing.JMenuItem();
        mBookShowLessLen = new javax.swing.JMenuItem();
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
        mDBQuickD = new javax.swing.JMenuItem();
        msg = new javax.swing.JMenu();

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

        mPageEditInfo.setMnemonic('e');
        mPageEditInfo.setText("编辑本章(E)");
        mPageEditInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPageEditInfoActionPerformed(evt);
            }
        });
        jPopupMenuPage.add(mPageEditInfo);

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

        jdEditBookInfo.setTitle("编辑信息");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "BookID | BookName |  QidianID | URL", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("宋体", 0, 12), new java.awt.Color(0, 0, 255))); // NOI18N

        uBookID.setText("xx");
        uBookID.setToolTipText("BookID");

        uBookName.setText("BookName");
        uBookName.setToolTipText("书名");

        uBookSearch.setMnemonic('d');
        uBookSearch.setText("搜D");
        uBookSearch.setToolTipText("搜索左侧小说名");
        uBookSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uBookSearchActionPerformed(evt);
            }
        });

        uQidianID.setText("QidianID");
        uQidianID.setToolTipText("起点书号，可在这里粘贴地址，按右边按钮得到");

        uQidianIDProc.setMnemonic('q');
        uQidianIDProc.setText("QD");
        uQidianIDProc.setToolTipText("将书籍地址复制到左边，按我来得到起点书号");
        uQidianIDProc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uQidianIDProcActionPerformed(evt);
            }
        });

        uBookURL.setText("URL");
        uBookURL.setToolTipText("目录页地址");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(uBookID, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(uBookName, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(uBookSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(uQidianID, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(uQidianIDProc))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(uBookURL, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(uBookID, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uBookName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uBookSearch)
                    .addComponent(uQidianID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uQidianIDProc))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uBookURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton7.setMnemonic('f');
        jButton7.setText("减肥(F)");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton8.setMnemonic('c');
        jButton8.setText("清空(C)");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jButton9.setMnemonic('s');
        jButton9.setText("保存(S)");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        uDelList.setColumns(20);
        uDelList.setRows(5);
        uDelListScrool.setViewportView(uDelList);

        javax.swing.GroupLayout jdEditBookInfoLayout = new javax.swing.GroupLayout(jdEditBookInfo.getContentPane());
        jdEditBookInfo.getContentPane().setLayout(jdEditBookInfoLayout);
        jdEditBookInfoLayout.setHorizontalGroup(
            jdEditBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdEditBookInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jdEditBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(uDelListScrool)
                    .addGroup(jdEditBookInfoLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jdEditBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton9, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jdEditBookInfoLayout.setVerticalGroup(
            jdEditBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdEditBookInfoLayout.createSequentialGroup()
                .addGroup(jdEditBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jdEditBookInfoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jdEditBookInfoLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jdEditBookInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jdEditBookInfoLayout.createSequentialGroup()
                                .addComponent(jButton7)
                                .addGap(18, 18, 18)
                                .addComponent(jButton8))
                            .addComponent(jButton9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uDelListScrool, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                .addContainerGap())
        );

        jdSearchBook.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        uSearchType.setMaximumRowCount(15);
        uSearchType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "S:起点中文", "S:追书神器", "S:快读", "S:宜搜", "E:SoGou", "E:GotoHell", "E:Yahoo", "E:Bing", "E:soso", "E:so", "E:ZhongSou", "E:youdao", "E:360" }));

        uSearchString.setEditable(true);
        uSearchString.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "书名", "书名 site:qidian.com" }));

        uSearchBookURL.setText("最终书籍URL，复制我哦");

        uSearchIt.setMnemonic('s');
        uSearchIt.setText("搜索(S)");
        uSearchIt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uSearchItActionPerformed(evt);
            }
        });

        uList.setModel(tList);
        uList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                uListMouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(uList);

        javax.swing.GroupLayout jdSearchBookLayout = new javax.swing.GroupLayout(jdSearchBook.getContentPane());
        jdSearchBook.getContentPane().setLayout(jdSearchBookLayout);
        jdSearchBookLayout.setHorizontalGroup(
            jdSearchBookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdSearchBookLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jdSearchBookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 849, Short.MAX_VALUE)
                    .addGroup(jdSearchBookLayout.createSequentialGroup()
                        .addComponent(uSearchType, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(uSearchString, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(uSearchBookURL)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(uSearchIt)))
                .addContainerGap())
        );
        jdSearchBookLayout.setVerticalGroup(
            jdSearchBookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdSearchBookLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jdSearchBookLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(uSearchType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uSearchString, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uSearchIt)
                    .addComponent(uSearchBookURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                .addContainerGap())
        );

        jdShowContent.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jdShowContent.setTitle("呵呵");

        jScrollPane2.setBorder(null);

        uPageContent.setEditable(false);
        uPageContent.setFont(new java.awt.Font("文泉驿正黑", 0, 24));
        jScrollPane2.setViewportView(uPageContent);

        javax.swing.GroupLayout jdShowContentLayout = new javax.swing.GroupLayout(jdShowContent.getContentPane());
        jdShowContent.getContentPane().setLayout(jdShowContentLayout);
        jdShowContentLayout.setHorizontalGroup(
            jdShowContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );
        jdShowContentLayout.setVerticalGroup(
            jdShowContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
        );

        jdEditPageInfo.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jdEditPageInfo.setTitle("编辑章节信息");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "PageID | BookID | Name | CharCount | URL", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("宋体", 0, 12), new java.awt.Color(0, 0, 255))); // NOI18N

        jlPID.setText("Page");
        jlPID.setToolTipText("PageID");

        jtfBID.setText("BID");
        jtfBID.setToolTipText("BookID");

        jtfPName.setText("PageName");
        jtfPName.setToolTipText("章节名");

        jtfCharCount.setText("Count");
        jtfCharCount.setToolTipText("章节字数");

        jtfPURL.setText("PageURL");
        jtfPURL.setToolTipText("章节内容地址");

        jbLeanPage.setMnemonic('r');
        jbLeanPage.setText("R");
        jbLeanPage.setToolTipText("精简内容");
        jbLeanPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbLeanPageActionPerformed(evt);
            }
        });

        jbSavePage.setMnemonic('s');
        jbSavePage.setText("保存(S)");
        jbSavePage.setToolTipText("保存所有修改的内容");
        jbSavePage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavePageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jlPID)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtfBID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtfPName, javax.swing.GroupLayout.PREFERRED_SIZE, 434, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jtfPURL, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtfCharCount, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbLeanPage, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jbSavePage)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jlPID, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jtfBID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jtfPName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(10, 10, 10))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jtfCharCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jtfPURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jbLeanPage, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jbSavePage, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jScrollPane5.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jtaPageContent.setColumns(20);
        jtaPageContent.setFont(new java.awt.Font("宋体", 0, 16)); // NOI18N
        jtaPageContent.setLineWrap(true);
        jtaPageContent.setRows(5);
        jScrollPane5.setViewportView(jtaPageContent);

        javax.swing.GroupLayout jdEditPageInfoLayout = new javax.swing.GroupLayout(jdEditPageInfo.getContentPane());
        jdEditPageInfo.getContentPane().setLayout(jdEditPageInfoLayout);
        jdEditPageInfoLayout.setHorizontalGroup(
            jdEditPageInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jdEditPageInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jdEditPageInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane5)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jdEditPageInfoLayout.setVerticalGroup(
            jdEditPageInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdEditPageInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                .addContainerGap())
        );

        jdFindnReplace.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jdFindnReplace.setTitle("查找和替换");

        jtaReplaceSrc.setColumns(20);
        jtaReplaceSrc.setRows(5);
        jtaReplaceSrc.setToolTipText("要查找的文本(正则表达式)");
        jScrollPane6.setViewportView(jtaReplaceSrc);

        jtaReplaceTar.setColumns(20);
        jtaReplaceTar.setRows(5);
        jtaReplaceTar.setToolTipText("要替换为的文本");
        jScrollPane7.setViewportView(jtaReplaceTar);

        jButton11.setMnemonic('f');
        jButton11.setText("查找(F)");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton12.setMnemonic('h');
        jButton12.setText("替换(H)");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        jButton10.setMnemonic('c');
        jButton10.setText("清空两框(C)");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jdFindnReplaceLayout = new javax.swing.GroupLayout(jdFindnReplace.getContentPane());
        jdFindnReplace.getContentPane().setLayout(jdFindnReplaceLayout);
        jdFindnReplaceLayout.setHorizontalGroup(
            jdFindnReplaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdFindnReplaceLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 278, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jdFindnReplaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jdFindnReplaceLayout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(jdFindnReplaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton11)
                            .addComponent(jButton12)))
                    .addGroup(jdFindnReplaceLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jButton10)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 278, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jdFindnReplaceLayout.setVerticalGroup(
            jdFindnReplaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jdFindnReplaceLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jdFindnReplaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jdFindnReplaceLayout.createSequentialGroup()
                        .addGroup(jdFindnReplaceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(30, Short.MAX_VALUE))
                    .addGroup(jdFindnReplaceLayout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jButton10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton11)
                        .addGap(60, 60, 60)
                        .addComponent(jButton12)
                        .addGap(59, 59, 59))))
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

        jToolBar1.setFloatable(false);
        jToolBar1.setOrientation(1);
        jToolBar1.setRollover(true);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/linpinger/icon/list.png"))); // NOI18N
        jButton1.setMnemonic('a');
        jButton1.setToolTipText("显示所有 (Alt + A)");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);
        jToolBar1.add(jSeparator9);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/linpinger/icon/refresh_yellow.png"))); // NOI18N
        jButton3.setMnemonic('d');
        jButton3.setToolTipText("更新所有 (Alt + D)");
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton3);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/linpinger/icon/mixx.png"))); // NOI18N
        jButton2.setMnemonic('m');
        jButton2.setToolTipText("所有转为Mobi (Alt + M)");
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton2);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/linpinger/icon/database_refresh.png"))); // NOI18N
        jButton6.setMnemonic('s');
        jButton6.setToolTipText("切换数据库 (Alt + S)");
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton6);
        jToolBar1.add(jSeparator8);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/linpinger/icon/sort_up_green.png"))); // NOI18N
        jButton4.setMnemonic('w');
        jButton4.setToolTipText("快捷顺序缩小数据库 (Alt + W)");
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton4);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/linpinger/icon/sort_down_green.png"))); // NOI18N
        jButton5.setMnemonic('e');
        jButton5.setToolTipText("快捷倒序缩小数据库 (Alt + E)");
        jButton5.setFocusable(false);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton5);

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

        mBookImportFromQidianTxt.setMnemonic('q');
        mBookImportFromQidianTxt.setText("导入起点txt(Q)");
        mBookImportFromQidianTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookImportFromQidianTxtActionPerformed(evt);
            }
        });
        jMenu1.add(mBookImportFromQidianTxt);

        mPageFindnReplace.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        mPageFindnReplace.setMnemonic('f');
        mPageFindnReplace.setText("查找替换所有章节内容(F)");
        mPageFindnReplace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mPageFindnReplaceActionPerformed(evt);
            }
        });
        jMenu1.add(mPageFindnReplace);
        jMenu1.add(jSeparator2);

        mBookShowAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        mBookShowAll.setText("显示所有");
        mBookShowAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookShowAllActionPerformed(evt);
            }
        });
        jMenu1.add(mBookShowAll);

        mBookShowSize.setMnemonic('z');
        mBookShowSize.setText("显示书籍占用空间(Z)");
        mBookShowSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookShowSizeActionPerformed(evt);
            }
        });
        jMenu1.add(mBookShowSize);

        mBookShowLessLen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        mBookShowLessLen.setText("显示字数过少章节");
        mBookShowLessLen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mBookShowLessLenActionPerformed(evt);
            }
        });
        jMenu1.add(mBookShowLessLen);

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

        mDBQuickA.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.ALT_MASK));
        mDBQuickA.setText("快捷顺序缩小数据库");
        mDBQuickA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBQuickAActionPerformed(evt);
            }
        });
        jMenu2.add(mDBQuickA);

        mDBQuickD.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.ALT_MASK));
        mDBQuickD.setText("快捷倒序缩小数据库");
        mDBQuickD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mDBQuickDActionPerformed(evt);
            }
        });
        jMenu2.add(mDBQuickD);

        jMenuBar1.add(jMenu2);

        msg.setForeground(new java.awt.Color(0, 0, 255));
        msg.setText("★　FoxBook Java Swing 版  作者: 爱尔兰之狐  Ver: 2015-03-23");
        msg.setToolTipText("★　哈哈我是消息栏，我总是萌萌哒");
        msg.setEnabled(false);
        msg.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jMenuBar1.add(msg);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 812, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
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
            
            String cc = mm.get("name").toString() + "\n\n" + mm.get("cc").toString();
            uPageContent.setText(cc.replace("\n", "\n　　"));
            uPageContent.setCaretPosition(0); // 跳到头部
            jdShowContent.setVisible(true);

 
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

    // 显示所有章节
    private void fShowAllPages() {
        tPage.setRowCount(0);
        List rsdata = oDB.getList("select page.name as name, page.CharCount as cc, page.ID as id, book.name as bname, page.url as url from book,Page where book.id=page.bookid order by page.bookid,page.ID");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            tPage.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("bname"), item.get("url")});
        }
        msg("★　章节总数: " + rsdata.size());
    }
    
    private void mBookShowAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookShowAllActionPerformed
        fShowAllPages();  // 显示所有章节
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
        int nRow = uBook.getSelectedRow();
//        String nBookName = uBook.getValueAt(nRow, 0).toString();
        String nBookID = uBook.getValueAt(nRow, 2).toString();

        uBookID.setText(nBookID);
        ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) oDB.getList("select name as name, url as url, DelURL as dlist, QidianID as qid from book where id=" + nBookID);
        uBookName.setText(list.get(0).get("name").toString());
        uQidianID.setText(list.get(0).get("qid").toString());
        uBookURL.setText(list.get(0).get("url").toString());
        uDelList.setText(list.get(0).get("dlist").toString());

        jdEditBookInfo.setVisible(true);

        uBookSearch.requestFocusInWindow(); // 设置焦点到搜索按钮上

    }//GEN-LAST:event_mBookInfoEditorActionPerformed

    private void mBookNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookNewActionPerformed
        uBookID.setText("0");
        uBookName.setText("书名");
        uQidianID.setText("起点书号");
        uBookURL.setText("目录地址");
        uDelList.setText("");
     
        jdEditBookInfo.setVisible(true);
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

    private void fSwitchDB() { // 切换数据库
         tPage.setRowCount(0);
        String nowDB = oDB.switchDB();
        refreshBookList();
        msg("★　切换到: " + nowDB);       
    }
    private void mDBSwichActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBSwichActionPerformed
        fSwitchDB() ; // 切换数据库
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
    
    private void fAll2Mobi() { // 转换为mobi
        tPage.setRowCount(0);
        msg.setForeground(Color.GREEN);
        msg("★　开始转换所有章节为 mobi");
        new Thread(new book2ebook(1, 0)).start();
    }
    
    private void mAll2MobiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mAll2MobiActionPerformed
        fAll2Mobi(); // 转换为mobi
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

    private void fQuickDBDesc() { // 快捷: 顺序排序并压缩
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
    }
    private void mDBQuickDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBQuickDActionPerformed
        fQuickDBDesc() ; // 快捷: 顺序排序并压缩
    }//GEN-LAST:event_mDBQuickDActionPerformed

    private void mDBSimpAllDelListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBSimpAllDelListActionPerformed
        msg("★　开始精简所有DelList");
        long sTime = System.currentTimeMillis();
        FoxBookDB.simplifyAllDelList(oDB);
        long eTime = System.currentTimeMillis() - sTime;
        msg("★　已精简所有DelList   耗时(ms): " + eTime);
    }//GEN-LAST:event_mDBSimpAllDelListActionPerformed
    
    private void fQuickDBAsc() { // 快捷: 顺序排序并压缩
         long sTime = System.currentTimeMillis();
        msg("★　开始顺序所有书籍...");
        FoxBookDB.regenID(1, oDB);
        refreshBookList();
        tPage.setRowCount(0);
        msg("★　开始重新生成pageID");
        FoxBookDB.regenID(9, oDB);
        msg("★　开始精简所有DelList");
        FoxBookDB.simplifyAllDelList(oDB);
        msg("★　开始缩小数据库...");
        double subSize = oDB.vacuumDB();
        long eTime = System.currentTimeMillis() - sTime;
        msg("★　已完成顺序并缩小数据库: " + subSize + " K   耗时(ms): " + eTime);       
    }
    private void mDBQuickAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDBQuickAActionPerformed
        fQuickDBAsc() ; // 快捷: 顺序排序并压缩
    }//GEN-LAST:event_mDBQuickAActionPerformed

    private void mBookShowLessLenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookShowLessLenActionPerformed
        tPage.setRowCount(0);
        List rsdata = oDB.getList("select page.name as name, page.CharCount as cc, page.ID as id, book.name as bname, page.url as url from book,Page where book.id=page.bookid and (length(Content) < 1000 or Content is null) order by page.bookid,page.ID");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            tPage.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("bname"), item.get("url")});
        }
        msg("★　字数少于1K的章节总数: " + rsdata.size());
    }//GEN-LAST:event_mBookShowLessLenActionPerformed

    private void mBookShowSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookShowSizeActionPerformed
        tPage.setRowCount(0);
        List rsdata = oDB.getList("select sum(length(p.content)) * 3 / 1024 as ss, p.bookid as bid, b.name as bname from page as p, book as b where b.id=p.bookid group by p.bookid order by ss, p.bookid");
        Iterator itr = rsdata.iterator();
        int allSize = 0;
        int nowSize = 0;
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            nowSize = Integer.valueOf(item.get("ss").toString());
            allSize += nowSize ;
            tPage.addRow(new Object[]{item.get("bname"), nowSize + " k", 0, item.get("bid"), 0});
        }
        msg("★　共占用空间(K): " + allSize);
    }//GEN-LAST:event_mBookShowSizeActionPerformed

    private void mBookImportFromQidianTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBookImportFromQidianTxtActionPerformed
        int returnVal = chooseTxt.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String txtPath = chooseTxt.getSelectedFile().getAbsolutePath();
            msg("★　开始导入起点Txt文件: " + txtPath);
            FoxBookDB.importQidianTxt(txtPath, oDB);
            msg("★　导入起点Txt文件完毕: " + txtPath);
            refreshBookList();
        }
    }//GEN-LAST:event_mBookImportFromQidianTxtActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        fShowAllPages();  // 显示所有章节
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        fAll2Mobi(); // 转换为mobi
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        msg.setForeground(Color.GREEN);
        new Thread(new UpdateAllBook()).start();  // 更新所有
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        fQuickDBAsc() ; // 快捷: 顺序排序并压缩
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        fQuickDBDesc() ; // 快捷: 顺序排序并压缩
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        fSwitchDB() ; // 切换数据库
    }//GEN-LAST:event_jButton6ActionPerformed

    private void uBookSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uBookSearchActionPerformed
        uSearchString.insertItemAt(uBookName.getText(), 0); // 设置要搜索的书名
        uSearchString.setSelectedIndex(0);
        jdSearchBook.setVisible(true);
    }//GEN-LAST:event_uBookSearchActionPerformed

    private void uQidianIDProcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uQidianIDProcActionPerformed
        int qidianid = site_qidian.qidian_getBookID_FromURL(uQidianID.getText());
        uQidianID.setText(String.valueOf(qidianid));
    }//GEN-LAST:event_uQidianIDProcActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        uDelList.setText(FoxBookLib.simplifyDelList(uDelList.getText()));
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        uDelList.setText("");
    }//GEN-LAST:event_jButton8ActionPerformed

    // 退出保存到数据库
    private void FoxDialogEditBookInfoSaveInfo() {
        String bookid = uBookID.getText();
        if ( bookid.equalsIgnoreCase("0") ) { // 添加书籍
            oDB.execPreOne("insert into book (DelURL, Name, QidianID, URL) values(?, \"" + uBookName.getText() + "\", \"" + uQidianID.getText() + "\", \"" + uBookURL.getText() + "\")", uDelList.getText());
        } else {
            oDB.execPreOne("update book set DelURL=?, name=\"" + uBookName.getText() + "\", qidianid=\"" + uQidianID.getText() + "\", url=\"" + uBookURL.getText() + "\" where id=" + bookid, uDelList.getText());
        }
        jdEditBookInfo.dispose();
        refreshBookList();
    }
    
    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        FoxDialogEditBookInfoSaveInfo(); // 退出保存到数据库
    }//GEN-LAST:event_jButton9ActionPerformed

    private void uSearchItActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uSearchItActionPerformed
        // TODO add your handling code here:
        String siteType = uSearchType.getSelectedItem().toString();
        String SearchString = uSearchString.getSelectedItem().toString();

        String html = "";
        String seURL = "";
        if (siteType.contains("E:")) { // 搜索引擎
            try {
                if (siteType.equalsIgnoreCase("E:SoGou")) {
                    seURL = "http://www.sogou.com/web?query=" + URLEncoder.encode(SearchString, "GB2312") + "&num=50";
                }
                if (siteType.equalsIgnoreCase("E:Yahoo")) {
                    seURL = "http://search.yahoo.com/search?n=40&p=" + URLEncoder.encode(SearchString, "UTF-8");
                }
                if (siteType.equalsIgnoreCase("E:Bing")) {
                    seURL = "http://cn.bing.com/search?q=" + URLEncoder.encode(SearchString, "UTF-8");
                }
                if (siteType.equalsIgnoreCase("E:soso")) {
                    seURL = "http://www.soso.com/q?w=" + URLEncoder.encode(SearchString, "GB2312");
                }
                if (siteType.equalsIgnoreCase("E:so")) {
                    seURL = "http://www.so.com/s?q=" + URLEncoder.encode(SearchString, "UTF-8");
                }
                if (siteType.equalsIgnoreCase("E:ZhongSou")) {
                    seURL = "http://www.zhongsou.com/third.cgi?w=" + URLEncoder.encode(SearchString, "UTF-8") + "&kid=&y=5&stag=1&dt=0&pt=0&utf=1";
                }
                if (siteType.equalsIgnoreCase("E:youdao")) {
                    seURL = "http://www.youdao.com/search?q=" + URLEncoder.encode(SearchString, "UTF-8") + "&ue=utf8&keyfrom=web.index";
                }
                if (siteType.equalsIgnoreCase("E:360")) {
                    seURL = "http://www.haosou.com/s?ie=utf-8&shb=1&src=360sou_newhome&q=" + URLEncoder.encode(SearchString, "UTF-8");
                }
                if (siteType.equalsIgnoreCase("E:GotoHell")) {
                    seURL = "http://devilfinder.com/search.php?q=" + URLEncoder.encode(SearchString, "UTF-8");
                }
                // 下载页面，分析链接
                html = FoxBookLib.downhtml(seURL);
                List<Map<String, Object>> seo = FoxBookLib.getSearchEngineHref(html, SearchString);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = seo.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_SE_SITE_LIST, mm.get("url"), mm.get("name")});
                }
            } catch (Exception e) {
                e.toString();
            }
        }
        if (siteType.contains("S:")) {  // 特殊站点
            if (siteType.equalsIgnoreCase("S:起点中文")) {
                seURL = site_qidian.qidian_getSearchURL_Mobile(SearchString);
                String json = FoxBookLib.downhtml(seURL, "utf-8");
                List<Map<String, Object>> qds = site_qidian.json2BookList(json);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = qds.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_SE_SITE_LIST, mm.get("url"), mm.get("name")});
                }
            }
            if (siteType.equalsIgnoreCase("S:追书神器")) {
                seURL = site_zssq.getUrlSE(SearchString);
                String json = site_zssq.json2BookID(FoxBookLib.downhtml(seURL, "utf-8"));
                uSearchBookURL.setText("&bid=" + json);
                uBookURL.setText("&bid=" + json);
                seURL = site_zssq.getUrlSL(json);
                json = FoxBookLib.downhtml(seURL, "utf-8");
                List<Map<String, Object>> ddl = site_zssq.json2SiteList(json);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = ddl.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_ZSSQ_SITE_LIST, mm.get("url"), mm.get("name")});
                }
            }
            if (siteType.equalsIgnoreCase("S:快读")) {
                seURL = site_qreader.qreader_Search(SearchString);
                uSearchBookURL.setText(seURL);
                uBookURL.setText(seURL);
                List<Map<String, Object>> kds = site_qreader.qreader_GetIndex(seURL, 22, 0);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = kds.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_QREADER_PAGE_LIST, mm.get("url"), mm.get("name")});
                }
            }
            if (siteType.equalsIgnoreCase("S:宜搜")) {
                seURL = site_easou.getUrlSE(SearchString);
                String json = FoxBookLib.downhtml(seURL, "utf-8");
                seURL = site_easou.getUrlSL(site_easou.json2IDs(json, 1));
                json = FoxBookLib.downhtml(seURL, "utf-8");
                List<Map<String, Object>> ddl = site_easou.json2SiteList(json);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = ddl.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_EASOU_SITE_LIST, mm.get("url"), mm.get("name")});
                }

            }

        }
    }//GEN-LAST:event_uSearchItActionPerformed

    private void uListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uListMouseClicked
        // TODO add your handling code here:
        String RootURL = "" ;
        
        if (2 == evt.getClickCount()) {
            int nRow = uList.getSelectedRow();
            String nowResultType = uList.getValueAt(nRow, 0).toString();
            String nowURL = uList.getValueAt(nRow, 1).toString();
            //System.out.println(uList.getValueAt(nRow, 0));

            String html = "";
            if (nowResultType.equalsIgnoreCase(TYPE_SE_SITE_LIST)) { // 处理:搜索引擎返回的站点列表
                uSearchBookURL.setText(nowURL);
                uBookURL.setText(nowURL);
                RootURL = nowURL; // 作为后面子目录的根路径
                html = FoxBookLib.downhtml(nowURL);
                List<Map<String, Object>> srt = FoxBookLib.tocHref(html, 22);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = srt.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_SE_PAGE_LIST, mm.get("url"), mm.get("name")});
                }
            }
            if (nowResultType.equalsIgnoreCase(TYPE_SE_PAGE_LIST)) { // 处理:章节列表
                if (RootURL.length() > 3) { // 获取完整路径
                    nowURL = FoxBookLib.getFullURL(RootURL, nowURL);
                }
                html = FoxBookLib.downhtml(nowURL);
                html = FoxBookLib.pagetext(html);
                if (html != null && html.length() > 200) {
                    JOptionPane.showMessageDialog(null, html.substring(0, 200));
                }
            }

            if (nowResultType.equalsIgnoreCase(TYPE_ZSSQ_SITE_LIST)) { // 处理: 追书神器 站点列表
                String oldBID = uBookURL.getText();
                if (oldBID.contains("&bid=")) {
                    uSearchBookURL.setText(nowURL + oldBID);
                    uBookURL.setText(nowURL + oldBID);
                } else {
                    uSearchBookURL.setText(nowURL);
                    uBookURL.setText(nowURL);
                }
                html = FoxBookLib.downhtml(nowURL, "utf-8");
                List<Map<String, Object>> dtl = site_zssq.json2PageList(html, 22);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = dtl.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_ZSSQ_PAGE_LIST, mm.get("url"), mm.get("name")});
                }
            }
            if (nowResultType.equalsIgnoreCase(TYPE_ZSSQ_PAGE_LIST)) { // 处理: 追书神器 章节列表
                html = FoxBookLib.downhtml(nowURL, "utf-8");
                html = site_zssq.json2Text(html);
                if (html != null && html.length() > 200) {
                    JOptionPane.showMessageDialog(null, html.substring(0, 200));
                }
            }
            if (nowResultType.equalsIgnoreCase(TYPE_QREADER_PAGE_LIST)) { // 处理: 快读 章节列表
                html = site_qreader.qreader_GetContent(nowURL);
                if (html != null && html.length() > 200) {
                    JOptionPane.showMessageDialog(null, html.substring(0, 200));
                }
            }
            if (nowResultType.equalsIgnoreCase(TYPE_EASOU_SITE_LIST)) { // 处理: 宜搜 站点列表
                uSearchBookURL.setText(nowURL);
                uBookURL.setText(nowURL);
                String sGIDNID = "";
                Matcher m = Pattern.compile("(?i).*(gid=[0-9]+&nid=[0-9]+)&").matcher(nowURL);
                while (m.find()) {
                    sGIDNID = m.group(1);
                }
                html = FoxBookLib.downhtml(nowURL, "utf-8");
                List<Map<String, Object>> dtl = site_easou.json2PageList(html, sGIDNID, 22);
                // 输出到列表
                tList.setRowCount(0); // 清空列表
                Iterator<Map<String, Object>> itr = dtl.iterator();
                while (itr.hasNext()) {
                    HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
                    tList.addRow(new Object[]{TYPE_EASOU_PAGE_LIST, mm.get("url"), mm.get("name")});
                }
            }
            if (nowResultType.equalsIgnoreCase(TYPE_EASOU_PAGE_LIST)) { // 处理: 宜搜 章节列表
                html = FoxBookLib.downhtml(nowURL, "utf-8");
                html = site_easou.json2Text(html);
                if (html != null && html.length() > 200) {
                    JOptionPane.showMessageDialog(null, html.substring(0, 200));
                }
            }
        }
        if (java.awt.event.MouseEvent.BUTTON3 == evt.getButton()) {
            int nRow = uList.rowAtPoint(evt.getPoint());
            uList.setRowSelectionInterval(nRow, nRow);
            //           jPopupMenuBook.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_uListMouseClicked

    private void mPageEditInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageEditInfoActionPerformed
        int nRow = uPage.getSelectedRow();
        String nPageID = uPage.getValueAt(nRow, 2).toString();

        jlPID.setText(nPageID);
        ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) oDB.getList("select bookid as bid, name as name, url as url, CharCount as cc, Content as ct from page where id=" + nPageID);
        jtfBID.setText(list.get(0).get("bid").toString());
        jtfPName.setText(list.get(0).get("name").toString());
        jtfCharCount.setText(list.get(0).get("cc").toString());
        jtfPURL.setText(list.get(0).get("url").toString());
        jtaPageContent.setText(list.get(0).get("ct").toString());
        jtaPageContent.setCaretPosition(0); // 跳到头部

        jdEditPageInfo.setVisible(true);
        jbLeanPage.requestFocusInWindow(); // 设置焦点到按钮上
    }//GEN-LAST:event_mPageEditInfoActionPerformed

    // 精简章节内容按钮
    private void jbLeanPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbLeanPageActionPerformed
        String pc = jtaPageContent.getText();
        pc = pc.replace("\r", "").replace("\n\n", "\n").replace("　", "").replace("\n ", "\n");
        jtaPageContent.setText(pc);
        jtaPageContent.setCaretPosition(0); // 跳到头部
    }//GEN-LAST:event_jbLeanPageActionPerformed

    // Page保存退出按钮
    private void jbSavePageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSavePageActionPerformed
        String pageid = jlPID.getText();
        oDB.execPreOne("update page set Content=?, name=\"" + jtfPName.getText() + "\", BookID=" + jtfBID.getText() + ", CharCount=" + jtfCharCount.getText()  + ", url=\"" + jtfPURL.getText() + "\" where id=" + pageid, jtaPageContent.getText());
        jdEditPageInfo.dispose();
    }//GEN-LAST:event_jbSavePageActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        // 查找
        String src = jtaReplaceSrc.getText();
        String tar = jtaReplaceTar.getText();
        
        tPage.setRowCount(0);
        List rsdata = oDB.getList("select page.name as name, page.CharCount as cc, page.ID as id, book.name as bname, page.url as url from book,Page where book.id=page.bookid and page.content like '%" + src + "%' order by page.bookid,page.ID");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            tPage.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("bname"), item.get("url")});
        }
        msg("★　搜索到的章节数: " + rsdata.size());
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        // 替换
        String src = jtaReplaceSrc.getText();
        String tar = jtaReplaceTar.getText();
        
        tPage.setRowCount(0);
        List rsdata = oDB.getList("select page.name as name, page.CharCount as cc, page.ID as id, book.name as bname, page.url as url, page.content as pc from book,Page where book.id=page.bookid and page.content like '%" + src + "%' order by page.bookid,page.ID");
        Iterator itr = rsdata.iterator();
        while (itr.hasNext()) {
            HashMap item = (HashMap) itr.next();
            oDB.execPreOne("update page set content=? where id=" + item.get("id").toString(), item.get("pc").toString().replace(src, tar)); // 替换内容
            tPage.addRow(new Object[]{item.get("name"), item.get("cc"), item.get("id"), item.get("bname"), item.get("url")});
        }
        msg("★　替换的章节数: " + rsdata.size());
        jdFindnReplace.dispose();
    }//GEN-LAST:event_jButton12ActionPerformed

    private void mPageFindnReplaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mPageFindnReplaceActionPerformed
        jdFindnReplace.setVisible(true);
    }//GEN-LAST:event_mPageFindnReplaceActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        jtaReplaceSrc.setText("");
        jtaReplaceTar.setText("");
    }//GEN-LAST:event_jButton10ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
    /* Set the Nimbus look and feel */
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
        javax.swing.UIManager.put("control", new Color(228, 242, 228));               // 控件背景色
        javax.swing.UIManager.put("nimbusLightBackground", new Color(228, 242, 228)); // 文本背景色
        javax.swing.UIManager.put("nimbusSelectionBackground", new Color( 129, 193, 115 ));          // 选定文本 129, 193, 115    55, 165, 55    64, 128, 128 

        javax.swing.UIManager.put("nimbusBlueGrey", new Color(179, 219, 179));        //控件色
        javax.swing.UIManager.put("nimbusBase", new Color(70, 140, 60));              //滚动条，基础颜色

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {  //Metal, Nimbus,CDE/Motif,Windows,Windows Classic
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            ex.toString();
        }

  
/*
         // 显示所有值
        UIManager.LookAndFeelInfo looks[] = UIManager.getInstalledLookAndFeels();

        for (UIManager.LookAndFeelInfo info : looks) {
            try {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());

                    UIDefaults defaults = UIManager.getDefaults();
                    Enumeration newKeys = defaults.keys();

                    while (newKeys.hasMoreElements()) {
                        Object obj = newKeys.nextElement();
                        System.out.printf("%50s : %s\n", obj, UIManager.get(obj));
                    }
                }
            } catch (Exception ex) {
                ex.toString();
            }
        }
*/
        
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
    private javax.swing.table.DefaultTableModel tList;  // 搜索结果列表
    final String TYPE_SE_SITE_LIST = "ES";  // 搜索结果类型
    final String TYPE_SE_PAGE_LIST = "EL";
    final String TYPE_ZSSQ_SITE_LIST = "SZS";
    final String TYPE_ZSSQ_PAGE_LIST = "SZL";
    final String TYPE_QREADER_PAGE_LIST = "SKL";
    final String TYPE_EASOU_SITE_LIST = "SES";
    final String TYPE_EASOU_PAGE_LIST = "SEL";
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser chooseTxt;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu jPopupMenuBook;
    private javax.swing.JPopupMenu jPopupMenuPage;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton jbLeanPage;
    private javax.swing.JButton jbSavePage;
    private javax.swing.JDialog jdEditBookInfo;
    private javax.swing.JDialog jdEditPageInfo;
    private javax.swing.JDialog jdFindnReplace;
    private javax.swing.JDialog jdSearchBook;
    private javax.swing.JDialog jdShowContent;
    private javax.swing.JLabel jlPID;
    private javax.swing.JTextArea jtaPageContent;
    private javax.swing.JTextArea jtaReplaceSrc;
    private javax.swing.JTextArea jtaReplaceTar;
    private javax.swing.JTextField jtfBID;
    private javax.swing.JTextField jtfCharCount;
    private javax.swing.JTextField jtfPName;
    private javax.swing.JTextField jtfPURL;
    private javax.swing.JMenuItem mAll2Epub;
    private javax.swing.JMenuItem mAll2Mobi;
    private javax.swing.JMenuItem mAll2Txt;
    private javax.swing.JMenuItem mBook2Epub;
    private javax.swing.JMenuItem mBook2Mobi;
    private javax.swing.JMenuItem mBook2Txt;
    private javax.swing.JMenuItem mBookDelete;
    private javax.swing.JMenuItem mBookImportFromQidianTxt;
    private javax.swing.JMenuItem mBookInfoEditor;
    private javax.swing.JMenuItem mBookMultiThreadUpdateOne;
    private javax.swing.JMenuItem mBookNew;
    private javax.swing.JMenuItem mBookShowAll;
    private javax.swing.JMenuItem mBookShowLessLen;
    private javax.swing.JMenuItem mBookShowSize;
    private javax.swing.JMenuItem mBookUpdateAll;
    private javax.swing.JMenuItem mBookUpdateOne;
    private javax.swing.JMenuItem mBookUpdateTocOne;
    private javax.swing.JMenuItem mDBQuickA;
    private javax.swing.JMenuItem mDBQuickD;
    private javax.swing.JMenuItem mDBRegenPageIDs;
    private javax.swing.JMenuItem mDBSimpAllDelList;
    private javax.swing.JMenuItem mDBSortAsc;
    private javax.swing.JMenuItem mDBSortDesc;
    private javax.swing.JMenuItem mDBSwich;
    private javax.swing.JMenuItem mDBVacuum;
    private javax.swing.JMenuItem mPageDeleteMulti;
    private javax.swing.JMenuItem mPageDeleteMultiNotUpdate;
    private javax.swing.JMenuItem mPageEditInfo;
    private javax.swing.JMenuItem mPageFindnReplace;
    private javax.swing.JMenuItem mPageUpdateOne;
    private javax.swing.JMenuItem mPages2Epub;
    private javax.swing.JMenuItem mPages2Mobi;
    private javax.swing.JMenuItem mPages2txt;
    private javax.swing.JMenuItem mRefreshBookList;
    private javax.swing.JMenu msg;
    private javax.swing.JTable uBook;
    private javax.swing.JLabel uBookID;
    private javax.swing.JTextField uBookName;
    private javax.swing.JButton uBookSearch;
    private javax.swing.JTextField uBookURL;
    private javax.swing.JTextArea uDelList;
    private javax.swing.JScrollPane uDelListScrool;
    private javax.swing.JTable uList;
    private javax.swing.JTable uPage;
    private javax.swing.JTextPane uPageContent;
    private javax.swing.JTextField uQidianID;
    private javax.swing.JButton uQidianIDProc;
    private javax.swing.JTextField uSearchBookURL;
    private javax.swing.JButton uSearchIt;
    private javax.swing.JComboBox uSearchString;
    private javax.swing.JComboBox uSearchType;
    // End of variables declaration//GEN-END:variables
}
