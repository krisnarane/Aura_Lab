import { createServer } from "node:http";
import { createReadStream, existsSync, statSync } from "node:fs";
import { extname, join } from "node:path";

/**
 * Servidor estático mínimo usado somente na demonstração local.
 * Rotas que não correspondem a um arquivo retornam index.html para a SPA resolvê-las.
 */
const DIRETORIO_RAIZ = process.cwd();
const TIPOS_DE_CONTEUDO = {
  ".html": "text/html",
  ".js": "text/javascript",
  ".css": "text/css",
  ".png": "image/png",
};

createServer((requisicao, resposta) => {
  const caminhoRequisitado = decodeURIComponent(
    new URL(
      requisicao.url,
      `http://${requisicao.headers.host}`,
    ).pathname,
  ).replace(/^\/+/, "");
  const arquivoRequisitado = join(
    DIRETORIO_RAIZ,
    caminhoRequisitado || "index.html",
  );
  const arquivoResposta =
    existsSync(arquivoRequisitado) && statSync(arquivoRequisitado).isFile()
      ? arquivoRequisitado
      : join(DIRETORIO_RAIZ, "index.html");

  resposta.writeHead(200, {
    "Content-Type":
      TIPOS_DE_CONTEUDO[extname(arquivoResposta)] ||
      "application/octet-stream",
  });
  createReadStream(arquivoResposta).pipe(resposta);
}).listen(process.env.PORT || 4200);
