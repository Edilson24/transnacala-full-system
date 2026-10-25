document.querySelectorAll(".route-stops-form").forEach(function (form) {
  form.addEventListener("submit", function () {
    var ids = Array.from(form.querySelectorAll("[data-route-stop-list] li"))
      .map(function (item) { return item.dataset.stopId; });
    form.querySelector("input[name='stop_ids']").value = ids.join(",");
  });
});

function updateEmptyStopMessage(dialog) {
  var list = dialog.querySelector("[data-route-stop-list]");
  var message = dialog.querySelector("[data-empty-stop-message]");
  message.hidden = list.children.length > 0;
}

function createRouteStopItem(stopId, stopName) {
  var item = document.createElement("li");
  item.dataset.stopId = stopId;
  var name = document.createElement("span");
  name.className = "route-stop-name";
  name.textContent = stopName;
  var actions = document.createElement("span");
  actions.className = "stop-order-actions";
  [
    { label: "↑", action: "up", title: "Mover " + stopName + " para cima" },
    { label: "↓", action: "down", title: "Mover " + stopName + " para baixo" },
    { label: "Remover", action: "remove", title: "Remover " + stopName + " da rota" }
  ].forEach(function (definition) {
    var button = document.createElement("button");
    button.type = "button";
    button.textContent = definition.label;
    button.title = definition.title;
    if (definition.action === "remove") button.dataset.removeStop = "";
    else button.dataset.moveStop = definition.action;
    actions.appendChild(button);
  });
  item.append(name, actions);
  return item;
}

document.querySelectorAll("[data-add-route-stop]").forEach(function (button) {
  button.addEventListener("click", function () {
    var dialog = button.closest("dialog");
    var picker = dialog.querySelector(".available-stop-picker");
    var option = picker.selectedOptions[0];
    if (!option || !option.value) {
      picker.reportValidity();
      return;
    }
    var list = dialog.querySelector("[data-route-stop-list]");
    var alreadyAdded = Array.from(list.querySelectorAll("li")).some(function (item) {
      return item.dataset.stopId === option.value;
    });
    if (alreadyAdded) {
      dialog.querySelector(".stop-picker-message").textContent =
        "Essa paragem já está associada a esta rota.";
      return;
    }
    list.appendChild(createRouteStopItem(option.value, option.textContent));
    dialog.querySelector(".stop-picker-message").textContent = "";
    picker.value = "";
    updateEmptyStopMessage(dialog);
  });
});

document.querySelectorAll("[data-route-stop-list]").forEach(function (list) {
  list.addEventListener("click", function (event) {
    var button = event.target.closest("button");
    if (!button) return;
    var item = button.closest("li");
    if (button.hasAttribute("data-remove-stop")) {
      item.remove();
    } else if (button.dataset.moveStop === "up" && item.previousElementSibling) {
      list.insertBefore(item, item.previousElementSibling);
    } else if (button.dataset.moveStop === "down" && item.nextElementSibling) {
      list.insertBefore(item.nextElementSibling, item);
    }
    updateEmptyStopMessage(list.closest("dialog"));
  });
});

document.querySelectorAll("[data-open-dialog]").forEach(function (button) {
  button.addEventListener("click", function () {
    var dialog = document.getElementById(button.dataset.openDialog);
    if (!dialog) return;

    if (dialog.id === "route-edit-dialog") {
      dialog.querySelectorAll("[data-route-field]").forEach(function (field) {
        var fieldName = field.dataset.routeField;
        var dataKey = "route" + fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
        field.value = button.dataset[dataKey] || "";
      });
    }
    if (dialog.id === "stop-create-dialog") {
      dialog.querySelector("[data-stop-route-id]").value = button.dataset.routeId;
      dialog.querySelector("[data-stop-route-name]").textContent = button.dataset.routeName;
    }
    dialog.showModal();
  });
});

document.querySelectorAll("[data-close-dialog]").forEach(function (button) {
  button.addEventListener("click", function () {
    button.closest("dialog").close();
  });
});

document.querySelectorAll(".admin-dialog").forEach(function (dialog) {
  dialog.addEventListener("click", function (event) {
    if (event.target === dialog) dialog.close();
  });
});

document.querySelectorAll("form[data-confirm]").forEach(function (form) {
  form.addEventListener("submit", function (event) {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  });
});
