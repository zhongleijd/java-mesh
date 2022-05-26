package net.grinder.engine.communication;

import net.grinder.communication.Message;

import java.util.Properties;

/**
 * 用于更新agent中启动压测进程的配置
 *
 * @author y30010171
 * @since 2022-04-29
 **/
public class AgentConfigGrinderMessage implements Message {

    private static final long serialVersionUID = 333L;

    private final Properties configProperties;

    private final int agentNumber;

    public AgentConfigGrinderMessage(Properties configProperties, int agentNumber) {
        this.configProperties = configProperties;
        this.agentNumber = agentNumber;
    }

    public Properties getConfigProperties() {
        return configProperties;
    }

    public int getAgentNumber() {
        return agentNumber;
    }
}
