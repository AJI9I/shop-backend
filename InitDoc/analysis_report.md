# Отчет анализа данных products

## Общая статистика

- **Всего товаров:** 90
- **Товаров с minerDetailId = null:** 90 (100%)
- **Товаров с minerDetailId != null:** 0 (0%)

Все товары требуют инициализации MinerDetail.

---

## Анализ на совпадения (дубликаты)

### Группы товаров с похожими названиями:

#### Группа 1: S19j PRO / S19j pro / S19J PRO / S19j pro+ / S19J PRO+
- Product ID=39: "S19j PRO" (Bitmain)
- Product ID=16: "S19j pro" (Bitmain)
- Product ID=44: "S19J PRO" (Bitmain)
- Product ID=32: "S19j pro+" (Bitmain)
- Product ID=42: "S19j PRO+" (Bitmain)
- Product ID=76: "S19J PRO+" (manufacturer=null)
- **Рекомендация:** Объединить в один MinerDetail с названием "Antminer S19j PRO"

#### Группа 2: S19k pro / S19K pro / S19K PRO / s19k pro
- Product ID=2: "s19k pro" (Bitmain)
- Product ID=20: "S19k pro" (Bitmain)
- Product ID=64: "S19K pro" (Bitmain)
- Product ID=41: "S19K PRO" (Bitmain)
- Product ID=43: "S19k PRO" (Bitmain)
- **Рекомендация:** Объединить в один MinerDetail с названием "Antminer S19k PRO"

#### Группа 3: S19 PRO / S19 Pro / S19 pro+
- Product ID=78: "S19 PRO" (Bitmain)
- Product ID=15: "S19 Pro" (Bitmain)
- Product ID=5: "s19 pro+" (manufacturer=null)
- **Рекомендация:** Объединить в один MinerDetail с названием "Antminer S19 PRO"

#### Группа 4: M30S+ / m30s+ / M30S++ / m30s++ / M30S++-108th
- Product ID=3: "m30s+" (MicroBT)
- Product ID=33: "M30s+" (MicroBT)
- Product ID=4: "m30s++" (MicroBT)
- Product ID=35: "M30s++" (MicroBT)
- Product ID=8: "M30S++" (MicroBT)
- Product ID=47: "M30S++-108th" (MicroBT)
- **Рекомендация:** Объединить в один MinerDetail с названием "Whatsminer M30S++"

#### Группа 5: M30S / M30S- / M30S-85th
- Product ID=66: "M30S" (MicroBT)
- Product ID=87: "M30S-" (MicroBT)
- Product ID=49: "M30S-85th" (MicroBT)
- **Рекомендация:** Объединить в один MinerDetail с названием "Whatsminer M30S"

#### Группа 6: M50 / М50 / M50S / M50S+ / M50S++ / M50-120th
- Product ID=11: "M50" (MicroBT)
- Product ID=84: "М50" (Bitmain) - **ОШИБКА: производитель указан как Bitmain, должен быть MicroBT**
- Product ID=12: "M50S" (MicroBT)
- Product ID=34: "M50s" (MicroBT)
- Product ID=51: "M50S+" (MicroBT)
- Product ID=52: "M50S++" (MicroBT)
- Product ID=46: "M50-120th" (MicroBT)
- **Рекомендация:** Объединить в один MinerDetail с названием "Whatsminer M50S++"

#### Группа 7: Z15 / Z15 Pro / Z15Pro / Z15 pro / z15 pro
- Product ID=31: "Z15" (MicroBT)
- Product ID=77: "Z15Pro" (Bitmain) - **ОШИБКА: производитель указан как Bitmain, должен быть MicroBT**
- Product ID=75: "Z15 PRO" (Bitmain) - **ОШИБКА: производитель указан как Bitmain, должен быть MicroBT**
- Product ID=80: "Z15 Pro" (Bitmain) - **ОШИБКА: производитель указан как Bitmain, должен быть MicroBT**
- Product ID=88: "Z15 pro" (manufacturer=null)
- Product ID=6: "z15 pro" (manufacturer=null)
- **Рекомендация:** Объединить в один MinerDetail с названием "Whatsminer Z15 PRO"

#### Группа 8: DG1 / Dg1 / DG1+ / Dg1+ / DG
- Product ID=27: "DG1+" (MicroBT)
- Product ID=61: "DG1" (ElphapeX)
- Product ID=29: "Dg1" (Bitmain) - **ОШИБКА: производитель**
- Product ID=30: "Dg1+" (Bitmain) - **ОШИБКА: производитель**
- Product ID=63: "DG" (Goldshell)
- **Рекомендация:** Разделить на две группы:
  - DG1/DG1+ (ElphapeX или MicroBT - уточнить)
  - DG (Goldshell)

#### Группа 9: S19 XP / S19xp / S21 XP
- Product ID=40: "S19 XP" (Bitmain)
- Product ID=13: "S19xp" (Bitmain)
- Product ID=50: "S21 XP" (Bitmain)
- **Рекомендация:** Разделить на две группы:
  - S19 XP (объединить)
  - S21 XP (отдельно)

#### Группа 10: S21 / S21+ / S21XP / S21 HYD / S21 PRO
- Product ID=18: "S21" (Bitmain)
- Product ID=24: "S21+" (Bitmain)
- Product ID=25: "S21XP" (Bitmain)
- Product ID=23: "S21 HYD" (Bitmain)
- Product ID=90: "S21 PRO" (Bitmain)
- **Рекомендация:** Объединить в один MinerDetail с названием "Antminer S21" (или разделить на S21 и S21 PRO)

#### Группа 11: X16-Pro / X16-Q
- Product ID=59: "X16-Pro" (Jasminer)
- Product ID=58: "X16-Q" (Jasminer)
- **Рекомендация:** Разделить на две модели (разные модификации)

#### Группа 12: P221B / P221C
- Product ID=72: "P221B" (Bitmain)
- Product ID=73: "P221C" (Bitmain)
- **Рекомендация:** Разделить на две модели (разные модификации)

---

## Определение майнеров

### Майнеры (ASIC майнеры) - 85 товаров:

**Bitmain:**
- S19, S19a, S19j, S19j PRO, S19k PRO, S19 PRO, S19 XP
- S21, S21+, S21XP, S21 HYD, S21 PRO
- L7, L9
- T21
- E9
- P221B, P221C
- KS5P
- Z15, Z15 PRO (ошибка в производителе - должен быть MicroBT)
- DG1, DG1+ (ошибка в производителе - должен быть ElphapeX)
- М50 (ошибка в производителе - должен быть MicroBT)
- М33S++ (ошибка в производителе - должен быть MicroBT)
- M60s+ (ошибка в производителе - должен быть MicroBT)
- M33s+ (ошибка в производителе - должен быть MicroBT)
- Avalon (ошибка в производителе - должен быть Canaan)

**MicroBT:**
- M30S, M30S+, M30S++, M30S-85th, M30S++-108th
- M50, M50S, M50S+, M50S++, M50-120th
- M60, M60s+
- M61, M61s
- M63s, M63s+
- Z15, Z15 PRO
- DG1+ (возможно ошибка - должен быть ElphapeX)
- JPRO, jpro 104
- KS3M
- X4-Q

**Canaan:**
- AvalonMiner 1246

**Jasminer:**
- X16-Pro, X16-Q

**Goldshell:**
- MiniDoge
- DG

**ElphapeX:**
- DG1
- DGHOME1

**Другие:**
- S19/S19j Pro (комбинированное название)
- S19 90 126
- S19i
- S19 k pro
- D9

### НЕ майнеры (5 товаров):

1. **APW12** (ID=70, Bitmain) - Блок питания
2. **APW17** (ID=71, Bitmain) - Блок питания
3. **apw9/9+** (ID=85, manufacturer=null) - Блок питания
4. **Шумбокс** (ID=69, Б/У) - Аксессуар (корпус для майнера)

---

## Проблемы в данных

### Ошибки в производителях:

1. **Product ID=84** ("М50"): manufacturer="Bitmain" → должно быть "MicroBT"
2. **Product ID=77** ("Z15Pro"): manufacturer="Bitmain" → должно быть "MicroBT"
3. **Product ID=75** ("Z15 PRO"): manufacturer="Bitmain" → должно быть "MicroBT"
4. **Product ID=80** ("Z15 Pro"): manufacturer="Bitmain" → должно быть "MicroBT"
5. **Product ID=29** ("Dg1"): manufacturer="Bitmain" → должно быть "ElphapeX" или "MicroBT"
6. **Product ID=30** ("Dg1+"): manufacturer="Bitmain" → должно быть "ElphapeX" или "MicroBT"
7. **Product ID=81** ("M33s+"): manufacturer="Bitmain" → должно быть "MicroBT"
8. **Product ID=82** ("M60s+"): manufacturer="Bitmain" → должно быть "MicroBT"
9. **Product ID=74** ("Avalon"): manufacturer="Bitmain" → должно быть "Canaan"

### Товары без производителя:

- Product ID=5: "s19 pro+" (manufacturer=null) → должно быть "Bitmain"
- Product ID=6: "z15 pro" (manufacturer=null) → должно быть "MicroBT"
- Product ID=76: "S19J PRO+" (manufacturer=null) → должно быть "Bitmain"
- Product ID=37: "jpro 104" (manufacturer=null) → должно быть "MicroBT"
- Product ID=85: "apw9/9+" (manufacturer=null) → блок питания, не майнер
- Product ID=86: "D9" (manufacturer=null) → возможно майнер, уточнить
- Product ID=88: "Z15 pro" (manufacturer=null) → должно быть "MicroBT"

---

## Рекомендации по инициализации

1. **Исправить ошибки в производителях** перед инициализацией
2. **Объединить дубликаты** в группы для создания одного MinerDetail на группу
3. **Создать MinerDetail** для каждого уникального майнера
4. **Пропустить не майнеры** (блоки питания, аксессуары)

---

*Отчет создан: 2024-01-15*

