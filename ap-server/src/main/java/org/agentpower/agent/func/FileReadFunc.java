package org.agentpower.agent.func;

import org.agentpower.api.StatusCode;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

public class FileReadFunc implements Function<FileReadFunc.Request, String> {
    private static final Map<String, FileReadFunc> funcMap = Map.of();

    @Override
    public String apply(Request request) {

        return "";
    }

    public record Request(String requestId, String fileAbsPath) {
    }

    public static int receiveFileContent(String requestId, String fileAbsPath, InputStream fileContentIfExist) {
        return StatusCode.REQUEST_ABORT;
    }

    private int handleFileContent(String requestId, String fileAbsPath, InputStream fileContentIfExist)

}
