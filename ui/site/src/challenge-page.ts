import * as xhr from 'common/xhr';

const li = window.lichess

interface ChallengeOpts {
  socketUrl: string;
  xhrUrl: string;
  owner: boolean;
  data: any;
}

export default function(opts: ChallengeOpts) {

  const selector = '.challenge-page';
  let accepting: boolean;

  li.socket = new li.StrongSocket(
    opts.socketUrl,
    opts.data.socketVersion, {
    events: {
      reload() {
        xhr.text(opts.xhrUrl).then(html => {
          $(selector).replaceWith($(html).find(selector));
          init();
        });
      }
    }
  });

  function init() {
    if (!accepting) $('#challenge-redirect').each(function(this: HTMLAnchorElement) {
      location.href = this.href;
    });
    $(selector).find('form.accept').submit(function(this: HTMLFormElement) {
      accepting = true;
      $(this).html('<span class="ddloader"></span>');
    });
    $(selector).find('form.xhr').submit(function(this: HTMLFormElement, e) {
      e.preventDefault();
      xhr.formToXhr(this);
      $(this).html('<span class="ddloader"></span>');
    });
    $(selector).find('input.friend-autocomplete').each(function(this: HTMLInputElement) {
      const $input = $(this);
      li.userAutocomplete($input, {
        focus: true,
        friend: true,
        tag: 'span',
        onSelect() {
          $input.parents('form').submit();
        }
      });
    });
  }

  init();

  function pingNow() {
    if (document.getElementById('ping-challenge')) {
      try {
        li.socket.send('ping');
      } catch (e) { }
      setTimeout(pingNow, 2000);
    }
  }

  pingNow();
}
