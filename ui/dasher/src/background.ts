import { h, type VNode } from 'snabbdom';
import { elementScrollBarWidthSlowGuess, header } from './util';
import { debounce, throttlePromiseDelay } from 'lib/async';
import { prefersLightThemeQuery } from 'lib/device';
import * as licon from 'lib/licon';
import { bind } from 'lib/snabbdom';
import { text as xhrText, form as xhrForm, textRaw as xhrTextRaw } from 'lib/xhr';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';

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
      { key: 'system', name: i18n.site.deviceTheme },
      { key: 'light', name: i18n.site.light },
      { key: 'dark', name: i18n.site.dark },
      { key: 'transp', name: 'Picture' },
    ];
  }

  render(): VNode {
    const cur = this.get();

    return h('div.sub.background', [
      header(i18n.site.background, this.close),
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

  set: (c: string) => Promise<void> = throttlePromiseDelay(
    () => 700,
    (c: string) => {
      this.data.current = c;
      this.apply();
      this.redraw();
      return xhrText('/pref/bg', { body: xhrForm({ bg: c }), method: 'post' }).then(
        this.reloadAllTheThings,
        this.announceFail,
      );
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
    this.data.image = i.startsWith('/assets/') ? i.slice(8) : i;
    xhrTextRaw('/pref/bgImg', { body: xhrForm({ bgImg: i }), method: 'post' })
      .then(res => (res.ok ? res.text() : Promise.reject(res.text())))
      .then(this.reloadAllTheThings, err => err.then(this.announceFail));
    this.apply();
    this.redraw();
  };

  private apply = () => {
    const key = this.data.current;
    document.body.dataset.theme = key === 'darkBoard' ? 'dark' : key;
    document.documentElement.className =
      key === 'system' ? (prefersLightThemeQuery().matches ? 'light' : 'dark') : key;

    if (key === 'transp') {
      const bgData = document.getElementById('bg-data');
      bgData
        ? (bgData.innerHTML = 'html.transp::before{background-image:url(' + this.data.image + ');}')
        : $('head').append(
            '<style id="bg-data">html.transp::before{background-image:url(' + this.data.image + ');}</style>',
          );
    }
    pubsub.emit('theme', key);
  };

  private imageInput = () =>
    h('div.image', [
      h('label', { attrs: { for: 'backgroundUrl' } }, i18n.site.backgroundImageUrl),
      h('input#backgroundUrl', {
        attrs: { type: 'text', placeholder: 'https://', value: this.getImage() },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            $(el).on(
              'change keyup paste',
              debounce(_ => {
                const url = el.value.trim();
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
            const assetUrl = site.asset.url(img);
            const divClass = this.data.image.endsWith(assetUrl) ? '.selected' : '';
            return h(`div#${urlId(assetUrl)}${divClass}`, { hook: bind('click', () => setImg(assetUrl)) });
          }),
        ),
      ]),
      this.imageInput(),
    ]);
  };
}
