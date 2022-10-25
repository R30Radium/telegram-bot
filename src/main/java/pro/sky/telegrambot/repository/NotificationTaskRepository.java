package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.NotificationTask.NotificationTask;
//import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    Collection<NotificationTask> findNotificationTasksByDateTime(LocalDateTime localDateTime);

}