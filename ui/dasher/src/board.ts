import { header } from './util';
import { hyphenToCamel, toggle } from 'common';
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
      header(this.trans.noarg('board'), this.close),
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
      this.showReset() &&
        h(
          'button.text.reset',
          {
            attrs: { 'data-icon': licon.Back, type: 'button' },
            hook: bind('click', this.reset),
          },
          this.trans.noarg('boardReset'),
        ),
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
    this.defaults
      .filter(([prop, v]) => this.getVar(prop) !== v)
      .forEach(([prop, v], i) => {
        this.setVar(prop, v);
        setTimeout(() => this.postPref(prop), i * 1100); // hack around debounce
      });
    this.showReset(false);
    this.sliderKey = Date.now();
    document.body.classList.add('simple-board');
    this.redraw();
  };

  private getVar = (prop: string) =>
    parseInt(window.getComputedStyle(document.body).getPropertyValue(`---${prop}`));

  private setVar = (prop: string, v: number) => {
    this.showReset(this.showReset() || !this.isDefault());
    document.body.style.setProperty(`---${prop}`, v.toString());
    document.body.classList.toggle('simple-board', this.isDefault());
    if (prop === 'zoom') window.dispatchEvent(new Event('resize'));
  };

  private postPref = debounce((prop: string) => {
    const body = new FormData();
    body.set(hyphenToCamel(prop), this.getVar(prop).toString());
    const path = prop === 'zoom' ? `/pref/zoom?v=${this.getVar(prop)}` : `/pref/${hyphenToCamel(prop)}`;
    xhr.text(path, { body, method: 'post' }).catch(() => site.announce({ msg: `Failed to save ${prop}` }));
  }, 1000);

  private set3d = async (v: boolean) => {
    if (this.is3d === v) return;
    this.data.is3d = v;
    xhr
      .text('/pref/is3d', { body: xhr.form({ is3d: v }), method: 'post' })
      .catch(() => site.announce({ msg: 'Failed to save preference' }));

    if (v) await site.asset.loadCssPath('common.board-3d');
    else site.asset.removeCssPath('common.board-3d');
    $('#main-wrap')
      .removeClass(v ? 'is2d' : 'is3d')
      .addClass(v ? 'is3d' : 'is2d');
    this.apply();
    this.redraw();
  };

  private apply = (t: Board = this.current) => {
    this.current = t;
    document.body.dataset[this.is3d ? 'board3d' : 'board'] = t;
    site.pubsub.emit('board.change', this.is3d);
    this.root?.piece.apply();
  };

  private defaults: [string, number][] = [
    ['board-opacity', 100],
    ['board-brightness', 100],
    ['board-hue', 0],
  ];

  private isDefault = () => this.defaults.every(([prop, v]) => this.getVar(prop) === v);
  private showReset = toggle(!this.isDefault());

  private propSliders = () => {
    const sliders = [];
    if (!Number.isNaN(this.getVar('zoom')))
      sliders.push(this.propSlider('zoom', this.trans.noarg('size'), { min: 0, max: 100, step: 1 }));
    if (document.body.dataset.theme === 'transp')
      sliders.push(
        this.propSlider('board-opacity', this.trans.noarg('opacity'), { min: 0, max: 100, step: 1 }),
      );
    else
      sliders.push(
        this.propSlider('board-brightness', this.trans.noarg('brightness'), { min: 20, max: 140, step: 1 }),
      );
    sliders.push(
      this.propSlider(
        'board-hue',
        this.trans.noarg('hue'),
        { min: 0, max: 100, step: 1 },
        v => `+ ${Math.round(v * 3.6)}°`,
      ),
    );
    return sliders;
  };

  private propSlider = (prop: string, label: string, range: Range, title?: (v: number) => string) =>
    h(
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
}
