package com.miners.shop.controller;

import com.miners.shop.entity.Offer;
import com.miners.shop.entity.OperationType;
import com.miners.shop.entity.Product;
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
            @RequestParam(required = false) String seriesFilter,
            Model model) {
        log.info("Открыта страница предложений /private/offers, страница: {}, размер: {}, производитель: {}, тип операции: {}, дата: {}", 
                page, size, manufacturer, operationType, dateFilter);
        
        // Получаем список уникальных производителей для фильтра
        long startTime = System.currentTimeMillis();
        List<String> manufacturers = offerRepository.findDistinctManufacturers();
        long manufacturersTime = System.currentTimeMillis() - startTime;
        log.info("Загрузка списка производителей заняла {} мс, найдено: {}", manufacturersTime, manufacturers.size());
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
        // Для фильтра по конкретному дню используем диапазон: от начала дня до начала следующего дня
        LocalDateTime dateFrom = null;
        LocalDateTime dateTo = null;
        if (dateFilter != null && !dateFilter.isEmpty()) {
            try {
                LocalDate filterDate = LocalDate.parse(dateFilter);
                dateFrom = filterDate.atStartOfDay(); // Начало выбранного дня
                dateTo = filterDate.plusDays(1).atStartOfDay(); // Начало следующего дня (конец выбранного дня)
                log.debug("Фильтр по дате установлен: от {} до {}", dateFrom, dateTo);
            } catch (Exception e) {
                log.warn("Неверный формат даты: {}", dateFilter);
            }
        }
        
        // Получаем предложения с фильтрацией
        // Используем универсальные методы, которые поддерживают все фильтры (включая серию)
        Page<Offer> offersPage;
        String manufacturerFilter = (manufacturer != null && !manufacturer.trim().isEmpty()) ? manufacturer.trim() : null;
        String seriesFilterValue = (seriesFilter != null && !seriesFilter.trim().isEmpty()) ? seriesFilter.trim() : null;
        
        long queryStartTime = System.currentTimeMillis();
        if (dateFrom != null && dateTo != null) {
            // Есть фильтр по дате (за конкретный день) - используем метод с диапазоном дат
            log.debug("Выполняется запрос с фильтром по дате: от {} до {}", dateFrom, dateTo);
            offersPage = offerRepository.findByManufacturerAndOperationTypeAndSeriesAndCreatedAtBetweenOrderByCreatedAtDesc(
                    manufacturerFilter, operationTypeEnum, seriesFilterValue, dateFrom, dateTo, pageable);
        } else {
            // Нет фильтра по дате - используем метод без даты
            log.debug("Выполняется запрос без фильтра по дате");
            offersPage = offerRepository.findByManufacturerAndOperationTypeAndSeriesOrderByCreatedAtDesc(
                    manufacturerFilter, operationTypeEnum, seriesFilterValue, pageable);
        }
        long queryTime = System.currentTimeMillis() - queryStartTime;
        log.info("Запрос предложений выполнен за {} мс, найдено: {} из {}", queryTime, offersPage.getNumberOfElements(), offersPage.getTotalElements());
        
        // Инициализируем связанные сущности (Product, MinerDetail и Seller) для избежания LazyInitializationException
        long initStartTime = System.currentTimeMillis();
        offersPage.getContent().forEach(offer -> {
            if (offer.getProduct() != null) {
                Product product = offer.getProduct();
                product.getId(); // Инициализируем Product
                // Инициализируем MinerDetail, если он связан с Product
                if (product.getMinerDetail() != null) {
                    product.getMinerDetail().getId();
                    product.getMinerDetail().getStandardName(); // Инициализируем название
                }
            }
            // Seller уже EAGER, но на всякий случай
            if (offer.getSeller() != null) {
                offer.getSeller().getId();
            }
        });
        long initTime = System.currentTimeMillis() - initStartTime;
        log.info("Инициализация связанных сущностей заняла {} мс", initTime);
        
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
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String series) {
        long ajaxStartTime = System.currentTimeMillis();
        log.info("AJAX запрос предложений, страница: {}, размер: {}, производитель: {}, тип операции: {}, дата: {}, серия: {}", 
                page, size, manufacturer, operationType, dateFilter, series);
        
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
        // Для фильтра по конкретному дню используем диапазон: от начала дня до начала следующего дня
        LocalDateTime dateFrom = null;
        LocalDateTime dateTo = null;
        if (dateFilter != null && !dateFilter.isEmpty()) {
            try {
                LocalDate filterDate = LocalDate.parse(dateFilter);
                dateFrom = filterDate.atStartOfDay(); // Начало выбранного дня
                dateTo = filterDate.plusDays(1).atStartOfDay(); // Начало следующего дня (конец выбранного дня)
                log.info("Фильтр по дате распарсен: {} -> от {} до {}", dateFilter, dateFrom, dateTo);
            } catch (Exception e) {
                log.warn("Неверный формат даты: {}", dateFilter, e);
            }
        }
        
        // Получаем предложения с фильтрацией
        // Используем универсальные методы, которые поддерживают все фильтры (включая серию)
        Page<Offer> offersPage;
        String manufacturerFilter = (manufacturer != null && !manufacturer.trim().isEmpty()) ? manufacturer.trim() : null;
        String seriesFilter = (series != null && !series.trim().isEmpty()) ? series.trim() : null;
        
        log.info("Фильтры для AJAX: manufacturer={}, operationType={}, series={}, dateFrom={}, dateTo={}", 
                manufacturerFilter, operationTypeEnum, seriesFilter, dateFrom, dateTo);
        
        long queryStartTime = System.currentTimeMillis();
        if (dateFrom != null && dateTo != null) {
            // Есть фильтр по дате (за конкретный день) - используем метод с диапазоном дат
            log.debug("Используем фильтр: универсальный (с датой)");
            offersPage = offerRepository.findByManufacturerAndOperationTypeAndSeriesAndCreatedAtBetweenOrderByCreatedAtDesc(
                    manufacturerFilter, operationTypeEnum, seriesFilter, dateFrom, dateTo, pageable);
        } else {
            // Нет фильтра по дате - используем метод без даты
            log.debug("Используем фильтр: универсальный (без даты)");
            offersPage = offerRepository.findByManufacturerAndOperationTypeAndSeriesOrderByCreatedAtDesc(
                    manufacturerFilter, operationTypeEnum, seriesFilter, pageable);
        }
        long queryTime = System.currentTimeMillis() - queryStartTime;
        log.info("Запрос предложений выполнен за {} мс, найдено: {} из {}", 
                queryTime, offersPage.getNumberOfElements(), offersPage.getTotalElements());
        
        // Инициализируем связанные сущности (Product, MinerDetail и Seller) для избежания LazyInitializationException
        long initStartTime = System.currentTimeMillis();
        offersPage.getContent().forEach(offer -> {
            if (offer.getProduct() != null) {
                Product product = offer.getProduct();
                product.getId();
                // Инициализируем MinerDetail, если он связан с Product
                if (product.getMinerDetail() != null) {
                    product.getMinerDetail().getId();
                    product.getMinerDetail().getStandardName(); // Инициализируем название
                }
            }
            if (offer.getSeller() != null) {
                offer.getSeller().getId();
            }
        });
        long initTime = System.currentTimeMillis() - initStartTime;
        log.info("Инициализация связанных сущностей заняла {} мс", initTime);
        
        // Формируем HTML таблицы
        long htmlStartTime = System.currentTimeMillis();
        StringBuilder html = new StringBuilder();
        
        if (offersPage.getContent().isEmpty()) {
            html.append("<tr><td colspan=\"12\" class=\"text-center py-24 text-muted\">Предложения не найдены</td></tr>");
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
                // Название майнер детали
                Product product = offer.getProduct();
                if (product != null && product.getMinerDetail() != null) {
                    html.append(escapeHtml(product.getMinerDetail().getStandardName()));
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
                if (offer.getHashrate() != null && !offer.getHashrate().isEmpty()) {
                    html.append(escapeHtml(offer.getHashrate()));
                } else {
                    html.append("<span class=\"text-muted\">-</span>");
                }
                html.append("</td>");
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
        long htmlTime = System.currentTimeMillis() - htmlStartTime;
        log.info("Формирование HTML заняло {} мс", htmlTime);
        
        long totalTime = System.currentTimeMillis() - ajaxStartTime;
        log.info("Общее время обработки AJAX запроса: {} мс", totalTime);
        
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
        long startTime = System.currentTimeMillis();
        log.info("[GET_OFFER_DETAILS] ========== НАЧАЛО запроса деталей предложения ==========");
        log.info("[GET_OFFER_DETAILS] ID предложения: {}", id);
        
        try {
        
        long findStartTime = System.currentTimeMillis();
        Optional<Offer> offerOpt = offerRepository.findById(id);
        long findTime = System.currentTimeMillis() - findStartTime;
        log.info("[GET_OFFER_DETAILS] Поиск предложения в БД занял {} мс, найдено: {}", findTime, offerOpt.isPresent());
        
        if (offerOpt.isEmpty()) {
            log.warn("[GET_OFFER_DETAILS] Предложение с id={} не найдено", id);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Предложение не найдено");
            return ResponseEntity.notFound().build();
        }
        
        Offer offer = offerOpt.get();
        log.info("[GET_OFFER_DETAILS] Предложение найдено: id={}, manufacturer={}, operationType={}", 
                offer.getId(), offer.getManufacturer(), offer.getOperationType());
        
        // Инициализируем связанные сущности для избежания LazyInitializationException
        long initStartTime = System.currentTimeMillis();
        log.info("[GET_OFFER_DETAILS] Начало инициализации связанных сущностей");
        try {
            if (offer.getProduct() != null) {
                long productInitStart = System.currentTimeMillis();
                offer.getProduct().getId();
                long productInitTime = System.currentTimeMillis() - productInitStart;
                log.info("[GET_OFFER_DETAILS] Инициализация Product заняла {} мс", productInitTime);
                
                // Инициализируем MinerDetail, если он связан с Product
                if (offer.getProduct().getMinerDetail() != null) {
                    long minerDetailInitStart = System.currentTimeMillis();
                    offer.getProduct().getMinerDetail().getId();
                    offer.getProduct().getMinerDetail().getStandardName();
                    long minerDetailInitTime = System.currentTimeMillis() - minerDetailInitStart;
                    log.info("[GET_OFFER_DETAILS] Инициализация MinerDetail заняла {} мс, standardName={}", 
                            minerDetailInitTime, offer.getProduct().getMinerDetail().getStandardName());
                } else {
                    log.info("[GET_OFFER_DETAILS] MinerDetail не связан с Product");
                }
            } else {
                log.info("[GET_OFFER_DETAILS] Product не связан с Offer");
            }
            
            if (offer.getSeller() != null) {
                long sellerInitStart = System.currentTimeMillis();
                offer.getSeller().getId();
                offer.getSeller().getPhone(); // Инициализируем телефон
                long sellerInitTime = System.currentTimeMillis() - sellerInitStart;
                log.info("[GET_OFFER_DETAILS] Инициализация Seller заняла {} мс, phone={}", 
                        sellerInitTime, offer.getSeller().getPhone());
            } else {
                log.info("[GET_OFFER_DETAILS] Seller не связан с Offer");
            }
        } catch (Exception e) {
            long initTime = System.currentTimeMillis() - initStartTime;
            log.error("[GET_OFFER_DETAILS] Ошибка при инициализации связанных сущностей за {} мс для предложения id={}: {}", 
                    initTime, id, e.getMessage(), e);
            // Продолжаем выполнение, даже если есть ошибка
        }
        long initTime = System.currentTimeMillis() - initStartTime;
        log.info("[GET_OFFER_DETAILS] Общая инициализация связанных сущностей заняла {} мс", initTime);
        
        long dataBuildStartTime = System.currentTimeMillis();
        log.info("[GET_OFFER_DETAILS] Начало формирования данных ответа");
        
        Map<String, Object> response = new HashMap<>();
        
        // Информация о предложении
        long offerDataStartTime = System.currentTimeMillis();
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
        
        long offerDataTime = System.currentTimeMillis() - offerDataStartTime;
        log.info("[GET_OFFER_DETAILS] Формирование offerData заняло {} мс", offerDataTime);
        
        response.put("offer", offerData);
        
        // Получаем исходное сообщение из WhatsApp
        long messageStartTime = System.currentTimeMillis();
        log.info("[GET_OFFER_DETAILS] Поиск исходного сообщения WhatsApp, sourceMessageId={}", offer.getSourceMessageId());
        
        if (offer.getSourceMessageId() != null && !offer.getSourceMessageId().isEmpty()) {
            long messageFindStartTime = System.currentTimeMillis();
            Optional<WhatsAppMessage> messageOpt = messageRepository.findByMessageId(offer.getSourceMessageId());
            long messageFindTime = System.currentTimeMillis() - messageFindStartTime;
            log.info("[GET_OFFER_DETAILS] Поиск сообщения в БД занял {} мс, найдено: {}", messageFindTime, messageOpt.isPresent());
            
            if (messageOpt.isPresent()) {
                WhatsAppMessage message = messageOpt.get();
                long messageDataStartTime = System.currentTimeMillis();
                log.info("[GET_OFFER_DETAILS] Сообщение найдено: id={}, chatName={}, senderName={}", 
                        message.getId(), message.getChatName(), message.getSenderName());
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
                
                long messageDataTime = System.currentTimeMillis() - messageDataStartTime;
                log.info("[GET_OFFER_DETAILS] Формирование messageData заняло {} мс", messageDataTime);
                
                response.put("sourceMessage", messageData);
            } else {
                log.warn("[GET_OFFER_DETAILS] Сообщение с messageId={} не найдено для предложения id={}", 
                        offer.getSourceMessageId(), id);
                response.put("sourceMessage", null);
            }
        } else {
            log.info("[GET_OFFER_DETAILS] sourceMessageId пуст, исходное сообщение не запрашивается");
            response.put("sourceMessage", null);
        }
        
        long messageTime = System.currentTimeMillis() - messageStartTime;
        log.info("[GET_OFFER_DETAILS] Обработка исходного сообщения заняла {} мс", messageTime);
        
        long dataBuildTime = System.currentTimeMillis() - dataBuildStartTime;
        log.info("[GET_OFFER_DETAILS] Формирование данных ответа заняло {} мс", dataBuildTime);
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[GET_OFFER_DETAILS] ========== КОНЕЦ запроса деталей предложения ==========");
        log.info("[GET_OFFER_DETAILS] Общее время обработки запроса деталей предложения id={}: {} мс", id, totalTime);
        log.info("[GET_OFFER_DETAILS] Разбивка времени: поиск={} мс, инициализация={} мс, данные={} мс, сообщение={} мс", 
                findTime, initTime, dataBuildTime, messageTime);
        
        // ДЕТАЛЬНОЕ ЛОГИРОВАНИЕ ОТПРАВЛЯЕМОГО ОТВЕТА
        log.info("[GET_OFFER_DETAILS] ========== СОДЕРЖИМОЕ ОТВЕТА ==========");
        log.info("[GET_OFFER_DETAILS] Размер ответа: {} ключей", response.size());
        log.info("[GET_OFFER_DETAILS] Ключи в ответе: {}", response.keySet());
        
        if (response.containsKey("offer")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> offerDataForLog = (Map<String, Object>) response.get("offer");
            log.info("[GET_OFFER_DETAILS] Данные предложения (offer):");
            log.info("[GET_OFFER_DETAILS]   - id: {}", offerDataForLog.get("id"));
            log.info("[GET_OFFER_DETAILS]   - productModel: {}", offerDataForLog.get("productModel"));
            log.info("[GET_OFFER_DETAILS]   - manufacturer: {}", offerDataForLog.get("manufacturer"));
            log.info("[GET_OFFER_DETAILS]   - hashrate: {}", offerDataForLog.get("hashrate"));
            log.info("[GET_OFFER_DETAILS]   - sellerPhone: {}", offerDataForLog.get("sellerPhone"));
            log.info("[GET_OFFER_DETAILS]   - operationType: {}", offerDataForLog.get("operationType"));
            log.info("[GET_OFFER_DETAILS]   - price: {}", offerDataForLog.get("price"));
            log.info("[GET_OFFER_DETAILS]   - currency: {}", offerDataForLog.get("currency"));
            log.info("[GET_OFFER_DETAILS]   - quantity: {}", offerDataForLog.get("quantity"));
            log.info("[GET_OFFER_DETAILS]   - condition: {}", offerDataForLog.get("condition"));
            log.info("[GET_OFFER_DETAILS]   - location: {}", offerDataForLog.get("location"));
            log.info("[GET_OFFER_DETAILS]   - notes: {}", offerDataForLog.get("notes"));
            log.info("[GET_OFFER_DETAILS]   - createdAt: {}", offerDataForLog.get("createdAt"));
            log.info("[GET_OFFER_DETAILS]   - updatedAt: {}", offerDataForLog.get("updatedAt"));
            log.info("[GET_OFFER_DETAILS]   - sourceChatName: {}", offerDataForLog.get("sourceChatName"));
        }
        
        if (response.containsKey("sourceMessage")) {
            Object sourceMessageObj = response.get("sourceMessage");
            if (sourceMessageObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = (Map<String, Object>) sourceMessageObj;
                log.info("[GET_OFFER_DETAILS] Данные исходного сообщения (sourceMessage):");
                log.info("[GET_OFFER_DETAILS]   - id: {}", messageData.get("id"));
                log.info("[GET_OFFER_DETAILS]   - messageId: {}", messageData.get("messageId"));
                log.info("[GET_OFFER_DETAILS]   - chatName: {}", messageData.get("chatName"));
                log.info("[GET_OFFER_DETAILS]   - senderName: {}", messageData.get("senderName"));
                log.info("[GET_OFFER_DETAILS]   - content (первые 200 символов): {}", 
                        messageData.get("content") != null ? 
                        ((String) messageData.get("content")).substring(0, Math.min(200, ((String) messageData.get("content")).length())) : "null");
            } else {
                log.info("[GET_OFFER_DETAILS] sourceMessage: null (исходное сообщение не найдено)");
            }
        }
        
        log.info("[GET_OFFER_DETAILS] ========== КОНЕЦ СОДЕРЖИМОГО ОТВЕТА ==========");
        
        log.info("[GET_OFFER_DETAILS] Возвращаем ResponseEntity.ok() с response");
        ResponseEntity<Map<String, Object>> result = ResponseEntity.ok().body(response);
        log.info("[GET_OFFER_DETAILS] ResponseEntity создан успешно");
        return result;
        
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[GET_OFFER_DETAILS] ========== КРИТИЧЕСКАЯ ОШИБКА ==========");
            log.error("[GET_OFFER_DETAILS] Ошибка при обработке запроса деталей предложения id={} за {} мс", id, totalTime);
            log.error("[GET_OFFER_DETAILS] Тип исключения: {}", e.getClass().getName());
            log.error("[GET_OFFER_DETAILS] Сообщение: {}", e.getMessage());
            log.error("[GET_OFFER_DETAILS] Stack trace:", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Ошибка при загрузке данных: " + e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
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
    
    /**
     * AJAX эндпоинт для получения списка серий по производителю
     * Серии берутся из MinerDetail через связь Offer -> Product -> MinerDetail
     * Используется для динамической загрузки фильтра серий при выборе производителя
     */
    @GetMapping(value = "/private/offers/series", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    @ResponseBody
    public ResponseEntity<List<String>> getSeriesByManufacturer(
            @RequestParam(required = false) String manufacturer) {
        log.info("AJAX запрос серий для производителя: {}", manufacturer);
        
        if (manufacturer == null || manufacturer.trim().isEmpty()) {
            return ResponseEntity.ok().body(List.of());
        }
        
        List<String> series = offerRepository.findDistinctSeriesByManufacturer(manufacturer.trim());
        log.info("Найдено серий для производителя {}: {}", manufacturer, series.size());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(series);
    }
}








