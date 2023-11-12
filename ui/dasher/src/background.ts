import { h, VNode } from 'snabbdom';
import { Redraw, Close, bind, header } from './util';
import debounce from 'common/debounce';
import * as licon from 'common/licon';
import { onInsert } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { elementScrollBarWidth } from 'common/scroll';
import { throttlePromiseDelay } from 'common/throttle';
import { supportsSystemTheme } from 'common/theme';

export interface BackgroundCtrl {
  list: Background[];
  set(k: string): void;
  get(): string;
  getImage(): string;
  setImage(i: string): void;
  trans: Trans;
  close: Close;
  data: BackgroundData;
}

export interface BackgroundData {
  current: string;
  image: string;
  gallery?: {
    images: string[];
    montage2: string;
    montage4: string;
  };
}

interface Background {
  key: string;
  name: string;
  title?: string;
}

export function ctrl(data: BackgroundData, trans: Trans, redraw: Redraw, close: Close): BackgroundCtrl {
  const list: Background[] = [
    { key: 'system', name: trans.noarg('deviceTheme') },
    { key: 'light', name: trans.noarg('light') },
    { key: 'dark', name: trans.noarg('dark') },
    { key: 'darkBoard', name: 'Dark Board', title: 'Like Dark, but chess boards are also darker' },
    { key: 'transp', name: 'Picture' },
  ];

  const announceFail = () => lichess.announce({ msg: 'Failed to save background preference' });

  const reloadAllTheThings = () => {
    if ($('canvas').length || window.Highcharts) lichess.reload();
  };

  return {
    list,
    trans,
    get: () => data.current,
    set: throttlePromiseDelay(
      () => 700,
      (c: string) => {
        data.current = c;
        applyBackground(data, list);
        redraw();
        return xhr
          .text('/pref/bg', {
            body: xhr.form({ bg: c }),
            method: 'post',
          })
          .then(reloadAllTheThings, announceFail);
      },
    ),
    getImage: () => data.image,
    setImage(i: string) {
      data.image = i;
      xhr
        .text('/pref/bgImg', {
          body: xhr.form({ bgImg: i }),
          method: 'post',
        })
        .then(reloadAllTheThings, announceFail);
      applyBackground(data, list);
      redraw();
    },
    close,
    data,
  };
}

export function view(ctrl: BackgroundCtrl): VNode {
  const cur = ctrl.get();

  return h('div.sub.background', [
    header(ctrl.trans.noarg('background'), ctrl.close),
    h(
      'div.selector.large',
      ctrl.list.map(bg => {
        return h(
          'button.text',
          {
            class: { active: cur === bg.key },
            attrs: { 'data-icon': licon.Checkmark, title: bg.title || '', type: 'button' },
            hook: bind('click', () => ctrl.set(bg.key)),
          },
          bg.name,
        );
      }),
    ),
    cur !== 'transp' ? null : ctrl.data.gallery ? galleryInput(ctrl) : imageInput(ctrl),
  ]);
}

function imageInput(ctrl: BackgroundCtrl) {
  return h('div.image', [
    h('p', ctrl.trans.noarg('backgroundImageUrl')),
    h('input', {
      attrs: {
        type: 'text',
        placeholder: 'https://',
        value: ctrl.getImage(),
      },
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).on(
            'change keyup paste',
            debounce(function (this: HTMLInputElement) {
              const url = (this.value as string).trim();
              // modules/pref/src/main/PrefForm.scala
              if (
                (url.startsWith('https://') || url.startsWith('//')) &&
                url.length >= 10 &&
                url.length <= 400
              )
                ctrl.setImage(url);
            }, 300),
          );
        },
      },
    }),
  ]);
}

function applyBackground(data: BackgroundData, list: Background[]) {
  const key = data.current;
  const cls = key == 'transp' ? 'dark transp' : key == 'darkBoard' ? 'dark dark-board' : key;

  $('body')
    .removeClass([...list.map(b => b.key), 'dark-board'].join(' '))
    .addClass(cls);

  const prev = document.body.dataset.theme!;
  const sheet = key == 'darkBoard' ? 'dark' : key;
  document.body.dataset.theme = sheet;
  if (prev === 'system') {
    const active = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
    const other = active === 'dark' ? 'light' : 'dark';
    $('link[href*=".' + other + '."]').remove();
    $('link[href*=".' + active + '."]').each(function (this: HTMLLinkElement) {
      replaceStylesheet(this, active, sheet);
    });
  } else {
    $('link[href*=".' + prev + '."]').each(function (this: HTMLLinkElement) {
      if (sheet === 'system') {
        if (supportsSystemTheme()) {
          replaceStylesheet(this, prev, 'light', 'light');
          replaceStylesheet(this, prev, 'dark', 'dark');
        } else {
          replaceStylesheet(this, prev, 'dark');
        }
      } else replaceStylesheet(this, prev, sheet);
    });
  }

  if (key === 'transp') {
    const bgData = document.getElementById('bg-data');
    bgData
      ? (bgData.innerHTML = 'body.transp::before{background-image:url(' + data.image + ');}')
      : $('head').append(
          '<style id="bg-data">body.transp::before{background-image:url(' + data.image + ');}</style>',
        );
  }
}

function replaceStylesheet(old: HTMLLinkElement, oldKey: string, newKey: string, media?: 'dark' | 'light') {
  const link = document.createElement('link') as HTMLLinkElement;
  link.rel = 'stylesheet';
  link.href = old.href.replace('.' + oldKey + '.', '.' + newKey + '.');
  if (media) link.media = `(prefers-color-scheme: ${media})`;
  link.onload = () => setTimeout(() => old.remove(), 100);
  document.head.appendChild(link);
}

function galleryInput(ctrl: BackgroundCtrl) {
  const urlId = (url: string) => url.replace(/[^\w]/g, '_');

  function setImg(url: string) {
    $('#images-grid .selected').removeClass('selected');
    $(`#${urlId(url)}`).addClass('selected');
    ctrl.setImage(url);
  }

  const gallery = ctrl.data.gallery!;
  const cols = window.matchMedia('(min-width: 650px)').matches ? 4 : 2; // $mq-x-small
  const montageUrl = lichess.assetUrl(gallery[`montage${cols}`], { noVersion: true });
  // our layout is static due to the single image gallery optimization. set width here
  // and allow for the possibility of non-overlaid scrollbars
  const width = cols * (160 + 2) + (gallery.images.length > cols * 4 ? elementScrollBarWidth() : 0);

  return h('div#gallery', { attrs: { style: `width: ${width}px` } }, [
    h(
      'div#images-viewport',
      h(
        'div#images-grid',
        { attrs: { style: `background-image: url(${montageUrl});` } },
        gallery.images.map(img => {
          const assetUrl = lichess.assetUrl(img, { noVersion: true });
          const divClass = ctrl.data.image.endsWith(assetUrl) ? '.selected' : '';
          return h(`div#${urlId(assetUrl)}${divClass}`, {
            hook: bind('click', () => setImg(assetUrl)),
          });
        }),
      ),
    ),
    h('span#url', [
      h('label', 'URL'),
      h('input', {
        attrs: { type: 'text', placeholder: 'https://', value: ctrl.data.image },
        hook: onInsert((el: HTMLInputElement) =>
          $(el).on(
            'change keyup paste',
            debounce(() => setImg(el.value.trim()), 300),
          ),
        ),
      }),
    ]),
  ]);
}
