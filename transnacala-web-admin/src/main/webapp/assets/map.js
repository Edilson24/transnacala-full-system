(function () {
  var mapElement = document.getElementById("nacala-map");
  if (!mapElement) return;
  if (typeof L === "undefined") {
    document.getElementById("live-map-status").textContent = "Mapa indisponível";
    document.getElementById("live-map-updated").textContent =
      "Não foi possível carregar a biblioteca do mapa. Verifique a ligação à Internet.";
    document.getElementById("map-route-legend").textContent =
      "As rotas não podem ser desenhadas sem a biblioteca do mapa.";
    return;
  }

  var palette = ["#ed8a24", "#268b70", "#8263bb", "#397bb5", "#cc5b51", "#bc9a2c"];
  var map = L.map(mapElement, { zoomControl: true }).setView([-14.542, 40.672], 13);
  var tiles = L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap contributors</a> · <a href=\"https://www.openstreetmap.org/fixthemap\">Reportar problema no mapa</a>"
  }).addTo(map);
  tiles.on("tileerror", function () {
    updated.textContent = "O servidor de mapas não autorizou o pedido. Verifique a ligação ou tente novamente mais tarde.";
  });

  var routeLayer = L.featureGroup().addTo(map);
  var vehicleLayer = L.featureGroup().addTo(map);
  var hasFocusedVehicles = false;
  var requestInProgress = false;
  var refreshTimer;
  var routeLegend = document.getElementById("map-route-legend");
  var vehicleList = document.getElementById("map-live-vehicles");
  var status = document.getElementById("live-map-status");
  var updated = document.getElementById("live-map-updated");
  var vehicleCount = document.getElementById("live-vehicle-count");

  function addText(parent, tag, className, text) {
    var element = document.createElement(tag);
    if (className) element.className = className;
    element.textContent = text;
    parent.appendChild(element);
    return element;
  }

  function renderRoutes(routes) {
    routeLayer.clearLayers();
    routeLegend.replaceChildren();
    var colorsByRoute = {};
    var drawableRoutes = 0;
    var otherRouteIndex = 0;

    routes.forEach(function (route) {
      var isBaixaAlta = /^baixa\s*[–-]\s*alta$/i.test(route.name || "");
      var color = isBaixaAlta
        ? palette[0]
        : palette[1 + (otherRouteIndex++ % (palette.length - 1))];
      colorsByRoute[route.id] = color;
      var stops = Array.isArray(route.stops) ? route.stops : [];
      var coordinates = stops.map(function (stop) {
        return [Number(stop.latitude), Number(stop.longitude)];
      }).filter(function (point) {
        return Number.isFinite(point[0]) && Number.isFinite(point[1]);
      });

      if (coordinates.length > 1) {
        var line = L.polyline(coordinates, {
          color: color,
          weight: 5,
          opacity: route.is_active ? 0.9 : 0.48,
          dashArray: route.is_active ? null : "9 8",
          lineCap: "round",
          lineJoin: "round"
        }).addTo(routeLayer);
        var tooltip = document.createElement("span");
        tooltip.textContent = route.name || "Rota sem nome";
        line.bindTooltip(tooltip, { sticky: true });
        drawableRoutes++;
      }

      var row = document.createElement("div");
      row.className = "map-route-row";
      var swatch = document.createElement("span");
      swatch.className = "map-route-swatch" + (route.is_active ? "" : " map-route-swatch-inactive");
      swatch.style.setProperty("--route-color", color);
      row.appendChild(swatch);
      var details = document.createElement("span");
      details.className = "map-route-details";
      addText(details, "strong", "", route.name || "Rota sem nome");
      addText(details, "small", "", coordinates.length > 1
        ? (coordinates.length + " paragens · " + (route.is_active ? "ativa" : "inativa"))
        : "Sem coordenadas suficientes para desenhar o percurso");
      row.appendChild(details);
      routeLegend.appendChild(row);
    });

    if (routes.length === 0) {
      addText(routeLegend, "p", "map-empty", "Ainda não existem rotas cadastradas.");
    } else if (drawableRoutes === 0) {
      addText(routeLegend, "p", "map-empty", "As rotas precisam de pelo menos duas paragens com coordenadas para desenhar linhas.");
    }
    return colorsByRoute;
  }

  function markerPopup(location, routeName) {
    var content = document.createElement("div");
    content.className = "map-popup";
    addText(content, "strong", "", location.vehicle_code || "Veículo");
    addText(content, "span", "", routeName || "Rota não atribuída");
    var position = new Date(location.updated_at);
    addText(content, "small", "", "Atualizado às " + (Number.isNaN(position.getTime())
      ? "—"
      : new Intl.DateTimeFormat("pt-MZ", { hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(position)));
    return content;
  }

  function renderVehicles(locations, colorsByRoute) {
    vehicleLayer.clearLayers();
    vehicleList.replaceChildren();
    vehicleCount.textContent = String(locations.length);

    locations.forEach(function (location) {
      var latitude = Number(location.latitude);
      var longitude = Number(location.longitude);
      if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return;
      var color = colorsByRoute[location.route_id] || "#1d7453";
      var icon = L.divIcon({
        className: "vehicle-marker-wrap",
        html: '<span class="vehicle-map-marker" aria-hidden="true">▰</span>',
        iconSize: [38, 38],
        iconAnchor: [19, 19]
      });
      var marker = L.marker([latitude, longitude], { icon: icon, title: location.vehicle_code || "Veículo" })
        .addTo(vehicleLayer);
      marker.getElement().style.setProperty("--vehicle-color", color);
      marker.bindPopup(markerPopup(location, location.route_name));

      var row = document.createElement("button");
      row.type = "button";
      row.className = "map-vehicle-row";
      row.setAttribute("aria-label", "Localizar veículo " + (location.vehicle_code || ""));
      var dot = document.createElement("span");
      dot.className = "map-vehicle-dot";
      dot.style.setProperty("--route-color", color);
      row.appendChild(dot);
      var details = document.createElement("span");
      details.className = "map-vehicle-details";
      addText(details, "strong", "", location.vehicle_code || "Veículo");
      addText(details, "small", "", location.route_name || "Rota não atribuída");
      row.appendChild(details);
      row.addEventListener("click", function () {
        map.setView([latitude, longitude], Math.max(map.getZoom(), 15));
        marker.openPopup();
      });
      vehicleList.appendChild(row);
    });

    if (vehicleLayer.getLayers().length === 0) {
      addText(vehicleList, "p", "map-empty", "Nenhum veículo está a partilhar uma localização recente.");
    } else if (!hasFocusedVehicles) {
      map.fitBounds(vehicleLayer.getBounds().pad(0.18), { maxZoom: 14 });
      hasFocusedVehicles = true;
    }
  }

  async function refreshLiveMap() {
    if (requestInProgress) return;
    requestInProgress = true;
    status.textContent = "A atualizar…";
    try {
      var response = await fetch(mapElement.dataset.liveUrl, {
        credentials: "same-origin",
        headers: { Accept: "application/json" },
        cache: "no-store"
      });
      if (response.status === 401) {
        window.location.assign(mapElement.dataset.liveUrl.replace(/\/admin\/map\/live$/, "/login?expired=1"));
        return;
      }
      var data = await response.json();
      if (!response.ok) throw new Error(data.error || "Não foi possível consultar os dados do mapa.");
      var colorsByRoute = renderRoutes(Array.isArray(data.routes) ? data.routes : []);
      renderVehicles(Array.isArray(data.locations) ? data.locations : [], colorsByRoute);
      status.textContent = "Mapa atualizado";
      updated.textContent = "Última atualização: " + new Intl.DateTimeFormat("pt-MZ", {
        hour: "2-digit", minute: "2-digit", second: "2-digit"
      }).format(new Date());
    } catch (error) {
      status.textContent = "Falha na atualização";
      updated.textContent = error.message || "Verifique a ligação à API.";
    } finally {
      requestInProgress = false;
    }
  }

  document.getElementById("refresh-live-map").addEventListener("click", refreshLiveMap);
  refreshLiveMap();
  refreshTimer = window.setInterval(refreshLiveMap, 15000);
  window.addEventListener("beforeunload", function () {
    window.clearInterval(refreshTimer);
  });
})();
