package com.miners.shop.repository;

import com.miners.shop.entity.TelegramGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramGroupRepository extends JpaRepository<TelegramGroup, Long> {
    
    Optional<TelegramGroup> findByChatId(String chatId);
    
    List<TelegramGroup> findByMonitoringEnabledOrderByChatNameAsc(Boolean monitoringEnabled);
    
    List<TelegramGroup> findAllByOrderByChatNameAsc();
    
    @Query("SELECT tg FROM TelegramGroup tg WHERE tg.monitoringEnabled = true ORDER BY tg.chatName ASC")
    List<TelegramGroup> findActiveGroups();
}


