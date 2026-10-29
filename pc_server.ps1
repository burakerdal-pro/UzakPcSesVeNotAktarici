$port = 5000
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://*:$port/")

try {
    $listener.Start()
} catch {
    Write-Host "[!] Hata: Port $port baslatilamadi. Lutfen PowerShell'i Yonetici olarak calistirin." -ForegroundColor Red
    exit
}

$localIp = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" }).IPAddress | Select-Object -First 1
$desktop = [Environment]::GetFolderPath("Desktop")

Write-Host "============================================================" -ForegroundColor Green
Write-Host "      PC NOT DEFTERI & SES SUNUCUSU (PowerShell)" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Bilgisayarinizin Yerel IP Adresi : $localIp" -ForegroundColor Yellow
Write-Host "Dinlenen Port                     : $port" -ForegroundColor Yellow
Write-Host "Dosyalarin Kaydedilecegi Klasor   : $desktop" -ForegroundColor Yellow
Write-Host "`nTelefondaki uygulamaya IP adresi olarak '$localIp' girin."
Write-Host "Sunucu calisiyor... Durdurmak icin bu pencereyi kapatin.`n"

while ($listener.IsListening) {
    try {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        if ($request.HttpMethod -eq "GET" -and $request.Url.AbsolutePath -eq "/ping") {
            $buf = [System.Text.Encoding]::UTF8.GetBytes('{"status":"ok","message":"PC Sunucusu Aktif!"}')
            $response.ContentType = "application/json; charset=utf-8"
            $response.OutputStream.Write($buf, 0, $buf.Length)
            $response.Close()
            continue
        }

        if ($request.HttpMethod -eq "POST" -and $request.Url.AbsolutePath -eq "/note") {
            $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
            $jsonStr = $reader.ReadToEnd()
            $reader.Close()

            $json = $jsonStr | ConvertFrom-Json

            $filename = if ($json.filename) { [System.IO.Path]::GetFileName($json.filename) } else { "notlar.txt" }
            if ([string]::IsNullOrWhiteSpace($filename)) { $filename = "notlar.txt" }

            $filePath = Join-Path $desktop $filename
            $content = $json.content

            if ($json.append -and (Test-Path $filePath)) {
                Add-Content -Path $filePath -Value "`n$content" -Encoding UTF8
            } else {
                Set-Content -Path $filePath -Value $content -Encoding UTF8
            }

            Write-Host "[+] Not kaydedildi: $filePath" -ForegroundColor Cyan

            if ($json.open_notepad) {
                Start-Process "notepad.exe" $filePath
            }

            $resObj = @{
                status = "success"
                message = "Not Masaustu'ndeki '$filename' dosyasina kaydedildi!"
            }
            $resJson = $resObj | ConvertTo-Json -Compress
            $buf = [System.Text.Encoding]::UTF8.GetBytes($resJson)
            $response.ContentType = "application/json; charset=utf-8"
            $response.OutputStream.Write($buf, 0, $buf.Length)
            $response.Close()

        } elseif ($request.HttpMethod -eq "POST" -and $request.Url.AbsolutePath -eq "/audio") {
            $reader = New-Object System.IO.StreamReader($request.InputStream, [System.Text.Encoding]::UTF8)
            $jsonStr = $reader.ReadToEnd()
            $reader.Close()

            $json = $jsonStr | ConvertFrom-Json

            $filename = if ($json.filename) { [System.IO.Path]::GetFileName($json.filename) } else { "ses_kaydi.m4a" }
            if ([string]::IsNullOrWhiteSpace($filename)) { $filename = "ses_kaydi.m4a" }

            $filePath = Join-Path $desktop $filename
            $audioBytes = [System.Convert]::FromBase64String($json.audio_data)

            [System.IO.File]::WriteAllBytes($filePath, $audioBytes)

            Write-Host "[+] Ses kaydi kaydedildi: $filePath" -ForegroundColor Green

            Start-Process $filePath

            $resObj = @{
                status = "success"
                message = "Ses kaydi Masaustu'ne '$filename' olarak kaydedildi!"
            }
            $resJson = $resObj | ConvertTo-Json -Compress
            $buf = [System.Text.Encoding]::UTF8.GetBytes($resJson)
            $response.ContentType = "application/json; charset=utf-8"
            $response.OutputStream.Write($buf, 0, $buf.Length)
            $response.Close()
        } else {
            $response.StatusCode = 404
            $response.Close()
        }
    } catch {
        Write-Host "[!] Bir istek islenirken hata olustu: $_" -ForegroundColor Red
    }
}
