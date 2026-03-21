document.addEventListener("DOMContentLoaded", () => {
  const fieldConfigs = {
    username: {
      required: true,
      pattern: ".{3,}",
      title: "Username must be at least 3 characters.",
    },
    password: {
      required: true,
      minLength: 8,
    },
    "password-confirm": {
      required: true,
      minLength: 8,
    },
    email: {
      required: true,
      type: "email",
      title: "Enter a valid email address.",
    },
    firstName: {
      required: true,
    },
    lastName: {
      required: true,
    },
  };

  const fields = document.querySelectorAll(
    "form input:not([type='hidden']):not([type='submit']):not([type='button']):not([type='reset']), form select, form textarea",
  );

  const passwordField = document.getElementById("password");
  const passwordConfirmField = document.getElementById("password-confirm");
  const forms = document.querySelectorAll("form");
  const touchedFields = new WeakSet();

  function ensureErrorNode(field) {
    const existing = field.parentElement?.querySelector(".library-inline-error");
    if (existing) {
      return existing;
    }

    const errorNode = document.createElement("div");
    errorNode.className = "library-inline-error";
    errorNode.setAttribute("aria-live", "polite");
    field.parentElement?.appendChild(errorNode);
    return errorNode;
  }

  function validationMessage(field) {
    if (!(field instanceof HTMLInputElement || field instanceof HTMLSelectElement || field instanceof HTMLTextAreaElement)) {
      return "";
    }

    if (field.id === "password-confirm" && passwordField instanceof HTMLInputElement) {
      if (!field.value.trim()) {
        return "Please confirm your password.";
      }
      if (field.value !== passwordField.value) {
        return "Passwords do not match.";
      }
    }

    if (field.validity.valueMissing) {
      return "This field is required.";
    }
    if (field.validity.typeMismatch) {
      return "Enter a valid email address.";
    }
    if (field.validity.patternMismatch) {
      return field.title || "Enter a valid value.";
    }
    if (field.validity.tooShort) {
      return `Use at least ${field.minLength} characters.`;
    }

    return "";
  }

  function applyCustomRules(field, options = {}) {
    if (!(field instanceof HTMLInputElement || field instanceof HTMLSelectElement || field instanceof HTMLTextAreaElement)) {
      return;
    }

    const { forceShow = false } = options;

    const config = fieldConfigs[field.id];
    if (config) {
      if (config.required) {
        field.required = true;
      }
      if (config.type && field instanceof HTMLInputElement) {
        field.type = config.type;
      }
      if (config.pattern && field instanceof HTMLInputElement) {
        field.pattern = config.pattern;
      }
      if (config.title) {
        field.title = config.title;
      }
      if (typeof config.minLength === "number" && field instanceof HTMLInputElement) {
        field.minLength = config.minLength;
      }
    }

    if (field.id === "password-confirm" && passwordField instanceof HTMLInputElement) {
      if (field.value && field.value !== passwordField.value) {
        field.setCustomValidity("Passwords do not match.");
      } else {
        field.setCustomValidity("");
      }
    }

    const errorNode = ensureErrorNode(field);
    const message = validationMessage(field);
    const shouldShowMessage =
      forceShow ||
      touchedFields.has(field) ||
      Boolean(field.value && field.value.trim());

    errorNode.textContent = shouldShowMessage ? message : "";
    field.setAttribute("aria-invalid", shouldShowMessage && message ? "true" : "false");
  }

  fields.forEach((field) => {
    applyCustomRules(field);

    field.addEventListener("input", () => applyCustomRules(field));
    field.addEventListener("blur", () => {
      touchedFields.add(field);
      applyCustomRules(field, { forceShow: true });
    });
  });

  if (passwordField instanceof HTMLInputElement && passwordConfirmField instanceof HTMLInputElement) {
    passwordField.addEventListener("input", () => {
      const shouldForceShow = touchedFields.has(passwordConfirmField) || Boolean(passwordConfirmField.value);
      applyCustomRules(passwordConfirmField, { forceShow: shouldForceShow });
    });
  }

  forms.forEach((form) => {
    form.setAttribute("novalidate", "novalidate");
    form.addEventListener("submit", (event) => {
      let hasErrors = false;
      fields.forEach((field) => {
        touchedFields.add(field);
        applyCustomRules(field, { forceShow: true });
        if (validationMessage(field)) {
          hasErrors = true;
        }
      });

      if (hasErrors) {
        event.preventDefault();
      }
    });
  });
});
