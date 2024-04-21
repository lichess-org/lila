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
  sliderKey = Date.now(); // changing the value attribute doesn't always flush to DOM.

  constructor(root: DasherCtrl) {
    super(root);
  }

  render = () =>
    h(`div.sub.board.${this.dimension}`, [
      header(
        'Board',
        this.close,
        !this.isDefault() &&
          h('button.text.reset', {
            attrs: { 'data-icon': licon.Back, type: 'button', title: 'Reset colors to default' },
            hook: bind('click', this.reset),
          }),
      ),
      h('div.selector.large', [
        h(
          'button.text',
          {
            class: { active: !this.is3d },
            attrs: { 'data-icon': licon.Checkmark, type: 'button' },
            hook: bind('click', () => this.set3d(false)),
          },
          '2D',
        ),
        h(
          'button.text',
          {
            class: { active: this.is3d },
            attrs: { 'data-icon': licon.Checkmark, type: 'button' },
            hook: bind('click', () => this.set3d(true)),
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
              hook: bind('click', () => this.setBoard(t)),
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

  private setBoard = (t: Board) => {
    this.apply(t);
    const field = `theme${this.is3d ? '3d' : ''}`;
    xhr
      .text(`/pref/${field}`, { body: xhr.form({ [field]: t }), method: 'post' })
      .catch(() => site.announce({ msg: 'Failed to save theme preference' }));
    this.redraw();
  };

  private reset = () => {
    this.setVar('board-opacity', 100);
    this.setVar('board-brightness', 100);
    this.setVar('board-hue', 0);
    this.postPref('board-opacity');
    this.postPref('board-brightness');
    this.postPref('board-hue');
    this.sliderKey = Date.now();
    document.body.classList.add('simple-board');
    this.root.redraw();
  };

  private getVar = (prop: string) =>
    parseInt(window.getComputedStyle(document.body).getPropertyValue(`---${prop}`));

  private setVar = (prop: string, v: number) => {
    document.body.style.setProperty(`---${prop}`, v.toString());
    document.body.classList.toggle('simple-board', this.isDefault());
    if (prop === 'zoom') window.dispatchEvent(new Event('resize'));
  };

  private postPref = debounce((prop: string) => {
    const body = new FormData();
    body.set(hyphenToCamel(prop), this.getVar(prop).toString());
    xhr
      .text(`/pref/${hyphenToCamel(prop)}`, { body, method: 'post' })
      .catch(() => site.announce({ msg: `Failed to save ${prop}` }));
  }, 1000);

  private set3d = async (v: boolean) => {
    this.data.is3d = v;
    xhr
      .text('/pref/is3d', { body: xhr.form({ is3d: v }), method: 'post' })
      .catch(() => site.announce({ msg: 'Failed to save preference' }));

    if (v) await site.asset.loadCssPath('board-3d');
    else site.asset.removeCssPath('board-3d');
    $('#main-wrap')
      .removeClass(v ? 'is2d' : 'is3d')
      .addClass(v ? 'is3d' : 'is2d');
    this.apply();
    this.redraw();
  };

  private apply = (t: Board = this.current) => {
    this.current = t;
    document.body.dataset[this.is3d ? 'board3d' : 'board'] = t;
    site.pubsub.emit('theme.change');
    this.root?.piece.apply();
  };

  private isDefault = () =>
    this.getVar('board-brightness') === 100 &&
    this.getVar('board-hue') === 0 &&
    this.getVar('board-opacity') === 100;

  private propSliders = () => {
    const sliders = [];
    if (!Number.isNaN(this.getVar('zoom')))
      sliders.push(this.propSlider('zoom', 'Size', { min: 0, max: 100, step: 1 }));
    if (document.body.dataset.theme === 'transp')
      sliders.push(this.propSlider('board-opacity', 'Opacity', { min: 0, max: 100, step: 1 }));
    else sliders.push(this.propSlider('board-brightness', 'Brightness', { min: 20, max: 140, step: 1 }));
    sliders.push(
      this.propSlider('board-hue', 'Hue', { min: 0, max: 100, step: 1 }, v => `+ ${Math.round(v * 3.6)}°`),
    );
    return sliders;
  };

  private propSlider = (prop: string, label: string, range: Range, title?: (v: number) => string) => {
    return h(
      `div.${prop}`,
      { attrs: { title: title ? title(this.getVar(prop)) : `${Math.round(this.getVar(prop))}%` } },
      [
        h('label', label),
        h('input.range', {
          key: this.sliderKey + prop,
          attrs: { ...range, type: 'range', value: this.getVar(prop) },

          hook: {
            insert: (vnode: VNode) => {
              const input = vnode.elm as HTMLInputElement;
              $(input).on('input', () => {
                this.setVar(prop, parseInt(input.value));
                this.redraw();
                this.postPref(prop);
              });
            },
          },
        }),
      ],
    );
  };
}
