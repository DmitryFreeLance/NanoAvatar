package com.example.nanoavatar.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiClient {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final String baseUrl;
    private final String agentId;
    private final String apiKey;
    private final String model;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    /**
     * Системный промпт для агента.
     * ВАЖНО: он должен быть в целом согласован с тем, что ты написал в панели Timeweb.
     */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "Ты — визуальный агент на базе Gemini 2.5 Flash для Telegram-бота NanoAvatar.",
            "",
            "Пользователь присылает портретное фото (селфи или фото по пояс) и текстовое описание стиля (фильтра).",
            "Бэкенд бота уже подготовил для тебя промпт и ссылку на исходное фото.",
            "",
            "Твоя задача:",
            "1. На основе исходного фото создать новое изображение с сохранением узнаваемости человека.",
            "2. Применить описанный стиль: одежда, фон, свет, художественные эффекты.",
            "3. Сделать картинку достаточно реалистичной и аккуратной, пригодной для аватара и соцсетей.",
            "",
            "Правила:",
            "- Сохраняй черты лица и пропорции, не превращай пользователя в другого человека.",
            "- Не меняй пол и возраст без явной просьбы.",
            "- Улучшай свет, цвет, резкость и детали.",
            "- Можно добавлять одежду, аксессуары и фон в рамках описанного стиля.",
            "- Не создавай NSFW, жёсткое насилие, политику и т.п.",
            "",
            "Формат ответа ДЛЯ БЭКЕНДА:",
            "- Используй переданный URL как исходное фото;",
            "- сгенерируй и сохрани итоговое изображение;",
            "- верни ТОЛЬКО ОДНУ ПРЯМУЮ ССЫЛКУ (https://...) на готовое изображение JPG или PNG;",
            "- не возвращай больше никакого текста, Markdown, JSON и т.п. — только URL."
    );

    public GeminiClient(String baseUrl, String agentId, String apiKey, String model) {
        this.baseUrl = baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : (baseUrl != null ? baseUrl : "https://agent.timeweb.cloud");
        this.agentId = agentId;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Запрос в OpenAI-совместимый endpoint Timeweb:
     *   POST /api/v1/cloud-ai/agents/{agent_id}/v1/chat/completions
     *
     * ВАЖНО: отправляем только текстовые messages (без image_url),
     * а URL фото передаём внутри текста.
     *
     * @param promptText     текстовый промпт (стиль фильтра + инструкции)
     * @param sourceImageUrl публичный URL исходного фото (из Telegram)
     */
    public byte[] generateImage(String promptText, String sourceImageUrl) throws IOException {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalStateException("TIMEWEB_AGENT_ID is not configured");
        }

        // messages
        JsonArray messages = new JsonArray();

        // system message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        // user message (просто строка; внутри — и URL фото, и инструкции)
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        StringBuilder userContent = new StringBuilder();
        userContent.append("Вот портретное фото пользователя (URL): ")
                .append(sourceImageUrl)
                .append("\n\n")
                .append("Используй это фото как исходное. ")
                .append("Применяй следующие стилистические инструкции к этому портрету:\n")
                .append(promptText)
                .append("\n\n")
                .append("Сохрани лицо и фигуру узнаваемыми, сделай аккуратный, качественный результат. ")
                .append("Когда картинка будет готова, верни ТОЛЬКО ПРЯМОЙ URL на готовое изображение.");

        userMsg.addProperty("content", userContent.toString());
        messages.add(userMsg);

        JsonObject payload = new JsonObject();
        // для агента model обычно не обязателен, но если хочешь — можно оставить:
        if (model != null && !model.isBlank()) {
            payload.addProperty("model", model);
        }
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.9);
        payload.addProperty("stream", false);

        String url = baseUrl + "/api/v1/cloud-ai/agents/" + agentId + "/v1/chat/completions";
        String json = gson.toJson(payload);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("authorization", "Bearer " + apiKey)
                .addHeader("content-type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // Теперь в логах будет понятное сообщение от Timeweb
                throw new IOException("Gemini API error: HTTP " + response.code() +
                        " Body: " + responseBody);
            }

            String content = extractMessageContent(responseBody);
            String imageUrl = extractFirstUrl(content);

            if (imageUrl == null) {
                throw new IOException("No image URL in Gemini response: " + content);
            }

            // Качаем сгенерированную картинку по ссылке
            Request imgReq = new Request.Builder()
                    .url(imageUrl)
                    .get()
                    .build();

            try (Response imgResp = client.newCall(imgReq).execute()) {
                if (!imgResp.isSuccessful()) {
                    throw new IOException("Failed to download generated image: HTTP " +
                            imgResp.code());
                }
                return imgResp.body().bytes();
            }
        }
    }

    private String extractMessageContent(String json) throws IOException {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new IOException("No choices in Gemini response");
        }
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message == null) {
            throw new IOException("No message in Gemini response");
        }
        JsonElement contentEl = message.get("content");
        if (contentEl == null) {
            throw new IOException("No content in Gemini response");
        }

        if (contentEl.isJsonPrimitive()) {
            return contentEl.getAsString();
        }

        if (contentEl.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : contentEl.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject part = el.getAsJsonObject();
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

    private String extractFirstUrl(String text) {
        Pattern pattern = Pattern.compile("(https?://\\S+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(1);
            // убираем хвостовую пунктуацию, если есть
            return url.replaceAll("[)\\]\\.,!?]*$", "");
        }
        return null;
    }
}