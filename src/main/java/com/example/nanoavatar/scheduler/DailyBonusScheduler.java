package com.example.nanoavatar.scheduler;

import com.example.nanoavatar.user.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyBonusScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final UserService userService;
    private final AbsSender sender;
    private final int bonusCredits;
    private final ZoneId moscowZone;

    public DailyBonusScheduler(UserService userService,
                               AbsSender sender,
                               int bonusCredits,
                               String moscowZoneId) {
        this.userService = userService;
        this.sender = sender;
        this.bonusCredits = bonusCredits;
        this.moscowZone = ZoneId.of(moscowZoneId);
    }

    public void start() {
        long initialDelay = computeInitialDelay();
        long period = TimeUnit.DAYS.toMillis(1);

        scheduler.scheduleAtFixedRate(this::sendDailyBonus,
                initialDelay, period, TimeUnit.MILLISECONDS);
    }

    private long computeInitialDelay() {
        ZonedDateTime now = ZonedDateTime.now(moscowZone);
        ZonedDateTime next10 = now.withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (now.compareTo(next10) >= 0) {
            next10 = next10.plusDays(1);
        }
        return Duration.between(now, next10).toMillis();
    }

    private void sendDailyBonus() {
        List<Long> chatIds = userService.getAllChatIds();
        LocalDate today = LocalDate.now(moscowZone);

        for (Long chatId : chatIds) {
            try {
                var lastBonus = userService.getLastBonusDate(chatId);
                if (lastBonus != null && !lastBonus.isBefore(today)) {
                    continue;
                }
                userService.changeBalance(chatId, bonusCredits, "DAILY_BONUS", "daily_bonus");
                userService.updateLastBonusDate(chatId, today);

                SendMessage msg = SendMessage.builder()
                        .chatId(chatId)
                        .text("🎁 Ежедневный бонус NanoBuddy!\n\n" +
                                "Я закинул на твой баланс +" + bonusCredits +
                                " кредит — этого хватает на один ответ.\n\n" +
                                "Напиши /start, выбери режим или просто задай вопрос ✨")
                        .build();

                sender.execute(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}