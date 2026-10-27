# Painel administrativo TransNacala

Painel web Java para gerir contas, rotas, paragens, veículos e atribuições
através da API REST TransNacala. O navegador não recebe o token da API e o
painel não se liga diretamente ao MySQL.

## Requisitos

- JDK 11 ou superior.
- Maven 3.8 ou superior.
- Apache Tomcat 10.1.x (versão de referência: 10.1.57).
- API TransNacala em execução e uma conta com perfil de administrador.

## Compilar e executar

Na pasta `transnacala-web-admin`, crie o ficheiro WAR:

```powershell
mvn clean package
```

A aplicação usa Jakarta Servlet 6.0 e JSP 3.1; não é compatível com Tomcat 9
nem com ambientes `javax.servlet`. Copie
`target/transnacala-web-admin.war` para a pasta `webapps` do Tomcat 10.1,
inicie o Tomcat e abra `http://localhost:8080/transnacala-web-admin/`. O nome
do WAR define o caminho da aplicação. A raiz encaminha para a página de início
de sessão. Antes de abrir o painel, confirme nos registos
`logs/catalina.*.log` que o Tomcat publicou a aplicação
`/transnacala-web-admin`.

Configure o endereço da API no processo do Tomcat antes de o iniciar:

```powershell
$env:TRANSNACALA_API_URL = "http://127.0.0.1:8000"
```

Quando o Tomcat e a API estão no mesmo computador, este endereço local é
suficiente; não é necessário iniciar o LocalTunnel. Use um túnel HTTPS apenas
se o Tomcat tiver de aceder a uma API noutra rede ou se a API de
desenvolvimento precisar de ser exposta externamente.

No IntelliJ com SmartTomcat, volte a carregar o projeto Maven depois de alterar
o `pom.xml` e reinicie a configuração SmartTomcat. Uma configuração existente
pode manter dependências antigas `javax.servlet` ou JSTL 1.2.

## Resolução de problemas

- Se o formulário indicar que não consegue contactar a API, inicie o FastAPI
  e confirme que `http://127.0.0.1:8000/health/live` responde. Se a API usar
  outro endereço, defina `TRANSNACALA_API_URL` na configuração do Tomcat e
  reinicie-o.
- Se aparecerem os avisos Uvicorn `Unsupported upgrade request` ou
  `Invalid HTTP request received`, atualize e volte a publicar o WAR; o
  cliente administrativo usa HTTP/1.1.
- Se a base de dados não estiver configurada ou acessível, defina
  `MYSQL_USER` e `MYSQL_PASSWORD` (ou `DATABASE_URL`) no ficheiro `.env` da
  API, confirme que o MySQL está em execução e reinicie a API. Consulte
  `http://127.0.0.1:8000/health/ready`.
- `TRANSNACALA_API_URL` deve apontar para a API, por exemplo
  `http://127.0.0.1:8000`; não deve apontar para o Tomcat nem terminar em
  `/api/v1`.
- Se o Tomcat apresentar 404, confirme que
  `webapps/transnacala-web-admin.war` existe, que a publicação terminou sem
  erros e que o endereço corresponde ao nome do WAR. A causa de falhas de
  publicação aparece em `logs/catalina.*.log`.

Em ambientes remotos, proteja tanto a API como o painel com HTTPS. O token da
API fica apenas na sessão do servidor; o cookie da sessão é HttpOnly e os
formulários usam tokens CSRF. Não use túneis de desenvolvimento em produção.

## Funcionalidades administrativas

- Autenticação de administradores e expiração de sessão.
- Páginas separadas para visão geral, rotas, equipa/frota, paragens e mapa em
  tempo real.
- Criação de rotas num formulário modal, edição e desativação no catálogo de
  passageiros.
- Gestão da ordem das paragens e adição de novas paragens associadas à rota
  escolhida.
- Criação e gestão de contas, alteração de palavra-passe e ativação ou
  desativação de utilizadores.
- Criação de veículos e atribuição de motoristas ativos e rotas.
- Mapa interativo de Nacala-Porto com linhas coloridas a partir das
  coordenadas das paragens e posições recentes partilhadas pelos motoristas.
  As posições são atualizadas automaticamente a cada 15 segundos. O mapa
  requer acesso à Internet para carregar os mapas OpenStreetMap. A aplicação
  conserva a atribuição visível e permite o envio da referência HTTP exigida
  pelo serviço público de mapas.
- Página pública de distribuição Android na raiz do contexto web. Para
  publicar uma atualização, gere o APK assinado da versão e coloque-o em
  `src/main/webapp/downloads/transnacala-mobile.apk` antes de criar o WAR.
  Mantenha esse caminho fixo para que a página pública passe a servir a nova
  versão automaticamente; aumente `versionCode`/`versionName` e reutilize a
  mesma chave de assinatura para permitir atualizações nos dispositivos.
  O APK não é incluído no repositório; sem o ficheiro, a página informa que a
  aplicação ainda não está disponível. O endereço público do Tomcat pode ser
  exposto pelo túnel HTTPS, sem expor o MySQL.

As páginas administrativas pedem até 100 registos por coleção. A API continua
a ser responsável pelas permissões, validações, códigos únicos e sincronização
do catálogo.
