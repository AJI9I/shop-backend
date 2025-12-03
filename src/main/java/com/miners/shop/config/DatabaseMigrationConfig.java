package com.miners.shop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Автоматическое выполнение миграции для добавления колонки duplicate_count
 * Выполняется при старте приложения после инициализации DataSource
 */
@Component
@Order(1) // Выполняется первым среди ApplicationRunner
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationConfig implements ApplicationRunner {
    
    private final DataSource dataSource;
    
    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("🔧 Проверка наличия колонки duplicate_count в таблице whatsapp_messages");
            log.info("═══════════════════════════════════════════════════════════════");
            
            // Проверяем, существует ли таблица (на случай первого запуска)
            String checkTableSql = """
                SELECT COUNT(*) 
                FROM information_schema.tables 
                WHERE table_name = 'whatsapp_messages'
                """;
            
            Integer tableExists;
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 var rs = stmt.executeQuery(checkTableSql)) {
                rs.next();
                tableExists = rs.getInt(1);
            }
            
            if (tableExists == null || tableExists == 0) {
                log.warn("⚠️  Таблица whatsapp_messages не найдена. Миграция будет выполнена после создания таблицы Hibernate.");
                return;
            }
            
            // Проверяем, существует ли колонка
            String checkColumnSql = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_name = 'whatsapp_messages' 
                AND column_name = 'duplicate_count'
                """;
            
            Integer columnExists;
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 var rs = stmt.executeQuery(checkColumnSql)) {
                rs.next();
                columnExists = rs.getInt(1);
            }
            
            if (columnExists == null || columnExists == 0) {
                log.info("📋 Колонка duplicate_count отсутствует, выполняется миграция...");
                
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    
                    // Добавляем колонку с возможностью NULL (временно)
                    String addColumnSql = """
                        ALTER TABLE whatsapp_messages 
                        ADD COLUMN IF NOT EXISTS duplicate_count INTEGER;
                        """;
                    
                    stmt.execute(addColumnSql);
                    log.info("✅ Колонка duplicate_count добавлена (nullable)");
                    
                    // Обновляем все существующие записи, устанавливая 0
                    String updateExistingSql = """
                        UPDATE whatsapp_messages 
                        SET duplicate_count = 0 
                        WHERE duplicate_count IS NULL
                        """;
                    
                    int updatedRows = stmt.executeUpdate(updateExistingSql);
                    log.info("✅ Обновлено существующих записей: {}", updatedRows);
                    
                    // Делаем колонку NOT NULL с DEFAULT 0
                    String setNotNullSql = """
                        ALTER TABLE whatsapp_messages 
                        ALTER COLUMN duplicate_count SET NOT NULL,
                        ALTER COLUMN duplicate_count SET DEFAULT 0;
                        """;
                    
                    stmt.execute(setNotNullSql);
                    log.info("✅ Колонка duplicate_count установлена как NOT NULL с DEFAULT 0");
                    
                    log.info("═══════════════════════════════════════════════════════════════");
                    log.info("✅✅✅ МИГРАЦИЯ УСПЕШНО ВЫПОЛНЕНА ✅✅✅");
                    log.info("═══════════════════════════════════════════════════════════════");
                } catch (Exception migrationError) {
                    log.error("❌ Ошибка при выполнении SQL миграции: {}", migrationError.getMessage(), migrationError);
                    // Не пробрасываем ошибку, чтобы приложение могло запуститься
                }
            } else {
                log.info("✅ Колонка duplicate_count уже существует, миграция не требуется");
                
                // Проверяем, может ли колонка быть NULL, и если да - исправляем
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement();
                     var rs = stmt.executeQuery("""
                        SELECT is_nullable 
                        FROM information_schema.columns 
                        WHERE table_name = 'whatsapp_messages' 
                        AND column_name = 'duplicate_count'
                        """)) {
                    if (rs.next() && "YES".equals(rs.getString("is_nullable"))) {
                        log.info("📋 Колонка duplicate_count существует, но nullable. Исправляем...");
                        
                        // Обновляем NULL значения
                        stmt.executeUpdate("UPDATE whatsapp_messages SET duplicate_count = 0 WHERE duplicate_count IS NULL");
                        
                        // Устанавливаем NOT NULL и DEFAULT
                        stmt.execute("""
                            ALTER TABLE whatsapp_messages 
                            ALTER COLUMN duplicate_count SET NOT NULL,
                            ALTER COLUMN duplicate_count SET DEFAULT 0;
                            """);
                        log.info("✅ Колонка duplicate_count исправлена: установлен NOT NULL и DEFAULT 0");
                    }
                } catch (Exception e) {
                    log.warn("⚠️  Не удалось проверить/исправить колонку duplicate_count: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌❌❌ КРИТИЧЕСКАЯ ОШИБКА ПРИ ВЫПОЛНЕНИИ МИГРАЦИИ ❌❌❌");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("Ошибка: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("⚠️  Приложение продолжит запуск, но миграция не выполнена!");
            log.error("⚠️  Выполните миграцию вручную через SQL скрипт:");
            log.error("⚠️  shop-backend/migration_add_duplicate_count_column.sql");
            log.error("═══════════════════════════════════════════════════════════════");
            // Не прерываем запуск приложения, так как это может быть временная проблема
        }
    }
}
