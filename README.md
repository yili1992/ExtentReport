# 简介
- 基于ExtendReport 实现 Suite->Test->Class->TestMethod 层次关系的报告。
- 报告中 包含tearDown setUp等 configurationTest 信息。并且Step的统计不会记录configurationTest。
- 增加ExtendX 以及修改ExtendX 源码进行适配当前结构(Doing)

![image](https://github.com/yili1992/ExtendReport/raw/master/asset/1.png)

# 如何使用
testng.xml 中增加listener


    <listeners>
          <listener class-name="com.XXX.XXX.ExtentTestNGIReporterListener"></listener>
    </listeners>
>基于api_autotest目中ExtendReport.java开发：
[api_autotest](https://github.com/ChenSen5/api_autotest/blob/master/src/main/java/com/sen/api/listeners/ExtentTestNGIReporterListener.java)

