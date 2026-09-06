/**
 * Utilitários puros compartilhados pelas regras de negócio.
 * Eles não conhecem a interface nem alteram o estado da aplicação.
 */

// Faz uma cópia profunda suficiente para os mocks, que contêm apenas dados serializáveis.
export const clonarDados = (valor) => JSON.parse(JSON.stringify(valor));

// Padroniza datas de auditoria e histórico no formato ISO.
export const agoraEmIso = () => new Date().toISOString();

// Formata datas simples para aparecerem como datas brasileiras nas telas.
export const dataAtualEmPtBr = () => new Date().toLocaleDateString("pt-BR");

// Gera ids legíveis por tipo de entidade, úteis para depuração do protótipo.
export const criarIdentificador = (prefixo) =>
  `${prefixo}-${Date.now()}-${Math.floor(Math.random() * 10000)}`;

// Centraliza o arredondamento monetário usado no checkout e nos cupons.
export const arredondarMoeda = (valor) =>
  Math.round((Number(valor) + Number.EPSILON) * 100) / 100;

export const formatarMoeda = (valor) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Number(valor || 0));

// Evita que conteúdo vindo de formulários seja interpretado como marcação HTML.
export const escaparHtml = (valor) =>
  String(valor ?? "").replace(
    /[&<>"']/g,
    (caractere) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[caractere],
  );
