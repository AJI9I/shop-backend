# Проверка запущенного приложения
$port = 8080
$connection = Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue

if ($connection) {
    Write-Host "Приложение работает на порту $port" -ForegroundColor Green
    Write-Host "URL: http://localhost:$port"
} else {
    Write-Host "Приложение не запущено на порту $port" -ForegroundColor Red
}

# Проверка Java процессов
$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "`nНайдено процессов Java: $($javaProcesses.Count)" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "  PID: $($_.Id) | CPU: $($_.CPU) | Memory: $([math]::Round($_.WorkingSet64 / 1MB, 2)) MB"
    }
} else {
    Write-Host "`nJava процессы не найдены" -ForegroundColor Red
}

