import spinner from 'common/spinner';
import { VNode, h } from 'snabbdom';
import { Open, Redraw, header, validateUrl } from './util';

export interface CustomThemeData {
  boardColor: string;
  boardImg: string;
  gridColor: string;
  gridWidth: number;
  handsColor: string;
  handsImg: string;
}

type Key = keyof CustomThemeData;
type ColorKey = Key & ('gridColor' | 'boardColor' | 'handsColor');

export interface CustomThemeCtrl {
  set: <K extends Key>(key: K, value: CustomThemeData[K]) => void;
  trans: Trans;
  data: CustomThemeData;
  redraw: Redraw;
  open: Open;
  loading?: 'loading' | 'done'; // load spectrum only once
}

const announceFail = () => window.lishogi.announce({ msg: 'Failed to save custom preferences' });

export function ctrl(data: CustomThemeData, trans: Trans, redraw: Redraw, open: Open): CustomThemeCtrl {
  const saveTheme = window.lishogi.debounce(function (this: HTMLElement) {
    // once anything changes we lock in the values
    const posInitials: ColorKey[] = ['gridColor', 'boardColor', 'handsColor'];
    posInitials.forEach(key => {
      if (data[key] === 'initial') {
        data[key] = defaultColor(key);
        applyCustomTheme(key, data[key]);
      }
    });
    $.post('/pref/customTheme', data).fail(announceFail);
  }, 500);

  return {
    set: <K extends Key>(key: K, value: CustomThemeData[K]) => {
      data[key] = value;
      applyCustomTheme(key, value);
      saveTheme();
    },
    trans,
    data,
    redraw,
    open,
  };
}

const gridTypes: I18nKey[] = ['none', 'gridSlim', 'gridThick', 'gridVeryThick'];

export function view(ctrl: CustomThemeCtrl): VNode {
  if (ctrl.loading === 'done') {
    return h(
      'div.sub.custom-theme',
      {
        hook: {
          init: () => applyEverything(ctrl),
        },
      },
      [
        header(ctrl.trans.noarg('customTheme'), () => ctrl.open('theme')),
        h('div.board', [
          h('div.title', ctrl.trans.noarg('board')),
          makeColorInput(ctrl, ctrl.trans.noarg('backgroundColor'), 'boardColor'),
          makeTextInput(ctrl, ctrl.trans.noarg('backgroundImageUrl'), 'boardImg'),
        ]),
        h('div.grid', [
          h('div.title', ctrl.trans.noarg('grid')),
          makeSelection(
            ctrl,
            ctrl.trans.noarg('gridWidth'),
            gridTypes.map(o => ctrl.trans.noarg(o))
          ),
          makeColorInput(ctrl, ctrl.trans.noarg('gridColor'), 'gridColor'),
        ]),
        h('div.hands', [
          h('div.title', ctrl.trans.noarg('hands')),
          makeColorInput(ctrl, ctrl.trans.noarg('backgroundColor'), 'handsColor'),
          makeTextInput(ctrl, ctrl.trans.noarg('backgroundImageUrl'), 'handsImg'),
        ]),
      ]
    );
  } else {
    if (!ctrl.loading) {
      ctrl.loading = 'loading';
      window.lishogi.spectrum().done(() => {
        ctrl.loading = 'done';
        ctrl.redraw();
      });
    }
    return h('div.sub.custom-theme.loading', [
      header(ctrl.trans.noarg('customTheme'), () => ctrl.open('theme')),
      spinner(),
    ]);
  }
}

function makeColorInput(ctrl: CustomThemeCtrl, title: string, key: Key): VNode {
  return h('div.color-wrap', [h('p', title), h('input', { hook: { insert: vn => makeColorPicker(ctrl, vn, key) } })]);
}

function makeTextInput(ctrl: CustomThemeCtrl, title: string, key: Key): VNode {
  return h('div.url-wrap', [
    h('p', title),
    h('input', {
      attrs: {
        type: 'text',
        placeholder: 'https://',
        value: ctrl.data[key],
      },
      hook: {
        insert: vm =>
          $(vm.elm as HTMLElement).on('change keyup paste', function (this: HTMLElement) {
            const url = ($(this).val() as string).trim();
            if (validateUrl(url)) ctrl.set(key, url);
          }),
      },
    }),
  ]);
}

function makeSelection(ctrl: CustomThemeCtrl, name: string, options: string[]): VNode {
  return h('div.select-wrap', [
    h('p', name),
    h(
      'select',
      {
        hook: {
          insert: vm =>
            $(vm.elm as HTMLElement).on('change', function (this: HTMLElement) {
              ctrl.set('gridWidth', $(this).val());
            }),
        },
      },
      options.map((o, i) =>
        h(
          'option',
          {
            attrs: {
              value: i,
              selected: ctrl.data['gridWidth'] == i,
            },
          },
          o
        )
      )
    ),
  ]);
}

function makeColorPicker(ctrl: CustomThemeCtrl, vnode: VNode, key: Key) {
  const move = (color: any) => {
    const hexColor = color.toHex8String(),
      prevColor = ctrl.data[key] as string;
    if (hexColor === prevColor) return;
    if (hexColor.slice(-2) === '00' && prevColor.slice(1, -2) !== hexColor.slice(1, -2))
      $('.sp-container:not(.sp-hidden) .sp-alpha-handle').addClass('highlight');
    else if (prevColor.slice(-2) === '00') $('.sp-container:not(.sp-hidden) .sp-alpha-handle').removeClass('highlight');
    ctrl.set(key, hexColor);
  };

  $(vnode.elm as HTMLElement).spectrum({
    type: 'component',
    color: ctrl.data[key] === 'initial' ? defaultColor(key as ColorKey) : ctrl.data[key],
    preferredFormat: 'hex8',
    showPalette: false,
    showButtons: false,
    allowEmpty: false,
    top: '37px',
    move: window.lishogi.debounce(move, 20),
  });
}

function defaultColor(key: ColorKey): string {
  const isDark = document.body.classList.contains('dark'),
    isTransp = document.body.classList.contains('transp');
  if (key === 'gridColor') return isTransp ? '#cccccc' : isDark ? '#bababa' : '#4d4d4d';
  else return isTransp ? '#00000099' : isDark ? '#262421' : '#ffffff';
}

function cssVariableName(key: keyof Omit<CustomThemeData, 'gridWidth'>): string {
  switch (key) {
    case 'boardColor':
      return `board-color`;
    case 'boardImg':
      return 'board-url';
    case 'gridColor':
      return 'grid-color';
    case 'handsColor':
      return 'hands-color';
    case 'handsImg':
      return 'hands-url';
  }
}

function applyCustomTheme(key: Key, value: string | number) {
  if (key === 'gridWidth') {
    const kls = ['grid-width-0', 'grid-width-1', 'grid-width-2', 'grid-width-3'];
    document.body.classList.remove(...kls);
    document.body.classList.add(kls[value as number]);
  } else {
    const prefix = '--c-';
    if (key === 'boardImg' || key === 'handsImg') value = value ? `url(${value})` : 'none';
    document.body.style.setProperty(prefix + cssVariableName(key), value as string);
  }
}

function applyEverything(ctrl: CustomThemeCtrl): void {
  let k: Key;
  for (k in ctrl.data) {
    applyCustomTheme(k, ctrl.data[k]);
  }
}
