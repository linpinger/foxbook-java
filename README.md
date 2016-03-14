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

**安装及使用方法:**
- 使用方法一句话: java -jar FoxBook.jar
- 安装[JRE](http://www.java.com/zh_CN/download/index.jsp)或JDK，绿色版也行，反正需要javaw.exe
- 将 FoxBook.db3 放到 FoxBook.jar 文件所在文件夹
- 将 kindlegen 放到 系统PATH变量中的文件夹，例如c:\WINDOWS\system32\ 或 /usr/bin/ 之类的
- 双击FoxBook.jar运行程序 或  绿色版java路径/javaw.exe -jar FoxBook.jar 或 Linux下 /xxx/ooo/java -jar FoxBook.jar
- 现在可以愉快的玩耍了
- 制作为mobi/epub/txt格式，如果存在c:\etc就保存到该文件夹，否则就在程序所在目录生成
- 新增书籍，先输入书名，然后按书名旁边的搜D按钮进入搜索界面，选择搜索渠道，复制目录地址(Ctrl+C)到新增书籍界面的地址栏，保存即可
- 2016-1-1 Jar文件中添加了配置文件com/linpinger/foxbook/foxbook.properties，里面的选项可以控制UI的表现及其他，后期会继续往里面添加选项，便于修改

**其他小提示:**

- Linux下使kindlegen能正确运行: chmod a+x kindlegen  然后添加kindlegen所在路径到 .bashrc中，例如: export PATH="/home/fox/bin:$PATH"
- Linux下有些中文字体有锯齿，添加这行到.bashrc : export _JAVA_OPTIONS='-Dawt.useSystemAAFontSettings=lcd'
- 修改.bashrc后，运行 source .bashrc使其生效

**工程中包含的其他文件:**

- sqlite3 java版 [sqlite-jdbc-3.7.15-M1.jar](https://bitbucket.org/xerial/sqlite-jdbc)
- kindlegen 程序 这个是amazon公司提供的转换为mobi格式的工具，提供win/linux/mac版，需翻Q下载
- [JSON-java](https://github.com/douglascrockford/JSON-java)库 和 android下的json库用法是一样滴
- icon 全部是从[findicons](http://findicons.com)网站上搜的图标


**更新日志:**

- 2016-03-12: 变更: 已删除列表机制修改，不再使用起止，直接取第一行之后的对比
- 2016-03-09: 整理: Swing-Android部分使用同一文件(文件编码不同):FoxBookLib/FoxEpub/site_*
- 2016-01-22: 添加: 起点手机站地址支持
- 2016-01-08: 修正: 起点目录处理错误
- 2016-01-01: 添加: 配置文件 修改jar包(zip压缩文件)中的 com/linpinger/foxbook/foxbook.properties
- 2015-12-31: 添加: 按钮:删除选定章节，菜单/按钮:全选，选择本章以上、以下，方便平板操作
- 2015-12-24: 修正: 新函数处理起点分卷错误
- 2015-12-23: 修改 tocHref函数，改进目录分析策略
- 2015-12-21: 添加: 阅读页面拖动效果，现在可以点击也可以拖动了 修改: qqxs域名修改成13xs
- 2015-12-20: 添加: win10平板触摸特性:单击列表第二列位置效果和双击该行效果相同，左侧工具栏移到右侧并增大到64像素，内容页以纵向30%为分割线，单击上面的区域向上翻页，单击下面的区域向下翻页，主界面自动随着窗口大小变动修改左右列表比例，检测到Windows系统大于等于6.2时:阅读页字体调整为微软雅黑，主界面各元素字体增大，方便点击
- 2015-11-17: 修正: 新起点网页地址规则
- 2015-10-27: 修改: 任务栏图标顺序，修改User-Agent
- 2015-07-24: 添加: 任务栏图标，修改: 消息颜色绿色改成红色
- 2015-07-14: 修改: 更新所有书时使用线程组替代等待线程，可以显示剩余线程数，便于了解进度
- 2015-06-29: 修正: linux下控件中文字体显示不正常，现使用派生字体，只使用系统字体，且只改变大小，有两个标题除外，但不影响，因内容是用英文
- 2015-04-16: 添加:下载过滤qidian的txt地址，默认下载.gz会造成使用cdn，然后出现故障
- 2015-03-23: 添加:多线程更新提示(不知道是否稳定)，添加修改搜索引擎
- 2015-03-09: 修正: 起点内容页尾部多余内容
- 2015-01-28: 添加: 查找替换所有章节内容对话框
- 2015-01-26: 添加: 修改章节内容对话框
- 2015-01-25: 修改: 主题颜色设置(基础颜色，选择颜色)，直接使用内置dialog，删除原有panel类文件，信息可响应esc键，并实时修改URL
- 2015-01-22: 修改: 调整工具栏按钮位置，说明添加小提示，便于Linux下使用
- 2014-12-09: 修改: 工具栏左侧垂直安放，修正: 未释放kindlegen标准输出缓冲区造成的卡死
- 2014-12-08: 添加: 工具栏
- 2014-10-15: 添加: 导入起点txt
- 2014-10-14: 添加: 显示各书籍占用空间，修正: 搜索sogou时显示的多余链接
- 2014-10-10: 修正: 比较新章节时未小写网址可能造成的无新章节状况，例如起点的网址
- 2014-09-30: 添加菜单项: 显示所有字数小于1K的章节
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
