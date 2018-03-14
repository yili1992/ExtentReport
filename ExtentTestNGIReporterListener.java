package com.leoao.test.listerner;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.ResourceCDN;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.model.Log;
import com.aventstack.extentreports.model.TestAttribute;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.ChartLocation;
import com.aventstack.extentreports.reporter.configuration.Theme;

import com.leoao.test.tool.DateHelper;
import org.testng.*;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.util.*;

public class ExtentTestNGIReporterListener implements IReporter {
    //生成的路径以及文件名
    private static final String OUTPUT_FOLDER = "test-output/";
    private static final String FILE_NAME = DateHelper.getCurrentDate("yyyy-MM-dd HH:mm")+".html";

    private ExtentReports extent;

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        init();
        boolean createSuiteNode = false;
        if(suites.size()>1){
            createSuiteNode=true;
        }
        for (ISuite suite : suites) {
            Map<String, ISuiteResult> result = suite.getResults();
            //如果suite里面没有任何用例，直接跳过，不在报告里生成
            if(result.size()==0){
                continue;
            }
            //统计suite下的成功、失败、跳过的总用例数
            int suiteFailSize=0;
            int suitePassSize=0;
            int suiteSkipSize=0;
            ExtentTest suiteTest=null;
            ExtentTest beforeSuiteTest=null;
            ExtentTest afterSuiteTest=null;
            IInvokedMethod beforeSuite=null;
            IInvokedMethod afterSuite=null;
            List<IInvokedMethod> allInvokeMethods = suite.getAllInvokedMethods();
            for(int i = 0;i<allInvokeMethods.size();i++){
                if (allInvokeMethods.get(i).isConfigurationMethod()){
                    beforeSuite = allInvokeMethods.get(i); //suite的 BeforeSuite Annotitation
                    break;
                }
            }
            for(int i=allInvokeMethods.size()-1;i>=0; i--){
                if (allInvokeMethods.get(i).isConfigurationMethod()){
                    afterSuite = allInvokeMethods.get(i); // suite的AfterSuite Annotitation
                    break;
                }
            }

            //存在多个suite的情况下，在报告中将同一个一个suite的测试结果归为一类，创建一级节点。

            if(createSuiteNode){
                suiteTest = extent.createTest(suite.getName()).assignCategory(suite.getName());
                if(null!=beforeSuite) {
                    beforeSuiteTest = suiteTest.createNode(beforeSuite.getTestMethod().getDescription());
                }
                if(null!=afterSuite) {
                    afterSuiteTest = suiteTest.createNode(afterSuite.getTestMethod().getDescription());
                }
            } else {
                if(null!=beforeSuite) {
                    beforeSuiteTest = extent.createTest(beforeSuite.getTestMethod().getDescription());
                }
                if(null!=afterSuite) {
                    afterSuiteTest = extent.createTest(afterSuite.getTestMethod().getDescription());
                }
            }
            //**** before suite and after suite set START *****
            if(null!=beforeSuite) {
                buildSuiteConfigurationNodes(beforeSuiteTest, beforeSuite);
            }
            if(null!=afterSuite) {
                buildSuiteConfigurationNodes(afterSuiteTest, afterSuite);
            }
            //**** before suite and after suite set END *****
            boolean createSuiteResultNode = false;
            if(result.size()>1){
                createSuiteResultNode=true;
            }
            Date suiteStartTime = null,suiteEndTime=new Date();
            for (ISuiteResult r : result.values()) {
                ExtentTest resultNode;
                ITestContext context = r.getTestContext();
                if(createSuiteResultNode){
                    //没有创建suite的情况下，将在SuiteResult的创建为一级节点，否则创建为suite的一个子节点。
                    if( null == suiteTest){
                        resultNode = extent.createTest(context.getName());
                    }else{
                        resultNode = suiteTest.createNode(context.getName());//Suite下面创建Test节点
                    }
                }else{
                    resultNode = suiteTest;
                }
                if(resultNode != null){
                    resultNode.assignCategory(suite.getName(),context.getName());
                    if(suiteStartTime == null){
                        suiteStartTime = context.getStartDate();
                    }
                    suiteEndTime = context.getEndDate();
                    resultNode.getModel().setStartTime(context.getStartDate());
                    resultNode.getModel().setEndTime(context.getEndDate());
                    //统计SuiteResult下的数据
                    int passSize = context.getPassedTests().size();
                    int failSize = context.getFailedTests().size();
                    int skipSize = context.getSkippedTests().size();
                    suitePassSize += passSize;
                    suiteFailSize += failSize;
                    suiteSkipSize += skipSize;
                    System.out.println(suitePassSize);
                    System.out.println(suiteFailSize);
                    System.out.println(suiteSkipSize);
                    if(failSize>0){
                        resultNode.getModel().setStatus(Status.FAIL);
                    }
                    resultNode.getModel().setDescription(String.format("Pass: %s ; Fail: %s ; Skip: %s ;",passSize,failSize,skipSize));
                }
                extendResultMap(context.getFailedTests(), context.getFailedConfigurations());
                extendResultMap(context.getSkippedTests(), context.getSkippedConfigurations());
                extendResultMap(context.getPassedTests(), context.getPassedConfigurations());
                IResultMap allTest = extendResultMap(context.getFailedTests(),context.getSkippedTests());
                allTest = extendResultMap(allTest,context.getPassedTests());
                buildTestNodes(resultNode,allTest);
            }
            if(suiteTest!= null){
                suiteTest.getModel().setDescription(String.format("Pass: %s ; Fail: %s ; Skip: %s ;",suitePassSize,suiteFailSize,suiteSkipSize));
                suiteTest.getModel().setStartTime(suiteStartTime==null?new Date():suiteStartTime);
                suiteTest.getModel().setEndTime(suiteEndTime);
                if(suiteFailSize>0){
                    suiteTest.getModel().setStatus(Status.FAIL);
                }
            }

        }
        extent.flush();
    }

    private IResultMap extendResultMap(IResultMap set1, IResultMap set2){
        Iterator resultIterator = set2.getAllResults().iterator();
        Iterator methodIterator = set2.getAllMethods().iterator();
        while(resultIterator.hasNext() && methodIterator.hasNext()){
            ITestResult testResult =(ITestResult)resultIterator.next();
            ITestNGMethod testNGMethod = (ITestNGMethod) methodIterator.next();
            if(testNGMethod.isAfterSuiteConfiguration()||testNGMethod.isBeforeSuiteConfiguration()){//不增加suite内容
                continue;
            }
            set1.addResult(testResult, testNGMethod);
        }
        return set1;
    }

    private void init() {
        //文件夹不存在的话进行创建
        File reportDir= new File(OUTPUT_FOLDER);
        if(!reportDir.exists()&& !reportDir .isDirectory()){
            reportDir.mkdir();
        }
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(OUTPUT_FOLDER + FILE_NAME);
        htmlReporter.config().setDocumentTitle("leoao_Test_report");
        htmlReporter.config().setReportName("leoao_Test_report");
        htmlReporter.config().setChartVisibilityOnOpen(true);
        htmlReporter.config().setTestViewChartLocation(ChartLocation.TOP);
        htmlReporter.config().setTheme(Theme.STANDARD);

        //设置点击效果：.node.level-1  ul{ display:none;} .node.level-1.active ul{display:block;}
        //设置系统信息样式：.card-panel.environment  th:first-child{ width:30%;}
        htmlReporter.config().setCSS(".node.level-1  ul{ display:none;} .node.level-1.active ul{display:block;}  .card-panel.environment  th:first-child{ width:30%;}");
        // 移除按键监听事件
        htmlReporter.config().setJS("$(window).off(\"keydown\");");
        //设置静态文件的DNS 如果cdn.rawgit.com访问不了，可以设置为：ResourceCDN.EXTENTREPORTS 或者ResourceCDN.GITHUB
        htmlReporter.config().setResourceCDN(ResourceCDN.EXTENTREPORTS);
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        extent.setReportUsesManualConfiguration(true);
        // 设置系统信息
        Properties properties = System.getProperties();
        extent.setSystemInfo("os.name",properties.getProperty("os.name","未知"));
        extent.setSystemInfo("os.arch",properties.getProperty("os.arch","未知"));
        extent.setSystemInfo("os.version",properties.getProperty("os.version","未知"));
        extent.setSystemInfo("java.version",properties.getProperty("java.version","未知"));
        extent.setSystemInfo("java.home",properties.getProperty("java.home","未知"));
        extent.setSystemInfo("user.name",properties.getProperty("user.name","未知"));
        extent.setSystemInfo("user.dir",properties.getProperty("user.dir","未知"));
    }

    private void buildSuiteConfigurationNodes(ExtentTest extent, IInvokedMethod invokemethod){
        List<String> outputList = Reporter.getOutput(invokemethod.getTestResult());
        for(String output : outputList) {
            extent.debug(output.replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
        }
        extent.getModel().setStartTime(getTime(invokemethod.getTestResult().getStartMillis()));
        extent.getModel().setEndTime(getTime(invokemethod.getTestResult().getEndMillis()));
        if(invokemethod.getTestResult().getTestContext().getFailedConfigurations().size()>0){
            extent.getModel().setStatus(Status.FAIL);
        }else if(invokemethod.getTestResult().getTestContext().getSkippedConfigurations().size()>0){
            extent.getModel().setStatus(Status.SKIP);
        }
        if (invokemethod.getTestResult().getThrowable() != null) {
            extent.log(Status.FAIL, invokemethod.getTestResult().getThrowable());
        }

    }

    private void buildTestNodes(ExtentTest extenttest,IResultMap tests) {
        //存在父节点时，获取父节点的标签
        String[] categories=new String[0];
        if(extenttest != null ){
            List<TestAttribute> categoryList = extenttest.getModel().getCategoryContext().getAll();
            categories = new String[categoryList.size()];
            for(int index=0;index<categoryList.size();index++){
                categories[index] = categoryList.get(index).getName();
            }
        }
        ExtentTest classNode =null;
        Map<String,ExtentTest> classNodeMap = new HashMap<>();
        ExtentTest test;
        if (tests.size() > 0) {
            //调整用例排序，按时间排序
            Set<ITestResult> treeSet = new TreeSet<ITestResult>(new Comparator<ITestResult>() {
                @Override
                public int compare(ITestResult o1, ITestResult o2) {
                    return o1.getStartMillis()<o2.getStartMillis()?-1:1;
                }
            });
            treeSet.addAll(tests.getAllResults());
            for (ITestResult result : treeSet) {
                Status status=null;
                switch (result.getStatus()) {
                    case ITestResult.SUCCESS:
                        status = Status.PASS;
                        break;
                    case ITestResult.FAILURE:
                        status = Status.FAIL;
                        break;
                    case ITestResult.SKIP:
                        status = Status.SKIP;
                        break;
                }
                String currentClassName = result.getMethod().getTestClass().getName();
                if(classNodeMap.containsKey(currentClassName)) {
                    classNode = classNodeMap.get(currentClassName);
                }else {
                    if (extenttest == null) {
                        classNode = extent.createTest(currentClassName);
                    } else {
                        //作为子节点进行创建时，设置同父节点的标签一致，便于报告检索。
                        classNode = extenttest.createNode(currentClassName).assignCategory(categories);
                    }
                    classNode.getModel().setStartTime(getTime(result.getStartMillis()));//获取class 下 第一个test开始时间
                    classNodeMap.put(currentClassName,classNode);
                }
                Object[] parameters = result.getParameters();
                String paramextend="";
                String name = result.getMethod().getMethodName();
                //如果有参数，则使用参数的toString在报告的name中显示出来
                for(Object param:parameters){
                    paramextend = paramextend+param.toString()+',';
                }
                if(paramextend.length()>0){
                    paramextend = paramextend.substring(0,paramextend.length()-1);
                    if(paramextend.length()>70){
                        paramextend= paramextend.substring(0,69)+"..."+paramextend.substring(paramextend.length()-6,paramextend.length());
                    }

                }
                if(null==result.getMethod().getDescription()||"".equals(result.getMethod().getDescription())){
                    name = name+'('+paramextend+')';
                }else {
                    name = result.getMethod().getDescription()+'('+paramextend+')';
                }

                //作为子节点进行创建时，设置同父节点的标签一致，便于报告检索。但是非Test()不设置categories
                if (result.getMethod().isTest()) {
                    test = classNode.createNode(name).assignCategory(categories);
                    String package_names[] = result.getTestClass().getName().split("\\.");
                    String package_name = package_names[0];
                    for (int i=1; i< package_names.length-1;i++)
                        package_name = package_name+"."+package_names[i];
                    test.assignCategory(package_name);
                    for (String group : result.getMethod().getGroups())
                        test.assignCategory(group);
                }else {
                    test = classNode.createNode(name);
                }

                //test.getModel().setDescription(description.toString());
                //test = extent.createTest(result.getMethod().getMethodName());

                List<String> outputList = Reporter.getOutput(result);
                if (result.getMethod().getDescription() != null) {
                    test.log(Status.DEBUG, result.getMethod().getDescription());//将description输出在报告中
                }
                for(String output:outputList){
                    //将用例的log输出报告中
                    test.debug(output.replaceAll("<","&lt;").replaceAll(">","&gt;"));
                }
                if (!result.getMethod().isTest()){
                    if (result.getThrowable() != null) {
                        test.log(Status.DEBUG, result.getThrowable());
                    }
                }else {
                    if (result.getThrowable() != null) {
                        test.log(status, result.getThrowable());
                    } else {
                        test.log(status, "Test " + status.toString().toLowerCase() + "ed");
                    }
                }
                //设置log的时间，根据ReportUtil.log()的特定格式进行处理获取数据log的时间
                Iterator logIterator = test.getModel().getLogContext().getIterator();
                while (logIterator.hasNext()){
                    Log log = (Log) logIterator.next();
                    String details = log.getDetails();
                    if(details.contains("===")){
                        String time = details.split("===")[0];
                        log.setTimestamp(getTime(Long.valueOf(time)));
                        log.setDetails(details.substring(time.length()+"===".length()));
                    }else{
                        log.setTimestamp(getTime(result.getEndMillis()));
                    }
                }
                test.getModel().setStartTime(getTime(result.getStartMillis()));
                test.getModel().setEndTime(getTime(result.getEndMillis()));
                classNode.getModel().setEndTime(getTime(result.getEndMillis()));
            }
        }
    }

    private Date getTime(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.getTime();
    }
}

