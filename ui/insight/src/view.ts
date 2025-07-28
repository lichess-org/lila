import { thunk } from 'snabbdom';
import { debounce } from 'lib/async';
import * as licon from 'lib/licon';
import axis from './axis';
import filters from './filters';
import presets from './presets';
import chart from './chart';
import { vert } from './table';
import help from './help';
import info from './info';
import boards from './boards';
import type Ctrl from './ctrl';
import type { ViewTab } from './interfaces';
import { bind, hl } from 'lib/snabbdom';

let forceRender = false;

// layout bifurcation due to the preset/filter tab views being hoisted to top level in portrait mode
export function view(ctrl: Ctrl) {
  window.onresize = debounce(() => {
    forceRender = true;
    ctrl.redraw();
  }, 33);
  if (isLandscapeLayout()) {
    ctrl.vm.view = 'combined';
    return landscapeView(ctrl);
  } else if (ctrl.vm.view === 'combined') {
    ctrl.vm.view = 'insights';
  }
  return portraitView(ctrl);
}

export function isLandscapeLayout() {
  return isAtLeastXSmall() || window.innerWidth > window.innerHeight;
}

// Key that determines whether or not renderMain needs to get rerendered
const cacheKey = (ctrl: Ctrl) => {
  if (forceRender) {
    forceRender = false;
    return Date.now().toString(); // cache buster
  }
  const q = ctrl.vm.answer?.question;
  return q ? ctrl.makeUrl(q.dimension, q.metric, q.filters) : ctrl.vm.broken;
};

const renderMain = (ctrl: Ctrl, _cacheKey: string | boolean) => {
  if (!ctrl.vm.answer) {
    return hl('div'); // returning undefined breaks snabbdom's thunks
  } else if (ctrl.vm.broken) {
    return hl('div.broken', [
      hl('i', { attrs: { 'data-icon': licon.DiscBig } }),
      'Insights are unavailable.',
      hl('br'),
      'Please try again later.',
    ]);
  }
  const sizer = widthStyle(mainW());
  return hl('div', sizer, [chart(ctrl), vert(ctrl, sizer), boards(ctrl, sizer)]);
};

const panelTabData = (ctrl: Ctrl, panel: 'filter' | 'preset') => ({
  class: { active: ctrl.vm.panel === panel },
  attrs: { 'data-panel': panel },
  hook: bind('click', () => ctrl.setPanel(panel)),
});

const viewTabData = (ctrl: Ctrl, view: ViewTab) => ({
  class: { active: ctrl.vm.view === view },
  hook: bind('click', () => ctrl.setView(view)),
});

function header(ctrl: Ctrl) {
  return hl('header', widthStyle(mainW()), [
    isAtLeastXSmall(mainW())
      ? hl('h2.text', { attrs: { 'data-icon': licon.Target } }, 'Chess Insights')
      : isAtLeastXXSmall(mainW())
        ? hl('h2.text', { attrs: { 'data-icon': licon.Target } }, 'Insights')
        : mainW() >= 460 && hl('h2.text', 'Insights'),
    axis(ctrl, mainW() < 460 ? { attrs: { style: 'justify-content: space-evenly;' } } : null),
  ]);
}

function landscapeView(ctrl: Ctrl) {
  return hl('main#insight', containerStyle(), [
    hl('div', { attrs: { class: ctrl.vm.loading ? 'loading' : 'ready' } }, [
      hl('div', widthStyle(sideW()), [
        info(ctrl),
        hl('div.panel-tabs', [
          hl('a.tab.preset', panelTabData(ctrl, 'preset'), 'Presets'),
          hl('a.tab.filter', panelTabData(ctrl, 'filter'), 'Filters'),
          !!Object.keys(ctrl.vm.filters).length && clearBtn(ctrl),
        ]),
        ctrl.vm.panel === 'filter' && filters(ctrl),
        ctrl.vm.panel === 'preset' && presets(ctrl),
        help(ctrl),
      ]),
      spacer(),
      hl('div', widthStyle(mainW()), [
        header(ctrl),
        thunk('div.insight__main.box', renderMain, [ctrl, cacheKey(ctrl)]),
      ]),
    ]),
  ]);
}

function portraitView(ctrl: Ctrl) {
  return hl('main#insight', containerStyle(), [
    hl('div.view-tabs', [
      hl('div.tab', viewTabData(ctrl, 'presets'), 'Presets'),
      hl('div.tab', viewTabData(ctrl, 'filters'), 'Filters'),
      hl('div.tab', viewTabData(ctrl, 'insights'), 'Insights'),
    ]),
    hl(
      'div',
      { attrs: { class: ctrl.vm.loading ? 'loading' : 'ready', style: 'display: block' } },
      ctrl.vm.view === 'insights'
        ? [header(ctrl), thunk('div.insight__main.box', renderMain, [ctrl, cacheKey(ctrl)])]
        : hl('div.left-side', [
            info(ctrl),
            ctrl.vm.view === 'filters' && clearBtn(ctrl),
            ctrl.vm.view === 'presets' ? presets(ctrl) : filters(ctrl),
          ]),
    ),
  ]);
}

function clearBtn(ctrl: Ctrl) {
  const btn = () =>
    hl(
      'a.clear',
      {
        attrs: { title: 'Clear all filters', 'data-icon': licon.X },
        hook: bind('click', ctrl.clearFilters.bind(ctrl)),
      },
      isLandscapeLayout() ? 'CLEAR' : 'CLEAR FILTERS',
    );
  return isLandscapeLayout() ? btn() : hl('div.center-clear', btn());
}

type Point = { x: number; y: number }; // y = f(x), not necessarily a point onscreen

function interpolateBetween(t: number, p1: Point, p2: Point) {
  if (t < p1.x || p2.x <= p1.x) return p1.y;
  else if (t > p2.x) return p2.y;
  else return Math.floor(p1.y + ((p2.y - p1.y) / (p2.x - p1.x)) * (t - p1.x)); // between
}

const totalW = () => Math.min(1300, window.innerWidth);
const vw = () => Math.floor(totalW() * 0.01); // 1vw in CSS
const availW = () => totalW() - (isAtLeastSmall() ? 2 * vw() : 0); // edge gaps from page grid

const sideW = () =>
  // width of the side in landscape layout, no side in portrait
  isLandscapeLayout() ? interpolateBetween(totalW(), { x: 400, y: 160 }, { x: 800, y: 280 }) : 0;

const gapW = () =>
  // width of the spacer between side & main in landscape, no gap in portrait
  isLandscapeLayout() ? interpolateBetween(totalW(), { x: 480, y: vw() }, { x: 800, y: 2 * vw() }) : 0;

const mainW = () => availW() - (!isLandscapeLayout() ? 0 : sideW() + gapW());

const spacer = () => (isLandscapeLayout() ? hl('span', widthStyle(gapW())) : null); // between side & main

const widthStyle = (width: number) => ({ attrs: { style: `width: ${width}px;` } });

const containerStyle = () => ({
  attrs: {
    // i would encrypt this if i could.  just look away
    style:
      ` width: ${availW()}px;` +
      ` ---header-height: ${interpolateBetween(mainW(), { x: 500, y: 30 }, { x: 800, y: 60 })}px;` +
      ` ---drop-menu-width: ${interpolateBetween(mainW(), { x: 320, y: 154 }, { x: 800, y: 200 })}px;` +
      ` ---chart-height: ${Math.max(300, Math.min(600, window.innerHeight - 100))}px;`,
  },
});

const isAtLeastXXSmall = (w = window.innerWidth) => w >= 500; // $mq-xx-small
const isAtLeastXSmall = (w = window.innerWidth) => w >= 650; // $mq-x-small
const isAtLeastSmall = (w = window.innerWidth) => w >= 800; // $mq-small
