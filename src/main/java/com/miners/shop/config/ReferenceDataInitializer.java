package com.miners.shop.config;

import com.miners.shop.entity.Currency;
import com.miners.shop.entity.HashrateUnit;
import com.miners.shop.repository.CurrencyRepository;
import com.miners.shop.repository.HashrateUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Инициализация справочных данных (валюты и единицы измерения хэшрейта)
 * Выполняется при развертывании приложения на любом сервере (dev/prod)
 * Гарантирует наличие справочных данных в базе данных
 */
@Component
@Order(2) // Выполняется после DatabaseMigrationConfig (Order=1)
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataInitializer implements ApplicationRunner {
    
    private final CurrencyRepository currencyRepository;
    private final HashrateUnitRepository hashrateUnitRepository;
    private final DataSource dataSource;
    
    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("🔧 Инициализация справочников: валюты и единицы измерения хэшрейта");
            log.info("═══════════════════════════════════════════════════════════════");
            
            // Проверяем наличие таблиц перед инициализацией
            boolean currenciesTableExists = checkTableExists("currencies");
            boolean hashrateUnitsTableExists = checkTableExists("hashrate_units");
            
            if (!currenciesTableExists || !hashrateUnitsTableExists) {
                log.warn("⚠️  Таблицы справочников не найдены. Инициализация будет выполнена после создания таблиц Hibernate.");
                log.warn("⚠️  currencies: {}, hashrate_units: {}", currenciesTableExists, hashrateUnitsTableExists);
                // Повторяем попытку через небольшую задержку
                Thread.sleep(2000);
                currenciesTableExists = checkTableExists("currencies");
                hashrateUnitsTableExists = checkTableExists("hashrate_units");
                
                if (!currenciesTableExists || !hashrateUnitsTableExists) {
                    log.error("❌ Таблицы справочников все еще не найдены после задержки. Инициализация пропущена.");
                    return;
                }
            }
            
            // Инициализация валют
            initializeCurrencies();
            
            // Инициализация единиц измерения хэшрейта
            initializeHashrateUnits();
            
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("✅✅✅ ИНИЦИАЛИЗАЦИЯ СПРАВОЧНИКОВ ЗАВЕРШЕНА ✅✅✅");
            log.info("═══════════════════════════════════════════════════════════════");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Инициализация справочников прервана: {}", e.getMessage());
        } catch (Exception e) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌❌❌ ОШИБКА ПРИ ИНИЦИАЛИЗАЦИИ СПРАВОЧНИКОВ ❌❌❌");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("Ошибка: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("⚠️  Приложение продолжит запуск, но справочники могут быть не инициализированы!");
            log.error("⚠️  Выполните инициализацию вручную через SQL скрипт:");
            log.error("⚠️  shop-backend/migration_init_reference_data.sql");
            log.error("═══════════════════════════════════════════════════════════════");
            // Не прерываем запуск приложения
        }
    }
    
    /**
     * Проверка существования таблицы в базе данных
     */
    private boolean checkTableExists(String tableName) {
        try {
            String checkTableSql = """
                SELECT COUNT(*) 
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = '%s'
                """.formatted(tableName);
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 var rs = stmt.executeQuery(checkTableSql)) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (Exception e) {
            log.debug("Ошибка при проверке существования таблицы {}: {}", tableName, e.getMessage());
        }
        return false;
    }
    
    /**
     * Инициализация валют
     */
    private void initializeCurrencies() {
        log.info("📋 Инициализация валют...");
        
        long count = currencyRepository.count();
        if (count > 0) {
            log.info("✅ Валюты уже инициализированы ({} записей)", count);
            return;
        }
        
        // Создаем валюты
        Currency rub = new Currency();
        rub.setCode("RUB");
        rub.setName("Российский рубль");
        rub.setSymbol("₽");
        rub.setIsBase(true);
        rub.setDisplayOrder(1);
        currencyRepository.save(rub);
        
        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setName("Доллар США");
        usd.setSymbol("$");
        usd.setIsBase(false);
        usd.setDisplayOrder(2);
        currencyRepository.save(usd);
        
        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Евро");
        eur.setSymbol("€");
        eur.setIsBase(false);
        eur.setDisplayOrder(3);
        currencyRepository.save(eur);
        
        Currency cny = new Currency();
        cny.setCode("CNY");
        cny.setName("Китайский юань");
        cny.setSymbol("¥");
        cny.setIsBase(false);
        cny.setDisplayOrder(4);
        currencyRepository.save(cny);
        
        log.info("✅ Валюты инициализированы: добавлено 4 записи");
    }
    
    /**
     * Инициализация единиц измерения хэшрейта
     */
    private void initializeHashrateUnits() {
        log.info("📋 Инициализация единиц измерения хэшрейта...");
        
        long count = hashrateUnitRepository.count();
        if (count > 0) {
            log.info("✅ Единицы измерения хэшрейта уже инициализированы ({} записей)", count);
            return;
        }
        
        // Создаем единицы измерения хэшрейта
        HashrateUnit hps = new HashrateUnit();
        hps.setName("Хеш в секунду");
        hps.setAbbreviation("H/s");
        hps.setMultiplier(BigDecimal.ONE);
        hps.setDisplayOrder(1);
        hashrateUnitRepository.save(hps);
        
        HashrateUnit khps = new HashrateUnit();
        khps.setName("КилоХеш в секунду");
        khps.setAbbreviation("KH/s");
        khps.setMultiplier(new BigDecimal("1000"));
        khps.setDisplayOrder(2);
        hashrateUnitRepository.save(khps);
        
        HashrateUnit mhps = new HashrateUnit();
        mhps.setName("МегаХеш в секунду");
        mhps.setAbbreviation("MH/s");
        mhps.setMultiplier(new BigDecimal("1000000"));
        mhps.setDisplayOrder(3);
        hashrateUnitRepository.save(mhps);
        
        HashrateUnit ghps = new HashrateUnit();
        ghps.setName("ГигаХеш в секунду");
        ghps.setAbbreviation("GH/s");
        ghps.setMultiplier(new BigDecimal("1000000000"));
        ghps.setDisplayOrder(4);
        hashrateUnitRepository.save(ghps);
        
        HashrateUnit thps = new HashrateUnit();
        thps.setName("ТераХеш в секунду");
        thps.setAbbreviation("TH/s");
        thps.setMultiplier(new BigDecimal("1000000000000"));
        thps.setDisplayOrder(5);
        hashrateUnitRepository.save(thps);
        
        HashrateUnit phps = new HashrateUnit();
        phps.setName("ПетаХеш в секунду");
        phps.setAbbreviation("PH/s");
        phps.setMultiplier(new BigDecimal("1000000000000000"));
        phps.setDisplayOrder(6);
        hashrateUnitRepository.save(phps);
        
        HashrateUnit ehps = new HashrateUnit();
        ehps.setName("ЭксаХеш в секунду");
        ehps.setAbbreviation("EH/s");
        ehps.setMultiplier(new BigDecimal("1000000000000000000"));
        ehps.setDisplayOrder(7);
        hashrateUnitRepository.save(ehps);
        
        HashrateUnit ksolps = new HashrateUnit();
        ksolps.setName("КилоСол в секунду");
        ksolps.setAbbreviation("kSol/s");
        ksolps.setMultiplier(new BigDecimal("1000"));
        ksolps.setDisplayOrder(8);
        hashrateUnitRepository.save(ksolps);
        
        HashrateUnit msolps = new HashrateUnit();
        msolps.setName("МегаСол в секунду");
        msolps.setAbbreviation("MSol/s");
        msolps.setMultiplier(new BigDecimal("1000000"));
        msolps.setDisplayOrder(9);
        hashrateUnitRepository.save(msolps);
        
        HashrateUnit gsolps = new HashrateUnit();
        gsolps.setName("ГигаСол в секунду");
        gsolps.setAbbreviation("GSol/s");
        gsolps.setMultiplier(new BigDecimal("1000000000"));
        gsolps.setDisplayOrder(10);
        hashrateUnitRepository.save(gsolps);
        
        log.info("✅ Единицы измерения хэшрейта инициализированы: добавлено 10 записей");
    }
}

