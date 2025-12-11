package com.example.nanoavatar.ai;

import com.google.gson.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Системный промпт, который отправляем в Chat Completions.
     * Он должен быть согласован с тем, что ты написал в панели Timeweb
     * (там можно оставить общий текст, а сюда — техзадание под бота).
     */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "Ты — визуальный агент на базе Gemini 2.5 Flash для Telegram-бота NanoAvatar.",
            "",
            "Пользователь присылает портретное фото (селфи, фото по пояс, портрет) и выбирает стиль оформления аватара.",
            "Бэкенд передаёт тебе:",
            "- URL исходного фото пользователя;",
            "- текстовый промпт со стилем (фильтром).",
            "",
            "Твоя задача:",
            "1) На основе исходного фото создать новое изображение с сохранением узнаваемости человека.",
            "2) Применить описанный стиль: одежда, фон, свет, художественные эффекты.",
            "3) Сделать картинку аккуратной и реалистичной, пригодной для аватарки/соцсетей.",
            "",
            "Правила:",
            "- Сохраняй черты лица и примерную фигуру пользователя, не превращай его в другого человека.",
            "- Не меняй пол и возраст без явной просьбы.",
            "- Улучшай свет, цвет, резкость и детали.",
            "- Можно добавлять одежду, аксессуары и фон, если это соответствует промпту.",
            "- Не создавай NSFW, жёсткое насилие, политику и прочий запрещённый контент.",
            "",
            "Формат ОТВЕТА:",
            "- Сгенерируй конечное изображение.",
            "- Верни ТОЛЬКО ОДНУ ПРЯМУЮ ССЫЛКУ (https://...) на готовое изображение JPG или PNG.",
            "- БЕЗ какого-либо дополнительного текста, без комментариев, без Markdown, без JSON."
    );

    public GeminiClient(String baseUrl, String agentId, String apiKey, String model) {
        // Для OpenAI‑совместимого API в примерах используется https://agent.timeweb.cloud
        // Мы ожидаем именно ДОМЕН, без хвоста /api/v1/...
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
     * Один запрос к OpenAI‑совместимому Chat Completions:
     * POST /api/v1/cloud-ai/agents/{agent_id}/v1/chat/completions
     *
     * На вход:
     *  - filterPrompt     — текст стиля/фильтра;
     *  - sourceImageUrl   — публичный URL исходного фото (из Telegram).
     *
     * На выход:
     *  - байты PNG/JPG сгенерированного изображения (для отправки в Telegram).
     */
    public byte[] generateImage(String filterPrompt, String sourceImageUrl) throws IOException {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalStateException("TIMEWEB_AGENT_ID is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        // messages[]
        JsonArray messages = new JsonArray();

        // system
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        // user
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        StringBuilder userContent = new StringBuilder();
        userContent
                .append("Исходное портретное фото пользователя (URL): ")
                .append(sourceImageUrl)
                .append("\n\n")
                .append("Применяй к этому фото следующий стиль/фильтр:\n")
                .append(filterPrompt)
                .append("\n\n")
                .append("Сохрани лицо и фигуру узнаваемыми. ")
                .append("Когда картинка будет готова, верни ТОЛЬКО прямой URL на итоговое изображение.");

        userMsg.addProperty("content", userContent.toString());
        messages.add(userMsg);

        JsonObject payload = new JsonObject();
        // модель в OpenAI‑совместимом API у Timeweb можно не указывать — они берут из настроек агента,
        // но если хочешь, оставим:
        if (model != null && !model.isBlank()) {
            payload.addProperty("model", model);
        }
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.9);
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
                // Это сообщение ты как раз видел раньше: Gemini API error: HTTP 400 Body: ...
                throw new IOException("Gemini API error: HTTP " + response.code()
                        + " Body: " + responseBody);
            }

            String content = extractMessageContent(responseBody);
            String imageUrl = extractFirstUrl(content);

            if (imageUrl == null) {
                throw new IOException("Gemini API response doesn't contain image URL. Content: " + content);
            }

            System.out.println("Gemini returned image URL: " + imageUrl); // лог на всякий случай

            // ⚠️ ВАЖНО: теперь мы не отдаём этот URL в Telegram.
            // Скачиваем картинку сами и возвращаем её байты.
            Request downloadReq = new Request.Builder()
                    .url(imageUrl)
                    .get()
                    .build();

            try (Response downloadResp = client.newCall(downloadReq).execute()) {
                if (downloadResp.body() == null || !downloadResp.isSuccessful()) {
                    String errBody = downloadResp.body() != null ? downloadResp.body().string() : "";
                    throw new IOException(
                            "Failed to download image from URL: " + imageUrl +
                                    ". HTTP " + downloadResp.code() +
                                    " Body: " + errBody
                    );
                }

                return downloadResp.body().bytes();
            }
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

    private String extractFirstUrl(String text) {
        if (text == null) return null;

        // Ищем http/https до первого пробела, кавычек, скобок и т.п.
        Pattern pattern = Pattern.compile("(https?://[^\\s\"'<>]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}