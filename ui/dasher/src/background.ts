import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header, validateUrl } from './util';
import { debounce } from 'common/timings';
import { i18n } from 'i18n';

type Key = 'light' | 'dark' | 'transp';

export interface BackgroundCtrl {
  list: Background[];
  set(k: Key): void;
  get(): Key;
  getImage(): string;
  setImage(i: string): void;
  close: Close;
}

export interface BackgroundData {
  current: Key;
  image: string;
}

interface Background {
  key: Key;
  name: string;
}

export function ctrl(data: BackgroundData, redraw: Redraw, close: Close): BackgroundCtrl {
  const list: Background[] = [
    { key: 'light', name: i18n('light') },
    { key: 'dark', name: i18n('dark') },
    { key: 'transp', name: i18n('transparent') },
  ];

  const announceFail = () =>
    window.lishogi.announce({ msg: 'Failed to save background preference' });

  return {
    list,
    get: () => data.current,
    set(c: Key) {
      data.current = c;
      window.lishogi.xhr.text('POST', '/pref/bg', { formData: { bg: c } }).catch(announceFail);
      applyBackground(data, list);
      redraw();
    },
    getImage: () => data.image,
    setImage(i: string) {
      data.image = i;
      window.lishogi.xhr
        .text('POST', '/pref/bgImg', { formData: { bgImg: i } })
        .catch(announceFail);
      applyBackground(data, list);
      redraw();
    },
    close,
  };
}

export function view(ctrl: BackgroundCtrl): VNode {
  const cur = ctrl.get();

  return h('div.sub.background', [
    header(i18n('background'), ctrl.close),
    h(
      'div.selector.large',
      ctrl.list.map(bg => {
        return h(
          'a.text',
          {
            class: { active: cur === bg.key },
            attrs: { 'data-icon': 'E' },
            hook: bind('click', () => ctrl.set(bg.key)),
          },
          bg.name,
        );
      }),
    ),
    cur === 'transp' ? imageInput(ctrl) : null,
  ]);
}

function imageInput(ctrl: BackgroundCtrl) {
  return h('div.image', [
    h('p', i18n('backgroundImageUrl')),
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
            debounce(function (this: HTMLElement) {
              const url = ($(this).val() as string).trim();
              if (validateUrl(url)) ctrl.setImage(url);
            }, 300),
          );
        },
      },
    }),
  ]);
}

function applyBackground(data: BackgroundData, list: Background[]) {
  const key = data.current;

  document.querySelectorAll('body, html').forEach(el => {
    el.classList.remove(...list.map(b => b.key));
    el.classList.add(...(key === 'transp' ? ['transp', 'dark'] : [key]));
  });

  if (key === 'transp') {
    document.body.style.setProperty('--tr-bg-url', 'url(' + data.image + ')');
  }
}

// function reloadAllTheThings() {
//   if (window.Chart && confirm('Page will be reloaded')) window.lishogi.reload();
// }
