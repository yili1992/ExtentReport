package com.lee.test.listener;

/**
 * Created by zhaoli on 2018/4/2 修改ExtentX 中报告时间问题
 */
import com.aventstack.extentreports.configuration.Config;
import com.aventstack.extentreports.configuration.ConfigMap;
import com.aventstack.extentreports.mediastorage.MediaStorage;
import com.aventstack.extentreports.mediastorage.MediaStorageManagerFactory;
import com.aventstack.extentreports.model.Author;
import com.aventstack.extentreports.model.BasicReportElement;
import com.aventstack.extentreports.model.Category;
import com.aventstack.extentreports.model.ExceptionInfo;
import com.aventstack.extentreports.model.Log;
import com.aventstack.extentreports.model.Media;
import com.aventstack.extentreports.model.ScreenCapture;
import com.aventstack.extentreports.model.Screencast;
import com.aventstack.extentreports.model.SystemAttribute;
import com.aventstack.extentreports.model.Test;
import com.aventstack.extentreports.reporter.AbstractReporter;
import com.aventstack.extentreports.reporter.ReportAppendable;
import com.aventstack.extentreports.reporter.configuration.ExtentXReporterConfiguration;
import com.aventstack.extentreports.utils.MongoUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

public class MyExtentXReporter extends AbstractReporter implements ReportAppendable {
    private static final String DEFAULT_CONFIG_FILE = "extentx-config.properties";
    private static final String DEFAULT_PROJECT_NAME = "Default";
    private Boolean appendExisting;
    private String url;
    private Map<String, ObjectId> categoryNameObjectIdCollection;
    private Map<String, ObjectId> exceptionNameObjectIdCollection;
    private ObjectId reportId;
    private ObjectId projectId;
    private MongoClient mongoClient;
    private MongoCollection<Document> projectCollection;
    private MongoCollection<Document> reportCollection;
    private MongoCollection<Document> testCollection;
    private MongoCollection<Document> logCollection;
    private MongoCollection<Document> exceptionCollection;
    private MongoCollection<Document> mediaCollection;
    private MongoCollection<Document> categoryCollection;
    private MongoCollection<Document> authorCollection;
    private MongoCollection<Document> categoryTestsTestCategories;
    private MongoCollection<Document> authorTestsTestAuthors;
    private MongoCollection<Document> environmentCollection;
    private MediaStorage media;
    private ExtentXReporterConfiguration userConfig;

    MyExtentXReporter() {
        this.loadDefaultConfig();
    }

    public MyExtentXReporter(String host) {
        this();
        this.mongoClient = new MongoClient(host, 27017);
    }

    public MyExtentXReporter(String host, MongoClientOptions options) {
        this();
        this.mongoClient = new MongoClient(host, options);
    }

    public MyExtentXReporter(String host, int port) {
        this();
        this.mongoClient = new MongoClient(host, port);
    }

    public MyExtentXReporter(MongoClientURI uri) {
        this();
        this.mongoClient = new MongoClient(uri);
    }

    public MyExtentXReporter(ServerAddress addr) {
        this();
        this.mongoClient = new MongoClient(addr);
    }

    public MyExtentXReporter(List<ServerAddress> seeds) {
        this();
        this.mongoClient = new MongoClient(seeds);
    }

    public MyExtentXReporter(List<ServerAddress> seeds, List<MongoCredential> credentialsList) {
        this();
        this.mongoClient = new MongoClient(seeds, credentialsList);
    }

    public MyExtentXReporter(List<ServerAddress> seeds, List<MongoCredential> credentialsList, MongoClientOptions options) {
        this();
        this.mongoClient = new MongoClient(seeds, credentialsList, options);
    }

    public MyExtentXReporter(List<ServerAddress> seeds, MongoClientOptions options) {
        this();
        this.mongoClient = new MongoClient(seeds, options);
    }

    public MyExtentXReporter(ServerAddress addr, List<MongoCredential> credentialsList) {
        this();
        this.mongoClient = new MongoClient(addr, credentialsList);
    }

    public MyExtentXReporter(ServerAddress addr, List<MongoCredential> credentialsList, MongoClientOptions options) {
        this();
        this.mongoClient = new MongoClient(addr, credentialsList, options);
    }

    public MyExtentXReporter(ServerAddress addr, MongoClientOptions options) {
        this();
        this.mongoClient = new MongoClient(addr, options);
    }

    public ExtentXReporterConfiguration config() {
        return this.userConfig;
    }

    public void start() {
        this.loadUserConfig();
        MongoDatabase db = this.mongoClient.getDatabase("extent");
        this.projectCollection = db.getCollection("project");
        this.reportCollection = db.getCollection("report");
        this.testCollection = db.getCollection("test");
        this.logCollection = db.getCollection("log");
        this.exceptionCollection = db.getCollection("exception");
        this.mediaCollection = db.getCollection("media");
        this.categoryCollection = db.getCollection("category");
        this.authorCollection = db.getCollection("author");
        this.environmentCollection = db.getCollection("environment");
        this.categoryTestsTestCategories = db.getCollection("category_tests__test_categories");
        this.authorTestsTestAuthors = db.getCollection("author_tests__test_authors");
        this.setupProject();
    }

    private void loadDefaultConfig() {
        this.configContext = new ConfigMap();
        this.userConfig = new ExtentXReporterConfiguration();
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream is = loader.getResourceAsStream("extentx-config.properties");
        this.loadConfig(is);
    }

    private void loadUserConfig() {
        this.userConfig.getConfigMap().forEach((k, v) -> {
            if (v != null) {
                Config c = new Config();
                c.setKey(k);
                c.setValue(v);
                this.configContext.setConfig(c);
            }

        });
    }

    private void setupProject() {
        String projectName = this.configContext.getValue("projectName").toString().trim();
        if (projectName == null || projectName.isEmpty()) {
            projectName = "Default";
        }

        Document doc = new Document("name", projectName);
        Document project = (Document)this.projectCollection.find(doc).first();
        if (project != null) {
            this.projectId = project.getObjectId("_id");
        } else {
            this.projectCollection.insertOne(doc);
            this.projectId = MongoUtil.getId(doc);
        }

        this.setupReport(projectName);
    }

    private void setupReport(String projectName) {
        String reportName = this.configContext.getValue("reportName").toString().trim();
        if (reportName == null || reportName.isEmpty()) {
            reportName = projectName + " - " + Calendar.getInstance().getTimeInMillis();
        }

        Object id = this.configContext.getValue("reportId");
        if (id != null && !id.toString().isEmpty() && this.appendExisting.booleanValue()) {
            FindIterable<Document> iterable = this.reportCollection.find(new Document("_id", new ObjectId(id.toString())));
            Document report = (Document)iterable.first();
            if (report != null) {
                this.reportId = report.getObjectId("_id");
                return;
            }
        }

        Document doc = (new Document("name", reportName)).append("startTime", this.getStartTime()).append("project", this.projectId);
        this.reportCollection.insertOne(doc);
        this.reportId = MongoUtil.getId(doc);
    }

    public void stop() {
        this.mongoClient.close();
    }

    public synchronized void flush() {
        this.setEndTime(Calendar.getInstance().getTime());
        if (this.testList != null && this.testList.size() != 0) {
            Document doc = (new Document("endTime", this.getEndTime())).append("startTime",this.getStartTime()).append("duration", this.getRunDuration()).append("parentLength", this.sc.getParentCount()).append("passParentLength", this.sc.getParentCountPass()).append("failParentLength", this.sc.getParentCountFail()).append("fatalParentLength", this.sc.getParentCountFatal()).append("errorParentLength", this.sc.getParentCountError()).append("warningParentLength", this.sc.getParentCountWarning()).append("skipParentLength", this.sc.getParentCountSkip()).append("exceptionsParentLength", this.sc.getChildCountExceptions()).append("childLength", this.sc.getChildCount()).append("passChildLength", this.sc.getChildCountPass()).append("failChildLength", this.sc.getChildCountFail()).append("fatalChildLength", this.sc.getChildCountFatal()).append("errorChildLength", this.sc.getChildCountError()).append("warningChildLength", this.sc.getChildCountWarning()).append("skipChildLength", this.sc.getChildCountSkip()).append("infoChildLength", this.sc.getChildCountInfo()).append("exceptionsChildLength", this.sc.getChildCountExceptions()).append("grandChildLength", this.sc.getGrandChildCount()).append("passGrandChildLength", this.sc.getGrandChildCountPass()).append("failGrandChildLength", this.sc.getGrandChildCountFail()).append("fatalGrandChildLength", this.sc.getGrandChildCountFatal()).append("errorGrandChildLength", this.sc.getGrandChildCountError()).append("warningGrandChildLength", this.sc.getGrandChildCountWarning()).append("skipGrandChildLength", this.sc.getGrandChildCountSkip()).append("exceptionsGrandChildLength", this.sc.getGrandChildCountExceptions());
            this.reportCollection.updateOne(new Document("_id", this.reportId), new Document("$set", doc));
            this.insertUpdateSystemAttribute();
        }
    }


    private void insertUpdateSystemAttribute() {
        List<SystemAttribute> systemAttrList = this.getSystemAttributeContext().getSystemAttributeList();
        Iterator var3 = systemAttrList.iterator();

        while(var3.hasNext()) {
            SystemAttribute sysAttr = (SystemAttribute)var3.next();
            Document doc = (new Document("project", this.projectId)).append("report", this.reportId).append("name", sysAttr.getName());
            Document envSingle = (Document)this.environmentCollection.find(doc).first();
            if (envSingle == null) {
                doc.append("value", sysAttr.getValue());
                this.environmentCollection.insertOne(doc);
            } else {
                ObjectId id = envSingle.getObjectId("_id");
                doc = (new Document("_id", id)).append("value", sysAttr.getValue());
                this.environmentCollection.updateOne(new Document("_id", id), new Document("$set", doc));
            }
        }

    }

    public void onTestStarted(Test test) {
        Document doc = (new Document("project", this.projectId)).append("report", this.reportId).append("level", test.getLevel()).append("name", test.getName()).append("status", test.getStatus().toString()).append("description", test.getDescription()).append("startTime", test.getStartTime()).append("endTime", test.getEndTime()).append("bdd", test.isBehaviorDrivenType()).append("childNodesLength", test.getNodeContext().size());
        if (test.isBehaviorDrivenType()) {
            doc.append("bddType", test.getBehaviorDrivenType().getSimpleName());
        }

        this.testCollection.insertOne(doc);
        ObjectId testId = MongoUtil.getId(doc);
        test.setObjectId(testId);
    }

    public synchronized void onNodeStarted(Test node) {
        Document doc = (new Document("parent", node.getParent().getObjectId())).append("parentName", node.getParent().getName()).append("project", this.projectId).append("report", this.reportId).append("level", node.getLevel()).append("name", node.getName()).append("status", node.getStatus().toString()).append("description", node.getDescription()).append("startTime", node.getStartTime()).append("endTime", node.getEndTime()).append("childNodesCount", node.getNodeContext().getAll().size()).append("bdd", node.isBehaviorDrivenType()).append("childNodesLength", node.getNodeContext().size());
        if (node.isBehaviorDrivenType()) {
            doc.append("bddType", node.getBehaviorDrivenType().getSimpleName());
        }

        this.testCollection.insertOne(doc);
        ObjectId nodeId = MongoUtil.getId(doc);
        node.setObjectId(nodeId);
        this.updateTestBasedOnNode(node.getParent());
    }

    private void updateTestBasedOnNode(Test test) {
        Document doc = new Document("childNodesLength", test.getNodeContext().size());
        this.testCollection.updateOne(new Document("_id", test.getObjectId()), new Document("$set", doc));
    }

    public synchronized void onLogAdded(Test test, Log log) {
        Document doc = (new Document("test", test.getObjectId())).append("project", this.projectId).append("report", this.reportId).append("testName", test.getName()).append("sequence", log.getSequence()).append("status", log.getStatus().toString()).append("timestamp", log.getTimestamp()).append("details", log.getDetails());
        this.logCollection.insertOne(doc);
        ObjectId logId = MongoUtil.getId(doc);
        log.setObjectId(logId);
        if (test.hasException()) {
            if (this.exceptionNameObjectIdCollection == null) {
                this.exceptionNameObjectIdCollection = new HashMap();
            }

            ExceptionInfo ex = (ExceptionInfo)test.getExceptionInfoList().get(0);
            doc = (new Document("report", this.reportId)).append("project", this.projectId).append("name", ex.getExceptionName());
            FindIterable<Document> iterable = this.exceptionCollection.find(doc);
            Document docException = (Document)iterable.first();
            if (!this.exceptionNameObjectIdCollection.containsKey(ex.getExceptionName())) {
                if (docException != null) {
                    this.exceptionNameObjectIdCollection.put(ex.getExceptionName(), docException.getObjectId("_id"));
                } else {
                    doc = (new Document("project", this.projectId)).append("report", this.reportId).append("name", ex.getExceptionName()).append("stacktrace", ex.getStackTrace()).append("testCount", Integer.valueOf(0));
                    this.exceptionCollection.insertOne(doc);
                    ObjectId exceptionId = MongoUtil.getId(doc);
                    docException = (Document)this.exceptionCollection.find(new Document("_id", exceptionId)).first();
                    this.exceptionNameObjectIdCollection.put(ex.getExceptionName(), exceptionId);
                }
            }

            Integer testCount = ((Integer)((Integer)docException.get("testCount"))).intValue() + 1;
            doc = new Document("testCount", testCount);
            this.exceptionCollection.updateOne(new Document("_id", docException.getObjectId("_id")), new Document("$set", doc));
            doc = new Document("exception", this.exceptionNameObjectIdCollection.get(ex.getExceptionName()));
            this.testCollection.updateOne(new Document("_id", test.getObjectId()), new Document("$set", doc));
        }

        this.endTestRecursive(test);
    }

    private void endTestRecursive(Test test) {
        Document doc = (new Document("status", test.getStatus().toString())).append("endTime", test.getEndTime()).append("duration", test.getRunDurationMillis()).append("categorized", test.hasCategory());
        this.testCollection.updateOne(new Document("_id", test.getObjectId()), new Document("$set", doc));
        if (test.getLevel() > 0) {
            this.endTestRecursive(test.getParent());
        }

    }

    public void onCategoryAssigned(Test test, Category category) {
        if (this.categoryNameObjectIdCollection == null) {
            this.categoryNameObjectIdCollection = new HashMap();
        }

        Document doc;
        if (!this.categoryNameObjectIdCollection.containsKey(category.getName())) {
            doc = (new Document("report", this.reportId)).append("project", this.projectId).append("name", category.getName());
            FindIterable<Document> iterable = this.categoryCollection.find(doc);
            Document docCategory = (Document)iterable.first();
            if (docCategory != null) {
                this.categoryNameObjectIdCollection.put(category.getName(), docCategory.getObjectId("_id"));
            } else {
                doc = (new Document("tests", test.getObjectId())).append("project", this.projectId).append("report", this.reportId).append("name", category.getName()).append("status", test.getStatus().toString()).append("testName", test.getName());
                this.categoryCollection.insertOne(doc);
                ObjectId categoryId = MongoUtil.getId(doc);
                this.categoryNameObjectIdCollection.put(category.getName(), categoryId);
            }
        }

        doc = (new Document("test_categories", test.getObjectId())).append("category_tests", this.categoryNameObjectIdCollection.get(category.getName())).append("category", category.getName()).append("test", test.getName());
        this.categoryTestsTestCategories.insertOne(doc);
    }

    public void onAuthorAssigned(Test test, Author author) {
        Document doc = (new Document("tests", test.getObjectId())).append("project", this.projectId).append("report", this.reportId).append("name", author.getName()).append("status", test.getStatus().toString()).append("testName", test.getName());
        this.authorCollection.insertOne(doc);
        ObjectId authorId = MongoUtil.getId(doc);
        doc = (new Document("test_authors", test.getObjectId())).append("author_tests", authorId).append("author", author.getName()).append("test", test.getName());
        this.authorTestsTestAuthors.insertOne(doc);
    }

    public void onScreenCaptureAdded(Test test, ScreenCapture screenCapture) throws IOException {
        this.initOnScreenCaptureAdded(screenCapture);
        this.createMedia(test, screenCapture);
        this.storeMedia(screenCapture);
    }

    public void onScreenCaptureAdded(Log log, ScreenCapture screenCapture) throws IOException {
        screenCapture.setLogObjectId(log.getObjectId());
        this.initOnScreenCaptureAdded(screenCapture);
        this.createMedia(log, screenCapture);
        this.storeMedia(screenCapture);
    }

    private void storeMedia(ScreenCapture screenCapture) throws IOException {
        this.media.storeMedia(screenCapture);
    }

    private void initOnScreenCaptureAdded(ScreenCapture screenCapture) throws IOException {
        this.storeUrl();
        screenCapture.setReportObjectId(this.reportId);
        this.initMedia();
    }

    private void storeUrl() throws IOException {
        if (this.url == null) {
            Object url = this.configContext.getValue("serverUrl");
            if (url == null) {
                throw new IOException("server url cannot be null, use extentx.config().setServerUrl(url)");
            }

            this.url = url.toString().trim();
        }

    }

    private void createMedia(BasicReportElement el, Media media) {
        Document doc = (new Document("project", this.projectId)).append("report", this.reportId).append("sequence", media.getSequence()).append("mediaType", media.getMediaType().toString().toLowerCase());
        if (el.getClass() == Test.class) {
            doc.append("test", el.getObjectId()).append("testName", ((Test)el).getName());
        } else {
            doc.append("log", el.getObjectId());
        }

        this.mediaCollection.insertOne(doc);
        ObjectId mediaId = MongoUtil.getId(doc);
        media.setObjectId(mediaId);
    }

    private void initMedia() throws IOException {
        if (this.media == null) {
            this.media = (new MediaStorageManagerFactory()).getManager("http");
            this.media.init(this.url);
        }

    }

    public void onScreencastAdded(Test test, Screencast screencast) throws IOException {
        this.storeUrl();
        screencast.setReportObjectId(this.reportId);
        this.createMedia(test, screencast);
        this.initMedia();
        this.media.storeMedia(screencast);
    }

    public void setTestList(List<Test> reportTestList) {
        this.testList = reportTestList;
    }

    public List<Test> getTestList() {
        if (this.testList == null) {
            this.testList = new ArrayList();
        }

        return this.testList;
    }

    public void setAppendExisting(Boolean b) {
        this.appendExisting = b;
    }

    public ObjectId getProjectId() {
        return this.projectId;
    }

    public ObjectId getReportId() {
        return this.reportId;
    }

    static {
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);
    }
}
