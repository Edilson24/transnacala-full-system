<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="pt">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Entrar · TransNacala</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin.css">
</head>
<body class="login-page">
  <main class="login-shell">
    <section class="login-visual">
      <div class="brand brand-light"><span class="brand-mark">TN</span><span>trans<span>nacala</span></span></div>
      <div class="visual-copy">
        <span class="eyebrow">CENTRO DE OPERAÇÕES</span>
        <h1>A cidade<br>em movimento.</h1>
        <p>Rotas, equipas e veículos — tudo sob controlo, num só lugar.</p>
      </div>
      <div class="visual-footer"><span class="pulse"></span> Plataforma de mobilidade · Nacala-Porto</div>
      <div class="visual-orbit orbit-one"></div><div class="visual-orbit orbit-two"></div>
    </section>
    <section class="login-panel">
      <div class="login-card">
        <div class="mobile-brand brand"><span class="brand-mark">TN</span><span>trans<span>nacala</span></span></div>
        <span class="eyebrow">BEM-VINDO DE VOLTA</span>
        <h2>Entrar na plataforma</h2>
        <p class="muted">Use as credenciais da sua conta de administrador.</p>
        <c:if test="${param.expired == '1'}"><div class="notice notice-warning">A sessão terminou. Entre novamente.</div></c:if>
        <c:if test="${not empty error}"><div class="notice notice-error"><c:out value="${error}"/></div></c:if>
        <form action="${pageContext.request.contextPath}/login" method="post" class="form-stack">
          <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrf}'/>">
          <label for="username">Utilizador</label>
          <input id="username" name="username" autocomplete="username" placeholder="ex.: admin" required autofocus>
          <label for="password">Palavra-passe</label>
          <input id="password" name="password" type="password" autocomplete="current-password" placeholder="A sua palavra-passe" required>
          <button class="button button-primary button-wide" type="submit">Entrar <span aria-hidden="true">→</span></button>
        </form>
        <div class="login-security"><span class="lock-icon">⌑</span> Ligação protegida · Acesso exclusivo a administradores</div>
      </div>
      <footer class="login-copyright">© TransNacala · Mobilidade urbana</footer>
    </section>
  </main>
</body>
</html>
