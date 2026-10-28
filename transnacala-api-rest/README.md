# API TransNacala

Serviço FastAPI do sistema TransNacala. As aplicações móveis e web comunicam
com esta API através de HTTPS/JSON; nunca se devem ligar diretamente ao MySQL.

## Preparação local

Requer Python 3.11 ou superior e MySQL 8.0.16 ou superior. Para usar o
processo de migração da API, crie no MySQL Workbench apenas uma base de dados
vazia:

```sql
CREATE DATABASE transnacala
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Não execute `database/mysql_schema.sql` nesta base de dados antes de usar o
Alembic: esse script cria as tabelas e a migração inicial tentaria criá-las
novamente. O script SQL é uma alternativa autónoma para preparar o esquema no
Workbench; o Alembic é o processo de gestão do esquema pela API.

Prepare a API:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
Copy-Item .env.example .env
```

As predefinições de ligação local apontam para `127.0.0.1:3306`, base de dados
`transnacala` e controlador PyMySQL. No ficheiro `.env`, defina `MYSQL_USER` e
`MYSQL_PASSWORD` com as credenciais usadas no MySQL Workbench. Mantenha essas
credenciais locais e não inclua o `.env` no Git. As variáveis `MYSQL_*`
suportam palavras-passe com caracteres especiais sem ser necessário codificá-las
como URL. Em alternativa, configure `DATABASE_URL`, que tem precedência.
A API pode iniciar sem credenciais para disponibilizar a verificação de
disponibilidade e a documentação; o estado de prontidão devolve `503` até o
MySQL estar configurado e acessível.

Preencha `MYSQL_USER` e `MYSQL_PASSWORD` no `.env`, aplique as migrações e
inicie a API:

```powershell
python -m alembic upgrade head
python -m uvicorn main:app --reload --port 8000
```

Numa base de dados recém-migrada, crie o primeiro administrador a partir de
um terminal interativo. A palavra-passe não aparece no ecrã e deve ter pelo
menos 12 caracteres:

```powershell
python -m app.cli create-admin admin
```

Antes de iniciar a API, defina `AUTH_SECRET_KEY` com um valor aleatório e
privado de, pelo menos, 32 bytes. Gere-o localmente com
`python -c "import secrets; print(secrets.token_urlsafe(48))"` e guarde-o
apenas no `.env` ou no serviço de segredos do ambiente de publicação. Se não
for definido em desenvolvimento, a API usa uma chave temporária do processo,
e os tokens existentes deixam de funcionar após um reinício. Por predefinição,
os tokens expiram após uma hora (`ACCESS_TOKEN_EXPIRE_SECONDS`). As
palavras-passe são guardadas como hashes PBKDF2-HMAC-SHA256 com salt e nunca
são incluídas nas respostas da API.

A documentação interativa está em `/docs`; o contrato legível por máquina
está em `/openapi.json`. A disponibilidade do processo e a prontidão da base
de dados podem ser consultadas em `/health/live` e `/health/ready`. As
alterações de esquema devem ser adicionadas como revisões Alembic; não altere
manualmente uma base de dados publicada.

Execute os testes automatizados com `pytest`. Os testes usam SQLite em memória
e não requerem MySQL. O script SQL de preparação e a migração Alembic inicial
mantêm o mesmo modelo relacional.

## Estado do contrato da API

`app/openapi.json` define o contrato HTTP revisto para autenticação, contas,
catálogo, atribuições, sincronização e localização. Os identificadores das
entidades são UUID em formato de texto. Os endpoints de listagem usam
`limit`/`offset`; a sincronização incremental usa
`since_id`/`next_since_id`. Os erros seguem o formato
`{"error":{"code":"...","message":"...","fields":[]}}`. As respostas de erro
nunca incluem palavras-passe, tokens ou detalhes da base de dados.

A autenticação, a gestão de contas, rotas, paragens, veículos e atribuições,
bem como a leitura incremental de alterações do catálogo, estão implementadas.
Os códigos dos veículos são atribuídos com um bloqueio transacional sobre a
sequência de sincronização partilhada. As alterações do catálogo e os
respetivos eventos de sincronização são confirmados na mesma transação.

Os motoristas podem iniciar e terminar sessões autenticadas de partilha da
localização e publicar posições validadas do veículo ativo que lhes foi
atribuído. O endpoint paginado de localizações permite consultar os veículos
que partilham a posição recentemente. Ao terminar uma sessão, a posição
publicada é removida; as sessões sem atualizações expiram. Coordenadas GPS e
valores dos sensores são arredondados à precisão das colunas antes da
validação, evitando rejeitar leituras com casas decimais extra. Os testes de
localização usam SQLite em memória. O endpoint autenticado
`GET /api/v1/tracking/sessions/active?vehicle_id=...` permite ao Android
recuperar a sessão após reiniciar ou reinstalar; repetir o pedido de início
retoma a sessão ativa em vez de falhar com conflito. Antes de usar em produção, confirme no
MySQL configurado o comportamento dos bloqueios de linhas e das datas.
