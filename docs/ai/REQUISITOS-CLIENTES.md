# Requisitos do módulo de clientes

Fonte disponível: `docs/DRS_LES_2_2026.docx`, versão 0.7 do documento. Este resumo serve para rastreabilidade; em caso de dúvida, consulte a tabela original do DRS.

## Requisitos funcionais

| ID | Nome do DRS | Cobertura atual |
| --- | --- | --- |
| RF0021 | Cadastrar cliente | Implementado no painel administrativo |
| RF0022 | Alterar cliente | Implementado para dados cadastrais, preservando código e vínculos |
| RF0023 | Inativar cadastro de cliente | Implementado como inativação lógica com confirmação; não é exclusão física |
| RF0024 | Consulta de clientes | Filtros isolados e combinados por todos os campos de identificação expostos |
| RF0025 | Consulta de transações | Implementado sobre massa persistida demonstrativa vinculada ao cliente |
| RF0026 | Cadastro de endereços de entrega | Múltiplos endereços com apelido e manutenção independente |
| RF0027 | Cadastro de cartões de crédito | Múltiplos cartões, exatamente um preferencial quando existem ativos |
| RF0028 | Alteração apenas de senha | Formulário e endpoint independentes dos demais dados cadastrais |

## Regras de negócio relacionadas

| ID | Regra resumida | Aplicação atual |
| --- | --- | --- |
| RN0021 | Cliente deve possuir ao menos um endereço de cobrança | O endereço inicial cumpre a finalidade; o último ativo é protegido |
| RN0022 | Cliente deve possuir ao menos um endereço de entrega | O endereço inicial cumpre a finalidade; o último ativo é protegido |
| RN0023 | Endereço exige tipo de residência, tipo de logradouro, logradouro, número, bairro, CEP, cidade, estado e país; observações são opcionais | Bean Validation, validação de domínio e mensagens por campo |
| RN0024 | Cartão recebe número, nome impresso, bandeira e código de segurança | Número/CVV são validados durante a requisição e descartados após obtenção dos últimos quatro dígitos |
| RN0025 | Bandeira deve estar cadastrada no sistema | Validação contra `bandeira` persistida |
| RN0026 | Cliente exige gênero, nome, nascimento, CPF, telefone com tipo/DDD/número, e-mail, senha e endereço residencial | Validado no cadastro; CPF/e-mail também têm unicidade no banco |
| RN0027 | Cliente recebe ranking numérico baseado no perfil de compra | Strategy calcula `min(5, 1 + floor(total / 200))`; a fórmula é decisão do projeto |

A RN0028 aparece no grupo de clientes do DRS, mas trata do retorno da operadora e da baixa de estoque. Ela pertence ao processo de vendas e não deve ser declarada como implementada pelo CRUD.

## Requisitos não funcionais aplicáveis

| ID | Exigência | Cobertura atual |
| --- | --- | --- |
| RNF0011 | Consulta em até um segundo | Teste com 1.000 clientes, aquecimento e 90 medições |
| RNF0012 | Toda escrita registra data, hora, usuário responsável e dados alterados | Auditoria transacional com ator técnico `ADMIN_DEMO` e snapshots seguros |
| RNF0031 | Senha forte | Mínimo de oito caracteres, maiúscula, minúscula e especial |
| RNF0032 | Confirmação de senha | Confirmação idêntica obrigatória |
| RNF0033 | Senha criptografada | BCrypt; texto puro não é persistido |
| RNF0034 | Alterar/adicionar endereço sem editar todos os dados | Endpoints e formulários próprios |
| RNF0035 | Código único de cliente | Código `CLI-` gerado a partir do identificador e protegido por restrição de banco |

## Regras complementares do projeto

CPF, e-mail, telefone, CEP, nascimento futuro, limites de tamanho e duplicidade são validados. Nome e e-mail usam busca parcial; código e os demais filtros usam igualdade normalizada; filtros são combinados com `E`. `%` e `_` são tratados como caracteres literais.

Essas decisões melhoram integridade e mensagens de erro, mas não devem receber IDs de RN inventados. Erros HTTP seguem `{codigo, mensagem, campos}`, usando 400 para entrada inválida, 404 para ausência ou associação incorreta e 409 para conflito.

## Evidência Selenium

Os cenários administrativos estão em `backend/src/test/java/br/com/auralab/e2e/ClienteCrudIT.java`; a jornada do cliente escolhido no perfil está em `backend/src/test/java/br/com/auralab/e2e/ClientePerfilIT.java`. Os nomes carregam RF/RN/RNF, e a execução gera `backend/target/evidencias/matriz.html`. SQL é usado para preparar massa e verificar invariantes; as operações avaliadas são acionadas no navegador.
