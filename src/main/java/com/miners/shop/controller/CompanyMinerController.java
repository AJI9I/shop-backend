package com.miners.shop.controller;

import com.miners.shop.dto.CompanyMinerDTO;
import com.miners.shop.entity.Currency;
import com.miners.shop.entity.HashrateUnit;
import com.miners.shop.entity.MinerDetail;
import com.miners.shop.repository.CurrencyRepository;
import com.miners.shop.repository.HashrateUnitRepository;
import com.miners.shop.repository.MinerDetailRepository;
import com.miners.shop.service.CompanyMinerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Контроллер для управления майнерами компании
 */
@Controller
@RequestMapping("/private/company-miners")
@RequiredArgsConstructor
@Slf4j
public class CompanyMinerController {
    
    private final CompanyMinerService companyMinerService;
    private final MinerDetailRepository minerDetailRepository;
    private final CurrencyRepository currencyRepository;
    private final HashrateUnitRepository hashrateUnitRepository;
    
    /**
     * Список всех майнеров компании
     */
    @GetMapping
    public String list(Model model) {
        List<CompanyMinerDTO.CompanyMinerInfo> companyMiners = companyMinerService.getAllCompanyMiners();
        model.addAttribute("companyMiners", companyMiners);
        return "company-miners/list";
    }
    
    /**
     * Форма создания майнера компании
     * Может быть вызвана с параметром minerDetailId для предзаполнения
     */
    @GetMapping("/create")
    public String createForm(
            @RequestParam(required = false) Long minerDetailId,
            Model model) {
        
        // Получаем все MinerDetail для выпадающего списка
        List<MinerDetail> minerDetails = minerDetailRepository.findAll();
        
        // Получаем валюты и единицы измерения хэшрейта
        List<Currency> currencies = currencyRepository.findAll();
        List<HashrateUnit> hashrateUnits = hashrateUnitRepository.findAll();
        
        // Если передан minerDetailId, получаем MinerDetail для предзаполнения
        MinerDetail selectedMinerDetail = null;
        if (minerDetailId != null) {
            selectedMinerDetail = minerDetailRepository.findById(minerDetailId).orElse(null);
        }
        
        // Создаем DTO для формы
        CompanyMinerDTO.CreateCompanyMinerDTO dto = CompanyMinerDTO.CreateCompanyMinerDTO.builder()
                .minerDetailId(minerDetailId != null ? minerDetailId : null)
                .price(null)
                .currencyId(null)
                .priceOld(null)
                .hashrateMin(null)
                .hashrateMax(null)
                .hashrateUnitId(null)
                .quantity(0)
                .condition(null)
                .active(true)
                .lowStockThreshold(null)
                .customFields(null)
                .build();
        
        model.addAttribute("companyMiner", dto);
        model.addAttribute("minerDetail", selectedMinerDetail);
        model.addAttribute("minerDetails", minerDetails);
        model.addAttribute("currencies", currencies);
        model.addAttribute("hashrateUnits", hashrateUnits);
        
        return "company-miners/create";
    }
    
    /**
     * Создание майнера компании
     */
    @PostMapping
    public String create(
            @Valid @ModelAttribute("companyMiner") CompanyMinerDTO.CreateCompanyMinerDTO dto,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Создание CompanyMiner для MinerDetail ID: {}", dto.minerDetailId());
            
            companyMinerService.createCompanyMiner(dto);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Майнер компании успешно создан");
            return "redirect:/private/company-miners";
            
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при создании CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/private/company-miners/create?minerDetailId=" + dto.minerDetailId();
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Произошла ошибка при создании майнера компании: " + e.getMessage());
            return "redirect:/private/company-miners/create?minerDetailId=" + dto.minerDetailId();
        }
    }
    
    /**
     * Форма редактирования майнера компании
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CompanyMinerDTO.CompanyMinerInfo companyMiner = companyMinerService.getCompanyMinerById(id)
                    .orElseThrow(() -> new IllegalArgumentException("CompanyMiner с ID " + id + " не найден"));
            
            // Получаем все MinerDetail для выпадающего списка
            List<MinerDetail> minerDetails = minerDetailRepository.findAll();
            
            // Получаем валюты и единицы измерения хэшрейта
            List<Currency> currencies = currencyRepository.findAll();
            List<HashrateUnit> hashrateUnits = hashrateUnitRepository.findAll();
            
            // Создаем DTO для формы редактирования
            CompanyMinerDTO.UpdateCompanyMinerDTO updateDto = CompanyMinerDTO.UpdateCompanyMinerDTO.builder()
                    .price(companyMiner.price())
                    .currencyId(companyMiner.currencyId())
                    .priceOld(companyMiner.priceOld())
                    .hashrateMin(companyMiner.hashrateMin())
                    .hashrateMax(companyMiner.hashrateMax())
                    .hashrateUnitId(companyMiner.hashrateUnitId())
                    .quantity(companyMiner.quantity())
                    .condition(companyMiner.condition())
                    .active(companyMiner.active())
                    .lowStockThreshold(companyMiner.lowStockThreshold())
                    .customFields(companyMiner.customFields() != null 
                            ? companyMiner.customFields().stream()
                                    .map(f -> CompanyMinerDTO.UpdateCustomFieldDTO.builder()
                                            .fieldName(f.fieldName())
                                            .fieldValue(f.fieldValue())
                                            .displayOrder(f.displayOrder())
                                            .build())
                                    .toList()
                            : null)
                    .build();
            
            model.addAttribute("companyMiner", updateDto);
            model.addAttribute("companyMinerInfo", companyMiner);
            model.addAttribute("minerDetails", minerDetails);
            model.addAttribute("currencies", currencies);
            model.addAttribute("hashrateUnits", hashrateUnits);
            
            return "company-miners/edit";
            
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при получении CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/private/company-miners";
        }
    }
    
    /**
     * Обновление майнера компании
     */
    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("companyMiner") CompanyMinerDTO.UpdateCompanyMinerDTO dto,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Обновление CompanyMiner с ID: {}", id);
            
            companyMinerService.updateCompanyMiner(id, dto);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Майнер компании успешно обновлен");
            return "redirect:/private/company-miners";
            
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при обновлении CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/private/company-miners/" + id + "/edit";
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обновлении CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Произошла ошибка при обновлении майнера компании: " + e.getMessage());
            return "redirect:/private/company-miners/" + id + "/edit";
        }
    }
    
    /**
     * Удаление майнера компании
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Удаление CompanyMiner с ID: {}", id);
            
            companyMinerService.deleteCompanyMiner(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Майнер компании успешно удален");
            return "redirect:/private/company-miners";
            
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при удалении CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/private/company-miners";
        } catch (Exception e) {
            log.error("Неожиданная ошибка при удалении CompanyMiner: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Произошла ошибка при удалении майнера компании: " + e.getMessage());
            return "redirect:/private/company-miners";
        }
    }
}

