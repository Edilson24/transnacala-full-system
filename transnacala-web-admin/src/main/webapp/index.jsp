<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  boolean mobileApkAvailable =
      application.getResource("/downloads/transnacala-mobile.apk") != null;
%>
<!DOCTYPE html>
<html lang="pt">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Descarregue a aplicação móvel TransNacala.">
  <title>TransNacala · Aplicação móvel</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/download.css">
</head>
<body>
  <main class="download-page">
    <header class="download-header">
      <a class="brand" href="<%= request.getContextPath() %>/">
        <span class="brand-mark">TN</span>
        <span>trans<span class="brand-accent">nacala</span></span>
      </a>
      <a class="admin-link" href="<%= request.getContextPath() %>/login">Painel administrativo</a>
    </header>

    <section class="download-content" aria-labelledby="download-title">
      <p class="eyebrow">MOBILIDADE EM NACALA</p>
      <h1 id="download-title">A sua viagem começa aqui.</h1>
      <p class="intro">Consulte rotas, paragens e veículos a partilhar a localização em tempo real.</p>
      <% if (mobileApkAvailable) { %>
        <a class="download-button" href="<%= request.getContextPath() %>/downloads/transnacala-mobile.apk" download>
          <span class="download-icon" aria-hidden="true">↓</span>
          <span><strong>Descarregar para Android</strong><small>Última versão publicada · ficheiro APK</small></span>
        </a>
        <p class="install-note">Depois de descarregar, abra o ficheiro APK no seu dispositivo Android para instalar a aplicação.</p>
      <% } else { %>
        <div class="unavailable" role="status">
          <strong>A aplicação estará disponível em breve.</strong>
          <span>A equipa está a preparar a próxima versão para Android.</span>
        </div>
      <% } %>
    </section>

    <footer class="download-footer">
      <span>TransNacala · Nacala-Porto, Moçambique</span>
      <a href="<%= request.getContextPath() %>/login">Acesso da administração</a>
    </footer>
  </main>
</body>
</html>
