package org.agentpower.service.secure.recognization;

public class RecognizationHelper {
    public static Recognizer generateRecognizer() {
        return RecognizerConfigurations.getRecognizer();
    }
}
