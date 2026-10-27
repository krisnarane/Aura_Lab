import { produtosApi as api } from "./produtosApi.js";
/** Facade: stable operations consumed by the administrative UI. */
export const produtosService = {
  listar: (f) => api.listar(f),
  buscar: (id) => api.buscar(id),
  cadastrar: (d) => api.cadastrar(d),
  editar: (id, d) => api.editar(id, d),
  categoriasProduto: () => api.categoriasProduto(),
  inativar: (id, d) => api.inativar(id, d),
  ativar: (id, d) => api.ativar(id, d),
  categoriasAtivacao: () => api.categoriasAtivacao(),
  categoriasInativacao: () => api.categoriasInativacao(),
};
