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
import org.telegram.telegrambots.meta.api.methods.GetFile;                    // <-- ВАЖНО
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
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
            String text = msg.getText();

            if ("/start".equals(text)) {
                session.setState(SessionState.BROWSING);
                session.setCurrentNodeId(FilterRegistry.ROOT_ID);
                sendMainMenu(chatId);
                return;
            }

            if ("/balance".equals(text)) {
                int bal = userService.getBalance(chatId);
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("💰 Твой баланс: " + bal + " кредитов.")
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

            // остальной текст
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("✏️ Текстовые запросы работают через подпись к фото.\n" +
                            "Нажми /start, выбери фильтр, отправь фото и при желании добавь описание в подписи.")
                    .build());
            return;
        }

        // фото
        if (msg.hasPhoto()) {
            if (session.getState() != SessionState.WAITING_FOR_PHOTO
                    || session.getSelectedFilterId() == null) {
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Чтобы применить эффект, сначала выбери фильтр через /start 🙂")
                        .build());
                return;
            }

            handlePhotoGeneration(msg, session);
        }
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
            showNode(chatId, msgId, registry.getNode(nodeId));
        } else if (data.startsWith("BACK:")) {
            String nodeId = data.substring("BACK:".length());
            session.setCurrentNodeId(nodeId);
            session.setState(SessionState.BROWSING);
            showNode(chatId, msgId, registry.getNode(nodeId));
        } else if (data.startsWith("SELECT:")) {
            String id = data.substring("SELECT:".length());
            session.setSelectedFilterId(id);
            session.setState(SessionState.WAITING_FOR_PHOTO);
            askForPhoto(chatId, msgId);
        } else if (data.startsWith("EXAMPLE:")) {
            String id = data.substring("EXAMPLE:".length());
            FilterNode node = registry.getNode(id);
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("🖼 Пример фильтра \"" + node.getTitle() + "\" пока не подключён.\n" +
                            "Но ты уже можешь попробовать его на своём фото 🙂")
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

    // ===== MENUS =====

    private void sendMainMenu(long chatId) throws TelegramApiException {
        InlineKeyboardMarkup kb = buildKeyboardForRoot();

        String text = "👋 Привет! Я *NanoAvatar* — бот, который делает красивые нейро-образы из твоих фото.\n\n" +
                "1️⃣ Выбери категорию ниже\n" +
                "2️⃣ Найди фильтр под настроение\n" +
                "3️⃣ Нажми «Выбрать фильтр» и отправь фото\n\n" +
                "Каждая генерация стоит *" + promptPriceCredits + "* кредит. " +
                "Пополнить баланс можно через кнопку в меню 👇";

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(kb)
                .build();

        execute(msg);
    }

    private void showNode(long chatId, int messageId, FilterNode node) throws TelegramApiException {
        if (node == null) return;

        if (node.getId().equals(FilterRegistry.ROOT_ID)) {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("👋 Главное меню. Выбирай категорию 👇")
                    .replyMarkup(buildKeyboardForRoot())
                    .build();
            execute(edit);
            return;
        }

        if (node.isLeaf()) {
            String text = "🧩 *" + node.getTitle() + "*\n\n" +
                    node.getDescription() + "\n\n" +
                    "Стоимость применения фильтра: *" + promptPriceCredits + "* кредит.";
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .parseMode(ParseMode.MARKDOWN)
                    .replyMarkup(buildKeyboardForLeaf(node))
                    .build();
            execute(edit);
        } else {
            String text = "Выбрано: *" + node.getTitle() + "*\n\n" +
                    "Тыкни на один из фильтров ниже 👇";
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .parseMode(ParseMode.MARKDOWN)
                    .replyMarkup(buildKeyboardForCategory(node))
                    .build();
            execute(edit);
        }
    }

    private void askForPhoto(long chatId, int messageId) throws TelegramApiException {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("📸 Пришлите *одно* фото для обработки.\n\n" +
                        "Можно добавить подпись к фото — она допишет промпт (особенно полезно в режимах \"Текстовый запрос\").")
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(buildBackOnlyKeyboard())
                .build();
        execute(edit);
    }

    private void showBalanceScreen(long chatId, int messageId) throws TelegramApiException {
        int bal = userService.getBalance(chatId);

        String text = "💳 *Баланс / пополнить*\n\n" +
                "Текущий баланс: *" + bal + "* кредитов.\n" +
                "Одна генерация стоит *" + promptPriceCredits + "* кредит.\n\n" +
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

    private InlineKeyboardMarkup buildKeyboardForCategory(FilterNode category) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<String> childIds = category.getChildrenIds();
        List<FilterNode> children = new ArrayList<>();
        for (String id : childIds) children.add(registry.getNode(id));

        for (int i = 0; i < children.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buttonForNode(children.get(i)));
            if (i + 1 < children.size()) {
                row.add(buttonForNode(children.get(i + 1)));
            }
            rows.add(row);
        }

        rows.add(List.of(backButton(FilterRegistry.ROOT_ID)));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardMarkup buildKeyboardForLeaf(FilterNode leaf) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("✅ Выбрать фильтр")
                        .callbackData("SELECT:" + leaf.getId())
                        .build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("🖼 Посмотреть пример")
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

    private InlineKeyboardButton backButton(String targetId) {
        return InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("BACK:" + targetId)
                .build();
    }

    // ===== GENERATION =====

    private void handlePhotoGeneration(Message msg, UserSession session) throws Exception {
        long chatId = msg.getChatId();

        int balance = userService.getBalance(chatId);
        if (balance < promptPriceCredits) {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("😔 Недостаточно кредитов. Твой баланс: " + balance +
                            ".\nКоманда для пополнения: /topup или кнопка \"💳 Баланс / пополнить\" в меню.")
                    .build());
            return;
        }

        // самое большое фото
        List<PhotoSize> photos = msg.getPhoto();
        PhotoSize largest = photos.get(photos.size() - 1);
        String fileId = largest.getFileId();

        // получаем путь файла у Telegram и строим публичный URL
        GetFile getFileMethod = new GetFile();          // <-- вот тут фикc
        getFileMethod.setFileId(fileId);
        org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFileMethod);
        String filePath = tgFile.getFilePath();
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

        String caption = msg.getCaption() != null ? msg.getCaption() : "";

        FilterNode filter = registry.getNode(session.getSelectedFilterId());
        String prompt = buildPromptForFilter(filter, caption);

        // списываем баланс заранее
        userService.changeBalance(chatId, -promptPriceCredits, "SPEND", filter.getId());

        try {
            byte[] resultBytes = geminiClient.generateImage(prompt, fileUrl);

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption("✨ Готово! Фильтр: " + filter.getTitle());
            sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(resultBytes), "result.jpg"));
            sendPhoto.setReplyMarkup(buildBackOnlyKeyboard());

            execute(sendPhoto);
        } catch (IOException | IllegalStateException ex) {
            // откат
            userService.changeBalance(chatId, promptPriceCredits, "REFUND", "gemini_error");
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("⚙️ Не удалось получить картинку от нейросети: " + ex.getMessage() + "\n" +
                            "Я вернул кредит на твой баланс.")
                    .build());
        }

        session.setState(SessionState.BROWSING);
        session.setSelectedFilterId(null);
    }

    private String buildPromptForFilter(FilterNode filter, String userCaption) {
        StringBuilder sb = new StringBuilder();
        sb.append("Apply the following creative style to the user's portrait photo: ");
        sb.append(filter.getPromptPart());

        if (userCaption != null && !userCaption.isBlank()) {
            sb.append(" Additionally, follow these extra user instructions (they may be in Russian): ");
            sb.append(userCaption);
        }

        sb.append(" Preserve the person's identity and facial features, keep the result realistic enough for a social-media avatar, ");
        sb.append("high-quality details, 4k, portrait orientation.");
        sb.append(" When you finish, generate the final image and respond ONLY with a direct https URL to that image, without any extra text.");

        return sb.toString();
    }
}