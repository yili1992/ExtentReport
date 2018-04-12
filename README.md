
# 项目简介
在TestNG 当中测试报告集成ExtentReport套件,但是在展示上有一点问题所以做了一些修改，
想使其提供一套完整可用的ExtentReport 与ExtentX

- ExtentReport 定制化 Suite->Test->Class->TestMethod 层次结构
- ExtentX performance 图的X坐标问题修改
- ExtentX 修改数据获取的 sql规则 使其符合Suite->Test->Class->TestMethod 层次结构
- ExtentX Scenario 定义为 实际执行的测试用例, Test为 所有的@Test 包含 setup,teardown
- 使用 MyExtentXReporter 来代替ExtentXReport 修复 report statTime 获取错误问题

## ExtentReport
- 基于ExtenReport 实现 Suite->Test->Class->TestMethod 层次关系的报告。
- 报告中 包含tearDown setUp等 configurationTest 信息。并且Step的统计不会记录configurationTest。
- ExtentTestNGIReporterListener.java中ConfigModel 存放了一些固定的配置信息，使用时可以根据自身情况修改
- 使用TestNG xml执行时候 SuiteName 对应的是ExtentX 中的Project 名字 TestName 对应的是 ExtentX中的Report名字


# 简介
- 基于ExtendReport 实现 Suite->Test->Class->TestMethod 层次关系的报告。
- 报告中 包含tearDown setUp等 configurationTest 信息。并且Step的统计不会记录configurationTest。
- 增加ExtendX 以及修改ExtendX 源码进行适配当前结构

![image](https://github.com/yili1992/ExtendReport/raw/master/asset/1.png)

### 如何使用

testng.xml 中增加listener


    <listeners>
          <listener class-name="com.XXX.XXX.ExtentTestNGIReporterListener"></listener>
    </listeners>
>基于api_autotest目中ExtendReport.java开发：
[api_autotest](https://github.com/ChenSen5/api_autotest/blob/master/src/main/java/com/sen/api/listeners/ExtentTestNGIReporterListener.java)

## ExtentX

Report Server for TestNG Report 项目目录是 根目录下的 extentx
![image](https://github.com/yili1992/ExtendReport/raw/master/asset/dashboard.png)

### 部署

* 下载安装node.js
* 下载安装mongodb  <3.6 验证过3.2.19
* 配置mongodb并运行 mongod  --fork   --storageEngine=mmapv1 --dbpath /data/mongodb --logpath /data/mongodb/log/mongodb.log
* 检出extentx，解压到某一目录
* 进入解压目录中，使用命令 npm install
* 使用命令 sails lift 启动服务（调试使用）
* 生产部署  npm -g forever
* 进入extentx 目录  forever start -l forever.log -o out.log -e err.log app.js
* http://localhost:1337 


### 引用
http://extentreports.com/docs/extentx/

### 默认密码
user:      root
password:  password
