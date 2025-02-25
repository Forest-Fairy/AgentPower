package org.agentpower.api;

/**
 * for client service calling the server api
 */
public interface AgentServer {
    class StatusCode {
        public static final int OK = 0;
        public static final int FAIL = -1;
        public static final int TIME_OUT = -2;
    }

    int sendingFileContent(String fileAbsPath, boolean existOrNot, byte[] fileContent);

}
