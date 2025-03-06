package org.agentpower;

import org.agentpower.client.plugins.file.reader.FileReaderFunc;
import org.junit.jupiter.api.Test;

public class FileReaderFuncTest {
    @Test
    void test() {
        FileReaderFunc fileReaderFunc = new FileReaderFunc();
        FileReaderFunc.Request request = new FileReaderFunc.Request(
                "test.txt", "utf-8"
        );
        System.out.println(fileReaderFunc.apply(request));
    }
}
