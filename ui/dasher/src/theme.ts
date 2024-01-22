import { VNode, h } from 'snabbdom';
import { Open, Redraw, bind, header } from './util';

type ThemeKey = string;
type Theme = {
  key: ThemeKey;
  name: string;
};

export interface ThemeData {
  current: ThemeKey;
  list: Theme[];
  thickGrid: boolean;
}

export interface ThemeCtrl {
  data: ThemeData;
  trans: Trans;
  set(t: ThemeKey): void;
  setThickGrid(isThick: boolean): void;
  open: Open;
}

export function ctrl(data: ThemeData, trans: Trans, redraw: Redraw, open: Open): ThemeCtrl {
  return {
    trans,
    data,
    set(key: ThemeKey) {
      data.current = key;
      applyTheme(key, data.list);
      $.post('/pref/theme', {
        theme: key,
      }).fail(() => window.lishogi.announce({ msg: 'Failed to save theme preference' }));
      redraw();
    },
    setThickGrid(isThick: boolean) {
      data.thickGrid = isThick;
      applyThickGrid(isThick);
      $.post('/pref/thickGrid', { thickGrid: isThick ? 1 : 0 }).fail(() =>
        window.lishogi.announce({ msg: 'Failed to save preference' })
      );
      redraw();
    },
    open,
  };
}

export function view(ctrl: ThemeCtrl): VNode {
  const lastIndex = ctrl.data.list.length - 1,
    list: (Theme | 'thickGrid')[] = [...ctrl.data.list.slice(0, lastIndex), 'thickGrid', ctrl.data.list[lastIndex]];
  return h('div.sub.theme', [
    header(ctrl.trans.noarg('boardTheme'), () => ctrl.open('links')),
    h(
      'div.list',
      list.map(i => themeView(ctrl, i))
    ),
  ]);
}

function themeView(ctrl: ThemeCtrl, t: Theme | 'thickGrid') {
  if (t === 'thickGrid') return thickGrid(ctrl);
  else if (t.key === 'custom') return customThemeView(ctrl);
  else
    return h(
      'a',
      {
        hook: bind('click', () => ctrl.set(t.key)),
        attrs: { title: t.name },
        class: { active: ctrl.data.current === t.key },
      },
      h('span.' + t.key)
    );
}

function thickGrid(ctrl: ThemeCtrl): VNode {
  const title = ctrl.trans.noarg('gridThick');
  return h(`div.thick-switch${['blue', 'gray', 'doubutsu'].includes(ctrl.data.current) ? '.disabled' : ''}`, [
    h('label', { attrs: { for: 'thickGrid' } }, title),
    h('div.switch', [
      h('input#thickGrid.cmn-toggle', {
        attrs: {
          type: 'checkbox',
          title: title,
          checked: ctrl.data.thickGrid,
        },
        hook: bind('change', (e: Event) => ctrl.setThickGrid((e.target as HTMLInputElement).checked)),
      }),
      h('label', { attrs: { for: 'thickGrid' } }),
    ]),
  ]);
}

function customThemeView(ctrl: ThemeCtrl): VNode {
  return h(
    'a.custom',
    {
      hook: bind('click', () => {
        ctrl.set('custom');
        ctrl.open('customTheme');
      }),
      attrs: { 'data-icon': 'H' },
      class: { active: ctrl.data.current === 'custom' },
    },
    ctrl.trans('customTheme')
  );
}

function applyThickGrid(isThick: boolean) {
  $('body').toggleClass('thick-grid', isThick);
}

function applyTheme(key: ThemeKey, list: Theme[]) {
  $('body')
    .removeClass(list.map(t => t.key).join(' '))
    .addClass(key);
}
