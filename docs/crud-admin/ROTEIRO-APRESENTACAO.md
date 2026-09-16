# Roteiro de apresentação — CRUD de clientes

## 1. Preparação antes da aula

Reserve aproximadamente 20 minutos para ensaio e ajuste o tempo ao limite dado pelo professor. Uma execução completa anterior levou cerca de 13 minutos, incluindo inicialização e downloads; não prometa esse mesmo tempo em outra máquina.

1. Abra Docker Desktop e confirme que terminou de iniciar.
2. Execute a suíte completa antes da aula para baixar dependências e conferir o ambiente.
3. Deixe abertos este roteiro, o relatório Selenium, a matriz de evidências e os arquivos de código da seção 5.
4. Confirme a versão do DRS: o arquivo disponível é `DRS_LES_2_2026.docx`, enquanto o enunciado menciona `DRS_LES_1_2026`.

No PowerShell, na raiz do projeto:

```powershell
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

Abra `http://localhost:4200/admin/clientes`. O banco Docker usa `127.0.0.1:5433`; a porta interna do PostgreSQL é 5432. Mantenha o terminal da aplicação aberto.

Em outro terminal, para ensaiar a apresentação completa:

```powershell
cd C:\Users\jukia\OneDrive\Desktop\Aura_Lab\backend
.\testar-crud.ps1 -Visivel
```

O Selenium abre seu próprio Chrome e inicia outra instância da aplicação em uma porta aleatória, com PostgreSQL exclusivo de testes. Os registros criados nos testes não aparecem na aplicação da porta 4200. Cada cenário reinicia sua massa; não depende do cadastro realizado no cenário anterior.

Não use `docker compose down -v`: isso apaga o volume da demonstração. Não execute scripts de limpeza da massa de testes contra o banco da aplicação.

## 2. Abertura — aproximadamente 1 minuto

Mostre a listagem administrativa e diga:

> “Esta entrega implementa a manutenção de clientes, RF0021 a RF0028: cadastro, consulta, alteração, inativação, endereços, cartões, senha e histórico. O frontend usa uma API Spring Boot com PostgreSQL. A comprovação será feita por testes Selenium, que preenchem e acionam a interface automaticamente.”

> “Não há autenticação nesta etapa. ADMIN_DEMO identifica o contexto técnico da auditoria. O RF0023 do documento disponível exige inativação: o registro e seus vínculos permanecem no banco.”

Identifique visualmente o botão de cadastro, os filtros, a situação e os links de consulta/edição. Essa navegação serve para orientar o professor; a comprovação das operações começa com o Selenium.

## 3. Fluxo de demonstração pelo navegador

Use a ordem abaixo para explicar o módulo. Na execução completa, o JUnit não garante essa ordem; acompanhe o cenário exibido. Para apresentar exatamente por blocos, use os comandos da seção 4.

Antes de cada bloco, fale o requisito e o resultado esperado. Depois, mostre a mensagem/resultado da interface e a asserção do teste correspondente.

| Bloco | O que o Selenium faz no frontend | O que explicar | RF/RN/RNF |
| --- | --- | --- | --- |
| 1. Cadastro válido | Abre “Cadastrar novo cliente”, preenche dados pessoais, senha/confirmacão e endereço, salva e verifica a listagem; reabre a página | O cliente recebe código único e o endereço inicial atende cobrança e entrega. A consulta posterior confirma persistência | RF0021, RN0021–RN0023, RN0026, RNF0035 |
| 2. Cadastro inválido | Envia formulário com campos ausentes, formatos inválidos, duplicidade ou nascimento futuro | O erro impede a gravação. Mostre a mensagem; validações adicionais de CPF/CEP/duplicidade são decisões complementares, sem inventar IDs de RN | RF0021, RN0023, RN0026 |
| 3. Consulta | Filtra por nome, CPF, código e demais campos; combina filtros; consulta inativos e resultado vazio | Filtros combinam com E. Nome/e-mail são parciais; código é exato. Por padrão aparecem ativos e inativos | RF0024 |
| 4. Alteração | Edita Marina, salva, consulta detalhe e tenta e-mail duplicado | Dados pessoais mudam, mas código, senha e vínculos permanecem. Tentativa inválida não produz alteração nem auditoria de sucesso | RF0022, RNF0012 |
| 5. Endereços | Adiciona Trabalho, edita complemento, muda preferência e tenta inativar o último endereço obrigatório | Pode haver vários endereços com apelido. Cobrança e entrega precisam manter pelo menos um endereço ativo cada | RF0026, RN0021–RN0023, RNF0034 |
| 6. Cartões | Adiciona cartões fictícios, troca preferência, inativa o preferencial e testa entradas inválidas | A bandeira deve existir. Há um preferencial entre ativos; a interface mostra somente dados mascarados | RF0027, RN0024–RN0025 |
| 7. Senha | Testa cada condição de força e confirmação divergente; depois altera somente a senha | Não exige editar todo o cadastro. O banco armazena hash BCrypt; não abra nem exponha hashes na apresentação | RF0028, RNF0031–RNF0033 |
| 8. Histórico e ranking | Abre detalhes das transações, cliente sem transações e faixas de ranking | As transações são exemplos persistidos. A fórmula de ranking é decisão do projeto e considera pedidos efetivados, sem somar pagamentos novamente | RF0025, RN0027 |
| 9. Inativação | Cancela a confirmação, confirma depois e tenta repetir | Cancelar não altera nada. Confirmar muda a situação e preserva histórico/endereço. Repetir apresenta conflito compreensível | RF0023, RF0025 |
| 10. Auditoria | Consulta o registro da alteração no detalhe | A escrita registra operação, instante, ator e antes/depois. Segredos não entram nos snapshots. Rollback é comprovado pelos testes de apoio | RNF0012 |

Exemplo de fala antes do teste de cadastro:

> “Este cenário valida RF0021 e RN0026. O Selenium vai preencher os dados obrigatórios e o endereço residencial. Depois de salvar, o teste verifica o nome e o código na lista e reabre a página para confirmar que o registro continua disponível.”

Exemplo de fala antes da inativação:

> “Neste cenário RF0023, primeiro cancelamos e confirmamos que o cliente continua ativo. Depois confirmamos a inativação e verificamos que seu histórico permanece. Uma segunda tentativa deve informar que ele já está inativo.”

## 4. Como executar por blocos

A opção mais simples é `.\testar-crud.ps1 -Visivel`, que roda tudo e gera a matriz automaticamente. Se houver pouco tempo, combine previamente com o professor quais cenários executar ao vivo e leve o relatório completo recente; uma seleção não comprova sozinha toda a cobertura.

Para controlar a ordem da fala, execute os comandos abaixo **um por vez**, dentro de `backend`. Eles compilam os fontes e executam os testes de interface selecionados com Chrome visível, sem repetir a suíte de apoio a cada bloco:

```powershell
# Cadastro: sucesso e principais validacoes
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0021*" "-Dselenium.headless=false"

# Consulta
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0024*" "-Dselenium.headless=false"

# Alteracao
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0022*" "-Dselenium.headless=false"

# Enderecos e protecao dos minimos
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0026*+RN0021_RN0022_protegeMinimos" "-Dselenium.headless=false"

# Cartoes
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0027*" "-Dselenium.headless=false"

# Senha
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0028*" "-Dselenium.headless=false"

# Historico e ranking
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0025*" "-Dselenium.headless=false"

# Inativacao
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClienteCrudIT#RF0023*" "-Dselenium.headless=false"
```

Cada comando inicializa novamente o ambiente de testes; executar todos separadamente pode demorar mais que a suíte completa. Os relatórios da mesma classe são sobrescritos pelas execuções parciais. Para obter novamente a evidência completa consolidada, execute `testar-crud.ps1` ao final. A matriz gerada anteriormente não se atualiza sozinha com esses comandos parciais.

## 5. Código — aproximadamente 4 a 6 minutos

Escolha o cadastro como exemplo e acompanhe a requisição. Não é necessário ler todas as classes.

| Ordem | Arquivo e trecho | Fala sugerida |
| --- | --- | --- |
| 1 | [admin-clientes.js](../../frontend/js/admin-clientes.js), submissão de `form-cliente` | “A tela transforma os campos em dados e chama a operação cadastrar.” |
| 2 | [clientesService.js](../../frontend/js/clientesService.js) e [clientesApi.js](../../frontend/js/clientesApi.js), `cadastrar` | “A fachada expõe as operações; o adaptador concentra HTTP e erros.” |
| 3 | [ClienteController.java](../../backend/src/main/java/br/com/auralab/clientes/api/ClienteController.java), `POST /clientes` | “O controller recebe e valida o DTO e delega o caso de uso.” |
| 4 | [ClienteService.java](../../backend/src/main/java/br/com/auralab/clientes/service/ClienteService.java), `cadastrar` | “O service aplica regras, cria cliente e endereço e grava a auditoria na mesma transação.” |
| 5 | [Cliente.java](../../backend/src/main/java/br/com/auralab/clientes/domain/Cliente.java) e [ClienteRepository.java](../../backend/src/main/java/br/com/auralab/clientes/repository/ClienteRepository.java) | “A entidade representa os dados e vínculos. Spring Data concentra o acesso ao banco.” |
| 6 | [ClienteCrudIT.java](../../backend/src/test/java/br/com/auralab/e2e/ClienteCrudIT.java), `RF0021_RN0021_RN0022_RN0026_RNF0035_cadastroPersistenteECodigos` | “Aqui estão a ação no navegador e as verificações de nome, código, consulta posterior e endereço.” |
| 7 | [ClientesAdminPage.java](../../backend/src/test/java/br/com/auralab/e2e/pages/ClientesAdminPage.java), `cadastro` e `esperar` | “Page Objects centralizam seletores e ações. Usamos esperas explícitas para aguardar a interface.” |

Em seguida, mostre rapidamente três regras relevantes:

- [EnderecoService.java](../../backend/src/main/java/br/com/auralab/clientes/service/EnderecoService.java): proteção dos mínimos de cobrança/entrega.
- [ClienteService.java](../../backend/src/main/java/br/com/auralab/clientes/service/ClienteService.java), `inativar`: altera situação e registra auditoria; não remove o cliente.
- [AuditoriaService.java](../../backend/src/main/java/br/com/auralab/clientes/service/AuditoriaService.java): exige transação existente. Em `ClienteIntegridadeTest`, mostre o cenário `RNF0012_falhaAuditoriaReverteCadastro` para explicar rollback.

Se o professor perguntar sobre padrões adicionais:

- **Strategy:** `PoliticaRankingCliente` / `PoliticaRankingPorCompras` isolam o cálculo.
- **Specification:** `ClienteSpecifications` combina filtros.
- **DTOs:** `ClienteInput` e `ClienteAlteracaoInput` evitam usar a entidade diretamente como contrato de entrada.
- **Bean Validation e políticas:** campos são validados nos DTOs e regras reutilizadas ficam em `ValidadorCliente` e services.
- **Flyway:** V1–V4 versionam banco e massa demonstrativa; novas mudanças exigem nova migração.

## 6. Evidências e encerramento — aproximadamente 2 minutos

Depois da execução completa, abra:

1. `backend/target/evidencias/matriz.html`: procure RF0021, RF0023 e RN0024 e mostre os testes associados.
2. `backend/target/reports/failsafe.html`: confira total de cenários, falhas, erros e ignorados.
3. `backend/target/reports/surefire.html`: mostre os testes de BCrypt, integridade, concorrência, rollback, migração e ranking.
4. `backend/target/evidencias/performance.csv`: explique 1.000 clientes, aquecimento e 30 medições de cada uma das três consultas, totalizando 90.

A referência verificada em 09/09/2026 foi de 63 cenários Selenium e 25 testes de apoio aprovados. Use os resultados da execução que está apresentando; esses números podem mudar quando novos testes forem adicionados.

Fala de encerramento:

> “Mostramos os fluxos principais e as validações pelo navegador automatizado. A matriz relaciona requisitos e evidências. O cadastro persiste em PostgreSQL, a inativação preserva vínculos e os testes de apoio verificam aspectos internos que não são visíveis na tela, como hash e rollback.”

## 7. Respostas curtas para perguntas prováveis

- **Por que não tem ativação de cliente?** No DRS disponível, o grupo de clientes (RF0021–RF0028) não prevê ativação — apenas inativação (RF0023). A ativação (RF0016) é do grupo de livros e está implementada como ativação de produto em `/admin/produtos`, com motivo obrigatório (RN0017).
- **Por que tem senha sem login?** RF0028 exige alteração isolada da senha; autenticação não faz parte desta entrega.
- **Quem é ADMIN_DEMO?** Ator técnico definido pelo servidor, sem identidade autenticada.
- **Por que o cadastro do Selenium não aparece na minha lista de 4200?** O teste usa aplicação e banco isolados, com massa reinicializada por cenário.
- **O histórico representa vendas completas implementadas?** Não. São transações demonstrativas persistidas para consulta e ranking; os processos completos são planejamento do DVP.
- **Qual diagrama corresponde ao código atual?** Figuras 24 e 25. O modelo completo da figura 10 representa a solução planejada.
- **Como organizaram senha e bandeiras?** Alterar senha pertence a ClienteService; listar bandeiras pertence a CartaoService.
- **E se um teste falhar na apresentação?** Mostre o erro e a captura correspondente, identifique o cenário e o requisito. Não substitua a comprovação por uma operação manual nem apresente relatório antigo como execução atual.
