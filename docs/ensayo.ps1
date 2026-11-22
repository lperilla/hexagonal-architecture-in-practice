# Funciones de apoyo para ensayar y dar la charla.
# Uso, desde la raiz del repositorio en PowerShell:   . .\docs\ensayo.ps1
# Despues: Get-Command -Noun Charla*,Etiqueta*,Adopcion*,Notificaciones*,Infraestructura*,Servicios*,Demo*,Consolas*
#
# El codigo en vivo se hace en una copia de trabajo aparte (git worktree), junto al repositorio:
# asi la presentacion de docs/ sigue abierta aunque se haga checkout de etiquetas antiguas.

$script:Repo = Split-Path -Parent $PSScriptRoot
$script:Live = Join-Path (Split-Path -Parent $script:Repo) 'hexagonal-live'
$script:Jdk = 'C:\dev-tools\java\jdk-25.0.4.1'
$script:SocketDir = 'C:\lperilla\tmp'
$script:Procesos = @{}
$script:Etiquetas = @('00-start', '01-domain', '02-input-port', '03-output-ports', '04-mongodb-adapter',
    '05-rabbitmq-adapter', '06-rest-adapter', '07-soap-adapter', '08-testing', '09-final', 'hexagonal/fase-2-notificaciones')

function Initialize-Charla {
    <# Java 25 y la carpeta de sockets AF_UNIX fuera de %TEMP%, solo para esta sesion de PowerShell. #>
    New-Item -ItemType Directory -Force $script:SocketDir | Out-Null
    $env:JAVA_HOME = $script:Jdk
    $env:Path = "$($script:Jdk)\bin;$env:Path"
    $env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF8 -Djdk.net.unixdomain.tmpdir=$($script:SocketDir)"
    $version = (& "$($script:Jdk)\bin\java.exe" -version 2>&1 | Select-Object -Skip 1 -First 1)
    Write-Host "Java: $version"
    Write-Host "Repositorio: $($script:Repo)"
    Write-Host "Copia en vivo: $($script:Live)"
}

function Start-Infraestructura {
    <# Arranca la maquina de Podman (con el arreglo del socket huerfano) y levanta compose.yaml. #>
    $estado = podman machine inspect --format '{{.State}}' 2>$null
    if ($estado -ne 'running') {
        $temp = Join-Path $env:TEMP 'podman'
        if (Test-Path $temp) {
            $destino = "podman.stale-$(Get-Date -Format yyyyMMddHHmmss)"
            Rename-Item $temp $destino
            Write-Host "Socket huerfano apartado: %TEMP%\$destino"
        }
        podman machine start
    }
    podman compose -f (Join-Path $script:Repo 'compose.yaml') up -d
    $limite = (Get-Date).AddSeconds(90)
    do {
        Start-Sleep -Seconds 2
        $sanos = @(podman ps --filter name=petstore --filter health=healthy -q).Count
    } while ($sanos -lt 2 -and (Get-Date) -lt $limite)
    podman ps --filter name=petstore --format "table {{.Names}}\t{{.Status}}"
}

function New-CopiaEnVivo {
    <# Crea la copia de trabajo del codigo en vivo en ..\hexagonal-live, en la etiqueta 00-start. #>
    if (Test-Path $script:Live) {
        Write-Host "Ya existe: $($script:Live)"
    } else {
        git -C $script:Repo worktree add --detach $script:Live 00-start
    }
}

function Use-Etiqueta {
    <# Pone la copia en vivo en una etiqueta. Ejemplo: Use-Etiqueta 04-mongodb-adapter #>
    param([Parameter(Mandatory)][string]$Etiqueta)
    $cambios = git -C $script:Live status --porcelain
    if ($cambios) {
        Write-Warning 'Hay cambios sin guardar en la copia en vivo. Descartalos con Reset-CopiaEnVivo o guardalos antes.'
        return
    }
    git -C $script:Live switch --detach --quiet $Etiqueta
    git -C $script:Live log --oneline -1 --decorate
}

function Reset-CopiaEnVivo {
    <# Descarta lo que se haya escrito en directo en la copia en vivo. #>
    git -C $script:Live reset --hard --quiet
    git -C $script:Live clean -fdq
    git -C $script:Live log --oneline -1 --decorate
}

function Show-Paso {
    <# Resume lo que cambia una etiqueta respecto a la anterior. Ejemplo: Show-Paso 06-rest-adapter #>
    param([Parameter(Mandatory)][string]$Etiqueta)
    $i = [array]::IndexOf($script:Etiquetas, $Etiqueta)
    if ($i -le 0) { Write-Warning "Etiqueta sin anterior en la lista: $Etiqueta"; return }
    $anterior = $script:Etiquetas[$i - 1]
    if ($Etiqueta -eq 'hexagonal/fase-2-notificaciones') { $anterior = '09-final' }
    Write-Host "$anterior -> $Etiqueta" -ForegroundColor Cyan
    git -C $script:Repo diff --stat $anterior $Etiqueta
}

function Build-EnVivo {
    <# Compila la copia en vivo sin conexion. -Tests ejecuta tambien los tests (necesita compose en marcha). #>
    param([switch]$Tests, [switch]$Online)
    $argumentos = @('-B', '-q', '-s', '.mvn/settings.xml')
    if (-not $Online) { $argumentos += '-o' }
    if ($Tests) { $argumentos += 'verify' } else { $argumentos += @('-DskipTests', 'package') }
    Push-Location $script:Live
    try {
        & .\mvnw.cmd @argumentos
        if ($LASTEXITCODE -eq 0) { Write-Host 'BUILD SUCCESS' -ForegroundColor Green } else { Write-Host 'BUILD FAILURE' -ForegroundColor Red }
    } finally { Pop-Location }
}

function Test-Modulo {
    <# Ejecuta los tests de un modulo en la copia en vivo, sin conexion. Ejemplo: Test-Modulo services/adoption-service/domain #>
    param([Parameter(Mandatory)][string]$Modulo)
    Push-Location $script:Live
    try {
        & .\mvnw.cmd -B -o -s .mvn/settings.xml -pl $Modulo -am clean test 2>&1 |
            Select-String -Pattern 'Tests run:|BUILD|ERROR' | ForEach-Object { $_.Line }
    } finally { Pop-Location }
}

function Get-JarEnVivo([string]$Servicio) {
    $boot = Join-Path $script:Live "services\$Servicio\boot\target\$Servicio-0.0.1-SNAPSHOT.jar"
    $capas = Join-Path $script:Live "services\$Servicio\target\$Servicio-0.0.1-SNAPSHOT.jar"
    if (Test-Path $boot) { return $boot }
    if (Test-Path $capas) { return $capas }
    throw "No hay JAR de $Servicio en la copia en vivo: ejecuta Build-EnVivo"
}

function Start-ServicioJar([string]$Nombre, [string]$Jar, [string[]]$Extra) {
    Stop-ServicioJar $Nombre
    $log = Join-Path $script:SocketDir "$Nombre.log"
    $argumentos = @('-jar', $Jar) + $Extra
    $p = Start-Process -FilePath "$($script:Jdk)\bin\java.exe" -ArgumentList $argumentos -RedirectStandardOutput $log `
        -RedirectStandardError "$log.err" -PassThru -WindowStyle Hidden
    $script:Procesos[$Nombre] = $p
    $limite = (Get-Date).AddSeconds(90)
    do {
        Start-Sleep -Milliseconds 500
        # [string]: con el fichero vacio, Get-Content devuelve un array y -notmatch filtraria en lugar de comparar
        $texto = [string](Get-Content $log -Raw -ErrorAction SilentlyContinue)
    } while ($texto -notmatch 'Started \w+Application|APPLICATION FAILED|Application run failed' -and (Get-Date) -lt $limite)
    if ($texto -match 'Started \w+Application') { Write-Host "$Nombre en marcha (log: $log)" -ForegroundColor Green }
    else { Write-Host "$Nombre no ha arrancado: revisa $log" -ForegroundColor Red }
}

function Stop-ServicioJar([string]$Nombre) {
    if ($script:Procesos.ContainsKey($Nombre)) {
        Stop-Process -Id $script:Procesos[$Nombre].Id -Force -ErrorAction SilentlyContinue
        $script:Procesos.Remove($Nombre)
    }
}

function Start-Adopcion {
    <# Arranca adoption-service desde la copia en vivo. -InMemory usa el perfil in-memory (desde 08-testing). #>
    param([switch]$InMemory)
    $extra = @()
    if ($InMemory) { $extra = @('--spring.profiles.active=in-memory') }
    Start-ServicioJar 'adopcion' (Get-JarEnVivo 'adoption-service') $extra
}

function Start-Notificaciones {
    <# Arranca notification-service desde la copia en vivo. -SoloLog usa el perfil log-notifications. #>
    param([switch]$SoloLog)
    $extra = @()
    if ($SoloLog) { $extra = @('--spring.profiles.active=log-notifications') }
    Start-ServicioJar 'notificaciones' (Get-JarEnVivo 'notification-service') $extra
}

function Stop-Servicios {
    <# Para los servicios arrancados con estas funciones. #>
    foreach ($nombre in @($script:Procesos.Keys)) { Stop-ServicioJar $nombre }
    Write-Host 'Servicios parados'
}

function Reset-Demo {
    <# Vuelve a las tres mascotas disponibles: borra la base de datos de la demo. Reinicia despues adoption-service. #>
    podman exec petstore-mongodb mongosh --quiet petstore-adoption --eval 'db.dropDatabase().ok' | Out-Null
    Write-Host 'Base de datos petstore-adoption borrada: reinicia adoption-service para cargar Luna, Thor y Kira'
}

function Invoke-Adopcion {
    <# Adopta por REST y muestra la respuesta completa. Ejemplo: Invoke-Adopcion 1 Ana #>
    param([Parameter(Mandatory)][string]$PetId, [string]$Dueno = 'Ana')
    $cuerpo = Join-Path $script:SocketDir 'adopcion.json'
    [IO.File]::WriteAllText($cuerpo, "{`"ownerName`":`"$Dueno`"}")
    curl.exe -s -i -X POST "http://localhost:8080/pets/$PetId/adoption" -H 'Content-Type: application/json' --data-binary "@$cuerpo"
    Write-Host ''
}

function Invoke-AdopcionSoap {
    <# Envia un sobre de soap-requests. Ejemplo: Invoke-AdopcionSoap 01-adopt-pet #>
    param([Parameter(Mandatory)][string]$Sobre)
    $fichero = Join-Path $script:Repo "soap-requests\$Sobre.xml"
    curl.exe -s -X POST 'http://localhost:8080/ws' -H 'Content-Type: text/xml; charset=utf-8' --data-binary "@$fichero" -w "`nHTTP %{http_code}`n"
}

function Open-Charla {
    <# Abre la presentacion en el navegador (funciona sin conexion). #>
    Start-Process (Join-Path $script:Repo 'docs\charla-arquitectura-hexagonal.html')
}

function Open-Consolas {
    <# Abre Mailpit y la consola de RabbitMQ (usuario y clave: petstore). #>
    Start-Process 'http://localhost:8025'
    Start-Process 'http://localhost:15672'
}

function Test-Etiquetas {
    <# Verifica todas las etiquetas en la copia en vivo con clean verify. Tarda unos 8 minutos. #>
    foreach ($etiqueta in $script:Etiquetas) {
        git -C $script:Live switch --detach --quiet $etiqueta
        Push-Location $script:Live
        try {
            & .\mvnw.cmd -B -q -s .mvn/settings.xml clean verify *> $null
            if ($LASTEXITCODE -eq 0) { Write-Host "${etiqueta}: OK" -ForegroundColor Green } else { Write-Host "${etiqueta}: FALLA" -ForegroundColor Red }
        } finally { Pop-Location }
    }
    git -C $script:Live switch --detach --quiet 00-start
}

Write-Host 'Funciones de la charla cargadas. Empieza por: Initialize-Charla' -ForegroundColor Cyan
