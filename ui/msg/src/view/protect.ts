import { memoize } from 'common';
import modal from 'common/modal';

export const makeLinkPopups = (dom: HTMLElement, trans: Trans) =>
  $(dom).on('click', 'their a[href^="http"]', function (this: HTMLLinkElement) {
    return onClick(this, trans);
  });

const onClick = (a: HTMLLinkElement, trans: Trans): boolean => {
  const url = new URL(a.href);
  if (url.host == 'lichess.org' || url.host.endsWith('.lichess.org') || isPassList(url)) return true;
  lichess.loadCssPath('modal');
  modal({
    content: $(
      `<div class="msg-modal">
        <div class="msg-modal__content">
          <div class="msg-modal__content__title">
            <h2>${trans('youAreLeavingLichess')}</h2>
            <p class="msg-modal__content__advice">${trans('neverTypeYourPassword')}</p>
          </div>
        </div>
        <div class="msg-modal__actions">
          <a class="cancel">Cancel</a>
          <a href="${a.href}" target="_blank" class="button button-red button-no-upper">Proceed to ${url.host}</a>
        </div>
      </div>`
    ),
    onInsert($wrap) {
      $wrap.find('.cancel').on('click', modal.close);
    },
  });
  return false;
};

const isPassList = (url: URL) => passList().find(h => h == url.host || url.host.endsWith('.' + h));

const passList = memoize<string[]>(() =>
  `lichess4545.com ligacatur.com
github.com discord.com crowdin.com mastodon.online
twitter.com facebook.com
wikipedia.org wikimedia.org
chess24.com chess.com chessable.com
`.split(/[ \n]/)
);
