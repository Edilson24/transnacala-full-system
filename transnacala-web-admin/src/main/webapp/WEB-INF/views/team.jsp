<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="fragments/layout-start.jspf" %>
<section class="content-section">
  <div class="section-heading"><div><span class="eyebrow">EQUIPA</span><h2>Utilizadores</h2><p>Crie e administre contas de administradores, motoristas e passageiros.</p></div><span class="section-count"><c:out value="${users.size()}"/> contas</span></div>
  <div class="workspace-grid team-workspace">
    <article class="panel form-panel">
      <div class="panel-heading"><span class="panel-icon icon-violet">＋</span><div><h3>Novo utilizador</h3><p>Credenciais de acesso à plataforma</p></div></div>
      <form action="${pageContext.request.contextPath}/admin/action" method="post" class="form-stack">
        <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="create-user">
        <label>Nome completo<input name="full_name" maxlength="160" required placeholder="Nome do utilizador"></label>
        <label>Nome de utilizador<input name="username" maxlength="80" required placeholder="ex.: joao.mateus"></label>
        <label>Palavra-passe<input name="password" type="password" minlength="12" required autocomplete="new-password" placeholder="Mínimo de 12 caracteres"></label>
        <label>Telefone <span class="optional">opcional</span><input name="phone" maxlength="32" placeholder="+258 8x xxx xxxx"></label>
        <label>Perfil<select name="role" required><option value="DRIVER">Motorista</option><option value="PASSENGER">Passageiro</option><option value="ADMIN">Administrador</option></select></label>
        <button class="button button-primary" type="submit">Criar conta <span aria-hidden="true">→</span></button>
      </form>
    </article>
    <article class="panel table-panel">
      <div class="panel-heading panel-heading-inline"><div><h3>Contas registadas</h3><p>Acesso e estado das contas</p></div></div>
      <div class="table-scroll"><table><thead><tr><th>Utilizador</th><th>Perfil</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
        <c:forEach var="user" items="${users}">
          <tr>
            <td><div class="person-cell"><span class="mini-avatar"><c:out value="${fn:substring(user.full_name, 0, 1)}"/></span><span><strong><c:out value="${user.full_name}"/></strong><small>@<c:out value="${user.username}"/></small></span></div></td>
            <td><span class="role-pill"><c:out value="${user.role}"/></span></td>
            <td><c:choose><c:when test="${user.is_active}"><span class="state state-active">Ativo</span></c:when><c:otherwise><span class="state state-inactive">Inativo</span></c:otherwise></c:choose></td>
            <td>
              <details class="edit-details"><summary>Editar</summary><form action="${pageContext.request.contextPath}/admin/action" method="post" class="mini-edit-form"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="update-user"><input type="hidden" name="entity_id" value="<c:out value='${user.id}'/>"><label>Nome<input name="full_name" value="<c:out value='${user.full_name}'/>" required></label><label>Telefone<input name="phone" value="<c:out value='${user.phone}'/>"></label><label>Nova palavra-passe <span class="optional">opcional</span><input name="password" type="password" minlength="12" autocomplete="new-password"></label><label>Perfil<select name="role"><option value="ADMIN" <c:if test="${user.role == 'ADMIN'}">selected</c:if>>Administrador</option><option value="DRIVER" <c:if test="${user.role == 'DRIVER'}">selected</c:if>>Motorista</option><option value="PASSENGER" <c:if test="${user.role == 'PASSENGER'}">selected</c:if>>Passageiro</option></select></label><button class="text-button" type="submit">Guardar edição</button></form></details>
              <form action="${pageContext.request.contextPath}/admin/action" method="post" class="inline-form"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="set-user-active"><input type="hidden" name="entity_id" value="<c:out value='${user.id}'/>"><c:choose><c:when test="${user.is_active}"><input type="hidden" name="active" value="false"><button class="text-button" type="submit">Desativar</button></c:when><c:otherwise><input type="hidden" name="active" value="true"><button class="text-button" type="submit">Ativar</button></c:otherwise></c:choose></form>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty users}"><tr><td colspan="4" class="empty-cell">Ainda não há utilizadores cadastrados.</td></tr></c:if>
      </tbody></table></div>
    </article>
  </div>
</section>
<section class="content-section">
  <div class="section-heading"><div><span class="eyebrow">FROTA</span><h2>Veículos</h2><p>Associe motoristas e rotas ativas aos veículos em operação.</p></div><span class="section-count"><c:out value="${vehicles.size()}"/> veículos</span></div>
  <article class="panel table-panel vehicle-panel">
    <div class="panel-heading panel-heading-inline"><div><h3>Frota cadastrada</h3><p>Os códigos são gerados automaticamente pela API.</p></div><form action="${pageContext.request.contextPath}/admin/action" method="post"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="create-vehicle"><button class="button button-primary button-small" type="submit">＋ Novo veículo</button></form></div>
    <div class="table-scroll"><table><thead><tr><th>Veículo</th><th>Motorista</th><th>Rota</th><th>Estado</th><th>Ações</th></tr></thead><tbody>
      <c:forEach var="vehicle" items="${vehicles}">
        <tr><td><span class="vehicle-code"><c:out value="${vehicle.code}"/></span></td>
          <td colspan="2"><form action="${pageContext.request.contextPath}/admin/action" method="post" class="assignment-form"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="assign-vehicle"><input type="hidden" name="vehicle_id" value="<c:out value='${vehicle.id}'/>">
            <select name="driver_user_id" aria-label="Motorista do veículo"><option value="">Sem motorista</option><c:forEach var="user" items="${users}"><c:if test="${user.role == 'DRIVER' && user.is_active}"><option value="<c:out value='${user.id}'/>" <c:if test="${vehicle.driver_user_id == user.id}">selected</c:if>><c:out value="${user.full_name}"/></option></c:if></c:forEach></select>
            <select name="route_id" aria-label="Rota do veículo"><option value="">Sem rota</option><c:forEach var="route" items="${routes}"><c:if test="${route.is_active}"><option value="<c:out value='${route.id}'/>" <c:if test="${vehicle.route_id == route.id}">selected</c:if>><c:out value="${route.name}"/></option></c:if></c:forEach></select><button class="text-button" type="submit">Guardar atribuição</button>
          </form></td>
          <td><c:choose><c:when test="${vehicle.is_active}"><span class="state state-active">Ativo</span></c:when><c:otherwise><span class="state state-inactive">Inativo</span></c:otherwise></c:choose></td>
          <td><form action="${pageContext.request.contextPath}/admin/action" method="post" class="inline-form"><input type="hidden" name="csrf" value="<c:out value='${csrf}'/>"><input type="hidden" name="action" value="set-vehicle-active"><input type="hidden" name="entity_id" value="<c:out value='${vehicle.id}'/>"><c:choose><c:when test="${vehicle.is_active}"><input type="hidden" name="active" value="false"><button class="text-button" type="submit">Desativar</button></c:when><c:otherwise><input type="hidden" name="active" value="true"><button class="text-button" type="submit">Reativar</button></c:otherwise></c:choose></form></td>
        </tr>
      </c:forEach>
      <c:if test="${empty vehicles}"><tr><td colspan="5" class="empty-cell">A frota ainda está vazia. Adicione o primeiro veículo.</td></tr></c:if>
    </tbody></table></div>
  </article>
</section>
<%@ include file="fragments/layout-end.jspf" %>
