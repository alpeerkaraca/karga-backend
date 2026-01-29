# ==========================================
# KARGA MICROSERVICES - BUILD & DEPLOY SCRIPT
# ==========================================

# Hata durumunda işlemi durdur
$ErrorActionPreference = "Stop"

# Servis Listesi
$services = @(
    "auth-service",
    "driver-service",
    "payment-service",
    "user-service",
    "trip-service"
)

# Versiyon etiketi
$tag = "latest"

# Renkli yazdırma fonksiyonu
function Log-Info
{
    param([string]$msg)
    Write-Host "[INFO] $msg" -ForegroundColor Cyan
}

function Log-Success
{
    param([string]$msg)
    Write-Host "[SUCCESS] $msg" -ForegroundColor Green
}

# ------------------------------------------
# ADIM 1: Karga Common Build (Maven Install)
# ------------------------------------------
Log-Info "1. ADIM: karga-common derleniyor ve .m2'ye ekleniyor..."

Set-Location "karga-common"
try
{
    # Testleri atlayarak hızlı build alıyoruz (-DskipTests)
    cmd /c "mvn clean install -DskipTests"
    if ($LASTEXITCODE -ne 0)
    {
        throw "Maven build hatası"
    }
}
catch
{
    Write-Error "karga-common derlenirken hata oluştu!"
    exit 1
}
Set-Location ..
Log-Success "karga-common başarıyla kuruldu."

# ------------------------------------------
# ADIM 2: Servisleri Döngüyle Build Et
# ------------------------------------------
foreach ($service in $services)
{
    Write-Host "--------------------------------------------------"
    Log-Info "$service İŞLENİYOR..."

    # B) Podman Build
    $imageName = "karga/$service`:$tag"
    Log-Info "Podman imajı oluşturuluyor: $imageName"

    # Senin verdiğin komut formatı: Root'tan çalıştırıyoruz
    podman build -t $imageName -f ".\$service\Dockerfile" .
    if ($LASTEXITCODE -ne 0)
    {
        Write-Error "$service Podman build hatası!"
        exit 1
    }

    # C) Minikube'e Yükleme
    Log-Info "İmaj Minikube ortamına taşınıyor (TAR yöntemi ile)..."

    # 1. İmajı geçici bir .tar dosyasına kaydet
    $tarFile = "$service.tar"
    podman save -o $tarFile $imageName

    # 2. .tar dosyasını Minikube'e yükle (Bu dosya yolundan okur, daemon aramaz)
    minikube image load $tarFile

    # 3. Temizlik
    Remove-Item $tarFile

    Log-Success "$service Minikube'e başarıyla yüklendi!"
}

Write-Host "--------------------------------------------------"
Log-Success "TÜM İŞLEMLER BAŞARIYLA TAMAMLANDI! 🚀"
Log-Info "Podları güncellemek için: kubectl rollout restart deployment <deployment-adi>"