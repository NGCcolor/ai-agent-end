package domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import domain.agent.model.entity.ArmoryCommandEntity;
import domain.agent.model.valobj.Enums.AiAgentEnumVO;
import domain.agent.model.valobj.AiClientToolMcpVO;
import domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MCP客户端配置节点
 * 角色：流水线上的“武器锻造与分发官”
 * 作用：把数据库里配置的外部工具（比如本地脚本、远程API）变成 Spring 容器里随时待命的 McpSyncClient 组件。
 */
@Slf4j
@Service
public class AiClientToolMcpNode extends AbstractArmorySupport {

    // 🌟 提前剧透：下一个车间是“模型车间”
    @Resource
    private AiClientModelNode aiClientModelNode;

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，Tool MCP 工具配置{}", JSON.toJSONString(requestParameter));

        // 步骤 1：从“万能托盘”里摸出 MCP 工具的配置图纸（List）
        List<AiClientToolMcpVO> aiClientToolMcpList = dynamicContext.getValue(dataName());

        // 防御性编程：如果这个机器人没配工具，直接放行，把托盘传给下一站
        if (aiClientToolMcpList == null || aiClientToolMcpList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client tool mcp");
            return router(requestParameter, dynamicContext);
        }

        // 步骤 2：拿着图纸，挨个锻造武器
        for (AiClientToolMcpVO mcpVO : aiClientToolMcpList) {

            // 🔥 核心动作 A：现场打造 MCP 客户端（连接外部工具的探针）
            McpSyncClient mcpSyncClient = createMcpSyncClient(mcpVO);

            // 🔥 核心动作 B：武器上户口！
            // 把造好的 MCP 客户端塞进 Spring 容器，名字类似于：ai_client_tool_mcp_5001
            registerBean(beanName(mcpVO.getMcpId()), McpSyncClient.class, mcpSyncClient);
        }

        // 步骤 3：本工段完工，放行！
        return router(requestParameter, dynamicContext);
    }

    /**
     * 🔥 路由指引：干完活后去哪？
     * 答案：去 `AiClientModelNode`（模型组装车间）。
     * 因为 API 通讯配好了，MCP 武器也配好了，接下来该把它们塞进 AI 大脑里了！
     */
    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientModelNode;
    }

    // --- 两个辅助方法：获取标准化的 Bean 名字和盘子里的 Key ---
    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_TOOL_MCP.getBeanName(beanId);
    }
    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_TOOL_MCP.getDataName();
    }

    // =====================================================================
    // 👇👇👇 下面是真正的硬核锻造技术：如何连接外部 MCP 服务 👇👇👇
    // =====================================================================
    /**
     * MCP 客户端实例化工厂方法
     * 核心作用：根据数据库里配的工具信息（AiClientToolMcpVO），建立与底层真实工具（Python脚本/Node服务/远程API）的物理连接。
     *
     * @param aiClientToolMcpVO 从数据库 ai_client_tool_mcp 表里查出来的配置实体
     * @return McpSyncClient 包装好的、已激活的标准 MCP 客户端
     */
    private McpSyncClient createMcpSyncClient(AiClientToolMcpVO aiClientToolMcpVO) {
        // 获取传输协议类型（目前 MCP 官方主推两种：sse 走网络，stdio 走本地命令行）
        String transportType = aiClientToolMcpVO.getTransportType();

        switch (transportType) {

            // =========================================================================
            // 模式 1：SSE (Server-Sent Events) - 适用于【远程/独立容器】部署的 MCP 工具
            // 机制：通过 HTTP 建立长连接，服务器可以随时向 Java 客户端推送执行结果。
            // =========================================================================
            case "sse" -> {
                // 获取数据库里存的 JSON 配置（比如 baseUri 等）
                AiClientToolMcpVO.TransportConfigSse transportConfigSse = aiClientToolMcpVO.getTransportConfigSse();

                // --- 下面这段看似繁琐的字符串截取，是为了“智能解析URL” ---
                // 假设数据库配的是："http://192.168.1.108:8101/sse?apikey=123"
                String originalBaseUri = transportConfigSse.getBaseUri();
                String baseUri;
                String sseEndpoint;

                // 寻找 "sse" 关键字的位置
                int queryParamStartIndex = originalBaseUri.indexOf("sse");
                if (queryParamStartIndex != -1) {
                    // 如果找到了，把字符串劈成两半：
                    // 前半部分作为主机地址：http://192.168.1.108:8101
                    baseUri = originalBaseUri.substring(0, queryParamStartIndex - 1);
                    // 后半部分作为接口路由及参数：/sse?apikey=123
                    sseEndpoint = originalBaseUri.substring(queryParamStartIndex - 1);
                } else {
                    // 如果没带 /sse，就直接用原地址，并从配置里拿 endpoint（通常就是 "/sse"）
                    baseUri = originalBaseUri;
                    sseEndpoint = transportConfigSse.getSseEndpoint();
                }

                // 兜底策略：如果没配 endpoint，默认给个 "/sse"
                sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

                // 1. 铺设底层网络管道：使用解析好的 IP 和路由，构建 HTTP SSE 传输通道
                HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                        .builder(baseUri)
                        .sseEndpoint(sseEndpoint)
                        .build();

                // 2. 套上 MCP 标准壳子：包装成同步客户端，并设置请求超时时间（这里单位是“分钟”）
                McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport)
                        .requestTimeout(Duration.ofMinutes(aiClientToolMcpVO.getRequestTimeout()))
                        .build();

                // 3. 🔥 极其重要的一步：握手激活 (Handshake)！
                // 此时 Java 程序会真正向目标 URL 发送一个初始化网络请求。
                // 对方返回自己支持哪些工具（比如：我有“发帖工具”、“查询工具”），MCP 客户端才算正式就绪。
                var init_sse = mcpSyncClient.initialize();
                log.info("Tool SSE MCP Initialized {}", init_sse);

                // 4. 交货：返回随时可以调用的客户端
                return mcpSyncClient;
            }

            // =========================================================================
            // 模式 2：Stdio (Standard Input/Output) - 适用于【本地电脑/同服务器】的脚本
            // 机制：不走网络端口！直接在操作系统底层通过 `Runtime.exec()` 类似的方式唤起黑窗口。
            // =========================================================================
            case "stdio" -> {
                // 获取数据库中 Stdio 模式的特有配置
                AiClientToolMcpVO.TransportConfigStdio transportConfigStdio = aiClientToolMcpVO.getTransportConfigStdio();
                // 解析 JSON 里的指令映射表
                Map<String, AiClientToolMcpVO.TransportConfigStdio.Stdio> stdioMap = transportConfigStdio.getStdio();
                // 根据当前 MCP 工具的名称（比如 "g-search"），精准拿到它的启动命令
                AiClientToolMcpVO.TransportConfigStdio.Stdio stdio = stdioMap.get(aiClientToolMcpVO.getMcpName());

                // 1. 组装操作系统进程指令
                // 相当于准备在终端敲入： npx -y @modelcontextprotocol/server-filesystem /Users/xxx
                var stdioParams = ServerParameters.builder(stdio.getCommand()) // 主命令，比如 npx 或 python
                        .args(stdio.getArgs()) // 后面的参数列表
                        .env(stdio.getEnv())   // 注入需要的系统环境变量
                        .build();

                // 2. 铺设本地进程管道：StdioClientTransport 会在后台静默启动那个外部脚本，并把输入输出流对接起来。
                // 同样套上 MCP 标准壳子，注意这里超时时间的单位是“秒” (ofSeconds)
                var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
                        .requestTimeout(Duration.ofSeconds(aiClientToolMcpVO.getRequestTimeout()))
                        .build();

                // 3. 🔥 握手激活！
                // 此时 Java 会通过标准输入流（管道）向那个刚刚启动的 Node.js/Python 进程发送初始化指令，进程通过标准输出流返回工具清单。
                var init_stdio = mcpClient.initialize();
                log.info("Tool Stdio MCP Initialized {}", init_stdio);

                // 4. 交货
                return mcpClient;
            }
        }

        // 兜底异常拦截：如果数据库里配了既不是 sse 也不是 stdio 的垃圾数据，直接阻断程序抛出异常
        throw new RuntimeException("err! transportType " + transportType + " not exist!");
    }
}