import { h, VNode } from 'snabbdom';
import * as xhr from 'common/xhr';
import { header, Close } from './util';
import { bind, Redraw } from 'common/snabbdom';

type Theme = string;

interface ThemeDimData {
  current: Theme;
  list: Theme[];
}

export interface ThemeData {
  d2: ThemeDimData;
  d3: ThemeDimData;
}

export class ThemeCtrl {
  constructor(
    private readonly data: ThemeData,
    readonly trans: Trans,
    readonly dimension: () => keyof ThemeData,
    readonly redraw: Redraw,
    readonly close: Close,
  ) {}
  dimensionData = () => this.data[this.dimension()];
  set = (t: Theme) => {
    const d = this.dimensionData();
    d.current = t;
    applyTheme(t, d.list, this.dimension() === 'd3');
    const field = `theme${this.dimension() === 'd3' ? '3d' : ''}`;
    xhr
      .text(`/pref/${field}`, { body: xhr.form({ [field]: t }), method: 'post' })
      .catch(() => site.announce({ msg: 'Failed to save theme preference' }));
    this.redraw();
  };
}

export function view(ctrl: ThemeCtrl): VNode {
  const d = ctrl.dimensionData();

  return h('div.sub.theme.' + ctrl.dimension(), [
    header(ctrl.trans.noarg('boardTheme'), () => ctrl.close()),
    h('div.list', d.list.map(themeView(d.current, ctrl.set))),
  ]);
}

const themeView = (current: Theme, set: (t: Theme) => void) => (t: Theme) =>
  h(
    'button',
    {
      hook: bind('click', () => set(t)),
      attrs: { title: t, type: 'button' },
      class: { active: current === t },
    },
    h('span.' + t),
  );

function applyTheme(t: Theme, list: Theme[], is3d: boolean) {
  $('body').removeClass(list.join(' ')).addClass(t);
  if (!is3d) document.body.dataset.boardTheme = t;
  site.pubsub.emit('theme.change');
}
