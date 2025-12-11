package com.example.nanoavatar;

import com.example.nanoavatar.ai.GeminiClient;
import com.example.nanoavatar.bot.NanoAvatarBot;
import com.example.nanoavatar.db.Database;
import com.example.nanoavatar.payment.PaymentService;
import com.example.nanoavatar.scheduler.DailyBonusScheduler;
import com.example.nanoavatar.user.UserService;
import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        } catch (Exception ignored) {
        }

        String token = requireEnv(dotenv, "BOT_TOKEN");
        String username = requireEnv(dotenv, "BOT_USERNAME");

        String dbPath = envOrDotenv(dotenv, "DATABASE_PATH", "bot.db");

        String providerToken = requireEnv(dotenv, "YOOKASSA_PROVIDER_TOKEN");
        int minTopup = Integer.parseInt(envOrDotenv(dotenv, "MIN_TOPUP_RUB", "100"));
        int creditsPerRub = Integer.parseInt(envOrDotenv(dotenv, "CREDITS_PER_RUBLE", "1"));
        int promptPrice = Integer.parseInt(envOrDotenv(dotenv, "PROMPT_PRICE_CREDITS", "1"));
        int dailyBonus = Integer.parseInt(envOrDotenv(dotenv, "DAILY_BONUS_CREDITS", "5"));
        String moscowZoneId = envOrDotenv(dotenv, "MOSCOW_TIMEZONE", "Europe/Moscow");

        // Timeweb AI‑агент (OpenAI‑совместимый API)
        String timewebBaseUrl = envOrDotenv(dotenv, "TIMEWEB_BASE_URL", "https://agent.timeweb.cloud");
        String timewebAgentId = requireEnv(dotenv, "TIMEWEB_AGENT_ID");
        String geminiApiKey = requireEnv(dotenv, "GEMINI_API_KEY");
        String geminiModel = envOrDotenv(dotenv, "GEMINI_MODEL", "gemini-2.5-flash");

        Database db = new Database(dbPath);
        UserService userService = new UserService(db);

        PaymentService paymentService =
                new PaymentService(providerToken, minTopup, creditsPerRub, userService);

        GeminiClient geminiClient =
                new GeminiClient(timewebBaseUrl, timewebAgentId, geminiApiKey, geminiModel);

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        NanoAvatarBot bot = new NanoAvatarBot(
                token, username, db, paymentService, geminiClient, promptPrice);
        api.registerBot(bot);

        DailyBonusScheduler scheduler =
                new DailyBonusScheduler(userService, bot, dailyBonus, moscowZoneId);
        scheduler.start();
    }

    private static String envOrDotenv(Dotenv dotenv, String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v;

        if (dotenv != null) {
            String dv = dotenv.get(key);
            if (dv != null && !dv.isBlank()) return dv;
        }
        return defaultValue;
    }

    private static String requireEnv(Dotenv dotenv, String key) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v;

        if (dotenv != null) {
            String dv = dotenv.get(key);
            if (dv != null && !dv.isBlank()) return dv;
        }

        throw new IllegalStateException("Required environment variable " + key + " is not set");
    }
}