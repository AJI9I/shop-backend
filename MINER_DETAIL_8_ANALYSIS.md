# Анализ и обновление MinerDetail ID = 8

## Текущее состояние
MinerDetail ID = 8 соответствует модели **Whatsminer Z15 PRO** (MicroBT, серия Z15)

**Алгоритм:** Equihash (не SHA-256!)
**Монеты:** ZEC (Zcash), ZEN (Zencash)

## Методы для работы с данными

### 1. Анализ текущих данных
GET `http://localhost:8080/api/init/miner-detail/8/analyze`

Возвращает:
- `currentData` - текущие данные MinerDetail
- `missingFields` - список отсутствующих полей
- `missingCount` - количество отсутствующих полей
- `filledCount` - количество заполненных полей

### 2. Обновление данных
POST `http://localhost:8080/api/init/miner-detail/8/update`

Автоматически заполняет все недостающие поля для Whatsminer Z15 PRO:

**Технические характеристики:**
- `hashrate`: "840 kSol/s"
- `algorithm`: "Equihash"
- `powerConsumption`: "1518W"
- `cooling`: "Воздушное"
- `operatingTemperature`: "от 0 до 40°C"
- `dimensions`: "370 x 195.5 x 290 мм"
- `noiseLevel`: "~75 дБ"
- `coins`: "ZEC, ZEN"
- `powerSource`: "Интегрированный"

**Описательные поля:**
- `description`: Краткое описание майнера для Equihash
- `features`: Особенности и функции (840 kSol/s, 1.8 Дж/МСол)
- `placementInfo`: Информация о размещении
- `producerInfo`: Информация о производителе и серии Z15

## Инструкция по использованию

1. **Проанализировать текущие данные:**
   ```bash
   curl http://localhost:8080/api/init/miner-detail/8/analyze
   ```

2. **Обновить данные:**
   ```bash
   curl -X POST http://localhost:8080/api/init/miner-detail/8/update
   ```

3. **Проверить результат:**
   ```bash
   curl http://localhost:8080/api/init/miner-detail/8/analyze
   ```

## Отличия от SHA-256 майнеров

Whatsminer Z15 PRO — это Equihash майнер, а не SHA-256:
- **Хэшрейт:** измеряется в kSol/s (килосол/сек), а не TH/s
- **Алгоритм:** Equihash, а не SHA-256
- **Монеты:** ZEC, ZEN, а не BTC, BCH, BSV
- **Энергоэффективность:** измеряется в Дж/МСол, а не Дж/Т
