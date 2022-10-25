package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.NotificationTask.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final NotificationTaskRepository notificationTaskRepository;

    static private final String NOTICE_PATTERN = "([0-9.:\\s]{16})(\\s)([\\W+|\\w+]+)";

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotification() {
        Collection<NotificationTask> sentNotifications = notificationTaskRepository
                .findNotificationTasksByDateTime(LocalDateTime.now().
                        truncatedTo(ChronoUnit.MINUTES));
        sentNotifications.forEach(notificationTask -> {
            long chatId = notificationTask.getChatId();
            String message = notificationTask.getNotificationText();
            if (!message.isEmpty()) {
                SendResponse sendNotification = telegramBot.execute(new SendMessage(chatId,
                        "новое уведомление - " + message));
                logger.info("notification - {}, chatId - {}", message, chatId);
                extracted(sendNotification);
            }
        });
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
       try {
           updates.forEach(update ->  {
               long chatId = update.message().chat().id();
               String text = update.message().text();
               logger.info("Processing update: {}", update);
               if (Objects.equals(text, "/start")) {
                   SendResponse response = telegramBot.execute(new SendMessage(chatId,
                           "Hi!"));
                   extracted(response);
               } else {
                   try  {
                       createNotification(update);
                       SendResponse response = telegramBot.execute(new SendMessage(chatId,
                               "Notification saved"));
                       extracted(response);
                       logger.info("Data saved");
                   } catch (DataFormatException e) {
                       logger.warn("Data unsaved");
                       SendResponse response = telegramBot.execute(new SendMessage(
                               chatId, "Incorrect notification, try to repeat"));
                       extracted(response);
                   }
               }
           }); } finally { { return UpdatesListener.CONFIRMED_UPDATES_ALL; }
       }
    }

    private void extracted(SendResponse response) {
        if (!response.isOk()) {
            logger.warn("Response error code: {}", response.errorCode());
        } else {
            logger.info("Response : {}", response.isOk());
        }
    }

    public Collection<String> createTextAndDateForNotice(String text)
            throws DataFormatException {
        Pattern pattern = Pattern.compile(NOTICE_PATTERN);
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()) {
            String date = matcher.group(1);
            String notification = matcher.group(3);
            return List.of(date,notification);
        } else {
            logger.warn("Incorrect format data");
            throw new DataFormatException("Incorrect format data");
        }
    }
    public void createNotification(Update update) throws DataFormatException {
        String message = update.message().text();
        long chatId = update.message().chat().id();
        createTextAndDateForNotice(message);
        List<String> textAndDate = new ArrayList<>(createTextAndDateForNotice(message));
        String date = textAndDate.get(0);
        String text = textAndDate.get(1);
        LocalDateTime localDateTime = LocalDateTime.parse
                (date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        NotificationTask notificationTask = new NotificationTask
                (1, chatId, text, localDateTime);
        notificationTaskRepository.save(notificationTask);
        logger.info("Notification save {}", notificationTask);
    }



   /*public void createNotification(Update update) {
        String text = update.message().text();
        long chatId = update.message().chat().id();
        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()) {
            System.out.println("работает");
        }
    }*/

}