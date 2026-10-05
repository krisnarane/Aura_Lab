# Aura Lab — front-end demonstrativo

**Revisão atual: 05/10/2026.** Este front-end combina módulos integrados à API com jornadas demonstrativas ainda mantidas no store local.

> Clientes, produtos, perfil e a jornada principal de vendas são integrados à API Spring Boot e ao PostgreSQL. Execute o sistema pelas instruções do `README.md` na raiz; o servidor Node isolado não oferece essa API.

HTML5, CSS3, Bootstrap 5 e JavaScript ES6 modular, sem frameworks de front-end. Catálogo, sacola, checkout, confirmação e cupons consomem a API real; pagamento é registrado como composição do pedido, sem autorização externa, conforme o escopo acadêmico.

O navegador guarda em `localStorage` apenas a identificação do cliente escolhido para a sessão integrada e o estado das jornadas demonstrativas fora deste escopo. Carrinho e pedido são persistidos no PostgreSQL.

Não há cadastro público nesta etapa. O cliente escolhe um registro já existente no PostgreSQL ao entrar em `/perfil/dados`; depois pode editar os próprios dados, senha, endereços e cartões. O perfil protege o último endereço obrigatório de cada finalidade. Cartões aceitam apenas bandeiras registradas; número completo e código de segurança são descartados após a validação e nunca entram no `localStorage`.

Em `/admin/clientes`, o administrador cadastra, consulta, altera e inativa clientes sem excluir seus históricos. Em `/admin/produtos`, cadastra e edita produtos completos, envia ou informa a URL da imagem, define categorias, descrição, atributos, preço, custo, estoque e publicação no catálogo.

O dashboard administrativo analisa vendas por período e categorias, exibe tooltip acessível e exporta CSV. A Consultora Aura tenta chamar a futura rota `/api/v1/beauty-advisor/recommendations`; se a API ainda não existir, usa recomendação local por regras e nunca altera regras transacionais.

```powershell
cd C:\Users\jukia\OneDrive\Desktop\Aura_Lab\frontend
npm start
```

Abra `http://localhost:4200`. Para validar a sintaxe e as regras centrais: `npm test`.

Rotas principais: `/`, `/produto/:id`, `/sacola`, `/checkout/endereco`, `/checkout/pagamento`, `/checkout/revisao`, `/checkout/confirmacao`, `/perfil`, `/perfil/dados`, `/perfil/seguranca`, `/perfil/enderecos`, `/perfil/cartoes`, `/perfil/pedidos`, `/perfil/pedidos/:id`, `/perfil/cupons`, `/consultora`, `/admin/dashboard`, `/admin/clientes`, `/admin/clientes/:id`, `/admin/pedidos`, `/admin/pedidos/:id`, `/admin/trocas`, `/admin/trocas/:id`, `/admin/produtos`, `/admin/cupons` e `/admin/configuracoes`.

> Fluxo canônico, decisões e roteiro estão em `../docs/README.md`.

## Mapa para estudo

- `js/dados-mock.js`: massa fixa usada para restaurar a demonstração.
- `js/store.js`: estado local das jornadas demonstrativas que ainda não foram integradas.
- `js/vendas-api.js`: catálogo, sacola, checkout, confirmação e cupons integrados à API real.
- `js/pages.js`: templates HTML das telas, sem alteração de estado.
- `js/app.js`: rotas, renderização e ligação dos eventos.
- `js/api.js`: adaptador HTTP opcional da futura API REST da Consultora Aura.
- `js/consultora-aura.js`: recomendação local por regras.
- `js/analytics.js`: interação e acessibilidade do gráfico.
- `js/utilitarios.js`: funções puras de data, moeda, identificadores e segurança de HTML.
- `assets/css/`: variáveis, base, componentes e estilos específicos de páginas.
