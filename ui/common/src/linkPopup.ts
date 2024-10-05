import { domDialog } from './dialog';

export const makeLinkPopups = (dom: HTMLElement | Cash, trans: Trans, selector = 'a[href^="http"]'): void => {
  const $el = $(dom);
  if (!$el.hasClass('link-popup-ready'))
    $el.addClass('link-popup-ready').on('click', selector, function (this: HTMLLinkElement) {
      return onClick(this, trans);
    });
};

export const onClick = (a: HTMLLinkElement, trans: Trans): boolean => {
  const url = new URL(a.href);
  if (isPassList(url)) return true;

  domDialog({
    class: 'link-popup',
    css: [{ hashed: 'bits.linkPopup' }],
    htmlText: `
      <div class="link-popup__content">
        <div class="link-popup__content__title">
          <h2>${trans('youAreLeavingLichess')}</h2>
          <p class="link-popup__content__advice">${trans('neverTypeYourPassword')}</p>
        </div>
      </div>
      <div class="link-popup__actions">
        <button class="cancel button-link" type="button">${trans('cancel')}</button>
        <a href="${a.href}" target="_blank" class="button button-red button-no-upper">
          ${trans('proceedToX', url.host)}
        </a>
      </div>`,
  }).then(dlg => {
    $('.cancel', dlg.view).on('click', dlg.close);
    $('a', dlg.view).on('click', () => setTimeout(dlg.close, 1000));
    dlg.showModal();
  });
  return false;
};

const isPassList = (url: URL) => passList().find(h => h == url.host || url.host.endsWith('.' + h));

const passList = () =>
  `lichess.org lichess4545.com ligacatur.com
github.com discord.com discord.gg mastodon.online
twitter.com facebook.com twitch.tv
wikipedia.org wikimedia.org
chess24.com chess.com chessable.com
`.split(/[ \n]/);
