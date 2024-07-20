package com.dailycodebuffer.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {

    @Autowired
    private OpenAiImageModel openAiImageModel;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;

    @GetMapping("/image/{prompt}")
    public String generateImage(@PathVariable("prompt")
                                    String prompt) {

        ImageResponse response=
                openAiImageModel.call(
                new ImagePrompt(prompt,
                        OpenAiImageOptions.builder()
                                .withHeight(1024)
                                .withQuality("hd")
                                .withWidth(1024)
                                .withN(1)
                                .build())
        );

        return response.getResult().getOutput().getUrl();
    }


    @GetMapping("/image-to-text")
    public String generateImageToText() {
        String response =
                ChatClient.create(chatModel).prompt()
                        .user(userSpec -> userSpec.text("Explain what do you see in this Image")
                                .media(MimeTypeUtils.IMAGE_JPEG,
                                        new FileSystemResource("/Users/shabbir/Documents/java-workspace/spring-ai-multimodel/src/main/resources/pexels-pixabay-270404.jpg")))
                        .call()
                        .content();
        return response;
    }


    @GetMapping("audio-to-text")
    public String generateTranscription() {

        OpenAiAudioTranscriptionOptions options
                = OpenAiAudioTranscriptionOptions.builder()
                .withLanguage("es")
                .withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.SRT)
                .withTemperature(0f)
                .build();

        AudioTranscriptionPrompt prompt
                = new AudioTranscriptionPrompt(
                        new FileSystemResource("/Users/shabbir/Documents/java-workspace/spring-ai-multimodel/src/main/resources/harvard.wav"),
                options);

        AudioTranscriptionResponse response
                = openAiAudioTranscriptionModel.call(prompt);

        return response.getResult().getOutput();
    }


    @GetMapping("/text-to-audio/{prompt}")
    public ResponseEntity<Resource> generateAudio(@PathVariable("prompt") String prompt) {
        OpenAiAudioSpeechOptions options
                = OpenAiAudioSpeechOptions.builder()
                .withModel("tts-1")
                .withSpeed(1.0f)
                .withVoice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
                .build();

        SpeechPrompt speechPrompt
                = new SpeechPrompt(prompt,options);

        SpeechResponse response
                    = openAiAudioSpeechModel.call(speechPrompt);

        byte[] responseBytes = response.getResult().getOutput();

        ByteArrayResource byteArrayResource
                = new ByteArrayResource(responseBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(byteArrayResource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("whatever.mp3")
                                .build().toString())
                .body(byteArrayResource);
    }
}
