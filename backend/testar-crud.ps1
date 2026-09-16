param([switch]$Visivel)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
  $headless = if ($Visivel) { 'false' } else { 'true' }
  & .\mvnw.cmd clean verify "-Dselenium.headless=$headless"
  $resultado = $LASTEXITCODE
  & .\mvnw.cmd surefire-report:report-only surefire-report:failsafe-report-only
  $pasta = Join-Path $PSScriptRoot 'target/evidencias'
  New-Item -ItemType Directory -Force $pasta | Out-Null
  $linhas = foreach ($arquivo in Get-ChildItem 'target/surefire-reports/TEST-*.xml','target/failsafe-reports/TEST-*.xml' -ErrorAction SilentlyContinue) {
    [xml]$relatorio = Get-Content -LiteralPath $arquivo.FullName -Raw
    foreach ($teste in $relatorio.testsuite.testcase) {
      $ids = ([regex]::Matches($teste.name, '(?:RF|RNF|RN)\d{4}') | ForEach-Object Value | Select-Object -Unique) -join ', '
      $status = if ($teste.failure -or $teste.error) { 'FALHOU' } elseif ($teste.skipped) { 'IGNORADO' } else { 'PASSOU' }
      $metodo = ($teste.name -split '\(')[0]
      $capturas = @(Get-ChildItem "target/screenshots/$metodo-*.png" -ErrorAction SilentlyContinue)
      if ($teste.name -match '\[(\d+)\]') {
        $indice = $Matches[1]
        $capturas = @($capturas | Where-Object Name -Like "*-_${indice}__*")
      }
      $links = @('<a href="../reports/surefire.html">JUnit</a>','<a href="../reports/failsafe.html">Selenium</a>')
      $links += @($capturas | ForEach-Object { '<a href="../screenshots/{0}">Captura</a>' -f [System.Uri]::EscapeDataString($_.Name) })
      '<tr><td>{0}</td><td>{1}</td><td>{2}</td><td>{3}</td><td>{4}</td></tr>' -f [System.Net.WebUtility]::HtmlEncode($ids),[System.Net.WebUtility]::HtmlEncode("$($teste.classname).$($teste.name)"),$status,$teste.time,($links -join ' · ')
    }
  }
  $html = '<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>Evidências do CRUD</title><style>body{font:15px system-ui;margin:32px}table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:8px;text-align:left}</style><h1>Requisito → teste → evidência</h1><table><tr><th>Requisitos</th><th>Teste</th><th>Resultado</th><th>Segundos</th><th>Evidência</th></tr>' + ($linhas -join "`n") + '</table></html>'
  Set-Content -LiteralPath (Join-Path $pasta 'matriz.html') -Value $html -Encoding utf8
  if ($resultado -ne 0) { throw 'Há testes com falha. Consulte target/evidencias/matriz.html e os relatórios Maven.' }
} finally { Pop-Location }
