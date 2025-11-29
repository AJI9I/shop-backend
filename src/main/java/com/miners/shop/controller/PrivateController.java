package com.miners.shop.controller;

import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.WhatsAppMessage;
import com.miners.shop.repository.OfferRepository;
import com.miners.shop.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер для приватной страницы администратора
 * Содержит ссылки на основные разделы: сообщения, товары, заявки
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PrivateController {
    
    private final OfferRepository offerRepository;
    private final WhatsAppMessageRepository messageRepository;
    
    /**
     * Приватная страница с навигацией по основным разделам
     */
    @GetMapping("/private")
    public String privatePage(Model model) {
        log.info("Открыта приватная страница /private");
        
        // Можно добавить статистику для отображения на странице
        model.addAttribute("pageTitle", "Приватная панель");
        
        return "private";
    }
    
    /**
     * Страница со всеми предложениями, отсортированными от последнего по убыванию
     * Доступна для администраторов и менеджеров
     */
    @GetMapping("/private/offers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public String offersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String dateFilter,
            Model model) {
        log.info("Открыта страница предложений /private/offers, страница: {}, размер: {}, производитель: {}, тип операции: {}, дата: {}", 
                page, size, manufacturer, operationType, dateFilter);
        
        // Получаем список уникальных производителей для фильтра
        List<String> manufacturers = offerRepository.findDistinctManufacturers();
        model.addAttribute("manufacturers", manufacturers);
        
        // Создаем Pageable с сортировкой по дате создания (от последнего)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Преобразуем строку operationType в enum, если она указана
        OperationType operationTypeEnum = null;
        if (operationType != null && !operationType.isEmpty()) {
            try {
                operationTypeEnum = OperationType.valueOf(operationType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Неверный тип операции: {}", operationType);
            }
        }
        
        // Обрабатываем фильтр по дате
        LocalDateTime dateFrom = null;
        if (dateFilter != null && !dateFilter.isEmpty()) {
            try {
                LocalDate filterDate = LocalDate.parse(dateFilter);
                dateFrom = filterDate.atStartOfDay();
                log.debug("Фильтр по дате установлен: {}", dateFrom);
            } catch (Exception e) {
                log.warn("Неверный формат даты: {}", dateFilter);
            }
        }
        
        // Получаем предложения с фильтрацией
        Page<Offer> offersPage;
        boolean hasFilters = (manufacturer != null && !manufacturer.isEmpty()) || operationTypeEnum != null || dateFrom != null;
        
        if (hasFilters) {
            offersPage = offerRepository.findByManufacturerAndOperationTypeAndDateOrderByCreatedAtDesc(
                    manufacturer != null && !manufacturer.trim().isEmpty() ? manufacturer.trim() : null,
                    operationTypeEnum,
                    dateFrom,
                    pageable);
        } else {
            offersPage = offerRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        // Инициализируем связанные сущности (Product и Seller) для избежания LazyInitializationException
        offersPage.getContent().forEach(offer -> {
            if (offer.getProduct() != null) {
                offer.getProduct().getId(); // Инициализируем Product
            }
            // Seller уже EAGER, но на всякий случай
            if (offer.getSeller() != null) {
                offer.getSeller().getId();
            }
        });
        
        model.addAttribute("offersPage", offersPage);
        model.addAttribute("offers", offersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", offersPage.getTotalPages());
        model.addAttribute("totalElements", offersPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("pageTitle", "Все предложения");
        model.addAttribute("selectedManufacturer", manufacturer);
        model.addAttribute("selectedOperationType", operationType);
        model.addAttribute("selectedDateFilter", dateFilter);
        
        log.info("Загружено предложений: {} из {}", offersPage.getNumberOfElements(), offersPage.getTotalElements());
        
        return "offers";
    }
    
    /**
     * AJAX эндпоинт для получения отфильтрованных предложений
     * Возвращает JSON с HTML таблицы и информацией о пагинации
     */
    @GetMapping(value = "/private/offers/ajax", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOffersAjax(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String dateFilter) {
        log.info("AJAX запрос предложений, страница: {}, размер: {}, производитель: {}, тип операции: {}, дата: {}", 
                page, size, manufacturer, operationType, dateFilter);
        
        // Создаем Pageable с сортировкой по дате создания (от последнего)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Преобразуем строку operationType в enum, если она указана
        OperationType operationTypeEnum = null;
        if (operationType != null && !operationType.isEmpty()) {
            try {
                operationTypeEnum = OperationType.valueOf(operationType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Неверный тип операции: {}", operationType);
            }
        }
        
        // Обрабатываем фильтр по дате
        LocalDateTime dateFrom = null;
        if (dateFilter != null && !dateFilter.isEmpty()) {
            try {
                LocalDate filterDate = LocalDate.parse(dateFilter);
                dateFrom = filterDate.atStartOfDay();
            } catch (Exception e) {
                log.warn("Неверный формат даты: {}", dateFilter);
            }
        }
        
        // Получаем предложения с фильтрацией
        Page<Offer> offersPage;
        boolean hasFilters = (manufacturer != null && !manufacturer.isEmpty()) || operationTypeEnum != null || dateFrom != null;
        
        if (hasFilters) {
            offersPage = offerRepository.findByManufacturerAndOperationTypeAndDateOrderByCreatedAtDesc(
                    manufacturer != null && !manufacturer.trim().isEmpty() ? manufacturer.trim() : null,
                    operationTypeEnum,
                    dateFrom,
                    pageable);
        } else {
            offersPage = offerRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        // Инициализируем связанные сущности
        offersPage.getContent().forEach(offer -> {
            if (offer.getProduct() != null) {
                offer.getProduct().getId();
            }
            if (offer.getSeller() != null) {
                offer.getSeller().getId();
            }
        });
        
        // Формируем HTML таблицы
        StringBuilder html = new StringBuilder();
        
        if (offersPage.getContent().isEmpty()) {
            html.append("<tr><td colspan=\"10\" class=\"text-center py-24 text-muted\">Предложения не найдены</td></tr>");
        } else {
            for (Offer offer : offersPage.getContent()) {
                html.append("<tr class=\"offer-row\" data-offer-id=\"").append(offer.getId()).append("\" style=\"cursor: pointer;\">");
                html.append("<td>").append(offer.getId()).append("</td>");
                html.append("<td>");
                if (offer.getProduct() != null) {
                    html.append(escapeHtml(offer.getProduct().getModel()));
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
                html.append("<td>");
                // Показываем только номер телефона
                String phone = null;
                if (offer.getSeller() != null && offer.getSeller().getPhone() != null) {
                    phone = offer.getSeller().getPhone();
                } else if (offer.getSellerPhone() != null) {
                    phone = offer.getSellerPhone();
                }
                if (phone != null && !phone.isEmpty()) {
                    html.append(escapeHtml(phone));
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
                html.append("<td>").append(offer.getOperationType() != null ? offer.getOperationType().name() : "-").append("</td>");
                html.append("<td>");
                if (offer.getPrice() != null) {
                    html.append(String.format("%.0f", offer.getPrice().doubleValue()));
                    html.append(offer.getCurrency() != null ? offer.getCurrency() : "u");
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
                html.append("<td>").append(offer.getQuantity() != null ? offer.getQuantity() : "-").append("</td>");
                html.append("<td>");
                if (offer.getCondition() != null) {
                    html.append(escapeHtml(offer.getCondition()));
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
                html.append("<td>");
                if (offer.getLocation() != null) {
                    html.append(escapeHtml(offer.getLocation()));
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
                html.append("<td>");
                if (offer.getNotes() != null && !offer.getNotes().isEmpty()) {
                    String notesShort = offer.getNotes().length() > 50 
                        ? offer.getNotes().substring(0, 50) + "..." 
                        : offer.getNotes();
                    html.append("<span title=\"").append(escapeHtml(offer.getNotes().replace("\"", "&quot;"))).append("\">");
                    html.append(escapeHtml(notesShort));
                    html.append("</span>");
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
                html.append("<td>");
                if (offer.getCreatedAt() != null) {
                    html.append(offer.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                } else {
                    html.append("-");
                }
                html.append("</td>");
                html.append("</tr>");
            }
        }
        
        // Формируем JSON ответ с HTML и информацией о пагинации
        Map<String, Object> response = new HashMap<>();
        response.put("html", html.toString());
        response.put("totalElements", offersPage.getTotalElements());
        response.put("totalPages", offersPage.getTotalPages());
        response.put("currentPage", offersPage.getNumber());
        response.put("pageSize", offersPage.getSize());
        response.put("numberOfElements", offersPage.getNumberOfElements());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * AJAX эндпоинт для получения полной информации о предложении
     * Включает информацию о предложении и исходное сообщение из WhatsApp
     */
    @GetMapping(value = "/private/offers/{id}/details", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOfferDetails(@PathVariable Long id) {
        log.info("Запрос полной информации о предложении: id={}", id);
        
        Optional<Offer> offerOpt = offerRepository.findById(id);
        if (offerOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Предложение не найдено");
            return ResponseEntity.notFound().build();
        }
        
        Offer offer = offerOpt.get();
        
        // Инициализируем связанные сущности
        if (offer.getProduct() != null) {
            offer.getProduct().getId();
        }
        if (offer.getSeller() != null) {
            offer.getSeller().getId();
        }
        
        Map<String, Object> response = new HashMap<>();
        
        // Информация о предложении
        Map<String, Object> offerData = new HashMap<>();
        offerData.put("id", offer.getId());
        offerData.put("productModel", offer.getProduct() != null ? offer.getProduct().getModel() : null);
        offerData.put("manufacturer", offer.getManufacturer());
        offerData.put("hashrate", offer.getHashrate());
        offerData.put("sellerPhone", offer.getSeller() != null ? offer.getSeller().getPhone() : offer.getSellerPhone());
        offerData.put("operationType", offer.getOperationType() != null ? offer.getOperationType().name() : null);
        offerData.put("price", offer.getPrice() != null ? offer.getPrice().toString() : null);
        offerData.put("currency", offer.getCurrency());
        offerData.put("quantity", offer.getQuantity());
        offerData.put("condition", offer.getCondition());
        offerData.put("location", offer.getLocation());
        offerData.put("notes", offer.getNotes());
        offerData.put("createdAt", offer.getCreatedAt() != null ? 
                offer.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : null);
        offerData.put("updatedAt", offer.getUpdatedAt() != null ? 
                offer.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : null);
        offerData.put("sourceChatName", offer.getSourceChatName());
        
        response.put("offer", offerData);
        
        // Получаем исходное сообщение из WhatsApp
        if (offer.getSourceMessageId() != null && !offer.getSourceMessageId().isEmpty()) {
            Optional<WhatsAppMessage> messageOpt = messageRepository.findByMessageId(offer.getSourceMessageId());
            if (messageOpt.isPresent()) {
                WhatsAppMessage message = messageOpt.get();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("id", message.getId());
                messageData.put("messageId", message.getMessageId());
                messageData.put("chatId", message.getChatId());
                messageData.put("chatName", message.getChatName());
                messageData.put("chatType", message.getChatType());
                messageData.put("senderId", message.getSenderId());
                messageData.put("senderName", message.getSenderName());
                messageData.put("senderPhoneNumber", message.getSenderPhoneNumber());
                messageData.put("content", message.getContent());
                messageData.put("timestamp", message.getTimestamp() != null ? 
                        message.getTimestamp().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : null);
                messageData.put("hasMedia", message.getHasMedia());
                messageData.put("messageType", message.getMessageType());
                messageData.put("isForwarded", message.getIsForwarded());
                messageData.put("isUpdate", message.getIsUpdate());
                messageData.put("originalMessageId", message.getOriginalMessageId());
                messageData.put("createdAt", message.getCreatedAt() != null ? 
                        message.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : null);
                
                response.put("sourceMessage", messageData);
            } else {
                log.warn("Сообщение с messageId={} не найдено для предложения id={}", offer.getSourceMessageId(), id);
                response.put("sourceMessage", null);
            }
        } else {
            response.put("sourceMessage", null);
        }
        
        return ResponseEntity.ok().body(response);
    }
    
    /**
     * Экранирует HTML символы для безопасного вывода
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#039;");
    }
}








