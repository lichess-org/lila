import * as xhr from 'common/xhr';

interface ChallengeOpts {
  socketUrl: string;
  xhrUrl: string;
  owner: boolean;
  data: any;
}

export default function (opts: ChallengeOpts) {
  const selector = '.challenge-page';
  let accepting: boolean;

  lichess.socket = new lichess.StrongSocket(opts.socketUrl, opts.data.socketVersion, {
    events: {
      reload() {
        xhr.text(opts.xhrUrl).then(html => {
          $(selector).replaceWith($(html).find(selector));
          init();
          lichess.contentLoaded($(selector)[0]);
        });
      },
    },
  });

  function init() {
    if (!accepting)
      $('#challenge-redirect').each(function (this: HTMLAnchorElement) {
        location.href = this.href;
      });
    $(selector)
      .find('form.accept')
      .on('submit', function (this: HTMLFormElement) {
        accepting = true;
        $(this).html('<span class="ddloader"></span>');
      });
    $(selector)
      .find('form.xhr')
      .on('submit', function (this: HTMLFormElement, e) {
        e.preventDefault();
        xhr.formToXhr(this);
        $(this).html('<span class="ddloader"></span>');
      });
    $(selector)
      .find('input.friend-autocomplete')
      .each(function (this: HTMLInputElement) {
        const input = this;
        lichess.userComplete().then(uac =>
          uac({
            input: input,
            friend: true,
            tag: 'span',
            focus: true,
            onSelect: () => setTimeout(() => (input.parentNode as HTMLFormElement).submit(), 100),
          })
        );
      });
  }

  init();

  function pingNow() {
    if (document.getElementById('ping-challenge')) {
      try {
        lichess.socket.send('ping');
      } catch (e) {}
      setTimeout(pingNow, 9000);
    }
  }

  pingNow();
}
