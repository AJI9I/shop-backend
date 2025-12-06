package com.miners.shop.service;

import com.miners.shop.dto.CompanyMinerDTO;
import com.miners.shop.entity.CompanyMiner;
import com.miners.shop.entity.CompanyMinerCustomField;
import com.miners.shop.entity.Currency;
import com.miners.shop.entity.HashrateUnit;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.repository.CompanyMinerCustomFieldRepository;
import com.miners.shop.repository.CompanyMinerRepository;
import com.miners.shop.repository.CurrencyRepository;
import com.miners.shop.repository.HashrateUnitRepository;
import com.miners.shop.repository.MinerDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с майнерами компании
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyMinerService {
    
    private final CompanyMinerRepository companyMinerRepository;
    private final CompanyMinerCustomFieldRepository customFieldRepository;
    private final MinerDetailRepository minerDetailRepository;
    private final CurrencyRepository currencyRepository;
    private final HashrateUnitRepository hashrateUnitRepository;
    
    /**
     * Получить все майнеры компании
     */
    @Transactional(readOnly = true)
    public List<CompanyMinerDTO.CompanyMinerInfo> getAllCompanyMiners() {
        List<CompanyMiner> companyMiners = companyMinerRepository.findAll();
        return companyMiners.stream()
                .map(CompanyMinerDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Получить майнер компании по ID
     */
    @Transactional(readOnly = true)
    public Optional<CompanyMinerDTO.CompanyMinerInfo> getCompanyMinerById(Long id) {
        return companyMinerRepository.findById(id)
                .map(CompanyMinerDTO::fromEntity);
    }
    
    /**
     * Получить майнер компании по ID MinerDetail
     */
    @Transactional(readOnly = true)
    public Optional<CompanyMinerDTO.CompanyMinerInfo> getCompanyMinerByMinerDetailId(Long minerDetailId) {
        return companyMinerRepository.findByMinerDetailId(minerDetailId)
                .map(CompanyMinerDTO::fromEntity);
    }
    
    /**
     * Создать майнер компании
     */
    @Transactional
    public CompanyMinerDTO.CompanyMinerInfo createCompanyMiner(CompanyMinerDTO.CreateCompanyMinerDTO dto) {
        log.info("Создание майнера компании для MinerDetail ID: {}", dto.minerDetailId());
        
        // Проверяем, что MinerDetail существует
        MinerDetail minerDetail = minerDetailRepository.findById(dto.minerDetailId())
                .orElseThrow(() -> new IllegalArgumentException("MinerDetail с ID " + dto.minerDetailId() + " не найден"));
        
        // Проверяем, что для этого MinerDetail еще нет CompanyMiner
        if (companyMinerRepository.existsByMinerDetailId(dto.minerDetailId())) {
            throw new IllegalArgumentException("Для MinerDetail с ID " + dto.minerDetailId() + " уже существует CompanyMiner");
        }
        
        // Получаем Currency и HashrateUnit
        Currency currency = currencyRepository.findById(dto.currencyId())
                .orElseThrow(() -> new IllegalArgumentException("Currency с ID " + dto.currencyId() + " не найден"));
        
        HashrateUnit hashrateUnit = hashrateUnitRepository.findById(dto.hashrateUnitId())
                .orElseThrow(() -> new IllegalArgumentException("HashrateUnit с ID " + dto.hashrateUnitId() + " не найден"));
        
        // Создаем CompanyMiner
        CompanyMiner companyMiner = new CompanyMiner();
        companyMiner.setMinerDetail(minerDetail);
        companyMiner.setPrice(dto.price());
        companyMiner.setCurrency(currency);
        companyMiner.setPriceOld(dto.priceOld());
        companyMiner.setHashrateMin(dto.hashrateMin());
        companyMiner.setHashrateMax(dto.hashrateMax());
        companyMiner.setHashrateUnit(hashrateUnit);
        companyMiner.setQuantity(dto.quantity());
        companyMiner.setCondition(dto.condition());
        companyMiner.setActive(dto.active() != null ? dto.active() : true);
        companyMiner.setLowStockThreshold(dto.lowStockThreshold());
        
        // Сохраняем CompanyMiner
        CompanyMiner saved = companyMinerRepository.save(companyMiner);
        log.info("Создан CompanyMiner с ID: {}", saved.getId());
        
        // Создаем дополнительные поля, если они есть
        if (dto.customFields() != null && !dto.customFields().isEmpty()) {
            for (CompanyMinerDTO.CreateCustomFieldDTO fieldDto : dto.customFields()) {
                CompanyMinerCustomField field = new CompanyMinerCustomField();
                field.setCompanyMiner(saved);
                field.setFieldName(fieldDto.fieldName());
                field.setFieldValue(fieldDto.fieldValue());
                field.setDisplayOrder(fieldDto.displayOrder() != null ? fieldDto.displayOrder() : 0);
                customFieldRepository.save(field);
            }
            log.info("Создано {} дополнительных полей для CompanyMiner ID: {}", dto.customFields().size(), saved.getId());
        }
        
        return CompanyMinerDTO.fromEntity(saved);
    }
    
    /**
     * Обновить майнер компании
     */
    @Transactional
    public CompanyMinerDTO.CompanyMinerInfo updateCompanyMiner(Long id, CompanyMinerDTO.UpdateCompanyMinerDTO dto) {
        log.info("Обновление CompanyMiner с ID: {}", id);
        
        CompanyMiner companyMiner = companyMinerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CompanyMiner с ID " + id + " не найден"));
        
        // Получаем Currency и HashrateUnit
        Currency currency = currencyRepository.findById(dto.currencyId())
                .orElseThrow(() -> new IllegalArgumentException("Currency с ID " + dto.currencyId() + " не найден"));
        
        HashrateUnit hashrateUnit = hashrateUnitRepository.findById(dto.hashrateUnitId())
                .orElseThrow(() -> new IllegalArgumentException("HashrateUnit с ID " + dto.hashrateUnitId() + " не найден"));
        
        // Обновляем поля
        companyMiner.setPrice(dto.price());
        companyMiner.setCurrency(currency);
        companyMiner.setPriceOld(dto.priceOld());
        companyMiner.setHashrateMin(dto.hashrateMin());
        companyMiner.setHashrateMax(dto.hashrateMax());
        companyMiner.setHashrateUnit(hashrateUnit);
        companyMiner.setQuantity(dto.quantity());
        companyMiner.setCondition(dto.condition());
        companyMiner.setActive(dto.active() != null ? dto.active() : true);
        companyMiner.setLowStockThreshold(dto.lowStockThreshold());
        
        // Удаляем старые дополнительные поля
        List<CompanyMinerCustomField> oldFields = customFieldRepository.findByCompanyMinerIdOrderByDisplayOrderAsc(id);
        customFieldRepository.deleteAll(oldFields);
        
        // Создаем новые дополнительные поля
        if (dto.customFields() != null && !dto.customFields().isEmpty()) {
            for (CompanyMinerDTO.UpdateCustomFieldDTO fieldDto : dto.customFields()) {
                CompanyMinerCustomField field = new CompanyMinerCustomField();
                field.setCompanyMiner(companyMiner);
                field.setFieldName(fieldDto.fieldName());
                field.setFieldValue(fieldDto.fieldValue());
                field.setDisplayOrder(fieldDto.displayOrder() != null ? fieldDto.displayOrder() : 0);
                customFieldRepository.save(field);
            }
            log.info("Обновлено {} дополнительных полей для CompanyMiner ID: {}", dto.customFields().size(), id);
        }
        
        // Сохраняем изменения
        CompanyMiner saved = companyMinerRepository.save(companyMiner);
        log.info("Обновлен CompanyMiner с ID: {}", saved.getId());
        
        return CompanyMinerDTO.fromEntity(saved);
    }
    
    /**
     * Удалить майнер компании
     */
    @Transactional
    public void deleteCompanyMiner(Long id) {
        log.info("Удаление CompanyMiner с ID: {}", id);
        
        if (!companyMinerRepository.existsById(id)) {
            throw new IllegalArgumentException("CompanyMiner с ID " + id + " не найден");
        }
        
        companyMinerRepository.deleteById(id);
        log.info("Удален CompanyMiner с ID: {}", id);
    }
    
    /**
     * Получить активные майнеры компании
     */
    @Transactional(readOnly = true)
    public List<CompanyMinerDTO.CompanyMinerInfo> getActiveCompanyMiners() {
        List<CompanyMiner> companyMiners = companyMinerRepository.findByActiveTrue();
        return companyMiners.stream()
                .map(CompanyMinerDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

