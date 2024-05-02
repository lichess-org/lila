import * as xhr from 'common/xhr';

interface ChallengeOpts {
  xhrUrl: string;
  owner: boolean;
  data: any;
}

export function initModule(opts: ChallengeOpts) {
  const selector = '.challenge-page';
  let accepting: boolean;

  site.socket = new site.StrongSocket(
    `/challenge/${opts.data.challenge.id}/socket/v5`,
    opts.data.socketVersion,
    {
      events: {
        reload() {
          xhr.text(opts.xhrUrl).then(html => {
            $(selector).replaceWith($(html).find(selector));
            init();
            site.contentLoaded($(selector)[0]);
          });
        },
      },
    },
  );

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
        site.asset.userComplete({
          input: input,
          friend: true,
          tag: 'span',
          focus: true,
          onSelect: () => setTimeout(() => (input.parentNode as HTMLFormElement).submit(), 100),
        });
      });
    $(selector)
      .find('.invite__user__recent button')
      .on('click', function (this: HTMLButtonElement) {
        $(selector)
          .find('input.friend-autocomplete')
          .val(this.dataset.user!)
          .parents('form')
          .each(function (this: HTMLFormElement) {
            this.submit();
          });
      });
  }

  init();

  function pingNow() {
    if (document.getElementById('ping-challenge')) {
      site.socket.send('ping');
      setTimeout(pingNow, 9000);
    }
  }

  pingNow();
}
