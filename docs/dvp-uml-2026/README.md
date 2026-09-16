# Figuras revisadas do DVP — Aura Lab

Edição de 09/09/2026, baseada em `DVP_Aura_Lab_FINALIZADO.docx`, nas fontes anteriores dos diagramas e na implementação administrativa de clientes. Os arquivos anteriores e o DOCX original foram preservados.

Abra [a galeria](index.html). Cada figura possui PlantUML independente (`.puml`) e imagem vetorial em `svg/`. [CODIGOS-UML.md](CODIGOS-UML.md) reúne todos os códigos. SVG pode ser inserido no Word e ampliado sem perda de resolução; os modelos gerais devem ocupar página em paisagem ou ser consultados com zoom.

## Escopo das figuras

- **1–17:** revisão das figuras originais, conservando a numeração. Representam a arquitetura alvo completa, com a etapa atual identificada quando pertinente. Não afirmam que vendas ou Gemini já foram implementados.
- **18, 19, 23, 24 e 25:** implementação atual e estrutura de testes. Em especial, 24/25 mostram `senhaHash` em Cliente e as entidades demonstrativas realmente existentes.
- **20–22:** propostas para estados de pedido, troca e responsabilidades simples do CRUD.

As figuras 16, 17 e 25 usam notação de engenharia da informação (pé de galinha), expressa em PlantUML; não são diagramas de classes UML estritos.

## Alterações

1. Cliente: código único e imutável, datas, CPF/e-mail únicos e componentes do telefone padronizados.
2. Endereço: tipos de residência/logradouro, complemento, observações, finalidades, preferência e situação; mínimo ativo explicitado.
3. Cartão: bandeira obrigatória, preferência entre ativos e ausência de PAN/CVV persistidos.
4. Senha permanece em `Cliente.senhaHash`, alterada pelo `ClienteService`. Não há autenticação, conta de acesso ou sessão.
5. Auditoria atual por cliente; evolução geral mantém somente ator técnico, sem vínculo com usuário autenticado.
6. Vendas: Pedido, ItemPedido, PagamentoSimulado, PartePagamento, Troca e Cupom com vínculos e estados próprios. `TransacaoCliente` aparece como demonstrativa no modelo atual.
7. Frontend, serviços, persistência, implantação e testes atualizados, incluindo Facade, Adapter, Strategy, Specification e Page Objects. Porta atual do Spring: 4200.
8. Este pacote não altera aplicação, banco ou migrações. Restringir `CascadeType.ALL` e implementar módulos futuros continuam trabalhos de código separados.

## Índice para o DVP

| Figura | Conteúdo |
|---|---|
| 1 | Representação arquitetural |
| 2 | Caso de uso: realizar pedido |
| 3 | Casos de uso: pedidos e trocas |
| 4 | Casos de uso: produtos |
| 5 | Pacotes e dependências |
| 6 | Tecnologias e padrões |
| 7 | Apresentação atual e futura |
| 8 | Negócio, políticas e integrações |
| 9 | Controllers e Services |
| 10 | Domínio completo proposto |
| 11 | Persistência |
| 12 | Sequência: checkout |
| 13 | Sequência: gerenciar pedidos |
| 14 | Sequência: gerenciar produtos |
| 15 | Implantação e testes isolados |
| 16 | Modelo lógico completo proposto |
| 17 | Modelo relacional completo proposto |
| 18 | Casos de uso do CRUD atual |
| 19 | Escrita e auditoria do CRUD atual |
| 20 | Estados do pedido |
| 21 | Estados da troca |
| 22 | Responsabilidades simples do CRUD |
| 23 | Testes e evidências do CRUD |
| 24 | Domínio do CRUD implementado |
| 25 | Banco implementado — V1 a V4 |

Arquivos: [manifesto.json](manifesto.json). Cardinalidades e migração: [REGRAS-E-MIGRACAO.md](REGRAS-E-MIGRACAO.md).

## Regenerar SVG

Com Java 21, execute nesta pasta:

```powershell
.\renderizar.ps1
```

O script usa PlantUML 1.2025.10, em diretório temporário, com SHA-256 fixado. Não requer PostgreSQL ou Docker. Também aceita `-PlantUmlJar C:\caminho\plantuml.jar` da mesma versão. As fontes `.puml` são a referência principal; após editá-las, atualize o documento de códigos reunidos caso pretenda utilizá-lo.

## Correção de escopo

Sem autenticação nesta solução, inclusive nos diagramas alvo. Consulta de bandeiras está em CartaoService; troca de senha em ClienteService. Não são necessários services próprios para essas duas operações.
