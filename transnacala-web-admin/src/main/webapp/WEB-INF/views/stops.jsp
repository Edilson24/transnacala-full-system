<%@ include file="fragments/layout-start.jspf" %>
<section class="content-section">
  <div class="section-heading"><div><span class="eyebrow">PONTOS DE EMBARQUE</span><h2>Paragens</h2><p>Consulte e atualize os locais e respetivas coordenadas.</p></div><span class="section-count"><c:out value="${stops.size()}"/> paragens</span></div>
  <div class="notice notice-info"><strong>Para manter o catálogo organizado:</strong> as novas paragens são criadas dentro da página de <a href="${pageContext.request.contextPath}/admin/routes">Rotas</a>, através da ação “Adicionar paragem” da rota correspondente.</div>
  <article class="panel table-panel">
    <div class="panel-heading panel-heading-inline"><div><h3>Paragens no catálogo</h3><p>Coordenadas e disponibilidade</p></div></div>
    <div class="table-scroll"><table><thead><tr><th>Nome</th><th>Latitude</th><th>Longitude</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
      <c:forEach var="stop" items="${stops}">
        <tr><td><strong><c:out value="${stop.name}"/></strong></td><td class="mono"><c:out value="${stop.latitude}"/></td><td class="mono"><c:out value="${stop.longitude}"/></td><td><c:choose><c:when test="${stop.is_active}"><span class="state state-active">Ativa</span></c:when><c:otherwise><span class="state state-inactive">Inativa</span></c:otherwise></c:choose></td>
          <td>
            <details class="edit-details"><summary>Editar</summary><form action="${pageContext.request.contextPath}/admin/action" method="post" class="mini-edit-form"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="update-stop"><input type="hidden" name="entity_id" value="<c:out value='${stop.id}'/>"><label>Nome<input name="name" maxlength="160" value="<c:out value='${stop.name}'/>" required></label><label>Latitude<input name="latitude" type="number" min="-90" max="90" step="0.0000001" value="<c:out value='${stop.latitude}'/>" required></label><label>Longitude<input name="longitude" type="number" min="-180" max="180" step="0.0000001" value="<c:out value='${stop.longitude}'/>" required></label><button class="text-button" type="submit">Guardar</button></form></details>
            <form action="${pageContext.request.contextPath}/admin/action" method="post" class="inline-form"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="set-stop-active"><input type="hidden" name="entity_id" value="<c:out value='${stop.id}'/>"><c:choose><c:when test="${stop.is_active}"><input type="hidden" name="active" value="false"><button class="text-button" type="submit">Desativar</button></c:when><c:otherwise><input type="hidden" name="active" value="true"><button class="text-button" type="submit">Reativar</button></c:otherwise></c:choose></form>
          </td>
        </tr>
      </c:forEach>
      <c:if test="${empty stops}"><tr><td colspan="5" class="empty-cell">Ainda não há paragens cadastradas. Adicione uma pela página de Rotas.</td></tr></c:if>
    </tbody></table></div>
  </article>
</section>
<%@ include file="fragments/layout-end.jspf" %>
