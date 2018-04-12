# ReportX

Report Server for TestNG Report
![image](http://gitlab.leoao-inc.com/qa/ReportX/raw/master/assets/images/dashboard.png)

### 部署

* 下载安装node.js
* 下载安装mongodb  <3.6 验证过3.2.19
* 配置mongodb并运行 mongod  --fork   --storageEngine=mmapv1 --dbpath /data/mongodb --logpath /data/mongodb/log/mongodb.log
* 检出ReportX，解压到某一目录
* 进入解压目录中，使用命令 npm install
* 使用命令 sails lift 启动服务（调试使用）
* 生产部署  npm -g forever
* 进入ReportX 目录  forever start -l forever.log -o out.log -e err.log app.js
* http://localhost:1337 


### 引用
http://extentreports.com/docs/extentx/

### 默认密码
user:      root
password:  password

