# Aplicação Android TransNacala

## Executar

Abra `transnacala-mobile-android` no Android Studio e execute a configuração
`app`. O catálogo SQLite permanece disponível sem rede; as rotas e paragens
recebidas da API são armazenadas localmente e atualizadas por WorkManager.

## Ligar à API

Na tela inicial, escolha **Configurar conexão com a API** e informe a URL HTTPS
da API publicada por um túnel de desenvolvimento, sem caminho adicional, por
exemplo `https://<subdominio>.loca.lt`. Não use HTTP público, não publique a
porta MySQL e não grave credenciais/URL com segredos no código.

Na lista de rotas, **Sincronizar agora** força uma tentativa imediata. Sem
conexão, o catálogo que já está no SQLite continua disponível. A sincronização
incremental usa `/api/v1/sync/changes`, aplica cada página numa transação local
e só então avança o cursor persistido.

## Contas de teste

As contas de desenvolvimento são criadas na API (MySQL), não no SQLite do
Android. Na pasta `transnacala-api-rest`, com `.env` local configurado e
migrações aplicadas, execute `python -m app.cli seed-test-users`. O comando
solicita as senhas interativamente, sem as incluir no código ou no terminal.
Use o perfil `DRIVER` para entrar na área do motorista.

A API ainda responde `501 DOMAIN_ENDPOINT_NOT_IMPLEMENTED` às operações de
localização ao vivo. O Android não deve indicar partilha ativa até à
implementação da Fase 4.
