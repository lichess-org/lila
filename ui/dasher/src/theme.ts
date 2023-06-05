import { VNode, h } from 'snabbdom';
import { Open, Redraw, bind, header } from './util';

type Theme = string;

export interface ThemeData {
  thickGrid: boolean;
  current: Theme;
  list: Theme[];
}

export interface ThemeCtrl {
  data: ThemeData;
  trans: Trans;
  set(t: Theme): void;
  setThickGrid(isThick: boolean): void;
  open: Open;
}

export function ctrl(data: ThemeData, trans: Trans, redraw: Redraw, open: Open): ThemeCtrl {
  return {
    trans,
    data,
    set(t: Theme) {
      data.current = t;
      applyTheme(t, data.list);
      $.post('/pref/theme', {
        theme: t,
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
    list = [...ctrl.data.list.slice(0, lastIndex), 'thickGrid', ctrl.data.list[lastIndex]];
  return h('div.sub.theme', [
    header(ctrl.trans.noarg('boardTheme'), () => ctrl.open('links')),
    h(
      'div.list',
      list.map(t => themeView(ctrl, t))
    ),
  ]);
}

function themeView(ctrl: ThemeCtrl, t: Theme) {
  if (t === 'custom') return customThemeView(ctrl);
  else if (t === 'thickGrid') return thickGrid(ctrl);
  else
    return h(
      'a',
      {
        hook: bind('click', () => ctrl.set(t)),
        attrs: { title: t },
        class: { active: ctrl.data.current === t },
      },
      h('span.' + t)
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
      attrs: { title: 'custom', 'data-icon': 'H' },
      class: { active: ctrl.data.current === 'custom' },
    },
    ctrl.trans('customTheme')
  );
}

function applyThickGrid(isThick: boolean) {
  $('body').toggleClass('thick-grid', isThick);
}

function applyTheme(t: Theme, list: Theme[]) {
  $('body').removeClass(list.join(' ')).addClass(t);
}
