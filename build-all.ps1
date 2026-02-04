$services = @("auth-service", "driver-service", "payment-service", "user-service", "trip-service")
$tag = "latest"
$quayUser = "your-username"
$quayRepo = "your-repo"

$services | ForEach-Object -Parallel {
    $service = $_
    $imagePath = "quay.io/$using:quayUser/$using:quayRepo-$service`:$using:tag"
    Write-Host "[BUILD] $service building..." -ForegroundColor Cyan
    podman build -t $imagePath -f ".\$service\Dockerfile" . -q

    if ($LASTEXITCODE -ne 0)
    {
        return
    }

    Write-Host "[QUAY-PUSH] $service pushing to Quay.io..." -ForegroundColor Yellow
    podman push $imagePath
    Write-Host "[SUCCESS] $service ready to flight on Quay.io!" -ForegroundColor Green
}