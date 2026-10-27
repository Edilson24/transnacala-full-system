<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/layout-start.jspf" %>
<section class="content-section live-map-page">
  <div class="section-heading">
    <div><span class="eyebrow">OPERAÇÕES · NACALA-PORTO</span><h2>Mapa de rotas e veículos</h2><p>Veja os percursos cadastrados e os veículos que estão a partilhar a localização.</p></div>
    <button class="button button-secondary" type="button" id="refresh-live-map"><span aria-hidden="true">↻</span> Atualizar agora</button>
  </div>
  <div class="live-map-summary" aria-live="polite">
    <article class="panel live-summary-card"><span class="live-summary-icon icon-blue">▰</span><span><strong id="live-vehicle-count">—</strong><small>Veículos com localização recente</small></span></article>
    <article class="panel live-summary-card"><span class="live-summary-icon icon-green"><i class="live-indicator"></i></span><span><strong id="live-map-status">A ligar ao serviço de localização…</strong><small id="live-map-updated">As posições atualizam automaticamente.</small></span></article>
  </div>
  <div class="live-map-layout">
    <section class="panel map-panel" aria-label="Mapa interativo de Nacala-Porto">
      <div id="nacala-map" class="nacala-map" role="application" aria-label="Mapa interativo de rotas e veículos em Nacala-Porto" data-live-url="${pageContext.request.contextPath}/admin/map/live"></div>
    </section>
    <aside class="map-sidebar">
      <section class="panel map-info-panel">
        <div class="map-panel-heading"><span class="panel-icon icon-orange">↗</span><div><h3>Percursos</h3><p>Traçado pelas paragens cadastradas</p></div></div>
        <div id="map-route-legend" class="map-route-legend" aria-live="polite"><p class="map-empty">A carregar rotas…</p></div>
      </section>
      <section class="panel map-info-panel">
        <div class="map-panel-heading"><span class="panel-icon icon-blue">▰</span><div><h3>Veículos ao vivo</h3><p>Posições partilhadas pelos motoristas</p></div></div>
        <div id="map-live-vehicles" class="map-live-vehicles" aria-live="polite"><p class="map-empty">A aguardar posições recentes…</p></div>
      </section>
      <p class="map-update-note"><span class="live-indicator"></span> Atualização automática a cada 15 segundos.</p>
    </aside>
  </div>
</section>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/assets/map.js" defer></script>
<%@ include file="fragments/layout-end.jspf" %>
