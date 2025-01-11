import { requestIdleCallbackWithFallback } from 'common/common';
import { isHoverable } from 'common/mobile';
import { spinnerHtml } from 'common/spinner';
import { pubsub } from './pubsub';

function containedIn(el: HTMLElement, container: Element | null) {
  return container?.contains(el);
}
function inCrosstable(el: HTMLElement) {
  return containedIn(el, document.querySelector('.crosstable'));
}

function onPowertipPreRender(id: string, preload?: (url: string) => void) {
  return function (this: HTMLElement) {
    const url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
    if (preload) preload(url);
    window.lishogi.xhr.text('GET', `${url}/mini`).then(html => {
      $(`#${id}`).html(html);
      pubsub.emit('content_loaded');
    });
  };
}

const uptA = (url: string, icon: string) =>
  `<a class="btn-rack__btn" href="${url}" data-icon="${icon}"></a>`;

const userPowertip = (el: HTMLElement, pos?: any) => {
  pos = pos || el.getAttribute('data-pt-pos') || (inCrosstable(el) ? 'n' : 's');
  $(el)
    .removeClass('ulpt')
    .powerTip({
      intentPollInterval: 200,
      placement: pos,
      smartPlacement: true,
      mouseOnToPopup: true,
      closeDelay: 200,
    })
    .data('powertip', ' ')
    .on({
      powerTipRender: onPowertipPreRender('powerTip', url => {
        const u = url.slice(3);
        const name = $(el).data('name') || $(el).html();
        $('#powerTip').html(
          `<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">${name}</span></div></div><div class="upt__actions btn-rack">${uptA(`/@/${u}/tv`, '1')}${uptA(`/inbox/new?user=${u}`, 'c')}${uptA(`/?user=${u}#friend`, 'U')}<a class="btn-rack__btn relation-button" disabled></a></div>`,
        );
      }),
    });
};

function gamePowertip(el: HTMLElement) {
  $(el)
    .removeClass('glpt')
    .powerTip({
      intentPollInterval: 200,
      placement: inCrosstable(el) ? 'n' : 'w',
      smartPlacement: true,
      mouseOnToPopup: true,
      closeDelay: 200,
      popupId: 'miniGame',
    })
    .on({
      powerTipPreRender: onPowertipPreRender('miniGame'),
    })
    .data('powertip', spinnerHtml);
}

function powerTipWith(el: HTMLElement, ev: Event, f: (el: HTMLElement) => any) {
  if (isHoverable()) {
    f(el);
    $.powerTip.show(el, ev);
  }
}

function onIdleForAll(par: HTMLElement, sel: string, fun: (...args: any[]) => any) {
  requestIdleCallbackWithFallback(() => {
    Array.prototype.forEach.call(par.querySelectorAll(sel), fun);
  });
}

export const powertip: any = {
  mouseover(e: Event) {
    const t = e.target as HTMLElement;
    const cl = t.classList;
    if (cl.contains('ulpt')) powerTipWith(t, e, userPowertip);
    else if (cl.contains('glpt')) powerTipWith(t, e, gamePowertip);
  },
  manualGameIn(parent: HTMLElement) {
    onIdleForAll(parent, '.glpt', gamePowertip);
  },
  manualGame: gamePowertip,
  manualUserIn(parent: HTMLElement) {
    onIdleForAll(parent, '.ulpt', (el: HTMLElement) => userPowertip(el));
  },
};
