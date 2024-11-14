import * as xhr from 'common/xhr';
import StrongSocket from 'common/socket';
import { userComplete } from 'common/userComplete';
import { isTouchDevice, isIOS } from 'common/device';

interface ChallengeOpts {
  xhrUrl: string;
  owner: boolean;
  data: any;
}

export function initModule(opts: ChallengeOpts): void {
  const selector = '.challenge-page';
  let accepting: boolean;

  site.socket = new StrongSocket(`/challenge/${opts.data.challenge.id}/socket/v5`, opts.data.socketVersion, {
    events: {
      reload() {
        xhr.text(opts.xhrUrl).then(html => {
          $(selector).replaceWith($(html).find(selector));
          init();
          window.lichess.initializeDom($(selector)[0]);
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
        userComplete({
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
    if (isTouchDevice() && typeof navigator.share === 'function') {
      const inviteUrl = document.querySelector<HTMLElement>('.invite__url');
      if (!inviteUrl) return;
      const instructions = document.querySelector<HTMLElement>(`.mobile-instructions`)!;
      instructions.classList.remove('none');
      inviteUrl.classList.add('none');
      instructions.closest<HTMLElement>('.details-wrapper')!.onclick = () =>
        navigator
          .share({
            title: `Fancy a game of chess?`,
            url: inviteUrl.querySelector<HTMLInputElement>('input')?.value,
          })
          .catch(() => {});
      if (isIOS()) instructions.classList.add('is-ios');
    }
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
