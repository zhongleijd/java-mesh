# Sermant-Hercules
基于NGrinder3.4.2开发的压测服务项目。<br />
ngrinder-controller模块是原ngrinder的controller端。<br />
ngrinder-controller-service是基于springboot重构的新controller版本，去掉了spring-security、liquibase和web层。
本说明主要是基于重构之后的版本进行。
### 一. 依赖环境
    （1）JDK1.8  
    （2）Mysql5.7或者gaussdb2.0.0
    （3）Maven 3.5以上
### 二. 数据库配置
支持mysql和gaussdb两种数据库。<br />
创建名为hercules的数据库，该数据库无法自动创建，需要人工提前创建。
#### 1. MySql建库
```
create database `hercules` character set utf8 collate utf8_general_ci;
```
#### 2. Gaussdb建库
```
CREATE DATABASE hercules ENCODING 'UTF-8' template = template0;
```
### 三. maven配置
#### 1. Maven配置
 maven主要作用是执行maven project类型脚本时需要进行maven编译。<br />
 保证maven中配置的镜像地址能成功访问，并且能拉取脚本中依赖的jar包。<br />
 以下内容为举例：
```
    <!--本地仓库地址-->
    <localRepository>/opt/maven-respository</localRepository>

    <mirrors>
        <!-- mirror
         | Specifies a repository mirror site to use instead of a given repository. The repository that
         | this mirror serves has an ID that matches the mirrorOf element of this mirror. IDs are used
         | for inheritance and direct lookup purposes, and must be unique across the set of mirrors.
         |
		-->

        <mirror>
            <id>nexus-aliyun-central</id>
            <name>aliyun4</name>
            <url>http://maven.aliyun.com/nexus/content/repositories/central/</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
```
### 四. Controller部署
#### 1. 新增nGrinder的home目录
```
mkdir /ngrinder_home
mkdir /ngrinder_ex_home
```

#### 2. 设置nGrinder的环境变量
这里是演示实例，所以只设置了一下临时变量，用户可考虑直接在/etc/profile配置成环境变量
```
export NGRINDER_HOME=/ngrinder_home
export NGRINDER_EX_HOME=/ngrinder_ex_home
```
> NGRINDER_HOME：controller配置主目录，集群部署时，需要使用共享文件系统处理，例如nfs共享目录来作为NGRINDER_HOME。 <br />
> NGRINDER_EX_HOME：controller集群部署的时候，单节点独立配置和数据存放的目录，单节点部署时没有使用该目录。 <br />
> 单节点不配置以上环境变量会默认在用户home目录下创建数据目录：~/.ngrinder和~/.ngrinder_ex。
#### 3. 在NGRINDER_HOME中新增配置文件database.conf
```
# H2 / cubrid / mysql / gaussdb can be set
database.type=mysql

# for cubrid. You should configure the following.
# database.url=localhost:33000:ngrinder

# for H2 remote connection, You should configure like followings.
# You can see how to run the H2 DB server by yourself in http://www.h2database.com/html/tutorial.html#using_server
# If this is not set, ngrinder will create the embedded DB.
# Specify database url
database.url=127.0.0.1:3306/hercules

# if you want to use HA mode in cubrid, you should enable following
database.url_option=useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai

# you should provide id / password who has a enough permission to create tables in the given db.
database.username=root
database.password=123456
```

#### 4. 在项目最上层目录使用maven命令打包
 代码仓地址
```
https://github.com/huaweicloud/Sermant/tree/develop
```
 使用maven打包,在sermant-plugins/sermant-hercules/ngrinder目录下执行命令

```mvn clean package -Dmaven.test.skip=true```
> maven命令执行完成后，ngrinder-controller-service模块中的target目录中ngrinder-controller-service-3.4.2.jar就是启动的springboot项目。
#### 5. 启动controller
##### 5.1 单节点部署controller
直接启动controller即可
```
java -jar ngrinder-controller-service-3.4.2.jar
```
> 启动命令未做多余配置，用户可根据自己需求添加适合部署服务器的参数。

##### 5.2 集群部署controller
第一步：需要把NGRINDER_HOME配置为NFS共享目录中的路径。
> NFS部署和配置步骤比较简便，用户自行查资料配置即可 

第二步：在NGRINDER_HOME中增加system.conf配置文件，并添加如下参数
```
# Set verbose to print the detailed log 
#controller.verbose=true

# If dev_mode is true, the log goes to stdout and
# the security mode and cluster config verification are disabled.
# In addition, agent force update and auto approval is enabled.
# finally the script console is activated as well.
#controller.dev_mode=false

# true if enabling security manager. The default value is false
#controller.security=false

# Determine security level of security manager. It only works if controller.security is true.
# If you set 'controller.security.level=light', The less security will be applied. The default value is normal.
#controller.security.level=normal

# true if the password change should not be allowed.
#controller.demo_mode=false

# false if disabling usage report. it will send host ip and executed tests number to Google Analytics.
# The default value is true
#controller.usage_report=true


# true if the plugin should be enabled.
# This is not the option applied on the fly. You need to restart the controller.
#controller.plugin_support=true

# false if you want to make some of the user profile fields not mandatory. Default value is false.
#controller.user_security=false

# true if you want to controller uses high level password encoding(sha256).
#controller.user_password_sha256=false


# The maximum number of agents which can be attached per one test.
#controller.max_agent_per_test=10

# The maximum number of vusers which can be initiated per one agent
# This should be carefully selected depending on the agent memory size.
#controller.max_vuser_per_agent=3000

# The maximum test runcount for one thread
#controller.max_run_count=10000

# The maximum running hour for one test.
#controller.max_run_hour=8

# The maximum count of concurrent tests.
#controller.max_concurrent_test=10

# The monitor connecting port. The default value is 13243.
#controller.monitor_port=13243

# The base URL of the controller. If not set, the controller URL is automatically selected.
#controller.url=

# The host name or IP of agents connection to the controller.
# If not set, controller binds to all currently available IPs.
#controller.host=

# The port for the agents connection to the controller. The default value is 16001
#controller.controller_port=16001

# The starting port number of consoles which will be mapped to each test.
# This is not the option applied on the fly. You need to reboot to apply this.
#controller.console_port_base=12000

# validation timeout in the unit of sec.
#controller.validation_timeout=100

# true if you want to make the script console available to diagnose ngrinder controller.
#controller.enable_script_console=false

# true if you want to make the agent automatically approved. The default value is false
#controller.enable_agent_auto_approval=false

# If your agent is located in the far places and the transmission is not reliable, you'd better to change this to true.
#controller.safe_dist=false

# Set the safe distribution threshold to enable safe distribution for specific transfer size by force.
#controller.safe_dist_threshold=1000000

# true if you want to allow users to sign up by themselves.
#controller.allow_sign_up=true

# If you server is behind the firewall which blocks the external access. please make this false.
#controller.front_page_enabled=true

# Point your own resources rss in you want to show in the front page.
#controller.front_page_resources_rss=https://github.com/naver/ngrinder/wiki.rss

# You can point your own QnA rss in the front page
#controller.front_page_qna_rss=

# You can point your own QnA ask a question URL
#controller.front_page_ask_question_url=

# You can point your own QnA site URL
#controller.front_page_qna_more_url=

# If you want to provide your own custom help page. please modify this
#controller.help_url=https://github.com/naver/ngrinder/wiki

# How much size of each agent update package is. The default is 1024*1024
# If it's bigger, agent update speed is higher but easy to be broken.
#controller.update_chunk_size=1024576

# Make the agent always updated even when the the same or latest agent is already deployed.
#controller.agent_force_update=false

# The default user language. en/kr/cn are available.
#controller.default_lang=en

# The default inactive client time out milliseconds.
# It might affect to make timed out of the socket connection between the console and the agent.
# If it didn't set or less than 0, it will use 30000 as the default.
#controller.inactive_client_time_out=30000

# The default value false
# If you set true, it provides statistic data(json format) as ehcache, dbcp, and so on by using restful api.
# Now, it supports http://HOST/stat APIs.
#controller.enable_statistics=true

# separator for csv report. comma is the default. tab/semicolon can be specified.
#controller.csv_separator=comma

######################################################################################
# clustering configuration.
# This is not the option applied on the fly. You need to reboot to apply this.
######################################################################################
# These should be very carefully set. 
# You can refer http://www.cubrid.org/wiki_ngrinder/entry/controller-clustering-guide

# if you want to enable controller clustering. please enable below.
cluster.enabled=true

# comma separated IP list of all clustered controller servers.
cluster.members=192.168.1.1;192.168.2.2;192.168.3.3

# cluster communication port. This port should be same across the controllers if advanced cluster mode is enabled.
cluster.port=40003
```
> 这里只打开集群配置必须参数 <br />
> cluster.enabled=true <br />
> cluster.members=192.168.1.1;192.168.2.2;192.168.3.3 <br />
> cluster.port=40003 <br />
> 其他参数用户可根据说明调整

第三步：在NGRINDER_EX_HOME中增加system-ex.conf配置文件，并添加如下参数
```
#Console binding IP of this region. If not set, console will be bound to all available IPs.
#cluster.host=

# cluster communication port. This port should be different across the controllers if easy cluster mode is enabled.
cluster.port=40003

cluster.region=Beijing

# true if the current region should be hide
#cluster.hidden_region=false

# true if the current region's file distribution should be done in safe way.
#cluster.safe_dist=false
```
> 这是集群中每一个controller独立配置，cluster.region是必须指定的，region用于区分controller已经
> 每一个controller建立的压测任务和挂载的agent

第四步：确定数据库需是mysql或者gaussdb，即NGRINDER_HOME中database.conf的配置确认
```
# H2 / cubrid / mysql / gaussdb can be set
database.type=mysql

# for cubrid. You should configure the following.
# database.url=localhost:33000:ngrinder

# for H2 remote connection, You should configure like followings.
# You can see how to run the H2 DB server by yourself in http://www.h2database.com/html/tutorial.html#using_server
# If this is not set, ngrinder will create the embedded DB.
# Specify database url
database.url=127.0.0.1:3306/hercules

# if you want to use HA mode in cubrid, you should enable following
database.url_option=useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai

# you should provide id / password who has a enough permission to create tables in the given db.
database.username=root
database.password=123456
```
第五步：在配置好的各节点分别启动controller
```
java -jar ngrinder-controller-service-3.4.2.jar
```
> 启动命令未做多余配置，用户可根据自己需求添加适合部署服务器的参数。

### 五. 部署agent
#### 1. 在上面项目启动之后，下载agent
获取agent包名称接口：http://127.0.0.1:9091/rest/agent/download/ <br />
下载agent包接口：http://127.0.0.1:9091/rest/agent/download/{packageName} 
> packageName：获取到的agent包名称

#### 2. 上传agent的tar包到指定服务器之后解压，执行结果如下
```
[root@ecs-flow-0005 ngrinder-agent]# pwd
/opt/package/ngrinder-agent
[root@ecs-flow-0005 ngrinder-agent]# ll
total 496
-rw-r--r-- 1 root root    535 Oct 14 10:16 __agent.conf
drwxr-xr-x 2 root root   4096 Oct  8 11:26 lib
-rwxr-xr-x 1 root root    367 Aug 24 15:30 run_agent.bat
-rwxr-xr-x 1 root root     83 Aug 24 15:30 run_agent_bg.sh
-rwxr-xr-x 1 root root    237 Aug 24 15:30 run_agent_internal.bat
-rwxr-xr-x 1 root root     99 Aug 24 15:30 run_agent_internal.sh
-rwxr-xr-x 1 root root    312 Aug 24 15:30 run_agent.sh
-rw-r--r-- 1 root root 463149 Oct 28 20:06 run.log
-rwxr-xr-x 1 root root    135 Aug 24 15:30 stop_agent.bat
-rwxr-xr-x 1 root root    136 Aug 24 15:30 stop_agent.sh
[root@ecs-flow-0005 ngrinder-agent]#

```
#### 3. agent启动命令参数解析
```
Usage: run_agent_bg.sh [options]
  Options:
    -ah, --agent-home
       this agent's unique home path. The default is ~/.ngrinder_agent
    -ch, --controller-host
       controller host or ip.
    -cp, --controller-port
       controller port.
    -hi, --host-id
       this agent's unique host id
    -o, --overwrite-config
       overwrite overwrite the existing .ngrinder_agent/agent.conf with the
       local __agent.conf
    -r, --region
       region
    -s, --silent
       silent mode
    -v, --version
       show version
    -help, -?, -h
       prints this message
```
#### 4. 当然也可以通过配置文件启动，修改agent当前目录中的__agent.conf文件，内容如下，然后使用[run_agent_bg.sh -o -ah ${home_path}]启动
```
common.start_mode=agent
# controller ip
agent.controller_host=127.0.0.1

# controller port for agent
agent.controller_port=16001

# agent region, 如果是单节点部署指定为NONE，如果是集群部署，需要指定为挂载的controller的region
agent.region=NONE

#agent.host_id=
#agent.server_mode=true

# provide more agent java execution option if necessary.
#agent.java_opt=
# set following false if you want to use more than 1G Xmx memory per a agent process.
#agent.limit_xmx=true
# please uncomment the following option if you want to send all logs to the controller.
#agent.all_logs=true
# some jvm is not compatible with DNSJava. If so, set this false.
#agent.enable_local_dns=false
```
>${home_path}就是该agent配置和数据保存的目录，切记一定要指定清楚 <br />
> agent.controller_host指定的controller的region一定要和agent.region配置的一致，否则压测任务无法执行

到此处，nGrinder controller和agent就部署完毕！
