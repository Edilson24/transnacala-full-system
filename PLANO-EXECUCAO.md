# TransNacala — Plano compartilhado de execução

Este é o documento de contexto e acompanhamento do projeto inteiro. Deve ser
lido por todos os agentes no PyCharm, Android Studio e IntelliJ IDEA antes de
alterar código. O plano fica na raiz de `trans-nacala-project`, ao lado dos
projetos `transnacala-api-rest`, `transnacala-mobile-android` e
`transnacala-web-admin`.

## Objetivo

Construir uma aplicação coordenada para transporte semi-coletivo em Nacala:

- Passageiros consultam rotas, paragens e veículos que estão a partilhar a
  localização.
- Motoristas autenticados iniciam e terminam a partilha por uma ação explícita
  na aplicação Android.
- Administradores gerem utilizadores, motoristas, veículos, rotas e paragens
  pela aplicação web.
- O Android mantém rotas e paragens disponíveis offline e sincroniza o
  catálogo com a API quando há conexão.

## Arquitetura e decisões comuns

```text
Android (SQLite) ─┐
                  ├── HTTPS/JSON ── API REST (FastAPI) ── MySQL
Web Admin ────────┘                    │
                                      └── túnel de desenvolvimento
                                          (LocalTunnel ou Cloudflare Tunnel)
```

- MySQL é a fonte remota de verdade. O SQLite é cache local para leitura offline
  de rotas e paragens; não se liga diretamente ao MySQL.
- A API é a única camada autorizada a aceder à base MySQL. JSON é o formato de
  transporte, não uma proteção contra SQL injection: consultas devem usar
  parâmetros/ORM, validação e autorização.
- O túnel serve apenas para desenvolvimento/demonstração. Não publicar o
  serviço MySQL nem credenciais; usar HTTPS e configuração por variáveis de
  ambiente. Nunca guardar segredos no repositório.
- IDs de entidades partilhadas devem ser UUIDs gerados pelo cliente/API para
  evitar colisões em operações offline. Fazer migração compatível no SQLite e
  nos contratos antes de mudar os IDs existentes, que hoje são inteiros.
- O código de veículo (`V001`, `V002`, ...) é um identificador de negócio único,
  atribuído pela API dentro de transação; não é o ID interno.
- A localização é dado transitório de uma sessão ativa, não parte do catálogo.
  Encerrar a partilha remove a localização publicada. Clientes não devem
  apresentar uma posição antiga como “ao vivo”; sessões sem atualização devem
  expirar/ser mostradas como indisponíveis.
- O serviço de sincronização Android deve usar WorkManager e operações
  idempotentes. Alterações administrativas são feitas no servidor e baixadas
  como catálogo; alterações locais pendentes não podem sobrescrever
  silenciosamente o estado administrativo.
- APIs e modelos JSON devem ser acordados antes de implementação paralela.
  Alterações ao contrato exigem atualizar este documento e os consumidores.

## Projetos e responsabilidades

| Projeto | Responsabilidade | IDE habitual |
|---|---|---|
| `transnacala-api-rest` | API FastAPI, autenticação/autorização, MySQL, sincronização e sessões de localização | PyCharm |
| `transnacala-mobile-android` | Aplicação Android, cache SQLite, sincronização, localização e mapa | Android Studio / IntelliJ |
| `transnacala-web-admin` | Administração web Java; comunica apenas com a API | IntelliJ IDEA |
| `database/mysql_schema.sql` | Bootstrap de desenvolvimento da base MySQL para MySQL Workbench | MySQL Workbench |

Manter os projetos separados nesta fase. Não mover código entre diretórios sem
atualizar os projetos das IDEs, comandos, documentação e este plano.

## Modelo de dados inicial

O script `database/mysql_schema.sql` cria:

- `users`: contas e papéis `ADMIN`, `DRIVER` e `PASSENGER`; guardar apenas hashes
  de senha.
- `routes`, `stops` e `route_stops`: catálogo e ordem das paragens da rota.
- `vehicles`: código público, motorista e rota atribuída.
- `tracking_sessions` e `live_locations`: histórico mínimo das sessões e a
  última posição apenas enquanto a partilha está ativa.
- `sync_sequence` e `sync_changes`: cursor incremental e tombstones para
  sincronização do catálogo, gravados pela API na mesma transação das
  alterações. A API deve bloquear/incrementar a sequência global na transação
  para que cursores não saltem alterações concorrentes ainda não confirmadas.

O SQL é o bootstrap inicial e deve evoluir junto com as migrações da API. Ao
implementar persistência/migrações, validar os nomes, tipos, restrições e
relacionamentos dos dois lados. Nunca alterar o schema remotamente sem
migração versionada.

## Plano de tarefas e estado

Marcar `[x]` apenas após cumprir os critérios de aceitação e verificar a tarefa.
Agentes devem atualizar este mesmo ficheiro logo após concluir trabalho, sem
desmarcar tarefas alheias. Se uma tarefa depender de contrato ou outra tarefa,
respeitar a ordem abaixo. Antes de trabalhar em paralelo, cada agente deve
anunciar/limitar os ficheiros que vai modificar.

### Fase 0 — alinhamento

- [x] **P0.1 — Consolidar contexto e arquitetura.** Este plano documenta objetivo,
  fronteiras dos projetos, decisões de sincronização, segurança e execução para
  todos os agentes.
- [x] **P0.2 — Criar bootstrap MySQL.** `database/mysql_schema.sql` cria o
  esquema inicial compatível com MySQL 8.0.16+ para execução no Workbench.
  Execução numa instância MySQL ainda não foi validada neste ambiente.

### Fase 1 — API e persistência

- [x] **P1.1 — Estruturar a API.** Organizar configuração, routers, schemas,
  serviços, persistência, testes e dependências; configurar por ambiente e
  manter a inicialização documentada. FastAPI está organizado em `app/`,
  SQLAlchemy concentra os modelos do schema MySQL, Alembic gere as migrações e
  `DATABASE_URL`/`APP_ENV` são configuráveis por ambiente.
- [x] **P1.2 — Ligar MySQL com migrações.** Implementar pool/conexões seguros,
  migrações versionadas e health check; validar o schema com uma base local
  limpa. Modelos, migração inicial, pool e health checks estão implementados.
  A ligação real ao MySQL local foi verificada e `/health/ready` respondeu
  `200`; as nove tabelas existentes coincidem com os modelos SQLAlchemy
  (nenhuma diferença estrutural), incluindo `ON UPDATE CURRENT_TIMESTAMP`.
  A revisão `329db46d9f9c` foi aplicada e verificada na base vazia separada
  `transnacala_migration_test`: nove tabelas, revisão registada, sequência
  inicial `(1, 0)`, IDs incrementais geridos pela aplicação e zero diferenças
  face aos modelos. O teste inicial revelou `AUTO_INCREMENT` indevido em duas
  chaves; a revisão foi corrigida e reaplicada com sucesso. A base original
  `transnacala` permaneceu intacta e sem revisão Alembic; não aplicar nela a
  migração inicial existente.
- [x] **P1.3 — Definir e testar o contrato HTTP.** Documentar endpoints JSON,
  paginação, erros, UUIDs e exemplos no OpenAPI. Cobrir catálogo, contas,
  atribuições, sincronização e tracking; confirmar contrato antes dos clientes.
  O OpenAPI 3.1 está em `transnacala-api-rest/app/openapi.json`; convenções de
  UUID, paginação e erros foram confirmadas. Autenticação, administração e
  catálogo foram implementados na Fase 2; apenas tracking permanece com resposta
  explícita `501` até à Fase 4.

### Fase 2 — identidade e administração

- [x] **P2.1 — Autenticar e autorizar.** PBKDF2-HMAC-SHA256 com salt aleatório,
  login, bearer tokens HS256 com expiração, verificação da conta ativa e
  autorização administrativa em cada pedido. Não são devolvidos hashes; a
  criação inicial de administrador é interativa (`python -m app.cli
  create-admin`). Testes cobrem login, token inválido, papéis, conta desativada
  e proteção do último administrador.
- [x] **P2.2 — Implementar catálogo e atribuições.** CRUD de utilizadores, rotas,
  paragens, ordenação de paragens e veículos; motorista/rota atribuídos apenas
  se ativos e válidos. O código `V001`, `V002`, ... é gerado sob bloqueio da
  linha da sequência de sincronização existente, e os eventos do catálogo são
  gravados atomicamente com cada alteração. Testes cobrem os fluxos e cursores
  incrementais na base SQLite em memória.
- [x] **P2.3 — Criar a interface web administrativa.** Implementados login e
  formulários responsivos para utilizadores, catálogo, paragens, ordenação e
  frota. A aplicação comunica com a API pelo servidor Java; token fica na
  sessão, cookie HttpOnly, formulários com CSRF e cabeçalhos de segurança.
  `transnacala-web-admin/README.md` documenta execução e deploy. Compatível com
  Jakarta Servlet 6/JSP 3.1 e Tomcat 10.1; `mvn clean package` gerou com sucesso
  `target/transnacala-web-admin.war` usando JDK 21 e Maven 3.9.5.

### Fase 3 — Android e sincronização offline

- [x] **P3.1 — Migrar o catálogo local.** SQLite v3 adiciona UUID remoto,
  estado ativo e cursor sem trocar os IDs inteiros nem apagar rotas/paragens
  existentes; associações duplicadas são consolidadas e as chaves estrangeiras
  ficam ativas. O catálogo sincronizado tenta associar registos locais antigos
  equivalentes antes de inserir para reduzir duplicações.
- [x] **P3.2 — Sincronizar catálogo.** Worker com WorkManager consulta páginas
  incrementais, carrega detalhes pelo contrato da API, aplica cada página e o
  cursor na mesma transação SQLite, preserva IDs locais, aplica tombstones e
  retenta falhas temporárias. Configuração exige HTTPS; a lista de rotas permite
  sincronizar manualmente ou continuar offline. Compilação Android concluída.
- [x] **P3.3 — Implementar experiências por papel.** Passageiros consultam
  rotas/paragens em cache e podem solicitar atualização; motoristas autenticam
  pela API, têm o perfil e veículo ativo identificados e controlam explicitamente
  o início/fim da partilha. A entrada administrativa foi removida do Android.
  Contas são geridas pelo painel/API; não há comando especial para semear contas
  de teste.

### Fase 4 — localização ao vivo e integração

- [x] **P4.1 — Publicar sessões de localização.** Criar/encerrar sessão e
  atualizar posição autenticada do veículo; validar latitude/longitude,
  propriedade do veículo, frequência e atualização mais recente. Serializar
  início de sessão por veículo para impedir sessões concorrentes ativas. TTLs
  encerram sessões sem atualização e testes de API cobrem autorização, ciclo de
  vida, localização recente e remoção ao terminar.
- [x] **P4.2 — Mostrar veículos ao vivo.** API paginada expõe somente posições
  recentes de sessões ativas; mapa Android desenha as rotas pelas coordenadas
  das paragens, associa cores aos veículos e atualiza posições a cada 15
  segundos. Baixa–Alta usa laranja. O mapa apresenta estado vazio quando não
  há veículos ativos.
- [x] **P4.3 — Tratar localização e permissões Android.** Solicitar permissões
  adequadas, cumprir restrições de localização em segundo plano, oferecer
  indicação visível durante partilha através de serviço foreground/notificação
  e parar a coleta ao encerrar. Build Android concluído; validação em dispositivo
  fica para P4.4.
- [ ] **P4.4 — Validar ponta a ponta.** Testar MySQL/API, web, Android offline e
  online, papéis, falhas de rede, duplicação de pedidos e fim de sessão. Ainda
  falta validar o percurso real no MySQL e em dispositivo/emulador.
- [ ] **P4.5 — Documentar execução local.** Instruções reproduzíveis para
  Workbench, API, Android, web e túnel; túnel não é configuração de produção.

### Fase 5 — Distribuição pública Android

- [x] **P5.1 — Preparar página pública de download.** A raiz pública do painel
  web apresenta uma página de distribuição e oferece a última versão quando
  `downloads/transnacala-mobile.apk` está presente no WAR. Sem o APK, mostra
  disponibilidade futura; o README explica como publicar o APK assinado. A
  compilação Maven empacotou a página pública e o seu CSS no WAR.
- [ ] **P5.2 — Publicar atualização Android.** Gerar uma versão de release
  assinada e copiá-la para o caminho público fixo; validar o download HTTPS e a
  atualização sobre uma instalação anterior com a mesma chave de assinatura.

## Critérios globais de conclusão

- Projetos compilam/testam com as ferramentas declaradas pelos próprios
  projetos; testes de API cobrem autorização, validação e persistência.
- Um administrador consegue gerir o catálogo pela web; Android atualiza o
  cache após reconectar; dados já em cache permanecem consultáveis offline.
- Um motorista autenticado controla explicitamente o início/fim da partilha;
  passageiros veem apenas posições ativas e recentes.
- Nenhum cliente acede diretamente ao MySQL; não há senhas/tokens/IPs privados
  nos logs ou respostas públicas.
- Sincronização repetida não duplica dados e exclusões não reaparecem após
  reconexão.

## Regras para agentes

1. Ler este plano e verificar o estado atual do repositório antes de editar.
2. Trabalhar apenas no escopo da tarefa escolhida e preservar alterações
   preexistentes de outras pessoas/agentes.
3. Respeitar dependências e interfaces; não inventar contratos independentes
   nos três projetos.
4. Executar os testes/validações mais próximos da mudança. Marcar a tarefa
   concluída somente depois de validação; anotar bloqueios neste plano se
   impedirem o avanço.
5. Não guardar segredos, credenciais, dados reais de utilizadores ou posições
   reais no código, fixtures, logs ou documentação.
