function main(opts: any): void {
  let accepting: boolean;
  const $el = $('.challenge-page');

  window.lishogi.socket = new window.lishogi.StrongSocket(opts.socketUrl, opts.data.socketVersion, {
    options: {
      name: 'challenge',
    },
    events: {
      reload: () => {
        window.lishogi.xhr.text('GET', opts.xhrUrl).then(html => {
          $el.replaceWith($(html).find($el));
          init();
        });
      },
    },
  });

  function init() {
    if (!accepting)
      $('#challenge-redirect').each(function (this: HTMLAnchorElement) {
        location.href = this.href;
      });
    $el.find('form.accept').on('submit', function () {
      accepting = true;
      $(this).html('<span class="ddloader"></span>');
    });
    $el.find('form.xhr').on('submit', function (this: HTMLFormElement, e) {
      e.preventDefault();
      window.lishogi.xhr.formToXhr(this);
      $(this).html('<span class="ddloader"></span>');
    });
    $el.find('input.friend-autocomplete').each(function () {
      const $input = $(this);
      window.lishogi.userAutocomplete($input, {
        focus: 1,
        friend: 1,
        tag: 'span',
        onSelect: () => {
          $input.parents('form').trigger('submit');
        },
      });
    });
  }

  init();

  function pingNow() {
    if (document.getElementById('ping-challenge')) {
      try {
        window.lishogi.socket.send('ping');
      } catch (_e) {}
      setTimeout(pingNow, 9000);
    }
  }

  pingNow();
}

window.lishogi.registerModule(__bundlename__, main);
