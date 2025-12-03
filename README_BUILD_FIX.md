# Быстрое исправление ошибки сборки

## Автоматическое решение

Запустите файл `stop-and-rebuild.bat` в директории `shop-backend/`:
- Двойной клик по файлу
- Или выполните в командной строке: `stop-and-rebuild.bat`

Скрипт автоматически:
1. ✅ Остановит все Java процессы
2. ✅ Удалит директорию `target`
3. ✅ Выполнит `mvn clean`
4. ✅ Выполнит `mvn compile`

## Ручное решение

Если автоматический скрипт не помог:

1. **Остановите все Java процессы**:
   ```cmd
   taskkill /F /IM java.exe
   taskkill /F /IM javaw.exe
   ```

2. **Закройте IDE** (IntelliJ IDEA, Eclipse)

3. **Удалите директорию target** через проводник Windows

4. **Выполните сборку**:
   ```cmd
   cd shop-backend
   mvn clean compile
   ```




