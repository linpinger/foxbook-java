# FoxBook(狐狸的小说下载阅读及转换工具) Java Swing版

**名称:** FoxBook

**功能:** 狐狸的小说下载阅读及转换工具(下载小说站小说，制作为mobi,epub,txt格式)

**作者:** 爱尔兰之狐(linpinger)

**邮箱:** <mailto:linpinger@gmail.com>

**主页:** <http://linpinger.github.io?s=FoxBook-Java_MD>

**缘起:** 用别人写的工具，总感觉不能随心所欲，于是自己写个下载管理工具，基本能用，基本满意

**原理:** 下载网页，分析网页，文本保存在数据库中，转换为其他需要的格式

**亮点:** 

-   通用小说网站规则能覆盖大部分文字站的目录及正文内容分析，不需要针对每个网站的规则
-   本版本是用Java语言开发的，所以可以在 win/linux/mac 下运行
-   和之前的[AHK版][foxbook-ahk](win专用/linux下wine)，[Android版][foxbook-android]使用同一个数据库

**源码及下载:**

-   [源码工程](https://github.com/linpinger/foxbook-java)

-   [文件下载点1:baidu][pan_baidu]
-   [文件下载点2:qiniu](http://linpinger.qiniudn.com/foxbook-java.7z)

**安装及使用方法:**

- 安装[JRE](http://www.java.com/zh_CN/download/index.jsp)或JDK，绿色版也行，反正需要javaw.exe
- 将 FoxBook.db3 放到 FoxBook.jar 文件所在文件夹
- 将 kindlegen 放到 系统PATH变量中的文件夹，例如c:\WINDOWS\system32\ 或 /usr/bin/ 之类的，也许 FoxBook.jar 文件所在的文件夹也行，试试呗
- 双击FoxBook.jar运行程序 或  绿色版java路进/javaw -jar FoxBook.jar 或 Linux下 /xxx/ooo/java -jar FoxBook.jar
- 现在可以愉快的玩耍了
- 制作为mobi/epub/txt格式，如果存在c:\etc就保存到该文件夹，否则就在程序所在目录生成
- 新增书籍，先输入书名，然后按书名旁边的搜D按钮进入搜索界面，选择搜索渠道，复制目录地址(Ctrl+C)到新增书籍界面的地址栏，保存即可


**工程中包含的其他文件:**

- sqlite3 java版 [sqlite-jdbc-3.7.15-M1.jar](https://bitbucket.org/xerial/sqlite-jdbc)
- kindlegen 程序 这个是amazon公司提供的转换为mobi格式的工具，提供win/linux/mac版，需翻Q下载
- [JSON-java](https://github.com/douglascrockford/JSON-java)库 和 android下的json库用法是一样滴


**更新日志:**

- 2014-09-25: 修正:新增书籍后更新目录不显示ID问题，修正:重新用JDK6编译，解决不适合linuxJDK6的问题，修正: 显示页面字体修改为 文泉驿正黑，避免在未设置好的linux系统下显示正常
- 2014-09-22: 添加顺序快捷键
- 2014-09-17: 修改查看空白健不连续翻页问题(更换事件)
- 2014-09-01: 添加菜单精简所有DelList，并添加进快捷菜单，修复regenID当数据库无记录的异常bug
- 2014-08-23: 使用sqlite的内存数据库功能，升级 sqlite-jdbc-xxx.jar，旧的backup和restore数据库到内存数据库有问题，将显示在右侧Table中的消息移到菜单栏显示，并设置颜色，一些小的空指针问题处理，无数据库时建立表结构
- 2014-08-22: 发布Java-Swing版，和FoxBook-ahk/android共用同一数据库文件，放在FoxBook.jar所在文件夹
- ...: 懒得写了，就这样吧


[foxbook-ahk]: https://github.com/linpinger/foxbook-ahk
[foxbook-android]: https://github.com/linpinger/foxbook-android
[pan_baidu]: http://pan.baidu.com/s/1bnqxdjL "百度网盘共享"
