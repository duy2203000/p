$ErrorActionPreference = 'Stop'
$ProjectPath = $PSScriptRoot
$GradleVersion = '8.11.1'
$ToolsPath = Join-Path $ProjectPath '.tools'
$GradleRoot = Join-Path $ToolsPath "gradle-$GradleVersion"
$GradleBin = Join-Path $GradleRoot 'bin\gradle.bat'

if (-not $env:JAVA_HOME) {
    $StudioJdk = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
    if (Test-Path (Join-Path $StudioJdk 'bin\java.exe')) { $env:JAVA_HOME = $StudioJdk }
    else { throw 'Hay cai Android Studio hoac JDK 17 va dat JAVA_HOME truoc khi chay.' }
}
if (-not $env:ANDROID_HOME) {
    $SdkPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $SdkPath) { $env:ANDROID_HOME = $SdkPath }
    else { throw 'Hay cai Android SDK trong Android Studio truoc. Xem HUONG-DAN.md.' }
}
if (-not (Test-Path (Join-Path $env:ANDROID_HOME 'platforms\android-35\android.jar'))) {
    throw 'Thieu Android SDK Platform 35. Mo Android Studio > SDK Manager va cai Android 15 (API 35).'
}
if (-not (Test-Path $GradleBin)) {
    New-Item -ItemType Directory -Force -Path $ToolsPath | Out-Null
    $Archive = Join-Path $ToolsPath "gradle-$GradleVersion-bin.zip"
    $DownloadUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
    Write-Host 'Dang tai Gradle tu trang chinh thuc...'
    Invoke-WebRequest -UseBasicParsing -Uri $DownloadUrl -OutFile $Archive
    $ExpectedHash = ([string](Invoke-RestMethod -Uri "$DownloadUrl.sha256")).Trim()
    $ActualHash = (Get-FileHash -Path $Archive -Algorithm SHA256).Hash
    if ($ActualHash -ine $ExpectedHash) { throw 'Kiem tra SHA256 khong khop. Dung tao APK.' }
    Expand-Archive -LiteralPath $Archive -DestinationPath $ToolsPath -Force
    Remove-Item -LiteralPath $Archive
}
& $GradleBin -p $ProjectPath --no-daemon wrapper --gradle-version $GradleVersion
if ($LASTEXITCODE -ne 0) { throw 'Khong tao duoc Gradle wrapper. Xem loi phia tren.' }
& $GradleBin -p $ProjectPath --no-daemon engineTest :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'Build khong thanh cong. Xem loi phia tren.' }
$OutputPath = Join-Path $ProjectPath 'GPS-DoDuong.apk'
Copy-Item (Join-Path $ProjectPath 'app\build\outputs\apk\debug\app-debug.apk') $OutputPath -Force
Write-Host "DA TAO APK: $OutputPath" -ForegroundColor Green
