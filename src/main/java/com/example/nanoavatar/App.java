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
        Dotenv dotenv = Dotenv.load();

        String token = dotenv.get("BOT_TOKEN");
        String username = dotenv.get("BOT_USERNAME");
        String dbPath = dotenv.get("DATABASE_PATH", "bot.db");

        String providerToken = dotenv.get("YOOKASSA_PROVIDER_TOKEN");
        int minTopup = Integer.parseInt(dotenv.get("MIN_TOPUP_RUB", "100"));
        int creditsPerRub = Integer.parseInt(dotenv.get("CREDITS_PER_RUBLE", "1"));
        int promptPrice = Integer.parseInt(dotenv.get("PROMPT_PRICE_CREDITS", "1"));
        int dailyBonus = Integer.parseInt(dotenv.get("DAILY_BONUS_CREDITS", "1"));
        String moscowZoneId = dotenv.get("MOSCOW_TIMEZONE", "Europe/Moscow");

        // Timeweb / Gemini
        String timewebBaseUrl = dotenv.get("TIMEWEB_BASE_URL", "https://agent.timeweb.cloud");
        String timewebAgentId = dotenv.get("TIMEWEB_AGENT_ID");
        String geminiApiKey = dotenv.get("GEMINI_API_KEY");
        String geminiModel = dotenv.get("GEMINI_MODEL", "gemini-2.5-flash");

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

        // ежедневное пополнение
        DailyBonusScheduler scheduler =
                new DailyBonusScheduler(userService, bot, dailyBonus, moscowZoneId);
        scheduler.start();
    }
}