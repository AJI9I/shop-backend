# Остановка Java процессов
Write-Host "Stopping Java processes..."
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Переход в директорию проекта
Set-Location $PSScriptRoot

# Очистка и сборка проекта
Write-Host "Cleaning project..."
& mvn clean -f pom.xml

Write-Host "Building project..."
& mvn package -DskipTests -f pom.xml

# Запуск приложения
Write-Host "Starting application..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-f","pom.xml" -WorkingDirectory $PSScriptRoot -WindowStyle Normal

Write-Host "Application is starting..."


