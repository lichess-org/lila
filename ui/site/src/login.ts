import * as xhr from "common/xhr";
import debounce from "common/debounce";
import spinnerHtml from "./component/spinner";
import { addPasswordChangeListener } from "./passwordComplexity";

export function loginStart() {
  const selector = ".auth-login form";

  (function load() {
    const form = document.querySelector(selector) as HTMLFormElement,
      $f = $(form);
    form.addEventListener("submit", (e: Event) => {
      e.preventDefault();
      $f.find(".submit").prop("disabled", true);
      fetch(form.action, {
        ...xhr.defaultInit,
        headers: xhr.xhrHeader,
        method: "post",
        body: new FormData(form),
      })
        .then((res) => res.text().then((text) => [res, text]))
        .then(([res, text]: [Response, string]) => {
          if (text === "MissingTotpToken" || text === "InvalidTotpToken") {
            $f.find(".one-factor").hide();
            $f.find(".two-factor").removeClass("none");
            requestAnimationFrame(() =>
              $f.find(".two-factor input").val("")[0]!.focus()
            );
            $f.find(".submit").prop("disabled", false);
            if (text === "InvalidTotpToken")
              $f.find(".two-factor .error").removeClass("none");
          } else if (res.ok)
            location.href = text.startsWith("ok:") ? text.substr(3) : "/";
          else {
            try {
              const el = $(text).find(selector);
              if (el.length) {
                $f.replaceWith(el);
                load();
              } else {
                alert(
                  text ||
                    res.statusText +
                      ". Please wait some time before trying again."
                );
                $f.find(".submit").prop("disabled", false);
              }
            } catch (e) {
              console.warn(e);
              $f.html(text);
            }
          }
        });
    });
  })();
}

export function signupStart() {
  const $form = $("#signup-form"),
    $exists = $form.find(".username-exists"),
    $username = $form
      .find('input[name="username"]')
      .on("change keyup paste", () => {
        $exists.hide();
        usernameCheck();
      });

  addPasswordChangeListener("form3-password");

  const usernameCheck = debounce(() => {
    const name = $username.val() as string;
    if (name.length >= 3)
      xhr
        .json(xhr.url("/player/autocomplete", { term: name, exists: 1 }))
        .then((res) => $exists.toggle(res));
  }, 300);

  $form.on("submit", () =>
    $form
      .find("button.submit")
      .prop("disabled", true)
      .removeAttr("data-icon")
      .addClass("frameless")
      .html(spinnerHtml)
  );

  window.signupSubmit = () => {
    const form = document.getElementById("signup-form") as HTMLFormElement;
    if (form.reportValidity()) form.submit();
  };
}
