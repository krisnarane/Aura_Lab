# Execução, testes e apresentação

## Pré-requisitos

- Java 21.
- Docker Desktop em execução.
- Chrome instalado para Selenium.
- Maven Wrapper incluído no repositório; não é necessário instalar Maven globalmente.
- Não é necessário instalar PostgreSQL localmente: Docker Compose fornece o banco da aplicação e Testcontainers fornece bancos descartáveis dos testes.

## Executar a aplicação

Na raiz do repositório:

```powershell
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:4200/admin/clientes`. As credenciais locais do banco de demonstração estão em `docker-compose.yml` e podem ser substituídas por `DB_URL`, `DB_USER` e `DB_PASSWORD`.

O PostgreSQL Docker do projeto usa `127.0.0.1:5433` no Windows e mantém a porta 5432 dentro do contêiner. Isso evita conflito com PostgreSQL instalado no Windows na porta 5432. O Spring usa essa porta 5433 por padrão. Se aparecer erro de senha, verifique primeiro o destino da conexão e eventuais variáveis `DB_URL`/`SPRING_DATASOURCE_URL`; não apague o volume nem redefina senhas antes de confirmar qual servidor respondeu.

## Executar os testes

Verificação completa em Chrome headless, com relatórios e matriz:

```powershell
cd backend
.\testar-crud.ps1
```

Apresentação com Chrome visível:

```powershell
cd backend
.\testar-crud.ps1 -Visivel
```

O script executa `clean verify`; portanto, testes unitários/de integração rodam pelo Surefire e os grupos `ClienteCrudIT`, `ClientePerfilIT`, `ProdutosCrudIT` e `VendasLojaIT` rodam pelo Failsafe. Não substitua PostgreSQL por H2 quando um teste falhar por falta de Docker.

Para validar somente a jornada do cliente escolhido no perfil:

```powershell
cd backend
.\mvnw.cmd clean test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClientePerfilIT" "-Dselenium.headless=true"
```

`ClientePerfilIT` cobre a seleção de cliente ativo, edição dos dados pessoais, troca isolada de senha, manutenção de endereço e cadastro de cartão mascarado, além das rejeições de dados pessoais, senha, endereço e cartão inválidos. Os oito cenários conferem a resposta visual e a persistência/auditoria no PostgreSQL de Testcontainers.

Para a demonstração da Gestão de Vendas, os testes funcionais Selenium ficam em `VendasLojaIT` e percorrem a jornada visual principal: catálogo `/`, sacola `/sacola`, checkout `/checkout/endereco` e confirmação `/checkout/confirmacao/:id`. Eles cobrem o roteiro mínimo do enunciado: carrinho com mais de um item e alteração de quantidade (RF0031/RF0032/RN0031), compra com endereço e cartão já cadastrados, compra com novo endereço e cartão incorporados ao perfil (RF0035/RF0036), pagamento com dois cartões respeitando o mínimo (RN0034), cartão abaixo de R$ 10,00 combinado com cupom (RN0035), cupons que superam a compra gerando cupom de troco (RN0036) e pedido finalizado com status EM PROCESSAMENTO (RF0038/RN0033). As regras de pagamento também são cobertas em nível de API por `VendaApiTest` (Surefire).

Na apresentação do RF0034, explique o critério de frete adotado: base de R$ 15,00 para o Sudeste, R$ 20,00 para o Sul e R$ 30,00 para os demais estados, acrescida de R$ 2,00 por unidade; pedidos com subtotal a partir de R$ 300,00 têm frete grátis. Essa fórmula é uma decisão do projeto porque o DRS exige o cálculo, mas não define seus valores.

```powershell
cd backend
.\mvnw.cmd clean test-compile failsafe:integration-test failsafe:verify "-Dit.test=VendasLojaIT" "-Dselenium.headless=true"
```

## Verificações registradas

Em 12/10/2026, na verificação dirigida aos casos CT026, CT032, CT035, CT038, CT039, CT041, CT042 e CT053:

- os oito novos casos Selenium foram aprovados no frontend existente;
- `ProdutosCrudIT`: cinco cenários aprovados, após repetir a classe por uma falha inicial de abertura de sessão do Chrome em um cenário antigo;
- `VendasLojaIT`: 15 cenários aprovados;
- `VendaApiTest`: 26 cenários aprovados, incluindo quatro verificações novas de endereço no resumo de frete;
- 46 cenários distintos aprovados nas três classes, com PostgreSQL 17.11 via Testcontainers e Chrome 154 headless;
- compilação e `git diff --check` concluídos sem erros;
- resultados dos oito CTs em `backend/target/evidencias/casos-plano.html`, com links para as capturas e relatórios XML.

Essa execução foi dirigida a produtos e vendas, não uma nova execução completa de todas as classes. A relação dos CTs com os métodos e o comando para executar somente os oito casos estão em `docs/crud-admin/CASOS-DE-TESTE.md`. CT041 e CT042 passam a ser classificados como sistema / funcional nesta automação Selenium.

Em 05/10/2026, após a integração da Gestão de Vendas:

- 83 cenários Selenium aprovados: 63 de clientes, 8 de perfil, 4 de produtos e 8 de vendas;
- 56 testes de apoio aprovados;
- total de 139 testes, sem falhas, erros ou ignorados;
- PostgreSQL 17.11 e oito migrações Flyway;
- RNF0011 com massa de 1.000 clientes: máximo de 936,63 ms na listagem, 271,39 ms no filtro combinado e 104,16 ms na busca por código;
- RNF0011 nas consultas de vendas, após carregamento em lote das coleções do pedido: máximo de 79,33 ms no carrinho, 34,83 ms nos cupons, 259,85 ms na lista de pedidos e 47,34 ms no detalhe;
- execução Selenium headless em Chrome 154.

Esses números descrevem aquela execução. Após qualquer mudança, use os relatórios novos em vez de repetir os valores como prova atual.

## Evidências geradas

| Artefato | Local |
| --- | --- |
| Matriz requisito → teste → resultado | `backend/target/evidencias/matriz.html` |
| Medições do RNF0011 | `backend/target/evidencias/performance.csv` |
| Relatório de testes de apoio | `backend/target/reports/surefire.html` |
| Relatório Selenium | `backend/target/reports/failsafe.html` |
| Capturas em falhas | `backend/target/screenshots/` |

`backend/target` é gerado e não deve ser versionado. Testes de apresentação devem manter os identificadores RF/RN/RNF nos nomes. Testes puramente arquiteturais ou de infraestrutura podem ser classificados como apoio para não fingir vínculo com uma regra do DRS.

## Verificação proporcional à mudança

- Java de domínio/service: teste unitário ou de integração afetado e, se alterar comportamento visível, Selenium correspondente.
- Controller/DTO/erros: `ClienteApiTest` e Selenium do fluxo.
- Migração/JPA: `MigrationPersistenceTest` e testes PostgreSQL relevantes.
- Frontend administrativo: teste JavaScript aplicável e Selenium.
- Ranking: `PoliticaRankingPorComprasTest`, integridade e cenários de faixa no navegador.
- UML: execute `docs/dvp-uml-2026/renderizar.ps1` e verifique `validacao.json`; isso não requer Docker.

Não execute repetidamente a suíte completa depois que ela passou, a menos que outra alteração relevante tenha sido feita.


## Operação do ciclo completo de vendas

Reservas: `aura.vendas.prazo-reserva=PT20M`, `aura.vendas.intervalo-job-ms=1000`, `aura.vendas.agendamento-ativo=true`. Testes desativam o job e injetam Clock controlado; chamam o processamento diretamente sem esperar minutos reais. Operadora usa APROVADO, REPROVADO ou FALHA; não há cobrança nem login.

Os detalhes de perfil e as páginas `/admin/pedidos`, `/admin/pedidos/:id`, `/admin/trocas` usam PostgreSQL. Para demonstração: comprar, validar no detalhe administrativo, despachar, confirmar entrega, solicitar troca no perfil, autorizar/receber na administração e usar o cupom no checkout seguinte. Não alterar o status diretamente no banco para demonstrar esse fluxo.

Quando o Docker CLI funcionar mas Testcontainers falhar no pipe padrão, confira `docker context inspect desktop-linux`. Neste ambiente o motor Linux usa `npipe:////./pipe/dockerDesktopLinuxEngine`. A verificação pode ser executada no PowerShell com:

```powershell
$env:DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'
.\mvnw.cmd '-Ddocker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy' verify
```

Isso muda somente a conexão do comando, sem reset de fábrica, remoção de volumes ou troca de PostgreSQL por H2. Não execute duas compilações Maven simultaneamente no mesmo `target`: a recompilação pode remover classes enquanto o outro processo testa.

Revisões acadêmicas: `docs/revisao-vendas/UC_Gestao_de_Vendas_Revisado.docx` e `DVP_Incremento_Vendas.docx`, renderizados e conferidos. A reestimativa por IDs da planilha está em `REESTIMATIVA-VENDAS.md`. As fontes originais permanecem como referências históricas; a nova revisão prevalece para este incremento. Confirmação do professor e aceitação manual do grupo são pendências externas.

## Resultado consolidado do incremento — 12/10/2026

Os últimos relatórios por suíte registram 84 testes backend: 82 aprovados e dois testes de desempenho reprovados, sem erros ou ignorados. Os 99 cenários Selenium estão aprovados (63 clientes, oito perfil, cinco produtos e 23 vendas). Os testes do frontend também passaram. São resultados acumulados por suíte, incluindo reexecuções dirigidas, não uma única execução simultânea de todas as classes.

RNF0011 permanece pendente: máximos de 1.646,51 ms em consultas de clientes e 1.027,18 ms em vendas, acima do limite de 1.000 ms. O usuário autorizou manter o desempenho atual e adiar a otimização. O limite dos testes não foi relaxado; não declarar a suíte completa como aprovada.

O registro dos cartões concluídos e pendentes está em `KANBAN-VENDAS.md`. Aceitação manual e comunicação das ambiguidades ao professor continuam pendentes.

## Clique no fluxo de troca — 19/10/2026

A execução de apresentação teve 23 cenários Selenium, com 22 aprovados e um erro `ElementClickIntercepted` no botão Solicitar troca (CT058/CT059). A captura mostra o formulário abaixo da área visível; as ações desse fluxo usavam clique direto. `VendasLojaIT` passou a reutilizar `InteracaoNavegador`, que centraliza o elemento, aguarda que esteja habilitado e desobstruído e executa um clique nativo do WebDriver. As asserções de negócio foram preservadas.

Para repetir somente o cenário de troca, a partir de `backend`, com Docker ativo:

```powershell
$env:DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=VendasLojaIT#*CT058*" "-Dselenium.headless=false" "-Ddocker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy"
```

O relatório da execução original com erro foi preservado em `backend/target/evidencias/reteste-clique-troca-2026-10-19/execucao-original-com-erro.xml`. A execução dirigida não substitui uma nova execução completa da suíte.

Reteste dirigido em 19/10/2026, Chrome 155 visível e PostgreSQL/Testcontainers: CT058/CT059 aprovado (um método, zero falhas, zero erros, zero ignorados), BUILD SUCCESS. Tempo total: 3 min 22 s. Incluiu compras, aprovação, despacho, entrega, troca parcial com reentrada, uso do cupom em nova compra e troca integral sem reentrada. Evidência: backend/target/evidencias/reteste-clique-troca-2026-10-19/reteste-aprovado.xml.
