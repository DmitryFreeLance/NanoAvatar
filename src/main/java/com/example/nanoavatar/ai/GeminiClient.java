package com.example.nanoavatar.ai;

import com.google.gson.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

/**
 * Клиент для обращения к AI‑агенту Timeweb по OpenAI‑совместимому Chat Completions API.
 * Работает ТОЛЬКО с текстом, без картинок.
 *
 * Эндпоинт:
 *   POST {baseUrl}/api/v1/cloud-ai/agents/{agent_id}/v1/chat/completions
 *
 * Документация:
 *   https://timeweb.cloud/docs/ai-agents/api-usage/openai-compatible-api
 */
public class GeminiClient {

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private final String baseUrl;
    private final String agentId;
    private final String apiKey;
    private final String model;

    /**
     * Базовый системный промпт для текстового ассистента.
     * Детальные настройки стиля прилетают отдельным блоком modePrompt.
     */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "Ты — NanoBuddy, дружелюбный русскоязычный текстовый помощник для Telegram‑бота.",
            "",
            "К тебе обращаются с бытовыми, учебными и рабочими вопросами, черновиками текстов,",
            "просьбами придумать идеи и расставить приоритеты.",
            "",
            "Общие правила:",
            "- Отвечай на РУССКОМ, если пользователь явно не просит другое.",
            "- Пиши понятно, структурировано, без лишней воды.",
            "- Эмодзи можно использовать, но не перебарщивай.",
            "- Не выдавай себя за врача, психотерапевта или юриста.",
            "- Если вопрос про здоровье, психику или юридические риски — мягко советуй обратиться к специалисту.",
            "- Не обещай генерацию картинок, видео, музыки — ты работаешь только с текстом.",
            "",
            "Дополнительные настройки стиля (личность, формат, юмор и т.п.) будут переданы отдельным блоком.",
            "Если такие инструкции есть — строго им следуй и не противоречь им."
    );

    public GeminiClient(String baseUrl, String agentId, String apiKey, String model) {
        // В доках для OpenAI‑совместимого API пример: https://agent.timeweb.cloud
        if (baseUrl == null || baseUrl.isBlank()) {
            this.baseUrl = "https://agent.timeweb.cloud";
        } else {
            String tmp = baseUrl.trim();
            if (tmp.endsWith("/")) tmp = tmp.substring(0, tmp.length() - 1);
            this.baseUrl = tmp;
        }
        this.agentId = agentId;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Один текстовый запрос к агенту.
     *
     * @param modePrompt  — доп. инструкции под активные настройки (может быть null/пусто)
     * @param userPrompt  — сообщение пользователя
     * @return текст ответа ассистента
     */
    public String generateReply(String modePrompt, String userPrompt) throws IOException {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalStateException("TIMEWEB_AGENT_ID is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        JsonArray messages = new JsonArray();

        // system
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        StringBuilder sysContent = new StringBuilder(SYSTEM_PROMPT);
        if (modePrompt != null && !modePrompt.isBlank()) {
            sysContent.append("\n\nДополнительные настройки стиля и поведения:\n");
            sysContent.append(modePrompt);
        }
        systemMsg.addProperty("content", sysContent.toString());
        messages.add(systemMsg);

        // user
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        JsonObject payload = new JsonObject();
        if (model != null && !model.isBlank()) {
            payload.addProperty("model", model);
        }
        payload.add("messages", messages);
        payload.addProperty("stream", false);

        String url = baseUrl + "/api/v1/cloud-ai/agents/" + agentId + "/v1/chat/completions";

        RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("content-type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error: HTTP " + response.code()
                        + " Body: " + responseBody);
            }

            String content = extractMessageContent(responseBody);
            if (content == null) {
                throw new IOException("Gemini API response doesn't contain message content");
            }
            return content.trim();
        }
    }

    /**
     * Достаём текст из choices[0].message.content
     * (поддерживаем и строку, и массив объектов с type=text).
     */
    private String extractMessageContent(String json) throws IOException {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new IOException("No choices in Gemini response");
        }

        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message == null) {
            throw new IOException("No message field in Gemini response");
        }

        JsonElement contentEl = message.get("content");
        if (contentEl == null) {
            throw new IOException("No content field in Gemini response");
        }

        if (contentEl.isJsonPrimitive()) {
            return contentEl.getAsString();
        }

        if (contentEl.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement partEl : contentEl.getAsJsonArray()) {
                if (!partEl.isJsonObject()) continue;
                JsonObject part = partEl.getAsJsonObject();
                JsonElement typeEl = part.get("type");
                if (typeEl != null && "text".equals(typeEl.getAsString())) {
                    JsonElement textEl = part.get("text");
                    if (textEl != null) {
                        sb.append(textEl.getAsString()).append(" ");
                    }
                }
            }
            return sb.toString().trim();
        }

        return contentEl.toString();
    }
}