# Script to create databases for Flyway migration
# Run this before first docker compose up if databases don't exist

Write-Host "🐘 Creating PostgreSQL databases for microservices..." -ForegroundColor Cyan

# Wait for postgres to be ready
Write-Host "Waiting for PostgreSQL to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Create auth_service database
Write-Host "Creating auth_service database..." -ForegroundColor Green
docker exec -it postgres-db psql -U postgres -c "CREATE DATABASE auth_service;" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ auth_service database created successfully" -ForegroundColor Green
} else {
    Write-Host "⚠️  auth_service database may already exist" -ForegroundColor Yellow
}

# Create user_service database
Write-Host "Creating user_service database..." -ForegroundColor Green
docker exec -it postgres-db psql -U postgres -c "CREATE DATABASE user_service;" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ user_service database created successfully" -ForegroundColor Green
} else {
    Write-Host "⚠️  user_service database may already exist" -ForegroundColor Yellow
}

# List all databases
Write-Host "`n📊 Current databases:" -ForegroundColor Cyan
docker exec -it postgres-db psql -U postgres -c "\l"

Write-Host "`n✨ Database setup complete! You can now run: docker compose up --build" -ForegroundColor Green
