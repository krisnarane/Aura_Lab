param([string]$PlantUmlJar)
$ErrorActionPreference = 'Stop'
$hashEsperado = '4A01EA09B317180FB8E7EEF712DFDCA725409D2EE1919E4B5ADFE9D8362B6FE5'
if (!$PlantUmlJar) {
    $PlantUmlJar = Join-Path $env:TEMP 'aura-plantuml-1.2025.10.jar'
    if (!(Test-Path -LiteralPath $PlantUmlJar)) {
        Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2025.10/plantuml-1.2025.10.jar' -OutFile $PlantUmlJar
    }
}
$PlantUmlJar = (Resolve-Path -LiteralPath $PlantUmlJar).Path
if ((Get-FileHash -LiteralPath $PlantUmlJar -Algorithm SHA256).Hash -ne $hashEsperado) {
    throw 'O arquivo não corresponde ao PlantUML 1.2025.10 verificado.'
}
Push-Location $PSScriptRoot
try {
    & java -jar $PlantUmlJar -charset UTF-8 -tsvg -o svg '*.puml'
    if ($LASTEXITCODE -ne 0) { throw 'Há erros PlantUML. Corrija as fontes indicadas.' }
    Write-Output 'SVGs gerados. Abra index.html para consultar as figuras.'
} finally { Pop-Location }
