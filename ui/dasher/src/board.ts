import { header } from './util';
import { hyphenToCamel } from 'common';
import debounce from 'common/debounce';
import * as licon from 'common/licon';
import * as xhr from 'common/xhr';
import { bind, looseH as h, VNode } from 'common/snabbdom';
import { DasherCtrl, PaneCtrl } from './interfaces';

type Board = string;
type Range = { min: number; max: number; step: number };
type BoardPicks = { current: Board; list: Board[] };

export interface BoardData {
  is3d: boolean;
  d2: BoardPicks;
  d3: BoardPicks;
}

export class BoardCtrl extends PaneCtrl {
  constructor(root: DasherCtrl) {
    super(root);
  }

  render = () =>
    h(`div.sub.board.${this.dimension}`, [
      header('Board', this.close),
      h('div.selector.large', [
        h(
          'button.text',
          {
            class: { active: !this.is3d },
            attrs: { 'data-icon': licon.Checkmark, type: 'button' },
            hook: bind('click', () => this.setIs3d(false)),
          },
          '2D',
        ),
        h(
          'button.text',
          {
            class: { active: this.is3d },
            attrs: { 'data-icon': licon.Checkmark, type: 'button' },
            hook: bind('click', () => this.setIs3d(true)),
          },
          '3D',
        ),
      ]),
      ...this.propSliders(),
      h(
        'div.list',
        this.data[this.dimension].list.map((t: Board) =>
          h(
            'button',
            {
              hook: bind('click', () => this.set(t)),
              attrs: { title: t, type: 'button' },
              class: { active: this.current === t },
            },
            h('span.' + t),
          ),
        ),
      ),
    ]);

  private get data() {
    return this.root.data.board;
  }

  private get current() {
    return this.data[this.dimension].current;
  }

  private set current(t: Board) {
    this.data[this.dimension].current = t;
  }

  private set = (t: Board) => {
    this.apply(t);
    const field = `theme${this.is3d ? '3d' : ''}`;
    xhr
      .text(`/pref/${field}`, { body: xhr.form({ [field]: t }), method: 'post' })
      .catch(() => site.announce({ msg: 'Failed to save theme preference' }));
    this.redraw();
  };

  private getPref = (prop: string) =>
    parseFloat(window.getComputedStyle(document.body).getPropertyValue(`---${prop}`));

  private postPref = debounce((prop: string) => {
    const body = new FormData();
    body.set(hyphenToCamel(prop), this.getPref(prop).toString());
    xhr
      .text(`/pref/${hyphenToCamel(prop)}`, { body, method: 'post' })
      .catch(() => site.announce({ msg: `Failed to save ${prop}` }));
  }, 1000);

  private setIs3d = async (v: boolean) => {
    this.data.is3d = v;
    xhr
      .text('/pref/is3d', { body: xhr.form({ is3d: v }), method: 'post' })
      .catch(() => site.announce({ msg: 'Failed to save preference' }));
    if (v) await site.asset.loadCssPath('board-3d');
    else site.asset.removeCssPath('board-3d'); // chalk it up to common/css/theme/board/*.css

    $('#main-wrap')
      .removeClass(v ? 'is2d' : 'is3d')
      .addClass(v ? 'is3d' : 'is2d');
    this.apply();
    this.redraw();
  };

  private setPref = (prop: string, v: number) => {
    document.body.style.setProperty(`---${prop}`, v.toString());
    if (prop === 'zoom') window.dispatchEvent(new Event('resize'));
    this.redraw();
    this.postPref(prop);
  };

  private apply = (t: Board = this.current) => {
    this.current = t;
    $('body')
      .removeClass([...this.data.d2.list, ...this.data.d3.list].join(' '))
      .addClass(t);
    if (!this.is3d) document.body.dataset.boardTheme = t;
    site.pubsub.emit('theme.change');
    this.root?.piece.apply();
  };

  private propSliders = () => {
    const sliders = [];
    if (!Number.isNaN(this.getPref('zoom')))
      sliders.push(this.propSlider('zoom', 'Size', { min: 0, max: 100, step: 1 }));
    if (document.body.classList.contains('simple-board')) return sliders;
    if (document.body.dataset.theme === 'transp')
      sliders.push(this.propSlider('board-opacity', 'Opacity', { min: 0, max: 1, step: 0.01 }));
    else sliders.push(this.propSlider('board-brightness', 'Brightness', { min: 0.4, max: 1.4, step: 0.01 }));
    sliders.push(
      this.propSlider('board-hue', 'Hue', { min: 0, max: 1, step: 0.01 }, v => `+ ${Math.round(v * 360)}°`),
    );
    return sliders;
  };

  private propSlider = (prop: string, label: string, range: Range, title?: (v: number) => string) =>
    h(
      `div.${prop}`,
      { attrs: { title: title ? title(this.getPref(prop)) : `${Math.round(this.getPref(prop) * 100)}%` } },
      [
        h('label', label),
        h('input.range', {
          attrs: { ...range, type: 'range', value: this.getPref(prop) },

          hook: {
            insert: (vnode: VNode) => {
              const input = vnode.elm as HTMLInputElement;
              $(input).on('input', () => this.setPref(prop, parseFloat(input.value)));
            },
          },
        }),
      ],
    );
}
