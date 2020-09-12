import * as xhr from 'common/xhr';
import spinnerHtml from './spinner';
import { requestIdleCallback } from './functions';

let hoverable: boolean;
function isHoverable() {
  if (typeof hoverable === 'undefined')
    hoverable = !('ontouchstart' in window) /* Firefox <= 63 */ || !!getComputedStyle(document.body).getPropertyValue('--hoverable');
  return hoverable;
}

const inCrosstable = (el: HTMLElement) =>
  document.querySelector('.crosstable')?.contains(el);

function onPowertipPreRender(id: string, preload?: (url: string) => void) {
  return function(this: HTMLElement) {
    const url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
    if (preload) preload(url);
    xhr.text(url + '/mini').then(html => {
      $('#' + id).html(html);
      window.lichess.pubsub.emit('content_loaded');
    });
  };
};

const uptA = (url: string, icon: string) =>
  `<a class="btn-rack__btn" href="${url}" data-icon="${icon}"></a>`;

const userPowertip = (el: HTMLElement, pos?: PowerTip.Placement) => {
  pos = pos || (el.getAttribute('data-pt-pos') as PowerTip.Placement) || (
    inCrosstable(el) ? 'n' : 's'
  );
  $(el).removeClass('ulpt').powerTip({
    intentPollInterval: 200,
    placement: pos,
    smartPlacement: true,
    closeDelay: 200
  }).data('powertip', ' ').on({
    powerTipRender: onPowertipPreRender('powerTip', (url: string) => {
      const u = url.substr(3);
      const name = $(el).data('name') || $(el).html();
      $('#powerTip').html('<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' + name + '</span></div></div><div class="upt__actions btn-rack">' +
        uptA('/@/' + u + '/tv', '1') +
        uptA('/inbox/new?user=' + u, 'c') +
        uptA('/?user=' + u + '#friend', 'U') +
        '<a class="btn-rack__btn relation-button" disabled></a></div>');
    })
  });
};

function gamePowertip(el: HTMLElement) {
  $(el).removeClass('glpt').powerTip({
    intentPollInterval: 200,
    placement: inCrosstable(el) ? 'n' : 'w',
    smartPlacement: true,
    closeDelay: 200,
    popupId: 'miniGame'
  }).on({
    powerTipPreRender: onPowertipPreRender('miniGame')
  }).data('powertip', spinnerHtml);
};

function powerTipWith(el: HTMLElement, ev, f) {
  if (isHoverable()) {
    f(el);
    $.powerTip.show(el, ev);
  }
};

function onIdleForAll(par: HTMLElement, sel, fun) {
  requestIdleCallback(() =>
    Array.prototype.forEach.call(par.querySelectorAll(sel), (el: HTMLElement) => fun(el)) // do not codegolf to `fun`
  )
}

const powertip = {
  watchMouse() {
    document.body.addEventListener('mouseover', e => {
      const t = e.target as HTMLElement, cl = t.classList;
      if (cl.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (cl.contains('glpt')) powerTipWith(t, e, gamePowertip);
    });
  },
  manualGameIn(parent: HTMLElement) {
    onIdleForAll(parent, '.glpt', gamePowertip);
  },
  manualGame: gamePowertip,
  manualUser: userPowertip,
  manualUserIn(parent: HTMLElement) {
    onIdleForAll(parent, '.ulpt', userPowertip);
  }
};

export default powertip;
