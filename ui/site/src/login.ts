import * as xhr from 'common/xhr';

window.lichess.load.then(() => {

  const selector = '.auth-login form';

  (function load() {
    const form = document.querySelector(selector) as HTMLFormElement, $f = $(form);
    form.addEventListener('submit', (e: Event) => {
      e.preventDefault();
      $f.find('.submit').prop('disabled', true);
      fetch(form.action, {
        ...xhr.defaultInit,
        headers: xhr.xhrHeader,
        method: 'post',
        body: new FormData(form)
      })
        .then(res => res.text().then(text => [res, text]))
        .then(([res, text]: [Response, string]) => {
          if (text === 'MissingTotpToken' || text === 'InvalidTotpToken') {
            $f.find('.one-factor').hide();
            $f.find('.two-factor').show();
            requestAnimationFrame(() => {
              $f.find('.two-factor input').val('').focus();
            });
            $f.find('.submit').prop('disabled', false);
            if (text === 'InvalidTotpToken') $f.find('.two-factor .error').show();
          }
          else if (res.ok) location.href = text.startsWith('ok:') ? text.substr(3) : '/';
          else {
            try {
              const el = $(text).find(selector);
              if (el.length) {
                $f.replaceWith(el);
                load();
              } else {
                alert(text || (res.statusText + '. Please wait some time before trying again.'));
                $f.find('.submit').prop('disabled', false);
              }
            } catch (e) {
              console.warn(e);
              $f.html(text);
            }
          }
        });
    });
  })();
});
