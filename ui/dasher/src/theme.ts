import { h, type VNode } from 'snabbdom';

import { debounce, throttlePromiseDelay } from 'lib/async';
import { prefersLightThemeQuery } from 'lib/device';
import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { bind, button, dataIcon, label, div, onInsert, span } from 'lib/view';
import { cmnToggleWrap } from 'lib/view/cmn-toggle';
import { text as xhrText, form as xhrForm, textRaw as xhrTextRaw } from 'lib/xhr';

import type { DasherCtrl } from '@/ctrl';

import { PaneCtrl, type Range } from './interfaces';
import { elementScrollBarWidthSlowGuess, header } from './util';

type BackgroundThemeGalleryData = {
  images: string[];
  montage2: string;
  montage4: string;
};

export interface BackgroundData {
  current: string;
  image: string;
  gallery: {
    light: BackgroundThemeGalleryData;
    dark: BackgroundThemeGalleryData;
  };
}

interface Background {
  key: string;
  name: string;
  title?: string;
}

export class ThemeCtrl extends PaneCtrl {
  sliderKey: number = Date.now(); // changing the value attribute doesn't always flush to DOM.
  private readonly list: Background[];
  constructor(root: DasherCtrl) {
    super(root);
    this.list = [
      { key: 'system', name: i18n.site.deviceTheme },
      { key: 'light', name: i18n.site.light },
      { key: 'dark', name: i18n.site.dark },
    ];
  }

  render(): VNode {
    const cur = this.get();
    const isTransp = cur.includes('transp');

    return div('.sub.theme', [
      header(i18n.site.theme, this.close),
      div('.selector.large', [
        this.list.map(bg => {
          return button(
            '.text',
            {
              class: { active: cur.includes(bg.key) },
              ...dataIcon(licon.Checkmark),
              title: bg.title || '',
              type: 'button',
              hook: bind('click', () => {
                const current = this.get();
                current.includes('transp') ? this.set(`transp ${bg.key}`) : this.set(bg.key);
              }),
            },
            bg.name,
          );
        }),
        this.propSlider('ui-roundness', i18n.site.roundness, { min: 0, max: 15, step: 1 }),
        cmnToggleWrap({
          id: 'background-picture-toggle',
          name: i18n.site.backgroundImage,
          checked: isTransp,
          change: () => {
            const current = this.get();
            const isTransp = current.includes('transp');
            if (current.includes('system')) {
              this.set(isTransp ? 'system' : 'transp system');
            } else if (current.includes('light')) {
              this.set(isTransp ? 'light' : 'transp light');
            } else {
              this.set(isTransp ? 'dark' : 'transp dark');
            }
          },
          redraw: this.redraw,
        }),
        isTransp
          ? this.propSlider(
              'bg-opacity',
              i18n.site.imageOpacity,
              { min: 5, max: 100, step: 1 },
              val => `${val}%`,
              '',
            )
          : null,
      ]),
      isTransp ? (this.backgroundData.gallery ? this.galleryInput() : this.imageInput()) : null,
    ]);
  }

  set: (c: string) => Promise<void> = throttlePromiseDelay(
    () => 700,
    (c: string) => {
      this.backgroundData.current = c;
      this.apply();
      this.redraw();
      return xhrText('/pref/bg', { body: xhrForm({ bg: c }), method: 'post' }).then(
        this.reloadAllTheThings,
        this.announceFail,
      );
    },
  );

  private get backgroundData() {
    return this.root.data.background;
  }

  private readonly announceFail = (err: string) =>
    site.announce({ msg: `Failed to save background preference: ${err}` });

  private readonly reloadAllTheThings = () => {
    if ($('canvas').length) site.reload();
  };

  private readonly get = () => this.backgroundData.current;
  private readonly getImage = () => this.backgroundData.image;
  private readonly setImage = (i: string) => {
    this.backgroundData.image = i.startsWith('/assets/') ? i.slice(8) : i;
    xhrTextRaw('/pref/bgImg', { body: xhrForm({ bgImg: i }), method: 'post' })
      .then(res => (res.ok ? res.text() : Promise.reject(res.text())))
      .then(this.reloadAllTheThings, err => err.then(this.announceFail));
    this.apply();
    this.redraw();
  };

  private readonly apply = () => {
    const key = this.get();
    const isTransp = key.includes('transp');
    const systemPrefersLight = prefersLightThemeQuery().matches;

    document.body.dataset.theme = key;

    if (isTransp) {
      document.documentElement.className =
        key === 'system' ? (systemPrefersLight ? 'transp light' : 'transp dark') : key;

      const bgData = document.getElementById('bg-data');
      const styleValue = `html.${key.replace(' ', '.')}::before{background-image:url(${this.backgroundData.image});opacity:calc(var(---bg-opacity)/100);}`;
      if (bgData) {
        bgData.innerHTML = styleValue;
      } else {
        $('head').append(`<style id="bg-data">${styleValue}</style>`);
      }
    } else {
      document.documentElement.className = key === 'system' ? (systemPrefersLight ? 'light' : 'dark') : key;
    }

    pubsub.emit('theme', key);
  };

  private readonly imageInput = () =>
    h('div.image', [
      h('label', { attrs: { for: 'dasher-theme-backgroundUrl' } }, i18n.site.backgroundImageUrl),
      h('input#dasher-theme-backgroundUrl', {
        attrs: { type: 'text', placeholder: 'https://', value: this.getImage() },
        hook: onInsert<HTMLInputElement>(el => {
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
        }),
      }),
    ]);

  private readonly galleryInput = () => {
    const cur = this.get();
    const light = cur.includes('light');
    const urlId = (url: string) => url.replace(/[^\w]/g, '_');

    const setImg = (url: string) => {
      $('#dasher-theme-images-grid .selected').removeClass('selected');
      $(`#${urlId(url)}`).addClass('selected');
      this.setImage(url);
    };

    const gallery = light ? this.backgroundData.gallery?.light : this.backgroundData.gallery?.dark;
    const cols = window.matchMedia('(min-width: 650px)').matches ? 4 : 2;
    const montageUrl = site.asset.url(gallery[`montage${cols}`]);

    console.warn(montageUrl);
    const width =
      cols * (160 + 2) + (gallery.images.length > cols * 4 ? elementScrollBarWidthSlowGuess() : 0);

    return h('div#dasher-theme-gallery', { attrs: { style: `width: ${width}px` } }, [
      h('div#dasher-theme-images-viewport', [
        h(
          'div#dasher-theme-images-grid',
          { attrs: { style: `background-image: url(${montageUrl});` } },
          gallery.images.map(img => {
            const assetUrl = site.asset.url(img);
            const divClass = this.backgroundData.image.endsWith(assetUrl) ? '.selected' : '';
            return h(`div#${urlId(assetUrl)}${divClass}`, { hook: bind('click', () => setImg(assetUrl)) });
          }),
        ),
      ]),
      this.imageInput(),
    ]);
  };

  private readonly setVar = (prop: string, v: number, unit = 'px') => {
    document.documentElement.style.setProperty(`---${prop}`, `${v.toString()}${unit}`);
  };

  private readonly propSlider = (
    prop: string,
    inputLabel: string,
    range: Range,
    formatter?: (v: number) => string,
    unit = 'px',
  ) => {
    const value = this.getVar(prop);
    const printValue = formatter ? formatter(value) : `${value}${unit}`;
    return div(`.${prop}`, { title: printValue }, [
      div('.slider-label', [label(inputLabel), span(printValue)]),
      h('input.range', {
        key: this.sliderKey + prop,
        attrs: { ...range, type: 'range', value },
        hook: onInsert<HTMLInputElement>(input => {
          const setAndSave = (v: number) => {
            if (v < range.min || v > range.max) return;
            this.setVar(prop, v, unit);
            this.redraw();
            this.postPref(prop);
          };
          $(input)
            .on('input', () => setAndSave(parseInt(input.value)))
            .on('wheel', e => {
              e.preventDefault();
              setAndSave(this.getVar(prop) + (e.deltaY > 0 ? -range.step : range.step));
            });
        }),
      }),
    ]);
  };
}
