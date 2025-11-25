package com.miners.shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Сервис для получения данных о криптовалютах из внешних API
 * - Курс криптовалюты
 * - Сложность сети
 * - Комиссия пула (средние значения)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoDataService {
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public CryptoDataService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Получить данные для калькулятора доходности
     * Использует Spring Cache для кэширования (TTL 5 минут)
     */
    @Cacheable(value = "cryptoData", key = "#crypto")
    public CryptoCalculatorData getCalculatorData(String crypto) {
        try {
            log.info("Получение данных для {} (не из кэша)", crypto);
            
            CryptoCalculatorData data = new CryptoCalculatorData();
            
            // Получаем курс криптовалюты
            data.setPrice(getCryptoPrice(crypto));
            
            // Получаем сложность сети
            data.setDifficulty(getNetworkDifficulty(crypto));
            
            // Получаем среднюю комиссию пула
            data.setPoolFee(getAveragePoolFee(crypto));
            
            log.info("Получены данные для {}: цена={}, сложность={}, комиссия={}", 
                    crypto, data.getPrice(), data.getDifficulty(), data.getPoolFee());
            
            return data;
        } catch (Exception e) {
            log.error("Ошибка при получении данных для {}: {}", crypto, e.getMessage(), e);
            // Возвращаем значения по умолчанию
            return getDefaultData(crypto);
        }
    }
    
    /**
     * Получить курс криптовалюты в рублях через CoinGecko API
     */
    private BigDecimal getCryptoPrice(String crypto) {
        try {
            String coinId = getCoinGeckoId(crypto);
            if (coinId == null) {
                log.warn("Неизвестная криптовалюта: {}", crypto);
                return getDefaultPrice(crypto);
            }
            
            // CoinGecko API для получения курса в рублях
            String url = String.format("https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=rub", coinId);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                JsonNode priceNode = json.get(coinId).get("rub");
                if (priceNode != null) {
                    return priceNode.decimalValue();
                }
            }
            
            log.warn("Не удалось получить курс для {} из CoinGecko", crypto);
            return getDefaultPrice(crypto);
        } catch (Exception e) {
            log.error("Ошибка при получении курса для {}: {}", crypto, e.getMessage());
            return getDefaultPrice(crypto);
        }
    }
    
    /**
     * Получить сложность сети
     * Сложность возвращается как есть (без конвертации), так как она используется в формуле
     * для расчета хешрейта сети: network_hashrate = difficulty * 2^32 / block_time
     */
    private BigDecimal getNetworkDifficulty(String crypto) {
        try {
            if ("BTC".equals(crypto)) {
                // Blockchain.com API для Bitcoin - возвращает сложность напрямую
                String url = "https://blockchain.info/q/getdifficulty";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    // Blockchain.info возвращает сложность напрямую (например, 95,000,000,000,000)
                    String responseBody = response.body().trim();
                    double difficulty = Double.parseDouble(responseBody);
                    log.info("Получена сложность Bitcoin из blockchain.info: {} (raw), {} T (normalized)", 
                            difficulty, difficulty / 1e12);
                    // Конвертируем в T (терахеши) для удобства отображения
                    // Сложность ~95T означает 95 триллионов
                    return BigDecimal.valueOf(difficulty / 1e12);
                }
            } else if ("LTC".equals(crypto)) {
                // Litecoin API - BlockCypher возвращает сложность
                String url = "https://api.blockcypher.com/v1/ltc/main";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode json = objectMapper.readTree(response.body());
                    JsonNode difficultyNode = json.get("difficulty");
                    if (difficultyNode != null) {
                        // BlockCypher возвращает сложность напрямую
                        double difficulty = difficultyNode.asDouble();
                        // Конвертируем в T для удобства
                        return BigDecimal.valueOf(difficulty / 1e12);
                    }
                }
            } else if ("DOGE".equals(crypto)) {
                // Dogecoin API - BlockCypher возвращает сложность
                String url = "https://api.blockcypher.com/v1/doge/main";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode json = objectMapper.readTree(response.body());
                    JsonNode difficultyNode = json.get("difficulty");
                    if (difficultyNode != null) {
                        // BlockCypher возвращает сложность напрямую
                        double difficulty = difficultyNode.asDouble();
                        // Конвертируем в T для удобства
                        return BigDecimal.valueOf(difficulty / 1e12);
                    }
                }
            }
            
            log.warn("Не удалось получить сложность для {}", crypto);
            return getDefaultDifficulty(crypto);
        } catch (Exception e) {
            log.error("Ошибка при получении сложности для {}: {}", crypto, e.getMessage());
            return getDefaultDifficulty(crypto);
        }
    }
    
    /**
     * Получить среднюю комиссию пула
     * Используем средние значения популярных пулов
     */
    private BigDecimal getAveragePoolFee(String crypto) {
        // Средние комиссии популярных пулов
        Map<String, BigDecimal> defaultFees = Map.of(
            "BTC", BigDecimal.valueOf(1.0),  // F2Pool, Antpool, ViaBTC обычно 1-2%
            "LTC", BigDecimal.valueOf(1.0), // LitecoinPool, F2Pool обычно 1-2%
            "DOGE", BigDecimal.valueOf(1.0) // F2Pool, Antpool обычно 1-2%
        );
        
        return defaultFees.getOrDefault(crypto, BigDecimal.valueOf(1.0));
    }
    
    /**
     * Получить ID криптовалюты для CoinGecko API
     */
    private String getCoinGeckoId(String crypto) {
        return switch (crypto) {
            case "BTC" -> "bitcoin";
            case "LTC" -> "litecoin";
            case "DOGE" -> "dogecoin";
            default -> null;
        };
    }
    
    /**
     * Значения по умолчанию
     */
    private CryptoCalculatorData getDefaultData(String crypto) {
        CryptoCalculatorData data = new CryptoCalculatorData();
        data.setPrice(getDefaultPrice(crypto));
        data.setDifficulty(getDefaultDifficulty(crypto));
        data.setPoolFee(getAveragePoolFee(crypto));
        return data;
    }
    
    private BigDecimal getDefaultPrice(String crypto) {
        return switch (crypto) {
            case "BTC" -> BigDecimal.valueOf(6500000);
            case "LTC" -> BigDecimal.valueOf(8500);
            case "DOGE" -> BigDecimal.valueOf(8.5);
            default -> BigDecimal.ZERO;
        };
    }
    
    private BigDecimal getDefaultDifficulty(String crypto) {
        return switch (crypto) {
            case "BTC" -> BigDecimal.valueOf(95.0);
            case "LTC" -> BigDecimal.valueOf(25.0);
            case "DOGE" -> BigDecimal.valueOf(500.0);
            default -> BigDecimal.ZERO;
        };
    }
    
    /**
     * DTO для данных калькулятора
     */
    public static class CryptoCalculatorData {
        private BigDecimal price;
        private BigDecimal difficulty;
        private BigDecimal poolFee;
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        public BigDecimal getDifficulty() {
            return difficulty;
        }
        
        public void setDifficulty(BigDecimal difficulty) {
            this.difficulty = difficulty;
        }
        
        public BigDecimal getPoolFee() {
            return poolFee;
        }
        
        public void setPoolFee(BigDecimal poolFee) {
            this.poolFee = poolFee;
        }
    }
    
}

