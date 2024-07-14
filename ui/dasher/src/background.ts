import { h, VNode } from 'snabbdom';
import { elementScrollBarWidthSlowGuess, header } from './util';
import debounce from 'common/debounce';
import { prefersLight } from 'common/theme';
import * as licon from 'common/licon';
import { bind, onInsert } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { throttlePromiseDelay } from 'common/throttle';
import { DasherCtrl, PaneCtrl } from './interfaces';

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

export class BackgroundCtrl extends PaneCtrl {
  private list: Background[];
  constructor(root: DasherCtrl) {
    super(root);
    this.list = [
      { key: 'system', name: this.trans.noarg('deviceTheme') },
      { key: 'light', name: this.trans.noarg('light') },
      { key: 'dark', name: this.trans.noarg('dark') },
      { key: 'transp', name: 'Picture' },
    ];
  }

  render(): VNode {
    const cur = this.get();

    return h('div.sub.background', [
      header(this.trans.noarg('background'), this.close),
      h(
        'div.selector.large',
        this.list.map(bg => {
          return h(
            'button.text',
            {
              class: { active: cur === bg.key },
              attrs: { 'data-icon': licon.Checkmark, title: bg.title || '', type: 'button' },
              hook: bind('click', () => this.set(bg.key)),
            },
            bg.name,
          );
        }),
      ),
      cur !== 'transp' ? null : this.data.gallery ? this.galleryInput() : this.imageInput(),
    ]);
  }

  set = throttlePromiseDelay(
    () => 700,
    (c: string) => {
      this.data.current = c;
      this.apply();
      this.redraw();
      return xhr
        .text('/pref/bg', { body: xhr.form({ bg: c }), method: 'post' })
        .then(this.reloadAllTheThings, this.announceFail);
    },
  );

  private get data() {
    return this.root.data.background;
  }

  private announceFail = (err: string) =>
    site.announce({ msg: `Failed to save background preference: ${err}` });

  private reloadAllTheThings = () => {
    if ($('canvas').length) site.reload();
  };

  private get = () => this.data.current;
  private getImage = () => this.data.image;
  private setImage = (i: string) => {
    this.data.image = i;
    xhr
      .textRaw('/pref/bgImg', { body: xhr.form({ bgImg: i }), method: 'post' })
      .then(res => (res.ok ? res.text() : Promise.reject(res.text())))
      .then(this.reloadAllTheThings, err => err.then(this.announceFail));
    this.apply();
    this.redraw();
  };

  private apply = () => {
    const key = this.data.current;
    document.body.dataset.theme = key === 'darkBoard' ? 'dark' : key;
    document.documentElement.className = key === 'system' ? (prefersLight().matches ? 'light' : 'dark') : key;

    if (key === 'transp') {
      const bgData = document.getElementById('bg-data');
      bgData
        ? (bgData.innerHTML = 'html.transp::before{background-image:url(' + this.data.image + ');}')
        : $('head').append(
            '<style id="bg-data">html.transp::before{background-image:url(' + this.data.image + ');}</style>',
          );
    }
  };

  private imageInput = () =>
    h('div.image', [
      h('p', this.trans.noarg('backgroundImageUrl')),
      h('input', {
        attrs: { type: 'text', placeholder: 'https://', value: this.getImage() },
        hook: {
          insert: vnode => {
            $(vnode.elm as HTMLElement).on(
              'change keyup paste',
              debounce((el: HTMLInputElement) => {
                const url = (el.value as string).trim();
                if (
                  (url.startsWith('https://') || url.startsWith('//')) &&
                  url.length >= 10 &&
                  url.length <= 400
                )
                  this.setImage(url);
              }, 300),
            );
          },
        },
      }),
    ]);

  private galleryInput = () => {
    const urlId = (url: string) => url.replace(/[^\w]/g, '_');

    const setImg = (url: string) => {
      $('#images-grid .selected').removeClass('selected');
      $(`#${urlId(url)}`).addClass('selected');
      this.setImage(url);
    };

    const gallery = this.data.gallery!;
    const cols = window.matchMedia('(min-width: 650px)').matches ? 4 : 2;
    const montageUrl = site.asset.url(gallery[`montage${cols}`]);
    const width =
      cols * (160 + 2) + (gallery.images.length > cols * 4 ? elementScrollBarWidthSlowGuess() : 0);

    return h('div#gallery', { attrs: { style: `width: ${width}px` } }, [
      h('div#images-viewport', [
        h(
          'div#images-grid',
          { attrs: { style: `background-image: url(${montageUrl});` } },
          gallery.images.map(img => {
            const assetUrl = site.asset.url(img, { version: false });
            const divClass = this.data.image.endsWith(assetUrl) ? '.selected' : '';
            return h(`div#${urlId(assetUrl)}${divClass}`, { hook: bind('click', () => setImg(assetUrl)) });
          }),
        ),
      ]),
      h('span#url', [
        h('label', 'URL'),
        h('input', {
          attrs: { type: 'text', placeholder: 'https://', value: this.data.image },
          hook: onInsert((el: HTMLInputElement) =>
            $(el).on(
              'change keyup paste',
              debounce(() => setImg(el.value.trim()), 300),
            ),
          ),
        }),
      ]),
    ]);
  };
}
