#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для анализа данных products и определения майнеров
"""

import json
import re
from collections import defaultdict
from typing import List, Dict, Set

# Производители майнеров
MINER_MANUFACTURERS = {
    'Bitmain', 'MicroBT', 'Canaan', 'Jasminer', 'Goldshell', 
    'ElphapeX', 'Avalon', 'Innosilicon', 'iPollo', 'YAMI'
}

# Паттерны для определения майнеров
MINER_PATTERNS = [
    r'^S\d+',  # S19, S21, S19j, S19k
    r'^L\d+',  # L7, L9
    r'^M\d+',  # M30S, M50, M60, M61
    r'^T\d+',  # T21
    r'^Z\d+',  # Z15
    r'^E\d+',  # E9
    r'^D\d+',  # D9, DG1
    r'^X\d+',  # X16, X4
    r'^P\d+',  # P221
    r'^KS\d+', # KS3M, KS5P
    r'^JPRO',  # JPRO
    r'^DG',    # DG, DG1, DG1+
    r'^Avalon', # AvalonMiner
    r'^MiniDoge', # MiniDoge
]

# Исключения (не майнеры)
NON_MINER_KEYWORDS = [
    'APW',  # Блоки питания (APW12, APW17, APW9)
    'Шумбокс',  # Аксессуар
    'кабель', 'cable',
    'плата', 'board',
    'блок питания', 'power supply'
]

def is_miner(product: Dict) -> bool:
    """Определяет, является ли товар майнером"""
    model = (product.get('model') or '').upper()
    manufacturer = (product.get('manufacturer') or '').strip()
    
    # Проверка на исключения
    for keyword in NON_MINER_KEYWORDS:
        if keyword.upper() in model.upper():
            return False
    
    # Проверка производителя
    if manufacturer and manufacturer in MINER_MANUFACTURERS:
        return True
    
    # Проверка паттернов
    for pattern in MINER_PATTERNS:
        if re.match(pattern, model, re.IGNORECASE):
            return True
    
    return False

def normalize_model(model: str) -> str:
    """Нормализует название модели для сравнения"""
    if not model:
        return ''
    # Убираем пробелы, приводим к верхнему регистру
    normalized = model.strip().upper()
    # Убираем лишние пробелы
    normalized = re.sub(r'\s+', ' ', normalized)
    return normalized

def find_duplicates(products: List[Dict]) -> Dict[str, List[Dict]]:
    """Находит дубликаты (товары с похожими названиями)"""
    normalized_groups = defaultdict(list)
    
    for product in products:
        model = product.get('model', '')
        normalized = normalize_model(model)
        if normalized:
            normalized_groups[normalized].append(product)
    
    # Возвращаем только группы с несколькими товарами
    duplicates = {k: v for k, v in normalized_groups.items() if len(v) > 1}
    return duplicates

def extract_series(model: str) -> str:
    """Извлекает серию из названия модели"""
    if not model:
        return ''
    
    # Паттерны для серий
    patterns = [
        r'^([A-Z]\d+[A-Z]?)',  # S19j, L7, M30S
        r'^([A-Z]+\d+)',       # Z15, X16
    ]
    
    for pattern in patterns:
        match = re.match(pattern, model, re.IGNORECASE)
        if match:
            return match.group(1).upper()
    
    return ''

def main():
    # Загружаем данные
    with open('products_data.json', 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    products = data.get('products', [])
    print(f"Всего товаров: {len(products)}")
    
    # Разделяем на майнеры и не майнеры
    miners = []
    non_miners = []
    
    for product in products:
        if is_miner(product):
            miners.append(product)
        else:
            non_miners.append(product)
    
    print(f"\nМайнеры: {len(miners)}")
    print(f"Не майнеры: {len(non_miners)}")
    
    # Находим дубликаты
    duplicates = find_duplicates(miners)
    print(f"\nНайдено групп дубликатов: {len(duplicates)}")
    
    # Создаем список майнеров для инициализации
    miners_list = []
    for miner in miners:
        series = extract_series(miner.get('model', ''))
        miners_list.append({
            'productId': miner.get('id'),
            'model': miner.get('model'),
            'manufacturer': miner.get('manufacturer'),
            'series': series,
            'needsInitialization': miner.get('minerDetailId') is None,
            'hasMinerDetail': miner.get('minerDetailId') is not None,
            'notes': ''
        })
    
    # Сохраняем результаты
    output = {
        'totalProducts': len(products),
        'minersCount': len(miners),
        'nonMinersCount': len(non_miners),
        'duplicatesCount': len(duplicates),
        'miners': miners_list,
        'nonMiners': non_miners,
        'duplicates': {k: [{'id': p['id'], 'model': p['model']} for p in v] 
                      for k, v in duplicates.items()}
    }
    
    with open('analysis_report.json', 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    
    print("\nАнализ завершен. Результаты сохранены в analysis_report.json")

if __name__ == '__main__':
    main()

