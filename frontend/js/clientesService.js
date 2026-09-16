import { clientesApi as api } from "./clientesApi.js";
/** Facade: stable operations consumed by the administrative UI. */
export const clientesService = {
  listar: (f) => api.listar(f),
  buscar: (id) => api.buscar(id),
  cadastrar: (d) => api.cadastrar(d),
  alterar: (id, d) => api.alterar(id, d),
  inativar: (id) => api.inativar(id),
  alterarSenha: (id, d) => api.alterarSenha(id, d),
  adicionarEndereco: (id, d) => api.adicionarEndereco(id, d),
  alterarEndereco: (id, e, d) => api.alterarEndereco(id, e, d),
  inativarEndereco: (id, e) => api.inativarEndereco(id, e),
  adicionarCartao: (id, d) => api.adicionarCartao(id, d),
  preferirCartao: (id, c) => api.preferirCartao(id, c),
  inativarCartao: (id, c) => api.inativarCartao(id, c),
  transacoes: (id) => api.transacoes(id),
  auditoria: (id) => api.auditoria(id),
  bandeiras: () => api.bandeiras(),
};
