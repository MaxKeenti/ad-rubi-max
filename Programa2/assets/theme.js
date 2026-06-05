(function () {
  var storageKey = "programa2-theme";
  var root = document.documentElement;

  function getCookieTheme() {
    var match = document.cookie.match(new RegExp("(^| )" + storageKey + "=([^;]+)"));
    return match ? decodeURIComponent(match[2]) : null;
  }

  function storeCookieTheme(theme) {
    try {
      document.cookie =
        storageKey + "=" + encodeURIComponent(theme) + "; path=/; max-age=31536000; SameSite=Lax";
    } catch (error) {
      return;
    }
  }

  function getStoredTheme() {
    try {
      if (window.localStorage) {
        return window.localStorage.getItem(storageKey);
      }
    } catch (error) {
      return getCookieTheme();
    }

    return getCookieTheme();
  }

  function storeTheme(theme) {
    try {
      if (window.localStorage) {
        window.localStorage.setItem(storageKey, theme);
        return;
      }
    } catch (error) {
      storeCookieTheme(theme);
    }

    storeCookieTheme(theme);
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
    button.setAttribute("aria-label", "Switch to " + nextTheme + " mode");
    button.setAttribute("title", "Switch to " + nextTheme + " mode");
  }

  function applyTheme(theme, button) {
    root.setAttribute("data-theme", theme);
    storeTheme(theme);
    syncButton(button);
  }

  function createToggle() {
    var button = document.createElement("button");
    var icon = document.createElement("span");

    button.type = "button";
    button.className = "theme-toggle";
    icon.className = "theme-icon";
    icon.setAttribute("aria-hidden", "true");
    button.appendChild(icon);

    button.addEventListener("click", function () {
      applyTheme(activeTheme() === "dark" ? "light" : "dark", button);
    });

    return button;
  }

  function init() {
    var savedTheme = getStoredTheme();
    var header = document.querySelector(".site-header");
    var indexLink = document.querySelector(".index-link");
    var actions = document.createElement("div");
    var button = createToggle();

    if (savedTheme === "dark" || savedTheme === "light") {
      root.setAttribute("data-theme", savedTheme);
    }

    if (!header || !indexLink) {
      return;
    }

    actions.className = "header-actions";
    header.appendChild(actions);
    actions.appendChild(button);
    actions.appendChild(indexLink);
    syncButton(button);

    var colorSchemeQuery = window.matchMedia("(prefers-color-scheme: dark)");
    var handleSystemChange = function () {
      if (!root.getAttribute("data-theme")) {
        syncButton(button);
      }
    };

    if (colorSchemeQuery.addEventListener) {
      colorSchemeQuery.addEventListener("change", handleSystemChange);
    } else if (colorSchemeQuery.addListener) {
      colorSchemeQuery.addListener(handleSystemChange);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
