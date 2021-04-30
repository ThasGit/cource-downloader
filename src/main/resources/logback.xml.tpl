<configuration>
    <property name="LOG_HOME" value="${user.home}/logs/cource-downloader"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>　
            <!--            <pattern>%-4relative [%thread] %-5level %logger{35} - %msg %n</pattern>-->
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度%msg：日志消息，%n是换行符-->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50}:%line - %msg%n</pattern>
            <charset>utf-8</charset>
        </encoder>
    </appender>

    <!-- 按照每天生成日志文件 -->
    <appender name="HTTP_FILE_INFO" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_HOME}/http.log</file>
        <!-- 追加方式记录日志 -->
        <append>true</append>
        <!-- 日志记录器的滚动策略，按日期，按大小记录 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- 归档的日志文件的路径，例如今天是2013-12-21日志，当前写的日志文件路径为file节点指定，可以将此文件与file指定文件路径设置为不同路径，从而将当前日志文件或归档日志文件置不同的目录。
            而2013-12-21的日志文件在由fileNamePattern指定。%d{yyyy-MM-dd}指定日期格式，%i指定索引 -->
            <FileNamePattern>${LOG_HOME}/http.%d{yyyy-MM-dd}.%i.log.gz</FileNamePattern>
            <!--日志文件保留天数-->
            <MaxHistory>30</MaxHistory>
            <totalSizeCap>1GB</totalSizeCap>
            <maxFileSize>100MB</maxFileSize>
        </rollingPolicy>

        <!-- 日志文件的格式 -->
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50}:%line - %msg%n</pattern>
            <charset>utf-8</charset>
        </encoder>

        <!-- 此日志文件只记录info级别的 -->
        <!--        <filter class="ch.qos.logback.classic.filter.LevelFilter">-->
        <!--            <level>info</level>-->
        <!--            <onMatch>ACCEPT</onMatch>-->
        <!--            <onMismatch>DENY</onMismatch>-->
        <!--        </filter>-->
    </appender>

    <!-- 异步输出 -->
    <appender name="ASYNC_HTTP_FILE_INFO" class="ch.qos.logback.classic.AsyncAppender">
        <!-- 不丢失日志.默认的,如果队列的80%已满,则会丢弃TRACT、DEBUG、INFO级别的日志 -->
        <discardingThreshold>0</discardingThreshold>
        <!-- 更改默认的队列的深度,该值会影响性能.默认值为256 -->
        <queueSize>256</queueSize>
        <!-- 添加附加的appender,最多只能添加一个 -->
        <appender-ref ref="HTTP_FILE_INFO"/>
    </appender>

    <logger name="cc.thas.http" level="INFO" additivity="false">
        <appender-ref ref="ASYNC_HTTP_FILE_INFO"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>