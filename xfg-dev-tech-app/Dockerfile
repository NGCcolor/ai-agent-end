# 基础镜像
FROM openjdk:17-jdk-slim

# 作者
MAINTAINER wgl

# 配置
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加主应用
ADD target/ai-rag-knowledge-app.jar /ai-rag-knowledge-app.jar

# 👇👇👇 添加这一行：把外部的 MCP 插件包也放进容器的根目录下
ADD mcp-server-csdn-1.0.0.jar /mcp-server-csdn-1.0.0.jar
ADD mcp-server-weixin-app.jar /mcp-server-weixin-app.jar
# 启动命令
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /ai-rag-knowledge-app.jar $PARAMS"]
#FROM openjdk:17-jdk-slim
#MAINTAINER wgl
#ENV PARAMS=""
#ENV TZ=PRC
#RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
#ADD target/ai-rag-knowledge-app.jar /ai-rag-knowledge-app.jar
#ADD mcp-server-csdn-1.0.0.jar /mcp-server-csdn-1.0.0.jar
#ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /ai-rag-knowledge-app.jar $PARAMS"]
