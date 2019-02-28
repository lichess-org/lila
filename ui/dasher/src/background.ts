import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind, header } from './util'

export interface BackgroundCtrl {
  list: Background[]
  set(k: string): void
    get(): string
  getImage(): string
  setImage(i: string): void
    trans: Trans
  close: Close
}

export interface BackgroundData {
  current: string
  image: string
}

interface Background {
  key: string
  name: string
}

export function ctrl(data: BackgroundData, trans: Trans, redraw: Redraw, close: Close): BackgroundCtrl {

  const list: Background[] = [
    { key: 'light', name: trans.noarg('light') },
    { key: 'dark', name: trans.noarg('dark') },
    { key: 'transp', name: trans.noarg('transparent') }
  ];

  return {
    list,
    trans,
    get: () => data.current,
    set(c: string) {
      data.current = c;
      $.post('/pref/bg', { bg: c }, reloadAllTheThings);
      applyBackground(data, list);
      redraw();
    },
    getImage: () => data.image,
    setImage(i: string) {
      data.image = i;
      $.post('/pref/bgImg', { bgImg: i }, reloadAllTheThings);
      applyBackground(data, list);
      redraw();
    },
    close
  };
}

export function view(ctrl: BackgroundCtrl): VNode {

  const cur = ctrl.get();

  return h('div.sub.background', [
    header(ctrl.trans.noarg('background'), ctrl.close),
    h('div.selector.large', ctrl.list.map(bg => {
      return h('a.text', {
        class: { active: cur === bg.key },
        attrs: { 'data-icon': 'E' },
        hook: bind('click', () => ctrl.set(bg.key))
      }, bg.name);
    })),
    cur === 'transp' ? imageInput(ctrl) : null
  ])
}

function imageInput(ctrl: BackgroundCtrl) {
  return h('div.image', [
    h('p', ctrl.trans.noarg('backgroundImageUrl')),
    h('input', {
      attrs: {
        type: 'text',
        placeholder: 'https://',
        value: ctrl.getImage()
      },
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).on('change keyup paste', window.lichess.debounce(function(this: HTMLElement) {
            ctrl.setImage($(this).val());
          }, 200));
        }
      }
    })
  ]);
}

function applyBackground(data: BackgroundData, list: Background[]) {

  const key = data.current;

  $('body')
    .removeClass(list.map(b => b.key).join(' '))
    .addClass(key === 'transp' ? 'transp dark' : key);

  if ($('body').data('resp')) {
    const prev = $('body').data('theme');
    $('body').data('theme', key);
    $('link[href*=".' + prev + '."]').each(function(this: HTMLElement) {
      console.log($(this).attr('href'));
      $(this).attr('href', $(this).attr('href').replace('.' + prev + '.', '.' + key + '.')).appendTo('head');
    });
  } else {

    if ((key === 'dark' || key === 'transp') && !$('link[href*="dark.css"]').length) {

      $('link[href*="common.css"]').clone().each(function(this: HTMLElement) {
        $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'dark.css')).appendTo('head');
      });
    }

    if (key === 'transp' && !$('link[href*="transp.css"]').length) {

      $('link[href*="common.css"]').clone().each(function(this: HTMLElement) {
        $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'transp.css')).appendTo('head');
      });
    }
  }

  if (key === 'transp') {
    const bgData = document.getElementById('bg-data');
    bgData ? bgData.innerHTML = 'body.transp::before{background-image:url(' + data.image + ');}' :
      $('head').append('<style id="bg-data">body.transp::before{background-image:url(' + data.image + ');}</style>');
  }
}

function reloadAllTheThings() {
  if (window.Highcharts) window.lichess.reload();
  window.lichess.reloadOtherTabs();
}
