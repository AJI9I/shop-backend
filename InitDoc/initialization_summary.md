# Сводка по инициализации MinerDetail

## Статистика

- **Всего товаров:** 90
- **Майнеров:** 85
- **Не майнеров:** 5 (блоки питания, аксессуары)
- **Групп для объединения:** 12

## Группы для объединения

### 1. S19j PRO (6 товаров)
- Product IDs: 16, 32, 39, 42, 44, 76
- Standard Name: "Antminer S19j PRO"
- Manufacturer: Bitmain
- Series: S19j

### 2. S19k PRO (7 товаров)
- Product IDs: 2, 20, 22, 38, 41, 43, 64
- Standard Name: "Antminer S19k PRO"
- Manufacturer: Bitmain
- Series: S19k

### 3. S19 PRO (3 товара)
- Product IDs: 5, 15, 78
- Standard Name: "Antminer S19 PRO"
- Manufacturer: Bitmain
- Series: S19

### 4. M30S++ (5 товаров)
- Product IDs: 4, 8, 35, 47, 48
- Standard Name: "Whatsminer M30S++"
- Manufacturer: MicroBT
- Series: M30S

### 5. M30S+ (3 товара)
- Product IDs: 3, 33, 65
- Standard Name: "Whatsminer M30S+"
- Manufacturer: MicroBT
- Series: M30S

### 6. M30S (3 товара)
- Product IDs: 49, 66, 87
- Standard Name: "Whatsminer M30S"
- Manufacturer: MicroBT
- Series: M30S

### 7. M50 (7 товаров)
- Product IDs: 11, 12, 34, 46, 51, 52, 84
- Standard Name: "Whatsminer M50S++"
- Manufacturer: MicroBT
- Series: M50

### 8. Z15 PRO (6 товаров)
- Product IDs: 6, 31, 75, 77, 80, 88
- Standard Name: "Whatsminer Z15 PRO"
- Manufacturer: MicroBT
- Series: Z15

### 9. DG1 (4 товара)
- Product IDs: 27, 29, 30, 61
- Standard Name: "ElphapeX DG1+"
- Manufacturer: ElphapeX
- Series: DG1

### 10. S19 XP (2 товара)
- Product IDs: 13, 40
- Standard Name: "Antminer S19 XP"
- Manufacturer: Bitmain
- Series: S19

### 11. S21 (4 товара)
- Product IDs: 18, 23, 24, 90
- Standard Name: "Antminer S21"
- Manufacturer: Bitmain
- Series: S21

### 12. JPRO (2 товара)
- Product IDs: 36, 37
- Standard Name: "Whatsminer JPRO"
- Manufacturer: MicroBT
- Series: JPRO

## Ошибки в данных, требующие исправления

1. **Product ID=5** (s19 pro+): manufacturer=null → должно быть "Bitmain"
2. **Product ID=6** (z15 pro): manufacturer=null → должно быть "MicroBT"
3. **Product ID=37** (jpro 104): manufacturer=null → должно быть "MicroBT"
4. **Product ID=67** (М33S++): manufacturer="Bitmain" → должно быть "MicroBT"
5. **Product ID=74** (Avalon): manufacturer="Bitmain" → должно быть "Canaan"
6. **Product ID=75** (Z15 PRO): manufacturer="Bitmain" → должно быть "MicroBT"
7. **Product ID=76** (S19J PRO+): manufacturer=null → должно быть "Bitmain"
8. **Product ID=77** (Z15Pro): manufacturer="Bitmain" → должно быть "MicroBT"
9. **Product ID=80** (Z15 Pro): manufacturer="Bitmain" → должно быть "MicroBT"
10. **Product ID=81** (M33s+): manufacturer="Bitmain" → должно быть "MicroBT"
11. **Product ID=82** (M60s+): manufacturer="Bitmain" → должно быть "MicroBT"
12. **Product ID=84** (М50): manufacturer="Bitmain" → должно быть "MicroBT"
13. **Product ID=88** (Z15 pro): manufacturer=null → должно быть "MicroBT"
14. **Product ID=27** (DG1+): manufacturer="MicroBT" → должно быть "ElphapeX"
15. **Product ID=29** (Dg1): manufacturer="Bitmain" → должно быть "ElphapeX"
16. **Product ID=30** (Dg1+): manufacturer="Bitmain" → должно быть "ElphapeX"

## Уникальные майнеры (не входят в группы)

- T21 (ID=1)
- S19 (ID=7)
- S19j (ID=9)
- AvalonMiner 1246 (ID=10)
- S19a (ID=14)
- S19 90 126 (ID=17)
- L7 (ID=19)
- S19/S19j Pro (ID=21)
- S21XP (ID=25)
- L1 PRO (ID=26)
- L9 (ID=28)
- S19i (ID=79)
- E9 (ID=45)
- S21 XP (ID=50)
- M60 (ID=53)
- M61 (ID=54)
- M61s (ID=89)
- M63s (ID=56)
- M63s+ (ID=55)
- KS5P (ID=57)
- X16-Q (ID=58)
- X16-Pro (ID=59)
- DGHOME1 (ID=60)
- MiniDoge (ID=62)
- DG (ID=63)
- KS3M (ID=68)
- P221B (ID=72)
- P221C (ID=73)
- М33S++ (ID=67) - после исправления manufacturer
- X4-Q (ID=83)
- M60s+ (ID=82) - после исправления manufacturer
- D9 (ID=86) - уточнить manufacturer

## Следующие шаги

1. Исправить ошибки в manufacturer для указанных товаров
2. Создать MinerDetail для каждой группы объединения (12 записей)
3. Создать MinerDetail для уникальных майнеров (~35 записей)
4. Связать Product с соответствующими MinerDetail
5. Пропустить не майнеры (ID=69, 70, 71, 85)

---

*Документ создан: 2024-01-15*

