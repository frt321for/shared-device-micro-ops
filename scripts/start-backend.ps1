$env:JWT_SECRET = 'dev-secret-key-please-change-in-production-32chr'
$env:DB_PASSWORD = 'iotops123'
Push-Location 'H:\iot\04-shared-device-micro-ops\src\backend\ops-app'
Write-Host 'Starting backend...'
mvn spring-boot:run '-Dspring-boot.run.profiles=dev' --log-file 'H:\iot\04-shared-device-micro-ops\scripts\backend-out.log'
Pop-Location