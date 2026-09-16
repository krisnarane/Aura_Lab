# CRUD administrativo de clientes

Para apresentar em sala, siga o [roteiro de demonstração](ROTEIRO-APRESENTACAO.md), com ordem dos fluxos, falas sugeridas, comandos Selenium e arquivos de código.

Referências: DRS_LES_2_2026 e DVP_Aura_Lab_FINALIZADO. Alguns enunciados citam DRS_LES_1_2026, arquivo que não está no repositório e precisa ser comparado antes de afirmar equivalência entre versões. Esta entrega corresponde a RF0021–RF0028 no painel `/admin/clientes`. Atores não são autenticados: `ADMIN_DEMO` é uma identificação técnica atribuída pelo servidor. Não há login no escopo. Cadastro público, perfil, checkout e os processos reais de vendas permanecem em outra etapa. O protótipo dessas telas ainda usa localStorage e não compartilha os registros administrativos.

## Executar e demonstrar

Com Java 21, Docker Desktop em execução e Chrome instalado:

```powershell
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

Abra `http://localhost:4200/admin/clientes`. Flyway aplica V1–V6 em banco novo ou somente as migrações pendentes em banco existente. Não modifique V1/V2 nem apague o volume para atualizar.

Em outro terminal, execute `backend/testar-crud.ps1 -Visivel` para apresentação Selenium ou sem `-Visivel` para headless. Testcontainers cria PostgreSQL 17 isolado; Docker indisponível causa falha, sem fallback para H2. O banco de demonstração não é reinicializado pelos testes.

Resultados são gerados em `backend/target/reports`, capturas em `backend/target/screenshots` e matriz requisito/teste/resultado em `backend/target/evidencias/matriz.html`. O teste RNF0011 gera `performance.csv` com 90 medições HTTP, após aquecimento, em massa de 1.000 clientes. O tempo é do ambiente local de referência, sem garantia para outra infraestrutura.

## Contratos e decisões

- Prefixo `/api/v1`; endpoints existentes preservados. Clientes seguem sem DELETE e sem reativação (o DRS não pede ativação de cliente). Produtos têm ativação e inativação lógicas sempre acompanhadas de categoria e justificativa (RF0012/RF0016, RN0015/RN0017) — ver [REQUISITOS-PRODUTOS](../ai/REQUISITOS-PRODUTOS.md).
- Cadastro exige endereço residencial que também atende cobrança e entrega. Alteração pessoal não modifica vínculos nem senha.
- Código `CLI-` gerado pelo PostgreSQL a partir do identificador; imutável e único. CPF/e-mail únicos também entre inativos. Cliente inativo pode ter dados corrigidos, sem mudança implícita de situação.
- Complemento opcional de até 100 caracteres, separado das observações. Endereço inativo não pode ser editado. Última cobrança/entrega ativa protegida.
- Um cartão preferencial entre os ativos; o primeiro recebe a preferência automaticamente. Ao inativar o preferencial, assume o ativo mais antigo.
- Apenas cartões fictícios. Número completo e código de segurança são entradas transitórias; persistência e auditoria usam titular, bandeira e últimos quatro dígitos.
- Senha BCrypt: oito caracteres, maiúscula, minúscula e especial, confirmação idêntica. Limite técnico de 72 bytes UTF-8, sem truncamento silencioso.
- Filtros combinados por E; nome/e-mail parciais, código e demais campos exatos normalizados. `%` e `_` são literais.
- Ranking derivado: `min(5, 1 + floor(totalCompras / 200))`, somente pedidos em PAGAMENTO_REALIZADO, EM_TRANSITO e ENTREGUE. Pagamentos, trocas e cupons não somam compras novamente. A fórmula é decisão de projeto; RN0027 não fixa o cálculo.
- `TransacaoCliente` é uma projeção demonstrativa persistida, não substitui as entidades futuras Pedido, ItemPedido, PagamentoSimulado, PartePagamento, Troca e Cupom. Os vínculos entre eventos fictícios estão descritos nos detalhes; sua associação ao cliente é uma chave estrangeira.
- Auditoria registra entidade, identificador, operação, ator, horário e snapshots permitidos, na transação da escrita. Senhas, hashes, PAN e CVV nunca entram nos snapshots. Eventos anteriores permanecem LEGADO, com snapshots nulos e texto original.
- Respostas de erro: `{codigo,mensagem,campos}`; validação 400, ausência/associação incorreta 404, conflito 409. A interface preserva campos não sensíveis após erro, limpa segredos e não usa mocks como fallback HTTP.
- RN0028 pertence ao pagamento/estoque e não é declarada implementada pelo CRUD.

## Padrões e extensão

| Padrão | Participantes e responsabilidade | Exemplo de extensão |
|---|---|---|
| MVC / Service Layer | ClienteController e services de cliente/endereço/cartão/transação/auditoria | Novo caso de uso coordenado em service transacional |
| Repository | Interfaces Spring Data, sem wrappers genéricos | Nova consulta agregada de histórico |
| Strategy | PoliticaRankingCliente e PoliticaRankingPorCompras | Trocar fórmula mantendo controller e DTO |
| Specification | ClienteSpecifications | Acrescentar predicado e parâmetro de filtro |
| Adapter | clientesApi.js | Adaptar novo formato HTTP em um módulo |
| Facade | clientesService.js | Expor nova operação estável para os eventos |
| Page Object | ClientesAdminPage, FormularioComponente, EnderecoComponente, CartaoComponente e ConfirmacaoComponente | Ajustar seletor em um componente sem alterar regras dos testes |

ClienteMapper somente transforma objetos; não consulta repositories. RegistroCliente produz snapshots por lista explícita de campos. Coleções das entidades são expostas sem permissão de remoção; inativação preserva registros. Bloqueio pessimista do cliente serializa mudanças de preferência, e um índice parcial protege o banco. A preferência antiga é gravada como falsa antes da nova, dentro da mesma transação.

## Rastreabilidade

| Requisito | Testes principais |
|---|---|
| RF0021, RN0021–RN0023, RN0026, RNF0035 | ClienteCrudIT: cadastroPersistenteECodigos, obrigatorios, validacoesEDuplicidadeInativos |
| RF0022, RN0026 | ClienteCrudIT: alteracaoPreservaVinculosEAudita, duplicidadeNaoAltera |
| RF0023, RF0025 | ClienteCrudIT: cancelarConfirmarRepetir |
| RF0024 | ClienteCrudIT: filtrosIsolados, filtrosCombinadosVazioESituacao |
| RF0025, RN0027 | ClienteCrudIT: detalhesHistoricoEVazio; ClienteIntegridadeTest: naoContaPagamentoTrocaOuCupom; PoliticaRankingPorComprasTest: faixas |
| RF0026, RNF0034, RN0021–RN0023 | ClienteCrudIT: variosEnderecosEdicaoEPreferencia, protegeMinimos |
| RF0027, RN0024–RN0025 | ClienteCrudIT: cartoesPreferenciaEMascara, recusaCartao; ClienteIntegridadeTest: preferenciaConcorrente |
| RF0028, RNF0031–RNF0033 | ClienteCrudIT: forcaEConfirmacao, alteracaoIsolada; ClienteIntegridadeTest: hashESegredosNaoPersistidos |
| RNF0011 | ConsultaPerformanceTest: consultasAteUmSegundo |
| RNF0012 | ClienteIntegridadeTest: falhaAuditoriaReverteCadastro, falhaAuditoriaRevertePreferencia; ClienteCrudIT: alteracaoPreservaVinculosEAudita; ProdutosCrudIT: ativacaoCancelarConfirmarRepetir, inativacaoRegistraMotivoEAuditoria |
| RF0012, RN0015 | ProdutosCrudIT: inativacaoRegistraMotivoEAuditoria; ProdutoApiTest: inativacaoExigeMotivoEAudita |
| RF0016, RN0017 | ProdutosCrudIT: ativacaoCancelarConfirmarRepetir, justificativaObrigatoriaNaoAtiva; ProdutoApiTest: ativacaoExigeMotivoEAudita, deveRecusarAtivarProdutoJaAtivo |
| RF0011 (produtos), RF0015, RNF0021 | ProdutosCrudIT: cadastroGeraCodigoEApareceNaLista; ProdutoApiTest: cadastroGeraCodigoUnico, consultaFiltraPorCamposDeIdentificacao |
| Migrações e persistência | MigrationPersistenceTest: migrarV2PreservaLegadoEReiniciarPreservaDados, reiniciarPreservaSituacaoEMotivoDeProduto |
| Arquitetura | ArquiteturaTest: controllersEMapperNaoAcessamRepositories |

Os nomes acima indicam cobertura pretendida; a aprovação é determinada pelos relatórios da execução, não pela presença do método. A apresentação é executada pelo navegador e usa SQL somente para preparar a massa e verificar persistência/invariantes.

## Execução verificada em 09/09/2026

`backend/testar-crud.ps1` concluiu uma compilação limpa com 88 testes aprovados: 63 cenários Selenium em Chrome headless e 25 testes de apoio (API, integridade, ranking, arquitetura, migrações, reinício e desempenho). Não houve falhas, erros ou testes ignorados. Os testes de persistência usaram PostgreSQL 17.11 em Testcontainers, no Windows com Java 21 e Chrome 152.

Nas 90 consultas HTTP medidas com 1.000 clientes, a média foi 158,77 ms e o máximo 415,60 ms, abaixo do limite de 1.000 ms no ambiente local. A matriz gerada contém os 88 resultados. Relatórios e capturas são artefatos gerados, não arquivos versionados; execute novamente o script para obter evidência do estado atual. A opção `-Visivel` habilita o Chrome para apresentação; esta execução de validação foi headless.

## Organização simplificada

A troca de senha pertence a ClienteService e a consulta de bandeiras a CartaoService. Não há services separados para essas operações, nem classe de usuário ou autenticação. Os contratos HTTP e a auditoria transacional foram preservados.
