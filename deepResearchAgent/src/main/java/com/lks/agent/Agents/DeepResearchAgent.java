package com.lks.agent.Agents;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.FilesystemInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.LargeResultEvictionInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.PatchToolCallsInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.SubAgentInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.SubAgentSpec;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static com.lks.agent.Agents.DeepResearchAgent.Prompts.researchInstructions;
import static com.lks.agent.Agents.DeepResearchAgent.Prompts.subCritiquePrompt;
import static com.lks.agent.Agents.DeepResearchAgent.Prompts.subResearchPrompt;


public class DeepResearchAgent {

    private static final String BASE_AGENT_PROMPT =
            "In order to complete the objective that the user asks of you, " +
                    "you have access to a number of standard tools.";

    private String systemPrompt;
    private ChatModel chatModel;

    // ==================== 拦截器（Interceptors）====================
    /** 大结果拦截器 - 当工具返回结果过大时自动保存到文件系统 */
    private LargeResultEvictionInterceptor largeResultEvictionInterceptor;
    /** 文件系统拦截器 - 控制对文件系统的读写操作 */
    private FilesystemInterceptor filesystemInterceptor;
    /** 待办事项列表拦截器 - 管理和跟踪研究任务进度 */
    private TodoListInterceptor todoListInterceptor;
    /** 工具调用补丁拦截器 - 修复或增强工具调用行为 */
    private PatchToolCallsInterceptor patchToolCallsInterceptor;
    /** 上下文编辑拦截器 - 当上下文过长时自动压缩 */
    private ContextEditingInterceptor contextEditingInterceptor;
    /** 工具重试拦截器 - 处理工具有失败时的重试逻辑 */
    private ToolRetryInterceptor toolRetryInterceptor;

    // ==================== 钩子（Hooks）====================
    /** 摘要钩子 - 当对话历史过长时自动生成摘要 */
    private SummarizationHook summarizationHook;
    /** 人类参与钩子 - 在关键操作前请求人类审批 */
    private HumanInTheLoopHook humanInTheLoopHook;
    /** 工具调用限制钩子 - 限制单次运行的工具调用次数 */
    private ToolCallLimitHook toolCallLimitHook;

    /**
     * DeepResearch代理构造函数
     *
     * 初始化整个代理系统，包括：
     * 1. 配置大语言模型连接
     * 2. 设置系统提示词
     * 3. 初始化所有拦截器
     * 4. 初始化所有钩子
     *
     * 注意：需要环境变量 AI_DASHSCOPE_API_KEY 来访问阿里云百炼API
     */
    public DeepResearchAgent() {
        // ==================== 初始化 ChatModel ====================
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
        this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

        // 组合研究指令和基础代理提示词
        this.systemPrompt = researchInstructions + "\n\n" + BASE_AGENT_PROMPT;

        // ==================== 拦截器 ====================

        /**
         * 配置大结果驱逐拦截器
         * 功能：当工具返回的结果超过指定token数量时，自动将其保存到文件系统
         * 配置：排除文件系统工具本身，避免递归；设置5000 token阈值
         */
        this.largeResultEvictionInterceptor = LargeResultEvictionInterceptor
                .builder()
                .excludeFilesystemTools()  // 排除文件系统工具，防止递归操作
                .toolTokenLimitBeforeEvict(5000)  // 当工具结果超过5000 tokens时触发驱逐
                .build();

        /**
         * 配置文件系统拦截器
         * 功能：控制代理对文件系统的读写权限
         * 配置：允许读写操作（非只读模式）
         */
        this.filesystemInterceptor = FilesystemInterceptor.builder()
                .readOnly(false)  // 允许读写文件系统
                .build();

        /**
         * 配置待办事项列表拦截器
         * 功能：管理研究任务的计划和进度跟踪
         */
        this.todoListInterceptor = TodoListInterceptor.builder().build();

        /**
         * 配置工具调用补丁拦截器
         * 功能：修复或增强工具调用的行为
         */
        this.patchToolCallsInterceptor = PatchToolCallsInterceptor.builder().build();

        /**
         * 配置工具重试拦截器
         * 功能：处理工具调用失败的情况
         * 配置：最多重试1次，失败时返回错误消息
         */
        this.toolRetryInterceptor = ToolRetryInterceptor.builder()
                .maxRetries(1)  // 最多重试1次
                .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)  // 失败时返回消息
                .build();

        // ==================== 初始化钩子 ====================

        /**
         * 配置摘要钩子
         * 功能：当对话历史过长时自动生成摘要以节省上下文空间
         * 配置：当达到120000 tokens时触发摘要，保留最近6条消息
         * 注意：建议使用专门的摘要模型而不是主聊天模型
         */
        this.summarizationHook = SummarizationHook.builder()
                .model(chatModel) // TODO: 应该使用另一个专门的摘要模型
                .maxTokensBeforeSummary(120000)  // 达到12万tokens时生成摘要
                .messagesToKeep(6)  // 保留最近6条消息
                .build();

        /**
         * 配置人类参与钩子
         * 功能：在执行特定敏感操作前请求人类审批
         * 配置：对web搜索工具调用要求人工批准
         */
        this.humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("jina_search", "Please approve the jina_search tool.")  // web搜索需要批准
                .approvalOn("download_paper", "Please approve the download_paper tool.")
                .build();

        /**
         * 配置工具调用限制钩子
         * 功能：防止单次运行中工具调用次数过多
         * 配置：限制每次运行最多调用25次工具
         */
        this.toolCallLimitHook = ToolCallLimitHook.builder()
                .runLimit(25)  // 单次运行限制25次工具调用
                .build();

        /**
         * 配置上下文编辑拦截器
         * 功能：当上下文长度超过阈值时自动清理和压缩
         * 配置：达到10000 tokens时触发，至少清理6000 tokens，保留4条消息
         * 特殊处理：排除write_todos工具以保持待办事项完整性
         */
        this.contextEditingInterceptor = ContextEditingInterceptor.builder()
                .trigger(10000)  // 触发阈值：10000 tokens
                .clearAtLeast(6000)  // 至少清理6000 tokens
                .keep(4)  // 保留最近4条消息
                .excludeTools("write_todos")  // 排除待办事项写入工具
                .build();

    }

    /**
     * 创建报告评审子代理规范
     *
     * @param subCritiquePrompt 评审代理使用的系统提示词
     * @return SubAgentSpec 评审子代理规范
     */
    private static SubAgentSpec createCritiqueAgent(String subCritiquePrompt) {
        return SubAgentSpec.builder()
                .name("critique-agent")  // 子代理名称
                .description("Used to critique the final report. Provide information about " +
                        "how you want the report to be critiqued.")  // 代理描述
                .systemPrompt(subCritiquePrompt)  // 系统提示词
                .enableLoopingLog(true)  // 启用循环日志记录
                .build();
    }

    /**
     * 创建深度研究子代理规范
     *
     * @param toolsFromMcp 来自MCP客户端的工具回调列表
     * @param subResearchPrompt 研究代理使用的系统提示词
     * @return SubAgentSpec 研究子代理规范
     */
    private static SubAgentSpec createResearchAgent(List<ToolCallback> toolsFromMcp, String subResearchPrompt) {
        return SubAgentSpec.builder()
                .name("research-agent")  // 子代理名称
                .description("Used to research in-depth questions. Only give one topic at a time. " +
                        "Break down large topics into components and call multiple research agents " +
                        "in parallel for each sub-question.")  // 代理描述：专注单一主题，可并行处理子问题
                .systemPrompt(subResearchPrompt)  // 系统提示词
                .tools(toolsFromMcp)  // 可用工具列表
                .enableLoopingLog(true)  // 启用循环日志记录
                .build();
    }

    /**
     * 获取配置好的研究代理实例
     *
     * 构建完整的ReactAgent实例，整合所有组件：
     * - 主聊天模型
     * - MCP工具集合
     * - 系统提示词
     * - 拦截器链
     * - 钩子机制
     * - 状态保存器
     *
     * @param toolsFromMcp 来自MCP客户端的工具回调列表
     * @return ReactAgent 配置完成的研究代理实例
     */
    public ReactAgent getResearchAgent(List<ToolCallback> toolsFromMcp) {
        // 使用建造者模式构建ReactAgent，配置所有必要组件
        return ReactAgent.builder()
                .name("DeepResearchAgent")  // 代理名称
                .model(chatModel)  // 使用已配置的聊天模型
                .tools(toolsFromMcp)  // 注入MCP工具
                .systemPrompt(systemPrompt)  // 设置系统提示词
                .enableLogging(true)  // 启用详细日志
                // 配置拦截器链（按顺序执行）
                .interceptors(todoListInterceptor,  // 待办事项管理
                        filesystemInterceptor,  // 文件系统操作
                        largeResultEvictionInterceptor,  // 大结果处理
                        patchToolCallsInterceptor,  // 工具调用增强
//						contextEditingInterceptor,  // 上下文编辑（暂时禁用）
                        toolRetryInterceptor,  // 工具重试机制
                        subAgentAsInterceptors(toolsFromMcp))  // 子代理拦截器
                // 配置钩子机制
                .hooks(humanInTheLoopHook,  // 人类参与控制
                        summarizationHook,  // 对话摘要
                        toolCallLimitHook)  // 工具调用限制
                .saver(new MemorySaver())  // 内存状态保存器
                .build();  // 构建最终代理实例

    }

    /**
     * 将子代理包装为拦截器
     *
     * 创建子代理拦截器，用于协调多个专用子代理的工作：
     * - research-agent：负责深入研究特定问题
     * - critique-agent：负责评审和完善报告质量
     *
     * @param toolsFromMcp 来自MCP客户端的工具回调列表
     * @return Interceptor 子代理拦截器实例
     */
    private Interceptor subAgentAsInterceptors(List<ToolCallback> toolsFromMcp) {
        // 创建两个专用子代理规范
        SubAgentSpec researchAgent = createResearchAgent(toolsFromMcp, subResearchPrompt);  // 研究代理
        SubAgentSpec critiqueAgent = createCritiqueAgent(subCritiquePrompt);  // 评审代理

        // 构建子代理拦截器
        SubAgentInterceptor.Builder subAgentBuilder = SubAgentInterceptor.builder()
                .defaultModel(chatModel)  // 默认使用主聊天模型
                // 为所有子代理设置默认拦截器
                .defaultInterceptors(
                        todoListInterceptor,  // 待办事项管理
                        filesystemInterceptor,  // 文件系统访问
//						contextEditingInterceptor,  // 上下文编辑（暂时禁用）
                        patchToolCallsInterceptor,  // 工具调用增强
                        largeResultEvictionInterceptor  // 大结果处理
                )
                // 为所有子代理设置默认钩子
                .defaultHooks(humanInTheLoopHook,  // 人类参与
                        summarizationHook,  // 摘要生成
                        toolCallLimitHook)  // 工具调用限制
                .addSubAgent(researchAgent)  // 添加研究子代理
                .includeGeneralPurpose(true)  // 包含通用目的工具
                .addSubAgent(critiqueAgent);  // 添加评审子代理
        return subAgentBuilder.build();  // 构建并返回拦截器
    }

    /**
     * 系统提示词常量类
     *
     * 包含三种不同场景的系统提示词：
     * 1. 主研究代理提示词 - 控制整体研究流程
     * 2. 研究子代理提示词 - 专注具体问题的深度研究
     * 3. 评审子代理提示词 - 负责报告质量和准确性审查
     */
    public static class Prompts {

        /**
         * 主研究代理系统提示词
         *
         * 定义了专家级研究员的行为准则和工作流程：
         * - 接收用户研究请求
         * - 分解复杂问题为子任务
         * - 协调多个研究代理并行工作
         * - 生成高质量研究报告
         * - 通过迭代改进确保质量
         *
         * 工作流程强调：
         * 1. 问题记录和分解
         * 2. 并行深度研究
         * 3. 报告撰写
         * 4. 质量评审和迭代改进
         */
        public static String researchInstructions = """
				You are an expert researcher. Your job is to conduct thorough research and write a polished report.
				
				**Workflow:**
				1. First, write the original user question to `question.txt` for reference
				2. Use the research-agent to conduct deep research on sub-topics
				   - Break down complex topics into specific sub-questions
				   - Call multiple research agents in parallel for independent sub-questions
				3. When you have enough information, write the final report to `final_report.md`
				4. Call the critique-agent to get feedback on the report
				5. Iterate: Do more research and edit `final_report.md` based on critique
				6. Repeat steps 4-5 until satisfied with the quality
				
				**Report Format Requirements:**
				- CRITICAL: Write in the SAME language as the user's question!
				- Use clear Markdown with proper structure (# for title, ## for sections, ### for subsections)
				- Include specific facts and insights from research
				- Reference sources using [Title](URL) format
				- Provide balanced, thorough analysis
				- Be comprehensive - users expect detailed, in-depth answers
				- End with a "### Sources" section listing all references
				
				**Citation Rules:**
				- Assign each unique URL a single citation number [1], [2], etc.
				- Number sources sequentially without gaps in the final list
				- Each source should be a separate list item
				- Format: [1] Source Title: URL
				
				Structure your report appropriately for the question type:
				- Comparison: intro → overview A → overview B → comparison → conclusion
				- List: Simple numbered/bulleted list or separate sections per item
				- Overview/Summary: intro → concept 1 → concept 2 → ... → conclusion
				- Analysis: thesis → evidence → analysis → conclusion
				""";

        /**
         * 研究子代理系统提示词
         *
         * 指导专门的研究代理如何进行深度调研：
         * - 专注单一研究主题
         * - 进行彻底的信息收集
         * - 提供全面详细的答案
         * - 确保最终输出的完整性和自包含性
         *
         * 强调只有最终答案会被传递给用户，因此必须足够详尽。
         */
        public static String subResearchPrompt = """
				You are a dedicated researcher. Your job is to conduct research based on the user's questions.
				
				Conduct thorough research and then reply to the user with a detailed answer to their question.
				
				IMPORTANT: Only your FINAL answer will be passed on to the user. They will have NO knowledge
				of anything except your final message, so your final report should be comprehensive and self-contained!
				""";

        /**
         * 评审子代理系统提示词
         *
         * 指导专业的编辑代理如何评审研究报告：
         * - 从指定文件读取报告和原始问题
         * - 提供详细的改进建议
         * - 关注结构、内容、语言等多个维度
         * - 不直接修改报告文件
         *
         * 评审重点包括：
         * - 章节命名和结构合理性
         * - 文章风格（学术论文式vs要点列表式）
         * - 内容完整性和深度分析
         * - 与研究主题的相关性
         * - 语言表达的清晰度
         */
        public static String subCritiquePrompt = """
				You are a dedicated editor. You are being tasked to critique a report.
				
				You can find the report at `final_report.md`.
				You can find the question/topic for this report at `question.txt`.
				
				The user may ask for specific areas to critique the report in.
				Respond with a detailed critique of the report. Focus on areas that could be improved.
				
				You can use the search tool to search for information, if that will help you critique the report.
				
				Do not write to the `final_report.md` yourself.
				
				Things to check:
				- Each section is appropriately named and structured
				- The report is written in essay/textbook style - text heavy, not just bullet points
				- The report is comprehensive without missing important details
				- The article covers key areas ensuring overall understanding
				- The article deeply analyzes causes, impacts, and trends with valuable insights
				- The article closely follows the research topic and directly answers questions
				- The article has clear structure, fluent language, and is easy to understand
				""";
    }
}
