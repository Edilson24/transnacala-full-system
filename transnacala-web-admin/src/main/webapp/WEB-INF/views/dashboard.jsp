<%@ include file="fragments/layout-start.jspf" %>
<section class="welcome-banner">
  <div><span class="eyebrow">OPERAÇÕES · NACALA</span><h2>Olá, <c:out value="${username}"/> <span>👋</span></h2><p>Uma visão clara da rede. Escolha uma área para continuar.</p></div>
  <div class="banner-art" aria-hidden="true"><span class="art-route"></span><span class="art-dot dot-a"></span><span class="art-dot dot-b"></span><span class="art-dot dot-c"></span><span class="art-bus">▰</span></div>
</section>
<section class="stats-grid" aria-label="Resumo da operação">
  <article class="stat-card"><div class="stat-top"><span>Utilizadores</span><span class="stat-icon icon-violet">◎</span></div><strong><c:out value="${users.size()}"/></strong><small>Contas registadas</small></article>
  <article class="stat-card"><div class="stat-top"><span>Rotas ativas</span><span class="stat-icon icon-blue">↗</span></div><strong><c:out value="${activeRouteCount}"/></strong><small>Disponíveis no catálogo</small></article>
  <article class="stat-card"><div class="stat-top"><span>Paragens</span><span class="stat-icon icon-orange">⌖</span></div><strong><c:out value="${stops.size()}"/></strong><small>Pontos de embarque</small></article>
  <article class="stat-card"><div class="stat-top"><span>Veículos</span><span class="stat-icon icon-green">▰</span></div><strong><c:out value="${vehicles.size()}"/></strong><small>Na frota cadastrada</small></article>
</section>
<section class="content-section">
  <div class="section-heading"><div><span class="eyebrow">ÁREAS DE GESTÃO</span><h2>Acesso rápido</h2><p>Os módulos agora têm páginas próprias para facilitar a operação.</p></div></div>
  <div class="admin-shortcuts">
    <a class="panel shortcut-card" href="${pageContext.request.contextPath}/admin/routes"><span class="panel-icon icon-blue">↗</span><span><strong>Rotas</strong><small>Percursos, tarifas e paragens associadas</small></span><b aria-hidden="true">→</b></a>
    <a class="panel shortcut-card" href="${pageContext.request.contextPath}/admin/team"><span class="panel-icon icon-violet">◎</span><span><strong>Utilizadores e frota</strong><small>Contas, motoristas e veículos</small></span><b aria-hidden="true">→</b></a>
    <a class="panel shortcut-card" href="${pageContext.request.contextPath}/admin/stops"><span class="panel-icon icon-orange">⌖</span><span><strong>Paragens</strong><small>Coordenadas e pontos já cadastrados</small></span><b aria-hidden="true">→</b></a>
  </div>
</section>
<%@ include file="fragments/layout-end.jspf" %>
