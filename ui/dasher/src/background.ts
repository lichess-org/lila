import { h, VNode } from 'snabbdom';
import { Close, elementScrollBarWidthSlowGuess, header } from './util';
import debounce from 'common/debounce';
import * as licon from 'common/licon';
import { bind, onInsert, Redraw } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { throttlePromiseDelay } from 'common/throttle';

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

export class BackgroundCtrl {
  list: Background[];

  constructor(
    readonly data: BackgroundData,
    readonly trans: Trans,
    readonly redraw: Redraw,
    readonly close: Close,
  ) {
    this.list = [
      { key: 'system', name: trans.noarg('deviceTheme') },
      { key: 'light', name: trans.noarg('light') },
      { key: 'dark', name: trans.noarg('dark') },
      { key: 'darkBoard', name: 'Dark Board', title: 'Like Dark, but chess boards are also darker' },
      { key: 'transp', name: 'Picture' },
    ];
  }

  private announceFail = (err: string) =>
    site.announce({ msg: `Failed to save background preference: ${err}` });

  private reloadAllTheThings = () => {
    if ($('canvas').length) site.reload();
  };

  get = () => this.data.current;
  set = throttlePromiseDelay(
    () => 700,
    (c: string) => {
      this.data.current = c;
      applyBackground(this.data);
      this.redraw();
      return xhr
        .text('/pref/bg', { body: xhr.form({ bg: c }), method: 'post' })
        .then(this.reloadAllTheThings, this.announceFail);
    },
  );
  getImage = () => this.data.image;
  setImage = (i: string) => {
    this.data.image = i;
    xhr
      .textRaw('/pref/bgImg', { body: xhr.form({ bgImg: i }), method: 'post' })
      .then(res => (res.ok ? res.text() : Promise.reject(res.text())))
      .then(this.reloadAllTheThings, err => err.then(this.announceFail));
    applyBackground(this.data);
    this.redraw();
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
      attrs: { type: 'text', placeholder: 'https://', value: ctrl.getImage() },
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).on(
            'change keyup paste',
            debounce(function (this: HTMLInputElement) {
              const url = this.value.trim();
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

function applyBackground(data: BackgroundData) {
  const key = data.current;
  document.body.dataset.theme = key === 'darkBoard' ? 'dark' : key;
  document.documentElement.className =
    key === 'system' ? (window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark') : key;

  if (key === 'transp') {
    const bgData = document.getElementById('bg-data');
    bgData
      ? (bgData.innerHTML = 'html.transp::before{background-image:url(' + data.image + ');}')
      : $('head').append(
          '<style id="bg-data">html.transp::before{background-image:url(' + data.image + ');}</style>',
        );
  }
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
  const montageUrl = site.asset.url(gallery[`montage${cols}`], { noVersion: true });
  // our layout is static due to the single image gallery optimization. set width here
  // and allow for the possibility of non-overlaid scrollbars
  const width = cols * (160 + 2) + (gallery.images.length > cols * 4 ? elementScrollBarWidthSlowGuess() : 0);

  return h('div#gallery', { attrs: { style: `width: ${width}px` } }, [
    h('div#images-viewport', [
      h(
        'div#images-grid',
        { attrs: { style: `background-image: url(${montageUrl});` } },
        gallery.images.map(img => {
          const assetUrl = site.asset.url(img, { noVersion: true });
          const divClass = ctrl.data.image.endsWith(assetUrl) ? '.selected' : '';
          return h(`div#${urlId(assetUrl)}${divClass}`, { hook: bind('click', () => setImg(assetUrl)) });
        }),
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
    ]),
  ]);
}
