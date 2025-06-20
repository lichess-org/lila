import { header, moreButton } from './util';
import { hyphenToCamel, Toggle, toggle } from 'lib';
import { debounce } from 'lib/async';
import * as licon from 'lib/licon';
import { text as xhrText, form as xhrForm } from 'lib/xhr';
import { bind, hl, type VNode } from 'lib/snabbdom';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';

type Range = { min: number; max: number; step: number };

export class BoardCtrl extends PaneCtrl {
  sliderKey: number = Date.now(); // changing the value attribute doesn't always flush to DOM.
  featured: { [key in 'd2' | 'd3']: string[] } = { d2: [], d3: [] };
  more: Toggle;

  constructor(root: DasherCtrl) {
    super(root);
    this.more = toggle(false, root.redraw);
    for (const dim of ['d2', 'd3'] as const) {
      this.featured[dim] = this.data[dim].list.filter(t => t.featured).map(t => t.name);
    }
  }

  get boardList(): string[] {
    const all = this.data[this.dimension].list.map(t => t.name);
    const visible = this.featured[this.dimension].slice();
    if (!visible.includes(this.current)) visible.push(this.current);
    return this.more() ? all : visible;
  }

  render = (): VNode =>
    hl(`div.sub.board.${this.dimension}`, [
      header(i18n.site.board, this.close),
      hl('div.selector.large', [
        hl(
          'button.text',
          {
            class: { active: !this.is3d },
            attrs: { 'data-icon': licon.Checkmark, type: 'button' },
            hook: bind('click', () => this.set3d(false)),
          },
          '2D',
        ),
        hl(
          'button.text',
          {
            class: { active: this.is3d },
            attrs: { 'data-icon': licon.Checkmark, type: 'button' },
            hook: bind('click', () => this.set3d(true)),
          },
          '3D',
        ),
      ]),
      this.propSliders(),
      this.showReset() &&
        hl(
          'button.text.reset',
          {
            attrs: { 'data-icon': licon.Back, type: 'button' },
            hook: bind('click', this.reset),
          },
          i18n.site.boardReset,
        ),
      hl(
        'div.list',
        this.boardList.map((t: string) =>
          hl(
            'button',
            {
              key: t,
              hook: bind('click', () => this.setBoard(t)),
              attrs: { title: t, type: 'button' },
              class: { active: this.current === t },
            },
            hl('span.' + t),
          ),
        ),
      ),
      moreButton(this.more),
    ]);

  private get data() {
    return this.root.data.board;
  }

  private get current() {
    return this.data[this.dimension].current;
  }

  private set current(t: string) {
    this.data[this.dimension].current = t;
  }

  private setBoard = (t: string) => {
    this.apply(t);
    const field = `theme${this.is3d ? '3d' : ''}`;
    xhrText(`/pref/${field}`, { body: xhrForm({ [field]: t }), method: 'post' }).catch(() =>
      site.announce({ msg: 'Failed to save theme preference' }),
    );
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
    xhrText(path, { body, method: 'post' }).catch(() => site.announce({ msg: `Failed to save ${prop}` }));
  }, 1000);

  private set3d = async (v: boolean) => {
    if (this.is3d === v) return;
    this.data.is3d = v;
    xhrText('/pref/is3d', { body: xhrForm({ is3d: v }), method: 'post' }).catch(() =>
      site.announce({ msg: 'Failed to save preference' }),
    );

    if (v) await site.asset.loadCssPath('lib.board-3d');
    else site.asset.removeCssPath('lib.board-3d');
    $('#main-wrap')
      .removeClass(v ? 'is2d' : 'is3d')
      .addClass(v ? 'is3d' : 'is2d');
    this.apply();
    this.redraw();
  };

  private apply = (t: string = this.current) => {
    this.current = t;
    document.body.dataset[this.is3d ? 'board3d' : 'board'] = t;
    pubsub.emit('board.change', this.is3d);
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
      sliders.push(this.propSlider('zoom', i18n.site.size, { min: 0, max: 100, step: 1 }));
    if (document.body.dataset.theme === 'transp')
      sliders.push(this.propSlider('board-opacity', i18n.site.opacity, { min: 0, max: 100, step: 1 }));
    else
      sliders.push(this.propSlider('board-brightness', i18n.site.brightness, { min: 20, max: 140, step: 1 }));
    sliders.push(
      this.propSlider(
        'board-hue',
        i18n.site.hue,
        { min: 0, max: 100, step: 1 },
        v => `+ ${Math.round(v * 3.6)}°`,
      ),
    );
    return sliders;
  };

  private propSlider = (prop: string, label: string, range: Range, title?: (v: number) => string) =>
    hl(
      `div.${prop}`,
      { attrs: { title: title ? title(this.getVar(prop)) : `${Math.round(this.getVar(prop))}%` } },
      [
        hl('label', label),
        hl('input.range', {
          key: this.sliderKey + prop,
          attrs: { ...range, type: 'range', value: this.getVar(prop) },
          hook: {
            insert: (vnode: VNode) => {
              const input = vnode.elm as HTMLInputElement;
              const setAndSave = (v: number) => {
                if (v < range.min || v > range.max) return;
                this.setVar(prop, v);
                this.redraw();
                this.postPref(prop);
              };
              $(input)
                .on('input', () => setAndSave(parseInt(input.value)))
                .on('wheel', e => {
                  e.preventDefault();
                  setAndSave(this.getVar(prop) + (e.deltaY > 0 ? -range.step : range.step));
                });
            },
          },
        }),
      ],
    );
}
