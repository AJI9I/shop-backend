package com.miners.shop.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для парсинга данных майнера из строковых полей MinerDetail
 */
@Slf4j
public class MinerDataParser {
    
    /**
     * Парсит хешрейт из строки (например: "234 TH/s", "104T", "860 kSol/s")
     * Возвращает значение в TH/s (для SHA-256) или kSol/s (для Scrypt)
     */
    public static Double parseHashrate(String hashrateStr) {
        if (hashrateStr == null || hashrateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Паттерн для поиска числа и единицы измерения
            // Поддерживает: TH/s, TH, kSol/s, MH/s и т.д.
            Pattern pattern = Pattern.compile("([\\d.,]+)\\s*([KMGTkmgt]?[A-Za-z]+)?/?(s|sec)?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(hashrateStr.trim());
            
            if (matcher.find()) {
                String numberStr = matcher.group(1).replace(",", ".");
                double number = Double.parseDouble(numberStr);
                String unit = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
                
                // Конвертируем в TH/s или kSol/s
                if (unit.contains("KSOL") || unit.contains("KS")) {
                    // Для Scrypt майнеров (Litecoin, Dogecoin)
                    return number; // Уже в kSol/s
                } else if (unit.contains("TH") || unit.contains("T")) {
                    return number; // Уже в TH/s
                } else if (unit.contains("GH") || unit.contains("G")) {
                    return number / 1000.0; // GH/s -> TH/s
                } else if (unit.contains("MH") || unit.contains("M")) {
                    return number / 1000000.0; // MH/s -> TH/s
                } else if (unit.contains("EH") || unit.contains("E")) {
                    return number * 1000.0; // EH/s -> TH/s
                } else {
                    // Если единица не указана, предполагаем TH/s
                    return number;
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось распарсить хешрейт из строки: {}", hashrateStr, e);
        }
        
        return null;
    }
    
    /**
     * Парсит энергопотребление из строки (например: "3750 Вт/ч", "3250W", "11180 Вт")
     * Возвращает значение в ваттах
     */
    public static Integer parsePowerConsumption(String powerStr) {
        if (powerStr == null || powerStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Паттерн для поиска числа и единицы измерения
            Pattern pattern = Pattern.compile("([\\d.,]+)\\s*([KWkw]+)?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(powerStr.trim());
            
            if (matcher.find()) {
                String numberStr = matcher.group(1).replace(",", ".");
                double number = Double.parseDouble(numberStr);
                String unit = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
                
                // Конвертируем в ватты
                if (unit.contains("KW") || unit.contains("КВТ")) {
                    return (int) (number * 1000); // кВт -> Вт
                } else {
                    // Предполагаем, что уже в ваттах
                    return (int) number;
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось распарсить энергопотребление из строки: {}", powerStr, e);
        }
        
        return null;
    }
}

