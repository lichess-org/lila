const selector = '.auth-login form';

window.lishogi.ready.then(() => {
  load($(selector));
});

function load($f: JQuery): void {
  $f.submit(function () {
    $f.find('.submit').prop('disabled', true);

    window.lishogi.xhr
      .formToXhr($f[0] as HTMLFormElement)
      .then(res => res.text())
      .then(res => {
        if (res === 'MissingTotpToken' || res === 'InvalidTotpToken') {
          $f.find('.one-factor').hide();
          $f.find('.two-factor').show();
          requestAnimationFrame(function () {
            $f.find('.two-factor input').val('').focus();
          });
          $f.find('.submit').prop('disabled', false);
          if (res === 'InvalidTotpToken') $f.find('.two-factor .error').show();
        } else location.href = res.startsWith('ok:') ? res.slice(3) : '/';
      })
      .catch(err => {
        try {
          const el = $(err.responseText).find(selector);
          if (el.length) {
            $f.replaceWith(el);
            load($(selector));
          } else {
            alert(
              err.responseText || err.statusText + '. Please wait some time before trying again.'
            );
            $f.find('.submit').prop('disabled', false);
          }
        } catch {
          $f.html(err.responseText);
        }
      });
    return false;
  });
}
