package com.miners.shop.service;

import com.miners.shop.dto.MinerDetailDTO;
import com.miners.shop.entity.MinerDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для экспорта и импорта MinerDetail в Excel
 */
@Service
@Slf4j
public class MinerDetailExcelService {
    
    /**
     * Экспортирует MinerDetail в Excel файл
     * 
     * @param minerDetails Список MinerDetail для экспорта
     * @return ByteArrayOutputStream с Excel файлом
     */
    public ByteArrayOutputStream exportToExcel(List<MinerDetail> minerDetails) throws IOException {
        log.info("Экспорт {} записей MinerDetail в Excel", minerDetails.size());
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Miner Details");
        
        // Создаем стили
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // Создаем заголовки
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
            "ID", "Название", "Производитель", "Серия", "Хэшрейт", "Алгоритм",
            "Потребление", "Монеты", "Источник питания", "Охлаждение",
            "Рабочая температура", "Размеры", "Уровень шума",
            "Описание", "Особенности", "Размещение", "О производителе",
            "Дата создания", "Дата обновления"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Заполняем данные
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (MinerDetail detail : minerDetails) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            row.createCell(colNum++).setCellValue(detail.getId() != null ? detail.getId() : 0);
            row.createCell(colNum++).setCellValue(detail.getStandardName() != null ? detail.getStandardName() : "");
            row.createCell(colNum++).setCellValue(detail.getManufacturer() != null ? detail.getManufacturer() : "");
            row.createCell(colNum++).setCellValue(detail.getSeries() != null ? detail.getSeries() : "");
            row.createCell(colNum++).setCellValue(detail.getHashrate() != null ? detail.getHashrate() : "");
            row.createCell(colNum++).setCellValue(detail.getAlgorithm() != null ? detail.getAlgorithm() : "");
            row.createCell(colNum++).setCellValue(detail.getPowerConsumption() != null ? detail.getPowerConsumption() : "");
            row.createCell(colNum++).setCellValue(detail.getCoins() != null ? detail.getCoins() : "");
            row.createCell(colNum++).setCellValue(detail.getPowerSource() != null ? detail.getPowerSource() : "");
            row.createCell(colNum++).setCellValue(detail.getCooling() != null ? detail.getCooling() : "");
            row.createCell(colNum++).setCellValue(detail.getOperatingTemperature() != null ? detail.getOperatingTemperature() : "");
            row.createCell(colNum++).setCellValue(detail.getDimensions() != null ? detail.getDimensions() : "");
            row.createCell(colNum++).setCellValue(detail.getNoiseLevel() != null ? detail.getNoiseLevel() : "");
            row.createCell(colNum++).setCellValue(detail.getDescription() != null ? detail.getDescription() : "");
            row.createCell(colNum++).setCellValue(detail.getFeatures() != null ? detail.getFeatures() : "");
            row.createCell(colNum++).setCellValue(detail.getPlacementInfo() != null ? detail.getPlacementInfo() : "");
            row.createCell(colNum++).setCellValue(detail.getProducerInfo() != null ? detail.getProducerInfo() : "");
            
            // Даты
            Cell createdAtCell = row.createCell(colNum++);
            if (detail.getCreatedAt() != null) {
                createdAtCell.setCellValue(detail.getCreatedAt().format(dateFormatter));
                createdAtCell.setCellStyle(dateStyle);
            } else {
                createdAtCell.setCellValue("");
            }
            
            Cell updatedAtCell = row.createCell(colNum++);
            if (detail.getUpdatedAt() != null) {
                updatedAtCell.setCellValue(detail.getUpdatedAt().format(dateFormatter));
                updatedAtCell.setCellStyle(dateStyle);
            } else {
                updatedAtCell.setCellValue("");
            }
        }
        
        // Автоматически подгоняем ширину колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Устанавливаем минимальную ширину
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
        
        // Записываем в ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        log.info("Экспорт завершен, размер файла: {} байт", outputStream.size());
        return outputStream;
    }
    
    /**
     * Импортирует MinerDetail из Excel файла
     * 
     * @param inputStream InputStream с Excel файлом
     * @return Список MinerDetailDTO для обновления
     */
    @Transactional(readOnly = true)
    public List<MinerDetailDTO> importFromExcel(InputStream inputStream) throws IOException {
        log.info("Импорт MinerDetail из Excel файла");
        
        List<MinerDetailDTO> dtos = new ArrayList<>();
        
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        
        // Пропускаем заголовок (первая строка)
        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                continue;
            }
            
            try {
                MinerDetailDTO dto = new MinerDetailDTO();
                int colNum = 0;
                
                // ID (обязательное поле)
                Cell idCell = row.getCell(colNum++);
                if (idCell == null || idCell.getCellType() == CellType.BLANK) {
                    log.warn("Строка {} пропущена: отсутствует ID", rowNum + 1);
                    continue;
                }
                
                double idValue = idCell.getNumericCellValue();
                dto.setId((long) idValue);
                
                // Остальные поля
                dto.setStandardName(getCellStringValue(row.getCell(colNum++)));
                dto.setManufacturer(getCellStringValue(row.getCell(colNum++)));
                dto.setSeries(getCellStringValue(row.getCell(colNum++)));
                dto.setHashrate(getCellStringValue(row.getCell(colNum++)));
                dto.setAlgorithm(getCellStringValue(row.getCell(colNum++)));
                dto.setPowerConsumption(getCellStringValue(row.getCell(colNum++)));
                dto.setCoins(getCellStringValue(row.getCell(colNum++)));
                dto.setPowerSource(getCellStringValue(row.getCell(colNum++)));
                dto.setCooling(getCellStringValue(row.getCell(colNum++)));
                dto.setOperatingTemperature(getCellStringValue(row.getCell(colNum++)));
                dto.setDimensions(getCellStringValue(row.getCell(colNum++)));
                dto.setNoiseLevel(getCellStringValue(row.getCell(colNum++)));
                dto.setDescription(getCellStringValue(row.getCell(colNum++)));
                dto.setFeatures(getCellStringValue(row.getCell(colNum++)));
                dto.setPlacementInfo(getCellStringValue(row.getCell(colNum++)));
                dto.setProducerInfo(getCellStringValue(row.getCell(colNum++)));
                
                // Даты (опционально)
                String createdAtStr = getCellStringValue(row.getCell(colNum++));
                String updatedAtStr = getCellStringValue(row.getCell(colNum++));
                
                if (createdAtStr != null && !createdAtStr.isEmpty()) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        dto.setCreatedAt(LocalDateTime.parse(createdAtStr, formatter));
                    } catch (Exception e) {
                        log.debug("Не удалось распарсить дату создания: {}", createdAtStr);
                    }
                }
                
                if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        dto.setUpdatedAt(LocalDateTime.parse(updatedAtStr, formatter));
                    } catch (Exception e) {
                        log.debug("Не удалось распарсить дату обновления: {}", updatedAtStr);
                    }
                }
                
                dtos.add(dto);
                
            } catch (Exception e) {
                log.error("Ошибка при обработке строки {}: {}", rowNum + 1, e.getMessage(), e);
                // Продолжаем обработку остальных строк
            }
        }
        
        workbook.close();
        
        log.info("Импортировано {} записей из Excel", dtos.size());
        return dtos;
    }
    
    /**
     * Получает строковое значение из ячейки
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Преобразуем число в строку без десятичных знаков, если это целое число
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * Создает стиль для заголовков
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Создает стиль для дат
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
}

