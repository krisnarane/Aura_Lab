# Casos de teste e comandos de execução — Aura Lab

Mapeamento entre requisitos do DRS (`DRS_LES_2_2026.docx`), casos de teste implementados e os comandos para executá-los. Os comandos partem do princípio de que o terminal está dentro de `backend/` e que o **Docker Desktop está em execução** (os testes usam PostgreSQL real via Testcontainers).

---

## 1. Execução completa (recomendada)

```powershell
docker compose up -d          # sobe o PostgreSQL da aplicação (demonstração)
cd backend
.\testar-crud.ps1             # suíte completa em Chrome headless
.\testar-crud.ps1 -Visivel    # mesma suíte com Chrome visível (para apresentação)
```

Roda `clean verify`: testes unitários/de apoio (Surefire) + cenários de interface (Failsafe/Selenium) e gera automaticamente a matriz de evidências em `target/evidencias/matriz.html`.

Consultas pontuais em JavaScript (store, adapters HTTP e utilitários do frontend):

```powershell
cd frontend
npm test
```

---

## 2. Cenários Selenium por grupo — módulo de clientes

Seletor base (a classe usa nomes de método prefixados pelos requisitos):

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#<PADRAO>" "-Dselenium.headless=true"
```

- Para ver a tela durante a execução, troque `true` por `false`.
- Para combinar cenários específicos, use `+` entre métodos: `#metodoA*+metodoB`.

### Grupo Cadastro (RF0021)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0021, RN0021, RN0022, RN0026, RNF0035 | `RF0021_RN0021_RN0026_RNF0035_cadastroPersistenteECodigos` | Cadastro válido gera código único, persiste, atende endereço de cobrança e entrega |
| RF0021, RN0023, RN0026 | `RF0021_RN0023_RN0026_obrigatorios` | Cada campo obrigatório vazio bloqueia o cadastro (parametrizado por campo) |
| RF0021 | `RF0021_validacoesEDuplicidadeInativos` | CPF inválido/duplicado, e-mail inválido/duplicado, formatos de telefone e CEP (parametrizado) |
| RF0021 | `RF0021_nascimentoFuturo` | Data de nascimento futura é recusada sem gravar |

```powershell
# Comando do grupo
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0021*" "-Dselenium.headless=true"
```

### Grupo Consulta (RF0024)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0024 | `RF0024_filtrosIsolados` | Cada campo identificador funciona sozinho como filtro (parcial em nome/e-mail, exato nos demais) |
| RF0024 | `RF0024_filtrosCombinadosVazioESituacao` | Filtros combinam com E, situação ativo/inativo, resultado vazio, `%` literal |
| RF0024 | `RF0024_nascimento` | Filtro por data de nascimento |
| RF0024 | `RF0024_interfaceResponsiva` | Listagem se adapta em largura de celular |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0024*" "-Dselenium.headless=true"
```

### Grupo Alteração (RF0022)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0022, RNF0012 | `RF0022_RNF0012_alteracaoPreservaVinculosEAudita` | Edição muda dados, preserva código/hash de senha/vínculos e grava auditoria antes/depois |
| RF0022 | `RF0022_duplicidadeNaoAltera` | E-mail já usado por outro cliente não altera nem audita como sucesso |
| RF0022 | `RF0022_dadosInvalidosELimites` | Limites de tamanho e formatos inválidos são recusados sem gravar |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0022*" "-Dselenium.headless=true"
```

### Grupo Inativação (RF0023)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0023, RF0025 | `RF0023_RF0025_cancelarConfirmarRepetir` | Cancelar não muda nada; confirmar inativa preservando histórico; repetir informa conflito |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0023*" "-Dselenium.headless=true"
```

### Grupo Histórico e ranking (RF0025)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0025, RN0027 | `RF0025_RN0027_detalhesHistoricoEVazio` | Detalhe de transações persistidas e cliente sem transações |
| RF0025, RN0027 | `RF0025_RN0027_faixasPeloNavegador` | Ranking reflete faixas de faturamento acumulado (parametrizado por valor) |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0025*" "-Dselenium.headless=true"
```

### Grupo Endereços (RF0026)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0026, RNF0034 | `RF0026_RNF0034_variosEnderecosEdicaoEPreferencia` | Vários endereços com apelido, edição isolada e preferência de entrega |
| RN0021, RN0022 | `RN0021_RN0022_protegeMinimos` | Último endereço de cobrança e o último de entrega ficam protegidos |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0026*+RN0021_RN0022_protegeMinimos" "-Dselenium.headless=true"
```

### Grupo Cartões (RF0027)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0027, RN0024, RN0025 | `RF0027_RN0024_RN0025_cartoesPreferenciaEMascara` | Cartão válido, preferencial único automático e exibição mascarada |
| RF0027, RN0024 | `RF0027_RN0024_recusaCartao` | Número inválido, titular vazio e CVV inválido não gravam (parametrizado) |
| RF0027, RN0025 | `RF0027_RN0025_bandeiraRetiradaEnquantoFormularioAberto` | Bandeira retirada do banco durante o uso recusa a gravação |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0027*" "-Dselenium.headless=true"
```

### Grupo Senha (RF0028)

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0028, RNF0031, RNF0032 | `RF0028_RNF0031_RNF0032_forcaEConfirmacao` | Todas as condições de senha fraca e confirmação divergente recusadas (parametrizado) |
| RF0028 | `RF0028_alteracaoIsolada` | Troca de senha sem editar os demais dados, com auditoria registrada |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0028*" "-Dselenium.headless=true"
```

---

## 3. Cenários Selenium — módulo de produtos

| Requisitos | Caso de teste | O que comprova |
| --- | --- | --- |
| RF0012, RN0015, RNF0012 | `RF0012_RN0015_RNF0012_inativacaoRegistraMotivoEAuditoria` | Inativação do produto exige categoria/motivo e grava auditoria |
| RF0016, RN0017, RNF0012 | `RF0016_RN0017_RNF0012_ativacaoCancelarConfirmarRepetir` | Ativação com motivo: cancelar, confirmar e repetir |
| RN0017 | `RN0017_justificativaObrigatoriaNaoAtiva` | Ativação sem justificativa não acontece |
| RF0011, RNF0021 | `RF0011_RNF0021_cadastroGeraCodigoEApareceNaLista` | Cadastro gera código único e aparece na listagem |

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ProdutosCrudIT" "-Dselenium.headless=true"
```

---

## 4. Testes de apoio (unitários e integração, sem navegador)

Seletor base:

```powershell
.\mvnw.cmd test "-Dtest=<CLASSE>"
```

| Classe | Requisitos | Casos |
| --- | --- | --- |
| `ClienteApiTest` | RF0021/RN0026/RNF0033, RNF0031, RF0024, RF0023 | Cadastro com código+hash, senha fraca recusada sem persistir, filtros combinados na API, inativação preservando cadastro |
| `ClienteIntegridadeTest` | RNF0012, RNF0033, RF0027, RF0021, RF0026, RN0027 | Rollback em falha de auditoria, segredos fora do banco, preferência e cadastro concorrentes, endereço inativo associado, ranking sem duplicar pagamentos |
| `ConsultaPerformanceTest` | RNF0011 | 90 consultas sobre 1.000 clientes abaixo de 1 segundo (gera `performance.csv`) |
| `PoliticaRankingPorComprasTest` | RN0027 | Fórmula da Strategy de ranking por faixas |
| `MigrationPersistenceTest` | Flyway V1–V6 | Reinício preserva dados, situação e motivos; massa novas migrações não perde o legado |
| `ArquiteturaTest` | Regra arquitetural (ArchUnit) | Controllers e mappers não acessam repositories diretamente |
| `ProdutoApiTest` | RF0015, RF0016/RN0017, RF0012/RN0015, RNF0021 | Consulta de produtos, ativação/inativação com motivo, código único de produto |

```powershell
# Exemplos
.\mvnw.cmd test "-Dtest=ClienteIntegridadeTest"
.\mvnw.cmd test "-Dtest=ConsultaPerformanceTest"
```

---

## 5. Evidências geradas

| Artefato | Local |
| --- | --- |
| Matriz requisito → teste → resultado | `backend/target/evidencias/matriz.html` |
| Medições RNF0011 | `backend/target/evidencias/performance.csv` |
| Relatório testes de apoio | `backend/target/reports/surefire.html` |
| Relatório Selenium | `backend/target/reports/failsafe.html` |
| Capturas de tela por cenário | `backend/target/screenshots/` |

Observações:

- Execuções isoladas com `failsafe` sobrescrevem os relatórios da mesma classe. Para a evidência consolidada, rode `.\testar-crud.ps1` ao final.
- Cada cenário Selenium reinicia a massa de testes; os dados não aparecem na aplicação da porta 4200, que usa outro banco (Docker Compose).
- Números históricos em `docs/ai/OPERACAO-E-TESTES.md` (63 cenários Selenium, 25 de apoio em 09/09/2026) são referência daquela execução; use sempre os relatórios gerados pela execução atual.
