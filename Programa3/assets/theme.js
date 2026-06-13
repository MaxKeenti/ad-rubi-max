(function () {
  var storageKey = "programa3-theme";
  var root = document.documentElement;

  function getStoredTheme() {
    try {
      return window.localStorage ? window.localStorage.getItem(storageKey) : null;
    } catch (error) {
      return null;
    }
  }

  function storeTheme(theme) {
    try {
      if (window.localStorage) {
        window.localStorage.setItem(storageKey, theme);
      }
    } catch (error) {
      return;
    }
  }

  function systemTheme() {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }

  function activeTheme() {
    return root.getAttribute("data-theme") || systemTheme();
  }

  function syncButton(button) {
    var theme = activeTheme();
    var nextTheme = theme === "dark" ? "light" : "dark";
    button.setAttribute("data-theme", theme);
    button.setAttribute("aria-label", "Cambiar a modo " + nextTheme);
    button.setAttribute("title", "Cambiar a modo " + nextTheme);
  }

  function applyTheme(theme, button) {
    root.setAttribute("data-theme", theme);
    storeTheme(theme);
    syncButton(button);
  }

  function init() {
    var savedTheme = getStoredTheme();
    var header = document.querySelector(".site-header");
    var indexLink = document.querySelector(".index-link");
    var actions = document.createElement("div");
    var button = document.createElement("button");
    var icon = document.createElement("span");

    if (savedTheme === "dark" || savedTheme === "light") {
      root.setAttribute("data-theme", savedTheme);
    }

    if (!header || !indexLink) return;

    button.type = "button";
    button.className = "theme-toggle";
    icon.className = "theme-icon";
    icon.setAttribute("aria-hidden", "true");
    button.appendChild(icon);
    button.addEventListener("click", function () {
      applyTheme(activeTheme() === "dark" ? "light" : "dark", button);
    });

    actions.className = "header-actions";
    header.appendChild(actions);
    actions.appendChild(button);
    actions.appendChild(indexLink);
    syncButton(button);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
