import { VNode, h } from 'snabbdom';
import { Open, Redraw, bind, header } from './util';

type Theme = string;

export interface ThemeData {
  current: Theme;
  list: Theme[];
}

export interface ThemeCtrl {
  data: ThemeData;
  trans: Trans;
  set(t: Theme): void;
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
    open,
  };
}

export function view(ctrl: ThemeCtrl): VNode {
  return h('div.sub.theme', [
    header(ctrl.trans.noarg('boardTheme'), () => ctrl.open('links')),
    h(
      'div.list',
      ctrl.data.list.map(t => themeView(ctrl, t))
    ),
  ]);
}

function themeView(ctrl: ThemeCtrl, t: Theme) {
  if (t === 'custom') return customThemeView(ctrl);
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

function applyTheme(t: Theme, list: Theme[]) {
  $('body').removeClass(list.join(' ')).addClass(t);
}
