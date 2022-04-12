import * as xhr from 'common/xhr';
import debounce from 'common/debounce';
import { storedJsonProp } from 'common/storage';

import type * as PasswordComplexity from './passwordComplexity';

class LoginHistory {
  historyStorage = storedJsonProp<number[]>('login.history', () => []);
  private now = () => Math.round(Date.now() / 1000);
  add = () => {
    const now = this.now();
    this.historyStorage([now, ...this.historyStorage().filter(d => d > now - 30)]);
  };
  lockSeconds = () => {
    const now = this.now();
    const recentTries = this.historyStorage().filter(d => d > now - 30);
    if (recentTries.length >= 3) return Math.max(0, recentTries[recentTries.length - 1] + 30 - now);
  };
}

export function loginStart() {
  const selector = '.auth-login form';
  const history = new LoginHistory();

  const toggleSubmit = ($submit: Cash, v: boolean) => $submit.prop('disabled', !v).toggleClass('disabled', !v);

  (function load() {
    const form = document.querySelector(selector) as HTMLFormElement,
      $f = $(form);
    const lockSeconds = history.lockSeconds();
    if (lockSeconds) {
      const $submit = $f.find('.submit');
      const submitText = toggleSubmit($submit, false).text();
      const refresh = () => {
        const seconds = history.lockSeconds();
        if (seconds) {
          $submit.text('' + seconds);
          setTimeout(refresh, 1000);
        } else $submit.text(submitText).prop('disabled', false).removeClass('disabled');
      };
      refresh();
    }
    form.addEventListener('submit', (e: Event) => {
      e.preventDefault();
      toggleSubmit($f.find('.submit'), false);
      fetch(form.action, {
        ...xhr.defaultInit,
        headers: xhr.xhrHeader,
        method: 'post',
        body: new FormData(form),
      })
        .then(res => res.text().then(text => [res, text]))
        .then(([res, text]: [Response, string]) => {
          if (text === 'MissingTotpToken' || text === 'InvalidTotpToken') {
            $f.find('.one-factor').hide();
            $f.find('.two-factor').removeClass('none');
            requestAnimationFrame(() => $f.find('.two-factor input').val('')[0]!.focus());
            toggleSubmit($f.find('.submit'), true);
            if (text === 'InvalidTotpToken') $f.find('.two-factor .error').removeClass('none');
          } else if (res.ok) location.href = text.startsWith('ok:') ? text.slice(3) : '/';
          else {
            try {
              const el = $(text).find(selector);
              if (el.length) {
                history.add();
                $f.replaceWith(el);
                load();
              } else {
                alert(text || res.statusText + '. Please wait some time before trying again.');
                toggleSubmit($f.find('.submit'), true);
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
  const $form = $('#signup-form'),
    $exists = $form.find('.username-exists'),
    $username = $form.find('input[name="username"]').on('change keyup paste', () => {
      $exists.hide();
      usernameCheck();
    });

  const usernameCheck = debounce(() => {
    const name = $username.val() as string;
    if (name.length >= 3)
      xhr.json(xhr.url('/player/autocomplete', { term: name, exists: 1 })).then(res => $exists.toggle(res));
  }, 300);

  $form.on('submit', () => {
    if ($form.find('[name="h-captcha-response"]').val() || !$form.hasClass('h-captcha-enabled'))
      $form
        .find('button.submit')
        .prop('disabled', true)
        .removeAttr('data-icon')
        .addClass('frameless')
        .html(lichess.spinnerHtml);
    else return false;
  });

  lichess
    .loadModule('passwordComplexity')
    .then(() => (window.passwordComplexity as typeof PasswordComplexity).addPasswordChangeListener('form3-password'));
}
