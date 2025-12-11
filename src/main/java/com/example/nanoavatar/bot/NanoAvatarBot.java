package com.example.nanoavatar.bot;

import com.example.nanoavatar.ai.GeminiClient;
import com.example.nanoavatar.db.Database;
import com.example.nanoavatar.filters.FilterNode;
import com.example.nanoavatar.filters.FilterRegistry;
import com.example.nanoavatar.payment.PaymentService;
import com.example.nanoavatar.user.SessionState;
import com.example.nanoavatar.user.UserService;
import com.example.nanoavatar.user.UserSession;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.*;

public class NanoAvatarBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final FilterRegistry registry;
    private final UserService userService;
    private final PaymentService paymentService;
    private final GeminiClient geminiClient;
    private final int promptPriceCredits;

    private final Map<Long, UserSession> sessions = new HashMap<>();

    public NanoAvatarBot(String token,
                         String botUsername,
                         Database db,
                         PaymentService paymentService,
                         GeminiClient geminiClient,
                         int promptPriceCredits) {
        super(token);
        this.botUsername = botUsername;
        this.registry = new FilterRegistry();
        this.userService = new UserService(db);
        this.paymentService = paymentService;
        this.geminiClient = geminiClient;
        this.promptPriceCredits = promptPriceCredits;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private UserSession getSession(long chatId) {
        return sessions.computeIfAbsent(chatId,
                id -> new UserSession(FilterRegistry.ROOT_ID));
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            } else if (update.hasPreCheckoutQuery()) {
                handlePreCheckout(update.getPreCheckoutQuery());
            } else if (update.hasMessage()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== MESSAGES =====

    private void handleMessage(Message msg) throws Exception {
        Long chatId = msg.getChatId();
        userService.ensureUser(chatId,
                msg.getFrom() != null ? msg.getFrom().getUserName() : null);

        // успешная оплата
        if (msg.hasSuccessfulPayment()) {
            paymentService.handleSuccessfulPayment(chatId, msg.getSuccessfulPayment());
            int bal = userService.getBalance(chatId);
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("✅ Оплата прошла успешно!\nТекущий баланс: " + bal + " кредитов.")
                    .build());
            return;
        }

        UserSession session = getSession(chatId);

        if (msg.hasText()) {
            String text = msg.getText().trim();

            // команды
            if ("/start".equals(text)) {
                session.setState(SessionState.BROWSING);
                session.setCurrentNodeId(FilterRegistry.ROOT_ID);
                sendMainMenu(chatId, session);
                return;
            }

            if ("/help".equals(text)) {
                sendHelp(chatId);
                return;
            }

            if ("/balance".equals(text)) {
                int bal = userService.getBalance(chatId);
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("💰 Твой баланс: " + bal + " кредитов.\n" +
                                "Один ответ бота стоит " + promptPriceCredits + " кредит.")
                        .build());
                return;
            }

            if ("/topup".equals(text)) {
                askTopupAmount(chatId, session);
                return;
            }

            if (session.getState() == SessionState.WAITING_FOR_TOPUP_AMOUNT) {
                try {
                    int amount = Integer.parseInt(text.trim());
                    session.setPendingTopupAmount(amount);
                    SendInvoice invoice = paymentService.createTopupInvoice(chatId, amount);
                    execute(invoice);
                    session.setState(SessionState.BROWSING);
                } catch (NumberFormatException e) {
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text("❗ Введи сумму числом, например: 300")
                            .build());
                }
                return;
            }

            // если это неизвестная команда
            if (text.startsWith("/")) {
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Я знаю команды: /start, /help, /balance, /topup 🙂")
                        .build());
                return;
            }

            // обычный текст — это запрос к AI
            processUserQuery(chatId, session, text);
            return;
        }

        // другие типы сообщений
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("Пока я работаю только с текстом. Напиши вопрос, задачу или черновик сообщения — я помогу 🙂")
                .build());
    }

    private void sendHelp(long chatId) throws TelegramApiException {
        String text = "🤖 *NanoBuddy* — настраиваемый текстовый ИИ‑ассистент.\n\n" +
                "Что он умеет:\n" +
                "• ✏️ Переписывать тексты красиво и без ошибок\n" +
                "• 📅 Помогать планировать день и разбирать задачи\n" +
                "• 📚 Объяснять сложные темы простым языком\n" +
                "• 💡 Придумывать идеи, названия и формулировки\n" +
                "• 🧩 Разбирать ситуации и предлагать варианты действий\n" +
                "• 🤝 Поддерживать, когда нужно выговориться\n\n" +
                "Через /start можно настроить его личность, стиль, юмор, формат ответов и фишки.\n" +
                "Потом просто пиши текст — и бот отвечает уже в выбранном стиле.";

        execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .build());
    }

    private void askTopupAmount(long chatId, UserSession session) throws TelegramApiException {
        session.setState(SessionState.WAITING_FOR_TOPUP_AMOUNT);
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("💳 Введи сумму пополнения в рублях (минимум 100 ₽):")
                .build());
    }

    // ===== PAYMENTS =====

    private void handlePreCheckout(PreCheckoutQuery preCheckoutQuery) throws TelegramApiException {
        var answer = paymentService.handlePreCheckout(
                preCheckoutQuery.getId(), true, null);
        execute(answer);
    }

    // ===== CALLBACKS =====

    private void handleCallback(CallbackQuery query) throws Exception {
        String data = query.getData();
        long chatId = query.getMessage().getChatId();
        int msgId = query.getMessage().getMessageId();

        UserSession session = getSession(chatId);

        if (data.startsWith("NODE:")) {
            String nodeId = data.substring("NODE:".length());
            session.setCurrentNodeId(nodeId);
            session.setState(SessionState.BROWSING);
            showNode(chatId, msgId, registry.getNode(nodeId), session);
        } else if (data.startsWith("BACK:")) {
            String nodeId = data.substring("BACK:".length());
            session.setCurrentNodeId(nodeId);
            session.setState(SessionState.BROWSING);
            showNode(chatId, msgId, registry.getNode(nodeId), session);
        } else if (data.startsWith("SELECT:")) {
            String id = data.substring("SELECT:".length());
            toggleOption(session, id);
            FilterNode node = registry.getNode(id);
            showNode(chatId, msgId, node, session);
        } else if (data.startsWith("EXAMPLE:")) {
            String id = data.substring("EXAMPLE:".length());
            FilterNode node = registry.getNode(id);
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("📝 Пример запроса с настройкой \"" + node.getTitle() + "\":\n\n" +
                            "Например: \"Сделай план на день с учётом моих задач, " +
                            "используя выбранные мной настройки стиля\".")
                    .build());
        } else if ("BALANCE".equals(data)) {
            session.setState(SessionState.BROWSING);
            showBalanceScreen(chatId, msgId);
        } else if ("TOPUP".equals(data)) {
            session.setState(SessionState.WAITING_FOR_TOPUP_AMOUNT);
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(msgId)
                    .text("💳 Введи сумму пополнения в рублях (минимум 100 ₽):")
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboard(List.of(List.of(backButton(FilterRegistry.ROOT_ID))))
                            .build())
                    .build();
            execute(edit);
        }
    }

    private void toggleOption(UserSession session, String id) {
        Set<String> active = session.getActiveOptionIds();
        if (active.contains(id)) {
            active.remove(id);
        } else {
            active.add(id);
        }
    }

    // ===== MENUS =====

    private void sendMainMenu(long chatId, UserSession session) throws TelegramApiException {
        InlineKeyboardMarkup kb = buildKeyboardForRoot();

        String text = "👋 Привет! Я *NanoBuddy* — настраиваемый текстовый ИИ‑помощник.\n\n" +
                "Как со мной работать:\n" +
                "1️⃣ Выбери, *каким* я должен быть — личность, юмор, формат ответов и фишки.\n" +
                "2️⃣ Включи несколько опций (можно много сразу).\n" +
                "3️⃣ Просто пиши свои вопросы и задачи — я отвечу в выбранном стиле.\n\n" +
                "Каждый ответ стоит *" + promptPriceCredits + "* кредит.\n" +
                "Стартовый подарок — 10 кредитов, а каждый день я докидываю бонус 🎁";

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(kb)
                .build();

        execute(msg);
    }

    private void showNode(long chatId, int messageId, FilterNode node, UserSession session) throws TelegramApiException {
        if (node == null) return;

        if (node.getId().equals(FilterRegistry.ROOT_ID)) {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("⚙️ Главное меню настроек. Выбирай блок, который хочешь подкрутить 👇")
                    .replyMarkup(buildKeyboardForRoot())
                    .build();
            execute(edit);
            return;
        }

        if (node.isLeaf()) {
            boolean active = session.getActiveOptionIds().contains(node.getId());
            String status = active
                    ? "🔘 Сейчас: *ВКЛЮЧЕНО*"
                    : "⚪ Сейчас: *ВЫКЛЮЧЕНО*";

            String text = "🧩 *" + node.getTitle() + "*\n\n" +
                    node.getDescription() + "\n\n" +
                    status + "\n\n" +
                    "Эта настройка влияет на то, как я формулирую ответы.";

            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .parseMode(ParseMode.MARKDOWN)
                    .replyMarkup(buildKeyboardForLeaf(node, session))
                    .build();
            execute(edit);
        } else {
            String text = node.getTitle() + "\n\n" +
                    "Выбирай конкретные опции ниже. Можно включать несколько — они суммируются.\n\n" +
                    "Активные опции отмечены галочкой ✅.";

            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(buildKeyboardForCategory(node, session))
                    .build();
            execute(edit);
        }
    }

    private void showBalanceScreen(long chatId, int messageId) throws TelegramApiException {
        int bal = userService.getBalance(chatId);

        String text = "💳 *Баланс / пополнить*\n\n" +
                "Текущий баланс: *" + bal + "* кредитов.\n" +
                "Один ответ бота стоит *" + promptPriceCredits + "* кредит.\n\n" +
                "Нажми «Пополнить», чтобы выбрать сумму пополнения.";

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("➕ Пополнить")
                        .callbackData("TOPUP")
                        .build()
        ));
        rows.add(List.of(backButton(FilterRegistry.ROOT_ID)));

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();

        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(kb)
                .build();

        execute(edit);
    }

    // ===== KEYBOARDS =====

    private InlineKeyboardMarkup buildKeyboardForRoot() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<FilterNode> categories = new ArrayList<>();
        for (FilterNode node : registry.getAllNodes()) {
            if (FilterRegistry.ROOT_ID.equals(node.getParentId())) {
                categories.add(node);
            }
        }

        for (int i = 0; i < categories.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buttonForNode(categories.get(i)));
            if (i + 1 < categories.size()) {
                row.add(buttonForNode(categories.get(i + 1)));
            }
            rows.add(row);
        }

        // строка Баланс / пополнить
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("💳 Баланс / пополнить")
                        .callbackData("BALANCE")
                        .build()
        ));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup buildKeyboardForCategory(FilterNode category, UserSession session) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<String> childIds = category.getChildrenIds();
        List<FilterNode> children = new ArrayList<>();
        for (String id : childIds) children.add(registry.getNode(id));

        for (int i = 0; i < children.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buttonForLeafInCategory(children.get(i), session));
            if (i + 1 < children.size()) {
                row.add(buttonForLeafInCategory(children.get(i + 1), session));
            }
            rows.add(row);
        }

        rows.add(List.of(backButton(FilterRegistry.ROOT_ID)));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup buildKeyboardForLeaf(FilterNode leaf, UserSession session) {
        boolean active = session.getActiveOptionIds().contains(leaf.getId());
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text(active ? "❌ Отключить настройку" : "✅ Включить настройку")
                        .callbackData("SELECT:" + leaf.getId())
                        .build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("📝 Пример запроса")
                        .callbackData("EXAMPLE:" + leaf.getId())
                        .build()
        ));
        rows.add(List.of(backButton(leaf.getParentId())));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup buildBackOnlyKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(backButton(FilterRegistry.ROOT_ID))
                ))
                .build();
    }

    private InlineKeyboardButton buttonForNode(FilterNode node) {
        return InlineKeyboardButton.builder()
                .text(node.getTitle())
                .callbackData("NODE:" + node.getId())
                .build();
    }

    private InlineKeyboardButton buttonForLeafInCategory(FilterNode leaf, UserSession session) {
        boolean active = session.getActiveOptionIds().contains(leaf.getId());
        String text = (active ? "✅ " : "") + leaf.getTitle();
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData("NODE:" + leaf.getId())
                .build();
    }

    private InlineKeyboardButton backButton(String targetId) {
        return InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("BACK:" + targetId)
                .build();
    }

    // ===== AI‑ЗАПРОСЫ =====

    private void processUserQuery(long chatId, UserSession session, String userText) throws TelegramApiException {
        int balance = userService.getBalance(chatId);
        if (balance < promptPriceCredits) {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("😔 Недостаточно кредитов. Твой баланс: " + balance +
                            ".\nКоманда для пополнения: /topup или кнопка \"💳 Баланс / пополнить\" в меню.")
                    .build());
            return;
        }

        // Собираем активные настройки
        Set<String> activeIds = session.getActiveOptionIds();
        StringBuilder settingsNames = new StringBuilder();
        StringBuilder settingsPrompt = new StringBuilder();

        if (activeIds.isEmpty()) {
            settingsNames.append("по умолчанию");
        } else {
            for (String id : activeIds) {
                FilterNode node = registry.getNode(id);
                if (node == null || !node.isLeaf()) continue;

                if (settingsNames.length() > 0) settingsNames.append(", ");
                settingsNames.append(node.getTitle());

                if (node.getPromptPart() != null && !node.getPromptPart().isBlank()) {
                    settingsPrompt.append("- ").append(node.getPromptPart()).append("\n");
                }
            }
        }

        String modePrompt = settingsPrompt.length() > 0
                ? settingsPrompt.toString()
                : "";

        // списываем баланс заранее
        userService.changeBalance(chatId, -promptPriceCredits, "SPEND",
                activeIds.isEmpty() ? "default" : String.join(",", activeIds));

        try {
            String reply = geminiClient.generateReply(modePrompt, userText);

            StringBuilder out = new StringBuilder();
            out.append("🧠 Активные настройки: ").append(settingsNames).append("\n\n");
            out.append(reply);

            SendMessage resp = SendMessage.builder()
                    .chatId(chatId)
                    .text(out.toString())
                    // без Markdown, чтобы не поймать спецсимволы из ответа
                    .replyMarkup(buildBackOnlyKeyboard())
                    .build();

            execute(resp);
        } catch (IOException | IllegalStateException ex) {
            // откат кредита
            userService.changeBalance(chatId, promptPriceCredits, "REFUND", "gemini_error");
            try {
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("⚙️ Не удалось получить ответ от нейросети: " + ex.getMessage() + "\n" +
                                "Я вернул кредит на твой баланс.")
                        .build());
            } catch (TelegramApiException e2) {
                e2.printStackTrace();
            }
        }
    }
}