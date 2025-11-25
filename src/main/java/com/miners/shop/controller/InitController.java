package com.miners.shop.controller;

import com.miners.shop.entity.MinerDetail;
import com.miners.shop.service.MinerDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер для инициализации данных
 */
@RestController
@RequestMapping("/api/init")
@RequiredArgsConstructor
@Slf4j
public class InitController {
    
    private final MinerDetailService minerDetailService;
    
    /**
     * API endpoint для анализа данных MinerDetail по ID
     * Возвращает текущие данные и список отсутствующих полей
     */
    @GetMapping("/miner-detail/{id}/analyze")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> analyzeMinerDetail(@PathVariable Long id) {
        try {
            Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(id);
            if (minerDetailOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "MinerDetail с ID=" + id + " не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            
            // Анализируем какие поля заполнены, а какие нет
            Map<String, Object> currentData = new HashMap<>();
            currentData.put("id", minerDetail.getId());
            currentData.put("standardName", minerDetail.getStandardName());
            currentData.put("manufacturer", minerDetail.getManufacturer());
            currentData.put("series", minerDetail.getSeries());
            currentData.put("hashrate", minerDetail.getHashrate());
            currentData.put("algorithm", minerDetail.getAlgorithm());
            currentData.put("powerConsumption", minerDetail.getPowerConsumption());
            currentData.put("coins", minerDetail.getCoins());
            currentData.put("powerSource", minerDetail.getPowerSource());
            currentData.put("cooling", minerDetail.getCooling());
            currentData.put("operatingTemperature", minerDetail.getOperatingTemperature());
            currentData.put("dimensions", minerDetail.getDimensions());
            currentData.put("noiseLevel", minerDetail.getNoiseLevel());
            currentData.put("description", minerDetail.getDescription());
            currentData.put("features", minerDetail.getFeatures());
            currentData.put("placementInfo", minerDetail.getPlacementInfo());
            currentData.put("producerInfo", minerDetail.getProducerInfo());
            currentData.put("createdAt", minerDetail.getCreatedAt());
            currentData.put("updatedAt", minerDetail.getUpdatedAt());
            
            // Определяем отсутствующие поля
            List<String> missingFields = new java.util.ArrayList<>();
            if (minerDetail.getHashrate() == null || minerDetail.getHashrate().trim().isEmpty()) {
                missingFields.add("hashrate");
            }
            if (minerDetail.getAlgorithm() == null || minerDetail.getAlgorithm().trim().isEmpty()) {
                missingFields.add("algorithm");
            }
            if (minerDetail.getPowerConsumption() == null || minerDetail.getPowerConsumption().trim().isEmpty()) {
                missingFields.add("powerConsumption");
            }
            if (minerDetail.getCooling() == null || minerDetail.getCooling().trim().isEmpty()) {
                missingFields.add("cooling");
            }
            if (minerDetail.getOperatingTemperature() == null || minerDetail.getOperatingTemperature().trim().isEmpty()) {
                missingFields.add("operatingTemperature");
            }
            if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
                missingFields.add("dimensions");
            }
            if (minerDetail.getNoiseLevel() == null || minerDetail.getNoiseLevel().trim().isEmpty()) {
                missingFields.add("noiseLevel");
            }
            if (minerDetail.getCoins() == null || minerDetail.getCoins().trim().isEmpty()) {
                missingFields.add("coins");
            }
            if (minerDetail.getPowerSource() == null || minerDetail.getPowerSource().trim().isEmpty()) {
                missingFields.add("powerSource");
            }
            if (minerDetail.getDescription() == null || minerDetail.getDescription().trim().isEmpty()) {
                missingFields.add("description");
            }
            if (minerDetail.getFeatures() == null || minerDetail.getFeatures().trim().isEmpty()) {
                missingFields.add("features");
            }
            if (minerDetail.getPlacementInfo() == null || minerDetail.getPlacementInfo().trim().isEmpty()) {
                missingFields.add("placementInfo");
            }
            if (minerDetail.getProducerInfo() == null || minerDetail.getProducerInfo().trim().isEmpty()) {
                missingFields.add("producerInfo");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("currentData", currentData);
            response.put("missingFields", missingFields);
            response.put("missingCount", missingFields.size());
            response.put("filledCount", 17 - missingFields.size()); // Всего 17 полей (кроме id, createdAt, updatedAt)
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при анализе MinerDetail ID={}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при анализе: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API endpoint для обновления данных MinerDetail по ID из источника
     * Принимает ID и обновляет запись на основе изученной информации со страницы
     */
    @PostMapping("/miner-detail/{id}/update-from-source")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateMinerDetailFromSource(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> data) {
        try {
            Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(id);
            if (minerDetailOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "MinerDetail с ID=" + id + " не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            log.info("Обновление MinerDetail ID={} ({}) из источника", id, minerDetail.getStandardName());
            
            // Обновляем поля из переданных данных
            if (data != null) {
                if (data.containsKey("standardName") && data.get("standardName") != null) {
                    minerDetail.setStandardName(data.get("standardName").toString());
                }
                if (data.containsKey("manufacturer") && data.get("manufacturer") != null) {
                    minerDetail.setManufacturer(data.get("manufacturer").toString());
                }
                if (data.containsKey("series") && data.get("series") != null) {
                    minerDetail.setSeries(data.get("series").toString());
                }
                if (data.containsKey("hashrate") && data.get("hashrate") != null) {
                    minerDetail.setHashrate(data.get("hashrate").toString());
                }
                if (data.containsKey("algorithm") && data.get("algorithm") != null) {
                    minerDetail.setAlgorithm(data.get("algorithm").toString());
                }
                if (data.containsKey("powerConsumption") && data.get("powerConsumption") != null) {
                    minerDetail.setPowerConsumption(data.get("powerConsumption").toString());
                }
                if (data.containsKey("coins") && data.get("coins") != null) {
                    minerDetail.setCoins(data.get("coins").toString());
                }
                if (data.containsKey("powerSource") && data.get("powerSource") != null) {
                    minerDetail.setPowerSource(data.get("powerSource").toString());
                }
                if (data.containsKey("cooling") && data.get("cooling") != null) {
                    minerDetail.setCooling(data.get("cooling").toString());
                }
                if (data.containsKey("operatingTemperature") && data.get("operatingTemperature") != null) {
                    minerDetail.setOperatingTemperature(data.get("operatingTemperature").toString());
                }
                if (data.containsKey("dimensions") && data.get("dimensions") != null) {
                    minerDetail.setDimensions(data.get("dimensions").toString());
                }
                if (data.containsKey("noiseLevel") && data.get("noiseLevel") != null) {
                    minerDetail.setNoiseLevel(data.get("noiseLevel").toString());
                }
                if (data.containsKey("description") && data.get("description") != null) {
                    minerDetail.setDescription(data.get("description").toString());
                }
                if (data.containsKey("features") && data.get("features") != null) {
                    minerDetail.setFeatures(data.get("features").toString());
                }
                if (data.containsKey("placementInfo") && data.get("placementInfo") != null) {
                    minerDetail.setPlacementInfo(data.get("placementInfo").toString());
                }
                if (data.containsKey("producerInfo") && data.get("producerInfo") != null) {
                    minerDetail.setProducerInfo(data.get("producerInfo").toString());
                }
            }
            
            MinerDetail updated = minerDetailService.updateMinerDetail(minerDetail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MinerDetail ID=" + id + " успешно обновлен");
            response.put("standardName", updated.getStandardName());
            response.put("hashrate", updated.getHashrate());
            response.put("powerConsumption", updated.getPowerConsumption());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обновлении MinerDetail ID={}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при обновлении: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API endpoint для обновления данных MinerDetail ID = 1 (Antminer S19j PRO)
     * Данные взяты с официальной страницы promminer.ru
     */
    @PostMapping("/miner-detail/1/update")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateMinerDetail1() {
        try {
            Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(1L);
            if (minerDetailOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "MinerDetail с ID=1 не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            log.info("Обновление MinerDetail ID=1 ({}) на основе данных с promminer.ru", minerDetail.getStandardName());
            
            // Технические характеристики с promminer.ru
            minerDetail.setStandardName("Antminer S19j PRO");
            minerDetail.setManufacturer("Bitmain");
            minerDetail.setSeries("S19j");
            minerDetail.setHashrate("104 TH/s");
            minerDetail.setAlgorithm("SHA-256");
            minerDetail.setPowerConsumption("3068W");
            minerDetail.setCoins("BTC, BCH");
            minerDetail.setPowerSource("Интегрированный");
            minerDetail.setCooling("Воздушное");
            minerDetail.setOperatingTemperature("от 0 до 40°C");
            minerDetail.setDimensions("400 x 195.5 x 290 мм");
            minerDetail.setNoiseLevel("75 дБ");
            
            // Описание на основе информации с promminer.ru
            minerDetail.setDescription("Antminer S19j PRO от компании Bitmain — это высокопроизводительный ASIC-майнер, предназначенный для добычи криптовалют на алгоритме SHA-256, включая Bitcoin (BTC) и Bitcoin Cash (BCH). Данная модель обладает хэшрейтом 104 TH/s при энергопотреблении 3068 Вт (±5%), что обеспечивает оптимальное сочетание эффективности и мощности. Устройство поддерживает интегрированный блок питания и воздушное охлаждение с четырьмя вентиляторами, что упрощает его эксплуатацию и обслуживание. S19j PRO относится к серии S19j и является улучшенной версией предыдущих моделей. Новая модель немного длиннее и тяжелее предыдущей версии из-за увеличенного размера хэш-платы с большим количеством чипов. Блок питания остался такой же, как в S19 PRO. Чипы в этой версии потребляют чуть меньше электроэнергии, что уменьшило тепловыделение. Плата охлаждается с одной стороны двумя металлическими пластинами радиатора, а с другой группой более мелких радиаторов, припаянных к плате — это необходимо для более эффективного теплоотвода с двух сторон платы. Улучшена схема расположения комплектующих для более эффективного теплоотведения и отказоустойчивости устройства. Устройство предназначено для использования как в промышленных майнинг-фермах, так и в условиях домашних или малых майнинг-провайдеров с учётом специальной вентиляции и температурного контроля.");
            
            // Особенности на основе информации с promminer.ru
            minerDetail.setFeatures("Высокая производительность с хэшрейтом 104 TH/s; Оптимизированное энергопотребление 3068 Вт (±5%) благодаря улучшенной архитектуре чипов; Надёжное интегрированное питание упрощает установку; Воздушное охлаждение с четырьмя вентиляторами (два мощных на вдув, два менее мощных на выдув) для эффективного теплоотвода; Улучшенная система охлаждения платы с металлическими пластинами радиатора с одной стороны и группой мелких радиаторов с другой стороны для двустороннего теплоотвода; Компактный и эргономичный дизайн облегчает интеграцию в майнинг-фермы; Высокая стабильность работы под нагрузкой; Оптимизированная схема расположения комплектующих для эффективного теплоотведения и отказоустойчивости; Поддержка управления и мониторинга через веб-интерфейс.");
            
            // Рекомендации по размещению
            minerDetail.setPlacementInfo("Для максимальной эффективности работы Antminer S19j PRO рекомендуется размещать в просторных, хорошо проветриваемых помещениях с контролируемой температурой от 0 до 40 градусов Цельсия. Во время работы устройство выделяет достаточно много тепла, поэтому необходимо обеспечить хорошую вентиляцию. Необходимо обеспечить снижение влажности и пыли для предотвращения перегрева и выхода из строя компонентов. Для крупномасштабных майнинг-ферм применяются специальные системы воздушного или кондиционированного охлаждения. Устройство требует стабильного электроснабжения с правильным заземлением и защитой от перепадов напряжения. Уровень шума до 75 дБ требует звукоизоляции при установке в жилых помещениях или выделения отдельного помещения для размещения оборудования.");
            
            // Информация о производителе
            minerDetail.setProducerInfo("Bitmain — ведущий мировой производитель ASIC-майнеров, известный своими инновациями и высокой надежностью оборудования. Компания славится передовыми технологиями в области вычислительной техники и надежностью своего оборудования. Модель S19j PRO является частью линейки S19, которая зарекомендовала себя как индустриальный стандарт в сфере майнинга SHA-256. Благодаря постоянному совершенствованию технологий Bitmain обеспечивает оптимальную производительность при снижении энергозатрат. S19j PRO ориентирован на пользователей, которые ценят стабильность, энергоэффективность и поддержку официального софта для управления устройствами. Эта модель стала одной из самых востребованных среди коммерческих майнеров и любителей благодаря оптимальному соотношению производительности, энергоэффективности и надежности.");
            
            MinerDetail updated = minerDetailService.updateMinerDetail(minerDetail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MinerDetail ID=1 успешно обновлен на основе данных с promminer.ru");
            response.put("standardName", updated.getStandardName());
            response.put("manufacturer", updated.getManufacturer());
            response.put("hashrate", updated.getHashrate());
            response.put("algorithm", updated.getAlgorithm());
            response.put("powerConsumption", updated.getPowerConsumption());
            response.put("dimensions", updated.getDimensions());
            
            log.info("MinerDetail ID=1 обновлен: hashrate={}, powerConsumption={}, dimensions={}", 
                    updated.getHashrate(), updated.getPowerConsumption(), updated.getDimensions());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обновлении MinerDetail ID=1: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при обновлении: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API endpoint для обновления данных MinerDetail ID = 8 (Whatsminer M30S++)
     */
    @PostMapping("/miner-detail/8/update")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateMinerDetail8() {
        try {
            Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(8L);
            if (minerDetailOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "MinerDetail с ID=8 не найден");
                return ResponseEntity.status(404).body(error);
            }
            
            MinerDetail minerDetail = minerDetailOpt.get();
            
            log.info("Обновление MinerDetail ID=8 ({})", minerDetail.getStandardName());
            
            // ИСПРАВЛЕНИЕ: Whatsminer Z15 PRO не существует!
            // Это должен быть Antminer Z15 Pro от Bitmain (не MicroBT!)
            String currentName = minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "";
            String currentManufacturer = minerDetail.getManufacturer() != null ? minerDetail.getManufacturer() : "";
            
            // Если название содержит "Whatsminer" и "Z15" (включая PRO или без), исправляем на Antminer Z15 Pro от Bitmain
            if (currentName.contains("Whatsminer") && currentName.contains("Z15")) {
                // Исправляем неправильное название на правильное
                minerDetail.setStandardName("Antminer Z15 Pro");
                minerDetail.setManufacturer("Bitmain");
                minerDetail.setSeries("Z15");
                log.warn("  → ИСПРАВЛЕНО: '{}' не существует! Изменено на: Antminer Z15 Pro (Bitmain)", currentName);
            } else if (currentName.contains("Z15") && currentManufacturer.equals("MicroBT")) {
                // Если Z15 от MicroBT - это ошибка, должна быть от Bitmain
                minerDetail.setStandardName("Antminer Z15 Pro");
                minerDetail.setManufacturer("Bitmain");
                minerDetail.setSeries("Z15");
                log.warn("  → ИСПРАВЛЕНО: Z15 от MicroBT не существует! Изменено на: Antminer Z15 Pro (Bitmain)");
            }
            
            // Определяем модель по standardName и заполняем соответствующие данные
            String model = minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "";
            
            // Обновляем model после исправления
            model = minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "";
            
            // Заполняем данные для Z15 майнеров (Equihash)
            if (model.contains("Z15")) {
                // Определяем правильные характеристики в зависимости от модели
                if (model.contains("Antminer")) {
                    // Antminer Z15 Pro от Bitmain (правильные характеристики)
                    if (minerDetail.getHashrate() == null || minerDetail.getHashrate().trim().isEmpty()) {
                        minerDetail.setHashrate("840 kSol/s");
                        log.info("  → Добавлен hashrate: 840 kSol/s");
                    }
                    
                    if (minerDetail.getAlgorithm() == null || minerDetail.getAlgorithm().trim().isEmpty()) {
                        minerDetail.setAlgorithm("Equihash");
                        log.info("  → Добавлен algorithm: Equihash");
                    }
                    
                    // Исправляем потребляемую мощность: для Antminer Z15 Pro это 2780W (не 1518W!)
                    String currentPower = minerDetail.getPowerConsumption() != null ? minerDetail.getPowerConsumption().trim() : "";
                    if (currentPower.isEmpty() || currentPower.equals("1518W") || !currentPower.equals("2780W")) {
                        minerDetail.setPowerConsumption("2780W");
                        log.info("  → Добавлен/исправлен powerConsumption: 2780W (официальная спецификация Antminer Z15 Pro от Bitmain, было: {})", currentPower);
                    }
                    
                    if (minerDetail.getCoins() == null || minerDetail.getCoins().trim().isEmpty()) {
                        minerDetail.setCoins("ZEC, ZEN");
                        log.info("  → Добавлен coins: ZEC, ZEN");
                    }
                    
                    if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
                        minerDetail.setDimensions("428 x 195 x 290 мм");
                        log.info("  → Добавлены dimensions: 428 x 195 x 290 мм (официальные размеры Antminer Z15 Pro)");
                    }
                } else {
                    // Whatsminer Z15 от MicroBT (если такая модель существует)
                    if (minerDetail.getHashrate() == null || minerDetail.getHashrate().trim().isEmpty()) {
                        minerDetail.setHashrate("840 kSol/s");
                        log.info("  → Добавлен hashrate: 840 kSol/s");
                    }
                    
                    if (minerDetail.getAlgorithm() == null || minerDetail.getAlgorithm().trim().isEmpty()) {
                        minerDetail.setAlgorithm("Equihash");
                        log.info("  → Добавлен algorithm: Equihash");
                    }
                    
                    if (minerDetail.getPowerConsumption() == null || minerDetail.getPowerConsumption().trim().isEmpty()) {
                        minerDetail.setPowerConsumption("1518W");
                        log.info("  → Добавлен powerConsumption: 1518W");
                    }
                    
                    if (minerDetail.getCoins() == null || minerDetail.getCoins().trim().isEmpty()) {
                        minerDetail.setCoins("ZEC, ZEN");
                        log.info("  → Добавлен coins: ZEC, ZEN");
                    }
                }
            } else {
                // По умолчанию заполняем для SHA-256 майнеров (M30S++ и др.)
                if (minerDetail.getHashrate() == null || minerDetail.getHashrate().trim().isEmpty()) {
                    minerDetail.setHashrate("112 TH/s");
                    log.info("  → Добавлен hashrate: 112 TH/s");
                }
                
                if (minerDetail.getAlgorithm() == null || minerDetail.getAlgorithm().trim().isEmpty()) {
                    minerDetail.setAlgorithm("SHA-256");
                    log.info("  → Добавлен algorithm: SHA-256");
                }
                
                if (minerDetail.getPowerConsumption() == null || minerDetail.getPowerConsumption().trim().isEmpty()) {
                    minerDetail.setPowerConsumption("3472W");
                    log.info("  → Добавлен powerConsumption: 3472W");
                }
                
                if (minerDetail.getCoins() == null || minerDetail.getCoins().trim().isEmpty()) {
                    minerDetail.setCoins("BTC, BCH, BSV");
                    log.info("  → Добавлен coins: BTC, BCH, BSV");
                }
            }
            
            if (minerDetail.getCooling() == null || minerDetail.getCooling().trim().isEmpty()) {
                minerDetail.setCooling("Воздушное");
                log.info("  → Добавлен cooling: Воздушное");
            }
            
            if (minerDetail.getOperatingTemperature() == null || minerDetail.getOperatingTemperature().trim().isEmpty()) {
                minerDetail.setOperatingTemperature("от 0 до 40°C");
                log.info("  → Добавлен operatingTemperature: от 0 до 40°C");
            }
            
            if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
                minerDetail.setDimensions("370 x 195.5 x 290 мм");
                log.info("  → Добавлен dimensions: 370 x 195.5 x 290 мм");
            }
            
            if (minerDetail.getNoiseLevel() == null || minerDetail.getNoiseLevel().trim().isEmpty()) {
                minerDetail.setNoiseLevel("~75 дБ");
                log.info("  → Добавлен noiseLevel: ~75 дБ");
            }
            
            // Общие поля для всех моделей
            if (minerDetail.getPowerSource() == null || minerDetail.getPowerSource().trim().isEmpty()) {
                minerDetail.setPowerSource("Интегрированный");
                log.info("  → Добавлен powerSource: Интегрированный");
            }
            
            if (minerDetail.getCooling() == null || minerDetail.getCooling().trim().isEmpty()) {
                minerDetail.setCooling("Воздушное");
                log.info("  → Добавлен cooling: Воздушное");
            }
            
            if (minerDetail.getOperatingTemperature() == null || minerDetail.getOperatingTemperature().trim().isEmpty()) {
                minerDetail.setOperatingTemperature("от 0 до 40°C");
                log.info("  → Добавлен operatingTemperature: от 0 до 40°C");
            }
            
            if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
                minerDetail.setDimensions("370 x 195.5 x 290 мм");
                log.info("  → Добавлен dimensions: 370 x 195.5 x 290 мм");
            }
            
            if (minerDetail.getNoiseLevel() == null || minerDetail.getNoiseLevel().trim().isEmpty()) {
                minerDetail.setNoiseLevel("~75 дБ");
                log.info("  → Добавлен noiseLevel: ~75 дБ");
            }
            
            // Описание в зависимости от модели
            // ПРИНУДИТЕЛЬНО обновляем описание для Antminer Z15 Pro (исправляем ошибку Whatsminer)
            String currentDescription = minerDetail.getDescription() != null ? minerDetail.getDescription() : "";
            if (model.contains("Antminer") && model.contains("Z15")) {
                // Для Antminer Z15 Pro - всегда обновляем описание
                minerDetail.setDescription("Antminer Z15 Pro — это высокопроизводительный ASIC-майнер от Bitmain для добычи криптовалют на алгоритме Equihash. Модель предназначена для майнинга Zcash (ZEC) и Horizen (ZEN) и отличается высокой производительностью 840 kSol/s при потребляемой мощности 2780W.");
                log.info("  → Обновлено description для Antminer Z15 Pro (Bitmain)");
            } else if (currentDescription.isEmpty() || currentDescription.contains("Whatsminer") || (currentDescription.contains("MicroBT") && model.contains("Z15"))) {
                if (model.contains("Z15")) {
                    minerDetail.setDescription("ASIC-майнер для добычи криптовалют на алгоритме Equihash. Модель предназначена для майнинга Zcash (ZEC) и Horizen (ZEN).");
                    log.info("  → Обновлено description для Z15");
                } else {
                    minerDetail.setDescription("Высокопроизводительный ASIC-майнер для добычи криптовалют. Модель отличается высокой энергоэффективностью и стабильной работой.");
                    log.info("  → Обновлено description");
                }
            }
            
            // Особенности (Features)
            String currentFeatures = minerDetail.getFeatures() != null ? minerDetail.getFeatures() : "";
            if (model.contains("Antminer") && model.contains("Z15")) {
                // Для Antminer Z15 Pro - всегда обновляем features
                minerDetail.setFeatures("Высокая производительность 840 kSol/s, энергоэффективность 3,31 Дж/кСол, встроенный блок питания, автоматическая настройка частоты чипов, защита от перегрева и перегрузки.");
                log.info("  → Обновлено features для Antminer Z15 Pro (Bitmain)");
            } else if (currentFeatures.isEmpty() || currentFeatures.contains("Whatsminer") || currentFeatures.contains("MicroBT") || currentFeatures.contains("1.8 Дж/МСол")) {
                if (model.contains("Z15")) {
                    minerDetail.setFeatures("Высокая производительность 840 kSol/s, встроенный блок питания, автоматическая настройка частоты чипов, защита от перегрева и перегрузки.");
                    log.info("  → Обновлено features для Z15");
                } else {
                    minerDetail.setFeatures("Высокая производительность, энергоэффективность, встроенный блок питания, автоматическая настройка частоты чипов, защита от перегрева и перегрузки.");
                    log.info("  → Обновлено features");
                }
            }
            
            // Информация о размещении
            if (minerDetail.getPlacementInfo() == null || minerDetail.getPlacementInfo().trim().isEmpty()) {
                minerDetail.setPlacementInfo("Требуется хорошо вентилируемое помещение с температурой от 0 до 40°C. Рекомендуется использование в майнинг-фермах с системой охлаждения. Уровень шума ~75 дБ, что требует звукоизоляции при установке в жилых помещениях.");
                log.info("  → Добавлен placementInfo");
            }
            
            // Информация о производителе
            String currentProducerInfo = minerDetail.getProducerInfo() != null ? minerDetail.getProducerInfo() : "";
            if (model.contains("Antminer") && model.contains("Z15")) {
                // Для Antminer Z15 Pro - всегда обновляем producerInfo
                minerDetail.setProducerInfo("Bitmain — один из ведущих производителей ASIC-майнеров. Компания известна своими высокопроизводительными решениями для добычи криптовалют. Antminer Z15 Pro является флагманской моделью серии Z15 для майнинга Equihash от Bitmain.");
                log.info("  → Обновлено producerInfo для Antminer Z15 Pro (Bitmain)");
            } else if (currentProducerInfo.isEmpty() || currentProducerInfo.contains("Whatsminer Z15 PRO") || (currentProducerInfo.contains("MicroBT") && model.contains("Z15") && minerDetail.getManufacturer() != null && minerDetail.getManufacturer().equals("Bitmain"))) {
                if (minerDetail.getManufacturer() != null && minerDetail.getManufacturer().equals("MicroBT")) {
                    minerDetail.setProducerInfo("MicroBT — один из ведущих производителей ASIC-майнеров. Компания известна своими энергоэффективными решениями для добычи криптовалют.");
                    log.info("  → Обновлено producerInfo для MicroBT");
                } else {
                    minerDetail.setProducerInfo("Ведущий производитель ASIC-майнеров для добычи криптовалют.");
                    log.info("  → Обновлено producerInfo");
                }
            }
            
            // Обновляем запись
            MinerDetail updated = minerDetailService.updateMinerDetail(minerDetail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MinerDetail ID=8 успешно обновлен");
            response.put("standardName", updated.getStandardName());
            response.put("manufacturer", updated.getManufacturer());
            response.put("hashrate", updated.getHashrate());
            response.put("algorithm", updated.getAlgorithm());
            response.put("powerConsumption", updated.getPowerConsumption());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обновлении MinerDetail ID=8: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при обновлении: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API endpoint для анализа всех MinerDetail записей
     * Возвращает статистику по заполненности полей
     */
    @GetMapping("/miner-details/analyze-all")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> analyzeAllMinerDetails() {
        try {
            List<MinerDetail> allMinerDetails = minerDetailService.getAllMinerDetails();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalCount", allMinerDetails.size());
            
            int fullyFilled = 0;
            int partiallyFilled = 0;
            int empty = 0;
            
            List<Map<String, Object>> details = new java.util.ArrayList<>();
            
            for (MinerDetail minerDetail : allMinerDetails) {
                Map<String, Object> detailInfo = new HashMap<>();
                detailInfo.put("id", minerDetail.getId());
                detailInfo.put("standardName", minerDetail.getStandardName());
                detailInfo.put("manufacturer", minerDetail.getManufacturer());
                
                // Подсчитываем заполненные поля
                int filledFields = 0;
                int totalFields = 17; // Общее количество полей (кроме id, createdAt, updatedAt)
                
                List<String> missingFields = new java.util.ArrayList<>();
                
                if (minerDetail.getHashrate() != null && !minerDetail.getHashrate().trim().isEmpty()) filledFields++;
                else missingFields.add("hashrate");
                
                if (minerDetail.getAlgorithm() != null && !minerDetail.getAlgorithm().trim().isEmpty()) filledFields++;
                else missingFields.add("algorithm");
                
                if (minerDetail.getPowerConsumption() != null && !minerDetail.getPowerConsumption().trim().isEmpty()) filledFields++;
                else missingFields.add("powerConsumption");
                
                if (minerDetail.getCooling() != null && !minerDetail.getCooling().trim().isEmpty()) filledFields++;
                else missingFields.add("cooling");
                
                if (minerDetail.getOperatingTemperature() != null && !minerDetail.getOperatingTemperature().trim().isEmpty()) filledFields++;
                else missingFields.add("operatingTemperature");
                
                if (minerDetail.getDimensions() != null && !minerDetail.getDimensions().trim().isEmpty()) filledFields++;
                else missingFields.add("dimensions");
                
                if (minerDetail.getNoiseLevel() != null && !minerDetail.getNoiseLevel().trim().isEmpty()) filledFields++;
                else missingFields.add("noiseLevel");
                
                if (minerDetail.getCoins() != null && !minerDetail.getCoins().trim().isEmpty()) filledFields++;
                else missingFields.add("coins");
                
                if (minerDetail.getPowerSource() != null && !minerDetail.getPowerSource().trim().isEmpty()) filledFields++;
                else missingFields.add("powerSource");
                
                if (minerDetail.getDescription() != null && !minerDetail.getDescription().trim().isEmpty()) filledFields++;
                else missingFields.add("description");
                
                if (minerDetail.getFeatures() != null && !minerDetail.getFeatures().trim().isEmpty()) filledFields++;
                else missingFields.add("features");
                
                if (minerDetail.getPlacementInfo() != null && !minerDetail.getPlacementInfo().trim().isEmpty()) filledFields++;
                else missingFields.add("placementInfo");
                
                if (minerDetail.getProducerInfo() != null && !minerDetail.getProducerInfo().trim().isEmpty()) filledFields++;
                else missingFields.add("producerInfo");
                
                detailInfo.put("filledFields", filledFields);
                detailInfo.put("missingFields", missingFields);
                detailInfo.put("fillPercentage", Math.round((filledFields * 100.0) / totalFields));
                
                if (filledFields == 0) empty++;
                else if (filledFields == totalFields) fullyFilled++;
                else partiallyFilled++;
                
                details.add(detailInfo);
            }
            
            statistics.put("fullyFilled", fullyFilled);
            statistics.put("partiallyFilled", partiallyFilled);
            statistics.put("empty", empty);
            
            // Вычисляем средний процент заполненности
            if (!allMinerDetails.isEmpty()) {
                double averageFill = details.stream()
                        .mapToInt(d -> (Integer) d.get("filledFields"))
                        .average()
                        .orElse(0.0);
                statistics.put("averageFillPercentage", Math.round((averageFill * 100.0) / 17)); // 17 - общее количество полей
            } else {
                statistics.put("averageFillPercentage", 0);
            }
            
            statistics.put("details", details);
            
            log.info("Анализ завершен: полностью заполнено {}, частично заполнено {}, пустых {}, средний процент заполненности {}%", 
                    fullyFilled, partiallyFilled, empty, statistics.get("averageFillPercentage"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("statistics", statistics);
            response.put("message", "Анализ завершен");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при анализе всех MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при анализе: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * API endpoint для заполнения всех MinerDetail записей недостающими данными
     * Проходит по всем записям и заполняет недостающие поля на основе стандартного названия модели
     */
    @PostMapping("/miner-details/fill-all")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> fillAllMinerDetails() {
        try {
            List<MinerDetail> allMinerDetails = minerDetailService.getAllMinerDetails();
            
            log.info("Начато заполнение данных для {} MinerDetail записей", allMinerDetails.size());
            
            int updated = 0;
            int skipped = 0;
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            
            for (MinerDetail minerDetail : allMinerDetails) {
                try {
                    Map<String, Object> result = fillMinerDetailFields(minerDetail);
                    result.put("id", minerDetail.getId());
                    result.put("standardName", minerDetail.getStandardName());
                    
                    MinerDetail updatedDetail = minerDetailService.updateMinerDetail(minerDetail);
                    result.put("success", true);
                    
                    updated++;
                    results.add(result);
                    
                    log.debug("Обновлен MinerDetail ID={} ({})", minerDetail.getId(), minerDetail.getStandardName());
                } catch (Exception e) {
                    log.error("Ошибка при обновлении MinerDetail ID={}: {}", minerDetail.getId(), e.getMessage(), e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("id", minerDetail.getId());
                    errorResult.put("standardName", minerDetail.getStandardName());
                    errorResult.put("success", false);
                    errorResult.put("error", e.getMessage());
                    results.add(errorResult);
                    skipped++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Обработано %d записей: обновлено %d, пропущено %d", allMinerDetails.size(), updated, skipped));
            response.put("totalProcessed", allMinerDetails.size());
            response.put("updated", updated);
            response.put("skipped", skipped);
            response.put("results", results);
            
            log.info("Заполнение данных завершено: обновлено {}, пропущено {}", updated, skipped);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при заполнении всех MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при заполнении: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Вспомогательный метод для заполнения полей MinerDetail на основе стандартного названия модели
     * Использует ту же логику, что и updateMinerDetail8, но для любой модели
     */
    private Map<String, Object> fillMinerDetailFields(MinerDetail minerDetail) {
        Map<String, Object> result = new HashMap<>();
        List<String> filledFields = new java.util.ArrayList<>();
        
        String model = minerDetail.getStandardName() != null ? minerDetail.getStandardName() : "";
        String manufacturer = minerDetail.getManufacturer() != null ? minerDetail.getManufacturer() : "";
        
        // ИСПРАВЛЕНИЕ: Whatsminer Z15 PRO не существует! Исправляем на Antminer Z15 Pro
        if (model.contains("Whatsminer") && model.contains("Z15")) {
            minerDetail.setStandardName("Antminer Z15 Pro");
            minerDetail.setManufacturer("Bitmain");
            minerDetail.setSeries("Z15");
            filledFields.add("standardName (исправлено)");
            filledFields.add("manufacturer (исправлено)");
            log.warn("  → ИСПРАВЛЕНО для ID={}: '{}' не существует! Изменено на: Antminer Z15 Pro (Bitmain)", minerDetail.getId(), model);
            model = "Antminer Z15 Pro";
        } else if (model.contains("Z15") && manufacturer.equals("MicroBT")) {
            minerDetail.setStandardName("Antminer Z15 Pro");
            minerDetail.setManufacturer("Bitmain");
            minerDetail.setSeries("Z15");
            filledFields.add("standardName (исправлено)");
            filledFields.add("manufacturer (исправлено)");
            log.warn("  → ИСПРАВЛЕНО для ID={}: Z15 от MicroBT не существует! Изменено на: Antminer Z15 Pro (Bitmain)", minerDetail.getId());
            model = "Antminer Z15 Pro";
        }
        
        // Заполняем данные для Z15 майнеров (Equihash)
        if (model.contains("Z15")) {
            if (model.contains("Antminer")) {
                // Antminer Z15 Pro от Bitmain
                if (minerDetail.getHashrate() == null || minerDetail.getHashrate().trim().isEmpty()) {
                    minerDetail.setHashrate("840 kSol/s");
                    filledFields.add("hashrate");
                }
                
                if (minerDetail.getAlgorithm() == null || minerDetail.getAlgorithm().trim().isEmpty()) {
                    minerDetail.setAlgorithm("Equihash");
                    filledFields.add("algorithm");
                }
                
                String currentPower = minerDetail.getPowerConsumption() != null ? minerDetail.getPowerConsumption().trim() : "";
                if (currentPower.isEmpty() || currentPower.equals("1518W") || !currentPower.equals("2780W")) {
                    minerDetail.setPowerConsumption("2780W");
                    filledFields.add("powerConsumption (исправлено на 2780W)");
                }
                
                if (minerDetail.getCoins() == null || minerDetail.getCoins().trim().isEmpty()) {
                    minerDetail.setCoins("ZEC, ZEN");
                    filledFields.add("coins");
                }
                
                if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
                    minerDetail.setDimensions("428 x 195 x 290 мм");
                    filledFields.add("dimensions");
                }
            }
        } else {
            // Для других моделей НЕ заполняем специфические поля (hashrate, algorithm, powerConsumption, coins)
            // Эти поля должны быть заполнены вручную для каждой конкретной модели
            // Заполняем только общие поля: cooling, operatingTemperature, dimensions, noiseLevel, powerSource
        }
        
        // Общие поля для всех моделей
        if (minerDetail.getCooling() == null || minerDetail.getCooling().trim().isEmpty()) {
            minerDetail.setCooling("Воздушное");
            filledFields.add("cooling");
        }
        
        if (minerDetail.getOperatingTemperature() == null || minerDetail.getOperatingTemperature().trim().isEmpty()) {
            minerDetail.setOperatingTemperature("от 0 до 40°C");
            filledFields.add("operatingTemperature");
        }
        
        if (minerDetail.getDimensions() == null || minerDetail.getDimensions().trim().isEmpty()) {
            if (!model.contains("Antminer") || !model.contains("Z15")) {
                minerDetail.setDimensions("370 x 195.5 x 290 мм");
                filledFields.add("dimensions");
            }
        }
        
        if (minerDetail.getNoiseLevel() == null || minerDetail.getNoiseLevel().trim().isEmpty()) {
            minerDetail.setNoiseLevel("~75 дБ");
            filledFields.add("noiseLevel");
        }
        
        if (minerDetail.getPowerSource() == null || minerDetail.getPowerSource().trim().isEmpty()) {
            minerDetail.setPowerSource("Интегрированный");
            filledFields.add("powerSource");
        }
        
        // Описание, features, producerInfo - обновляем только для Antminer Z15 Pro или если пустые
        String currentDescription = minerDetail.getDescription() != null ? minerDetail.getDescription() : "";
        if (model.contains("Antminer") && model.contains("Z15")) {
            minerDetail.setDescription("Antminer Z15 Pro — это высокопроизводительный ASIC-майнер от Bitmain для добычи криптовалют на алгоритме Equihash. Модель предназначена для майнинга Zcash (ZEC) и Horizen (ZEN) и отличается высокой производительностью 840 kSol/s при потребляемой мощности 2780W.");
            if (currentDescription.isEmpty() || currentDescription.contains("Whatsminer") || currentDescription.contains("MicroBT")) {
                filledFields.add("description (обновлено)");
            }
        } else if (currentDescription.isEmpty()) {
            minerDetail.setDescription("Высокопроизводительный ASIC-майнер для добычи криптовалют. Модель отличается высокой энергоэффективностью и стабильной работой.");
            filledFields.add("description");
        }
        
        String currentFeatures = minerDetail.getFeatures() != null ? minerDetail.getFeatures() : "";
        if (model.contains("Antminer") && model.contains("Z15")) {
            minerDetail.setFeatures("Высокая производительность 840 kSol/s, энергоэффективность 3,31 Дж/кСол, встроенный блок питания, автоматическая настройка частоты чипов, защита от перегрева и перегрузки.");
            if (currentFeatures.isEmpty() || currentFeatures.contains("Whatsminer") || currentFeatures.contains("1.8 Дж/МСол")) {
                filledFields.add("features (обновлено)");
            }
        } else if (currentFeatures.isEmpty()) {
            minerDetail.setFeatures("Высокая производительность, энергоэффективность, встроенный блок питания, автоматическая настройка частоты чипов, защита от перегрева и перегрузки.");
            filledFields.add("features");
        }
        
        if (minerDetail.getPlacementInfo() == null || minerDetail.getPlacementInfo().trim().isEmpty()) {
            minerDetail.setPlacementInfo("Требуется хорошо вентилируемое помещение с температурой от 0 до 40°C. Рекомендуется использование в майнинг-фермах с системой охлаждения. Уровень шума ~75 дБ, что требует звукоизоляции при установке в жилых помещениях.");
            filledFields.add("placementInfo");
        }
        
        String currentProducerInfo = minerDetail.getProducerInfo() != null ? minerDetail.getProducerInfo() : "";
        if (model.contains("Antminer") && model.contains("Z15")) {
            minerDetail.setProducerInfo("Bitmain — один из ведущих производителей ASIC-майнеров. Компания известна своими высокопроизводительными решениями для добычи криптовалют. Antminer Z15 Pro является флагманской моделью серии Z15 для майнинга Equihash от Bitmain.");
            if (currentProducerInfo.isEmpty() || currentProducerInfo.contains("Whatsminer Z15 PRO") || currentProducerInfo.contains("MicroBT")) {
                filledFields.add("producerInfo (обновлено)");
            }
        } else if (currentProducerInfo.isEmpty()) {
            if (manufacturer.equals("MicroBT")) {
                minerDetail.setProducerInfo("MicroBT — один из ведущих производителей ASIC-майнеров. Компания известна своими энергоэффективными решениями для добычи криптовалют.");
            } else {
                minerDetail.setProducerInfo("Ведущий производитель ASIC-майнеров для добычи криптовалют.");
            }
            filledFields.add("producerInfo");
        }
        
        result.put("filledFields", filledFields);
        result.put("fieldsCount", filledFields.size());
        
        return result;
    }
    
    /**
     * API endpoint для массового обновления MinerDetail записей из JSON массива
     * Принимает массив объектов с данными и обновляет соответствующие записи
     */
    @PostMapping("/miner-details/batch-update")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> batchUpdateMinerDetails(
            @RequestBody List<Map<String, Object>> minerDetailsData) {
        try {
            log.info("Начато массовое обновление MinerDetail записей: получено {} записей", minerDetailsData.size());
            
            int updated = 0;
            int skipped = 0;
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            
            for (Map<String, Object> data : minerDetailsData) {
                try {
                    Long id = null;
                    if (data.get("id") instanceof Integer) {
                        id = ((Integer) data.get("id")).longValue();
                    } else if (data.get("id") instanceof Long) {
                        id = (Long) data.get("id");
                    } else if (data.get("id") instanceof String) {
                        id = Long.parseLong((String) data.get("id"));
                    }
                    
                    if (id == null) {
                        log.warn("Пропущена запись без ID: {}", data);
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("data", data);
                        errorResult.put("success", false);
                        errorResult.put("error", "ID не указан или имеет неверный формат");
                        results.add(errorResult);
                        skipped++;
                        continue;
                    }
                    
                    Optional<MinerDetail> minerDetailOpt = minerDetailService.getMinerDetailById(id);
                    if (minerDetailOpt.isEmpty()) {
                        log.warn("MinerDetail с ID={} не найден", id);
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("id", id);
                        errorResult.put("success", false);
                        errorResult.put("error", "MinerDetail с ID=" + id + " не найден");
                        results.add(errorResult);
                        skipped++;
                        continue;
                    }
                    
                    MinerDetail minerDetail = minerDetailOpt.get();
                    
                    // Обновляем поля из данных
                    if (data.containsKey("standardName") && data.get("standardName") != null) {
                        minerDetail.setStandardName(data.get("standardName").toString());
                    }
                    if (data.containsKey("manufacturer") && data.get("manufacturer") != null) {
                        minerDetail.setManufacturer(data.get("manufacturer").toString());
                    }
                    if (data.containsKey("series") && data.get("series") != null) {
                        minerDetail.setSeries(data.get("series").toString());
                    }
                    if (data.containsKey("hashrate") && data.get("hashrate") != null) {
                        minerDetail.setHashrate(data.get("hashrate").toString());
                    }
                    if (data.containsKey("algorithm") && data.get("algorithm") != null) {
                        minerDetail.setAlgorithm(data.get("algorithm").toString());
                    }
                    if (data.containsKey("powerConsumption") && data.get("powerConsumption") != null) {
                        minerDetail.setPowerConsumption(data.get("powerConsumption").toString());
                    }
                    if (data.containsKey("coins") && data.get("coins") != null) {
                        minerDetail.setCoins(data.get("coins").toString());
                    }
                    if (data.containsKey("powerSource") && data.get("powerSource") != null) {
                        minerDetail.setPowerSource(data.get("powerSource").toString());
                    }
                    if (data.containsKey("cooling") && data.get("cooling") != null) {
                        minerDetail.setCooling(data.get("cooling").toString());
                    }
                    if (data.containsKey("operatingTemperature") && data.get("operatingTemperature") != null) {
                        minerDetail.setOperatingTemperature(data.get("operatingTemperature").toString());
                    }
                    if (data.containsKey("dimensions") && data.get("dimensions") != null) {
                        minerDetail.setDimensions(data.get("dimensions").toString());
                    }
                    if (data.containsKey("noiseLevel") && data.get("noiseLevel") != null) {
                        minerDetail.setNoiseLevel(data.get("noiseLevel").toString());
                    }
                    if (data.containsKey("description") && data.get("description") != null) {
                        minerDetail.setDescription(data.get("description").toString());
                    }
                    if (data.containsKey("features") && data.get("features") != null) {
                        minerDetail.setFeatures(data.get("features").toString());
                    }
                    if (data.containsKey("placementInfo") && data.get("placementInfo") != null) {
                        minerDetail.setPlacementInfo(data.get("placementInfo").toString());
                    }
                    if (data.containsKey("producerInfo") && data.get("producerInfo") != null) {
                        minerDetail.setProducerInfo(data.get("producerInfo").toString());
                    }
                    
                    // Сохраняем обновленную запись
                    MinerDetail updatedDetail = minerDetailService.updateMinerDetail(minerDetail);
                    String standardName = updatedDetail.getStandardName();
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", id);
                    result.put("standardName", standardName);
                    result.put("success", true);
                    result.put("message", "MinerDetail ID=" + id + " успешно обновлен");
                    
                    updated++;
                    results.add(result);
                    
                    log.debug("Обновлен MinerDetail ID={} ({})", id, updatedDetail.getStandardName());
                } catch (Exception e) {
                    log.error("Ошибка при обновлении записи: {}", data, e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("data", data);
                    errorResult.put("success", false);
                    errorResult.put("error", e.getMessage());
                    results.add(errorResult);
                    skipped++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Обработано %d записей: обновлено %d, пропущено %d", 
                    minerDetailsData.size(), updated, skipped));
            response.put("totalProcessed", minerDetailsData.size());
            response.put("updated", updated);
            response.put("skipped", skipped);
            response.put("results", results);
            
            log.info("Массовое обновление завершено: обновлено {}, пропущено {}", updated, skipped);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при массовом обновлении MinerDetail: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Ошибка при обновлении: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
