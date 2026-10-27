# Aplicação Android TransNacala

Aplicação móvel para consultar o catálogo de rotas e paragens e para os
motoristas partilharem a localização dos veículos durante o serviço.

## Executar

Abra a pasta `transnacala-mobile-android` no Android Studio e execute a
configuração `app`. O catálogo SQLite continua disponível sem ligação à
Internet. As rotas e paragens recebidas da API são guardadas localmente e
atualizadas em segundo plano pelo WorkManager.

## Ligar à API

A aplicação usa o endereço HTTPS de desenvolvimento
`https://transnacala-api.loca.lt`, definido em `ApiConfig`. O LocalTunnel tem
de estar ativo no computador onde a API está a ser executada. Não exponha a
porta do MySQL nem use HTTP numa ligação pública. O endereço configurado serve
apenas para desenvolvimento e demonstração.
O cliente Android envia o cabeçalho `bypass-tunnel-reminder: true` para
ultrapassar a página de aviso do LocalTunnel; isso não substitui o túnel ativo
nem ultrapassa indisponibilidade da API.

Na lista de rotas, a opção **Sincronizar agora** inicia uma tentativa imediata
de sincronização. Sem ligação, o catálogo já guardado no SQLite continua
disponível. A sincronização incremental usa `/api/v1/sync/changes`, aplica
cada página numa transação local e só depois atualiza o cursor guardado.

## Contas e acesso

As contas são geridas pelo administrador no painel web e na API; não são
criadas na base de dados SQLite do Android. Para abrir a área de motorista,
use uma conta com o perfil `DRIVER`.

O motorista inicia e termina a partilha da localização de forma explícita.
Durante a partilha, a aplicação mantém um serviço em primeiro plano e envia
as posições à API. Uma notificação persistente indica quando a partilha está
ativa. É necessária a permissão de localização precisa do dispositivo.

Na área de rotas, **Ver chapas no mapa** abre o mapa OpenStreetMap com os
percursos desenhados pelas coordenadas das paragens e os veículos que estão
a partilhar a localização. As rotas são diferenciadas por cor (Baixa–Alta em
laranja), e as posições recentes atualizam a cada 15 segundos. O mapa informa
a atribuição OpenStreetMap e usa um identificador próprio da aplicação para
os pedidos de tiles. Quando o motorista termina a sessão, a posição
partilhada é removida; as sessões sem atualizações também expiram.

Os detalhes de cada rota apresentam as paragens numa sequência visual de
percurso, em vez de uma lista simples.
