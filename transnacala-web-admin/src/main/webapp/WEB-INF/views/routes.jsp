<%@ include file="fragments/layout-start.jspf" %>
<section class="content-section">
  <div class="section-heading">
    <div><span class="eyebrow">CATÁLOGO</span><h2>Gestão de rotas</h2><p>Edite percursos e associe cada paragem à rota correspondente.</p></div>
    <button class="button button-primary" type="button" data-open-dialog="route-create-dialog"><span aria-hidden="true">＋</span> Nova rota</button>
  </div>
  <c:if test="${not empty routes}">
    <article class="panel table-panel route-table-panel">
      <div class="panel-heading panel-heading-inline"><div><h3>Rotas cadastradas</h3><p>As rotas inativas não aparecem para os passageiros.</p></div><span class="section-count"><c:out value="${routes.size()}"/> rotas</span></div>
      <div class="table-scroll"><table><thead><tr><th>Rota</th><th>Percurso</th><th>Paragens</th><th>Tarifa</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
        <c:forEach var="route" items="${routes}">
          <tr>
            <td><div class="route-name-cell"><span class="route-symbol">↗</span><strong><c:out value="${route.name}"/></strong></div></td>
            <td><c:out value="${route.origin}"/> <span class="muted">→</span> <c:out value="${route.destination}"/></td>
            <td><span class="route-stop-count"><c:out value="${route.stops.size()}"/></span><span class="muted"> pontos</span><c:if test="${not empty route.stops}"><details class="stop-preview"><summary>Ver ordem</summary><ol><c:forEach var="stop" items="${route.stops}"><li><c:out value="${stop.name}"/></li></c:forEach></ol></details></c:if></td>
            <td><strong><c:out value="${route.fare}"/> MT</strong></td>
            <td><c:choose><c:when test="${route.is_active}"><span class="state state-active">Ativa</span></c:when><c:otherwise><span class="state state-inactive">Inativa</span></c:otherwise></c:choose></td>
            <td><div class="row-actions">
              <button class="action-icon" type="button" title="Editar rota" aria-label="Editar rota <c:out value='${route.name}'/>" data-open-dialog="route-edit-dialog" data-route-id="<c:out value='${route.id}'/>" data-route-name="<c:out value='${route.name}'/>" data-route-origin="<c:out value='${route.origin}'/>" data-route-destination="<c:out value='${route.destination}'/>" data-route-fare="<c:out value='${route.fare}'/>" data-route-description="<c:out value='${route.description}'/>">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>
              </button>
              <button class="action-icon" type="button" title="Gerir paragens e ordem" aria-label="Gerir paragens da rota <c:out value='${route.name}'/>" data-open-dialog="route-stops-dialog-<c:out value='${route.id}'/>">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 6h13M8 12h13M8 18h13"/><path d="M3 6h.01M3 12h.01M3 18h.01"/></svg>
              </button>
              <button class="action-icon action-icon-add" type="button" title="Adicionar paragem a esta rota" aria-label="Adicionar paragem à rota <c:out value='${route.name}'/>" data-open-dialog="stop-create-dialog" data-route-id="<c:out value='${route.id}'/>" data-route-name="<c:out value='${route.name}'/>">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21s7-5.2 7-12a7 7 0 1 0-14 0c0 6.8 7 12 7 12Z"/><circle cx="12" cy="9" r="2.5"/><path d="M19 16h4m-2-2v4"/></svg>
              </button>
              <form action="${pageContext.request.contextPath}/admin/action" method="post" class="inline-form" data-confirm="Confirmar a alteração do estado desta rota?">
                <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="set-route-active"><input type="hidden" name="entity_id" value="<c:out value='${route.id}'/>">
                <c:choose><c:when test="${route.is_active}"><input type="hidden" name="active" value="false"><button class="action-icon action-icon-danger" type="submit" title="Remover do catálogo" aria-label="Remover do catálogo"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5m4-5v5"/></svg></button></c:when><c:otherwise><input type="hidden" name="active" value="true"><button class="action-icon" type="submit" title="Reativar rota" aria-label="Reativar rota"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 7v5h-5"/><path d="M20 12a8 8 0 1 1-2.3-5.7L20 9"/></svg></button></c:otherwise></c:choose>
              </form>
            </div></td>
          </tr>
        </c:forEach>
      </tbody></table></div>
    </article>
    <c:forEach var="route" items="${routes}">
      <dialog class="admin-dialog" id="route-stops-dialog-<c:out value='${route.id}'/>" aria-labelledby="route-stops-title-<c:out value='${route.id}'/>">
        <form action="${pageContext.request.contextPath}/admin/action" method="post" class="dialog-form route-stops-form">
          <div class="dialog-heading"><div><span class="eyebrow">PERCURSO</span><h2 id="route-stops-title-<c:out value='${route.id}'/>">Paragens da rota</h2><p>Selecione as paragens incluídas neste percurso.</p></div><button class="dialog-close" type="button" data-close-dialog aria-label="Fechar">×</button></div>
          <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="save-route-stops"><input type="hidden" name="route_id" value="<c:out value='${route.id}'/>"><input type="hidden" name="stop_ids" value="">
          <label>Ordem do percurso <span class="optional">reordene, remova ou adicione pontos existentes</span></label>
          <ol class="route-stop-list" data-route-stop-list>
            <c:forEach var="routeStop" items="${route.stops}">
              <li data-stop-id="<c:out value='${routeStop.stop_id}'/>"><span class="route-stop-name"><c:out value="${routeStop.name}"/></span><span class="stop-order-actions"><button type="button" data-move-stop="up" aria-label="Mover <c:out value='${routeStop.name}'/> para cima">↑</button><button type="button" data-move-stop="down" aria-label="Mover <c:out value='${routeStop.name}'/> para baixo">↓</button><button type="button" data-remove-stop>Remover</button></span></li>
            </c:forEach>
          </ol>
          <p class="empty-stop-list" data-empty-stop-message <c:if test="${not empty route.stops}">hidden</c:if>>Esta rota ainda não tem paragens.</p>
          <div class="add-existing-stop"><label for="available-stops-<c:out value='${route.id}'/>">Adicionar paragem existente<select class="available-stop-picker" id="available-stops-<c:out value='${route.id}'/>"><option value="">Selecione um ponto ativo</option><c:forEach var="stop" items="${stops}"><c:if test="${stop.is_active}"><option value="<c:out value='${stop.id}'/>"><c:out value="${stop.name}"/></option></c:if></c:forEach></select></label><button class="button button-secondary button-small" type="button" data-add-route-stop>Adicionar</button></div>
          <p class="stop-picker-message" aria-live="polite"></p>
          <div class="dialog-actions"><button class="button button-secondary" type="button" data-close-dialog>Cancelar</button><button class="button button-primary" type="submit">Guardar paragens</button></div>
        </form>
      </dialog>
    </c:forEach>
  </c:if>
  <c:if test="${empty routes}"><article class="panel empty-panel">Ainda não existem rotas. Use “Nova rota” para cadastrar o primeiro percurso.</article></c:if>
</section>

<dialog class="admin-dialog" id="route-create-dialog" aria-labelledby="route-create-title">
  <form action="${pageContext.request.contextPath}/admin/action" method="post" class="dialog-form">
    <div class="dialog-heading"><div><span class="eyebrow">CATÁLOGO</span><h2 id="route-create-title">Nova rota</h2><p>Registe o percurso; as paragens podem ser adicionadas depois.</p></div><button class="dialog-close" type="button" data-close-dialog aria-label="Fechar">×</button></div>
    <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="create-route">
    <label>Nome da rota<input name="name" maxlength="120" required placeholder="ex.: Centro · Porto"></label>
    <div class="field-row"><label>Origem<input name="origin" maxlength="160" required></label><label>Destino<input name="destination" maxlength="160" required></label></div>
    <label>Tarifa (MT)<input name="fare" type="number" min="0" step="0.01" required placeholder="0.00"></label>
    <label>Descrição <span class="optional">opcional</span><textarea name="description" rows="3" maxlength="1000" placeholder="Detalhes do percurso ou informações por confirmar"></textarea></label>
    <div class="dialog-actions"><button class="button button-secondary" type="button" data-close-dialog>Cancelar</button><button class="button button-primary" type="submit">Criar rota</button></div>
  </form>
</dialog>

<dialog class="admin-dialog" id="route-edit-dialog" aria-labelledby="route-edit-title">
  <form action="${pageContext.request.contextPath}/admin/action" method="post" class="dialog-form">
    <div class="dialog-heading"><div><span class="eyebrow">CATÁLOGO</span><h2 id="route-edit-title">Editar rota</h2><p>Atualize os dados principais do percurso.</p></div><button class="dialog-close" type="button" data-close-dialog aria-label="Fechar">×</button></div>
    <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="update-route"><input type="hidden" name="entity_id" data-route-field="id">
    <label>Nome da rota<input name="name" maxlength="120" required data-route-field="name"></label>
    <div class="field-row"><label>Origem<input name="origin" maxlength="160" required data-route-field="origin"></label><label>Destino<input name="destination" maxlength="160" required data-route-field="destination"></label></div>
    <label>Tarifa (MT)<input name="fare" type="number" min="0" step="0.01" required data-route-field="fare"></label>
    <label>Descrição <span class="optional">opcional</span><textarea name="description" rows="3" maxlength="1000" data-route-field="description"></textarea></label>
    <div class="dialog-actions"><button class="button button-secondary" type="button" data-close-dialog>Cancelar</button><button class="button button-primary" type="submit">Guardar alterações</button></div>
  </form>
</dialog>

<dialog class="admin-dialog" id="stop-create-dialog" aria-labelledby="stop-create-title">
  <form action="${pageContext.request.contextPath}/admin/action" method="post" class="dialog-form">
    <div class="dialog-heading"><div><span class="eyebrow">PONTO DE EMBARQUE</span><h2 id="stop-create-title">Adicionar paragem</h2><p>A paragem ficará associada à rota <strong data-stop-route-name></strong>.</p></div><button class="dialog-close" type="button" data-close-dialog aria-label="Fechar">×</button></div>
    <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="create-stop-for-route"><input type="hidden" name="route_id" data-stop-route-id>
    <label>Nome da paragem<input name="name" maxlength="160" required placeholder="ex.: Mercado Central"></label>
    <div class="field-row"><label>Latitude<input name="latitude" type="number" min="-90" max="90" step="0.0000001" required placeholder="-14.5428"></label><label>Longitude<input name="longitude" type="number" min="-180" max="180" step="0.0000001" required placeholder="40.6729"></label></div>
    <div class="dialog-actions"><button class="button button-secondary" type="button" data-close-dialog>Cancelar</button><button class="button button-primary" type="submit">Adicionar à rota</button></div>
  </form>
</dialog>
<%@ include file="fragments/layout-end.jspf" %>
