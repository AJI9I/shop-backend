package com.miners.shop.repository;

import com.miners.shop.entity.WhatsAppMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    
    Optional<WhatsAppMessage> findByMessageId(String messageId);
    
    List<WhatsAppMessage> findByChatIdOrderByTimestampDesc(String chatId);
    
    List<WhatsAppMessage> findByChatTypeOrderByTimestampDesc(String chatType);
    
    Page<WhatsAppMessage> findAllByOrderByTimestampDesc(Pageable pageable);
    
    @Query("SELECT m FROM WhatsAppMessage m WHERE m.timestamp >= :since ORDER BY m.timestamp DESC")
    List<WhatsAppMessage> findRecentMessages(LocalDateTime since);
    
    Page<WhatsAppMessage> findByChatTypeOrderByTimestampDesc(String chatType, Pageable pageable);
    
    long countByChatType(String chatType);
    
    /**
     * Находит сообщения от продавца в конкретном чате, отсортированные по времени (новые сначала)
     * Используется для обнаружения дубликатов и обновлений
     */
    List<WhatsAppMessage> findBySenderPhoneNumberAndChatIdOrderByTimestampDesc(
            String senderPhoneNumber, String chatId);
}
