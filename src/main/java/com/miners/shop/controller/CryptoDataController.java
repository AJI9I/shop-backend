package com.miners.shop.controller;

import com.miners.shop.service.CryptoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST контроллер для предоставления данных о криптовалютах для калькулятора доходности
 */
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
@Slf4j
public class CryptoDataController {
    
    private final CryptoDataService cryptoDataService;
    
    /**
     * Получить данные для калькулятора доходности
     * GET /api/crypto/calculator-data?crypto=BTC
     */
    @GetMapping(value = "/calculator-data", produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> getCalculatorData(@RequestParam(defaultValue = "BTC") String crypto) {
        log.info("Запрос данных калькулятора для криптовалюты: {}", crypto);
        
        try {
            CryptoDataService.CryptoCalculatorData data = cryptoDataService.getCalculatorData(crypto);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "crypto", crypto,
                "price", data.getPrice(),
                "difficulty", data.getDifficulty(),
                "poolFee", data.getPoolFee()
            ));
        } catch (Exception e) {
            log.error("Ошибка при получении данных для {}: {}", crypto, e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", "Не удалось получить данные",
                "crypto", crypto
            ));
        }
    }
}

