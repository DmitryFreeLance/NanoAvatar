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

    private static final String SYSTEM_PROMPT = String.join("\n",
            "Ты — нейросеть Gemini 2.5 Flash, встроенная в Telegram‑бота NanoAvatar.",
            "Пользователь присылает портретное фото и текстовое описание стиля (фильтра).",
            "Твоя задача — на основе этого фото создать новый портрет в заданном стиле.",
            "",
            "Правила обработки:",
            "- сохраняй узнаваемость лица и основные пропорции человека;",
            "- не меняй пол и возраст без явной просьбы в описании;",
            "- улучшай качество изображения: свет, цвет, резкость, детали;",
            "- аккуратно добавляй одежду, фон и эффекты, не превращая человека в другого.",
            "",
            "Безопасность:",
            "- не создавай NSFW‑контент, жестокость, кровь, политику и прочие спорные темы;",
            "- изображение должно быть уместно для аватарки и соцсетей.",
            "",
            "Формат ответа:",
            "- сгенерируй и сохрани итоговое изображение;",
            "- верни ТОЛЬКО ОДНУ прямую ссылку (https://...) на готовую картинку JPG или PNG;",
            "- не добавляй текст, Markdown, пояснения или дополнительные данные — только URL."
    );

    public GeminiClient(String baseUrl, String agentId, String apiKey, String model) {
        this.baseUrl = baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : (baseUrl != null ? baseUrl : "https://agent.timeweb.cloud");
        this.agentId = agentId;
        this.apiKey = apiKey;
        this.model = model;
    }

    public byte[] generateImage(String promptText, String sourceImageUrl) throws IOException {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalStateException("TIMEWEB_AGENT_ID is not configured");
        }

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        JsonArray contentArray = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", promptText);
        contentArray.add(textPart);

        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrlObj = new JsonObject();
        imageUrlObj.addProperty("url", sourceImageUrl);
        imagePart.add("image_url", imageUrlObj);
        contentArray.add(imagePart);

        userMsg.add("content", contentArray);
        messages.add(userMsg);

        JsonObject payload = new JsonObject();
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
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error: HTTP " + response.code() +
                        " " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            String content = extractMessageContent(responseBody);
            String imageUrl = extractFirstUrl(content);

            if (imageUrl == null) {
                throw new IOException("No image URL in Gemini response: " + content);
            }

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
            return url.replaceAll("[)\\]\\.,!?]*$", "");
        }
        return null;
    }
}