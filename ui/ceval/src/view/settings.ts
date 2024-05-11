import { VNode } from 'snabbdom';
import { ParentCtrl } from '../types';
import CevalCtrl from '../ctrl';
import { fewerCores } from '../util';
import { rangeConfig } from 'common/controls';
import { isChrome } from 'common/device';
import { onInsert, bind, dataIcon, looseH as h } from 'common/snabbdom';
import * as Licon from 'common/licon';
import { onClickAway, clamp } from 'common';

const allSearchTicks: [number, string][] = [
  [4000, '4s'],
  [6000, '6s'],
  [8000, '8s'],
  [10000, '10s'],
  [12000, '12s'],
  [15000, '15s'],
  [20000, '20s'],
  [30000, '30s'],
  [Number.POSITIVE_INFINITY, '∞'],
];

const formatHashSize = (v: number): string => (v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB');

export function renderCevalSettings(ctrl: ParentCtrl): VNode | null {
  const ceval = ctrl.getCeval(),
    noarg = ctrl.trans.noarg,
    minThreads = ceval.engines.active?.minThreads ?? 1,
    maxThreads = ceval.maxThreads,
    engCtrl = ctrl.getCeval().engines,
    searchTicks = allSearchTicks.filter(x => x[0] <= ceval.engines.maxMovetime);

  let observer: ResizeObserver;

  function clickThreads(x = ceval.recommendedThreads) {
    ceval.setThreads(x);
    ctrl.restartCeval?.();
    ceval.opts.redraw();
  }

  function threadsTick(dir: 'up' | 'down') {
    return h(`div.arrow-${dir}`, { hook: bind('click', () => clickThreads()) });
  }

  function searchTick() {
    const millis = ceval.storedMovetime();
    return clamp(
      allSearchTicks.findIndex(([tickMs]) => tickMs >= millis),
      { min: 0, max: searchTicks.length - 1 },
    );
  }

  return ceval.showEnginePrefs()
    ? h(
        'div#ceval-settings-anchor',
        h(
          'div#ceval-settings',
          {
            hook: onInsert(
              onClickAway(() => {
                ceval.showEnginePrefs(false);
                ceval.opts.redraw();
              }),
            ),
          },
          [
            ...engineSelection(ctrl),
            !ceval.customSearch &&
              (id => {
                return h('div.setting', { attrs: { title: 'Set time to evaluate fresh positions' } }, [
                  h('label', 'Search time'),
                  h('input#' + id, {
                    attrs: { type: 'range', min: 0, max: searchTicks.length - 1, step: 1 },
                    hook: rangeConfig(searchTick, n => {
                      ceval.storedMovetime(searchTicks[n][0]);
                      ctrl.restartCeval?.();
                    }),
                  }),
                  h('div.range_value', searchTicks[searchTick()][1]),
                ]);
              })('engine-search-ms'),
            !ceval.customSearch &&
              (id => {
                const max = 5;
                return h(
                  'div.setting',
                  { attrs: { title: 'Set number of evaluation lines and move arrows on the board' } },
                  [
                    h('label', { attrs: { for: id } }, noarg('multipleLines')),
                    h('input#' + id, {
                      attrs: { type: 'range', min: 0, max, step: 1 },
                      hook: rangeConfig(
                        () => ceval.storedPv(),
                        (pvs: number) => {
                          ceval.storedPv(pvs);
                          ctrl.clearCeval?.();
                        },
                      ),
                    }),
                    h('div.range_value', `${ceval.storedPv()} / ${max}`),
                  ],
                );
              })('analyse-multipv'),
            maxThreads > minThreads &&
              (id => {
                return h(
                  'div.setting',
                  {
                    attrs: {
                      title:
                        fewerCores() && !ceval.engines.external
                          ? 'More threads will use more battery for better analysis'
                          : "Set this below your CPU's thread count\nThe ticks mark a good safe choice",
                    },
                  },
                  [
                    h('label', { attrs: { for: id } }, 'Threads'),
                    h('span', [
                      h('input#' + id, {
                        attrs: {
                          type: 'range',
                          min: minThreads,
                          max: maxThreads,
                          step: 1,
                        },
                        hook: rangeConfig(() => ceval.threads, clickThreads),
                      }),
                      h(
                        'div.tick',
                        {
                          hook: {
                            update: (_, v) => setupTick(v, ceval),
                            insert: v => {
                              setupTick(v, ceval);
                              let animationFrameRequestId: number;
                              observer = new ResizeObserver(() => {
                                cancelAnimationFrame(animationFrameRequestId);
                                animationFrameRequestId = requestAnimationFrame(() => setupTick(v, ceval));
                              });
                              observer.observe(v.elm!.parentElement!);
                            },
                            destroy: () => observer?.disconnect(),
                          },
                        },
                        !ceval.engines.external && [threadsTick('up'), threadsTick('down')],
                      ),
                    ]),
                    h('div.range_value', `${ceval.threads} / ${maxThreads}`),
                  ],
                );
              })('analyse-threads'),
            (id =>
              h('div.setting', { attrs: { title: 'Higher values may improve performance' } }, [
                h('label', { attrs: { for: id } }, noarg('memory')),
                h('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 4,
                    max: Math.floor(Math.log2(engCtrl.active?.maxHash ?? 4)),
                    step: 1,
                    disabled: ceval.maxHash <= 16,
                  },
                  hook: rangeConfig(
                    () => Math.floor(Math.log2(ceval.hashSize)),
                    v => {
                      ceval.setHashSize(Math.pow(2, v));
                      ctrl.restartCeval?.();
                    },
                  ),
                }),

                h('div.range_value', formatHashSize(ceval.hashSize)),
              ]))('analyse-memory'),
          ],
        ),
      )
    : null;
}

function setupTick(v: VNode, ceval: CevalCtrl) {
  const tick = v.elm as HTMLElement;
  const parentSpan = tick.parentElement!;
  const minThreads = ceval.engines.active?.minThreads ?? 1;
  const thumbWidth = isChrome() ? 17 : 19; // it is what it is
  const trackWidth = parentSpan.querySelector('input')!.offsetWidth - thumbWidth;
  const tickRatio = (ceval.recommendedThreads - minThreads) / (ceval.maxThreads - minThreads);
  const tickLeft = Math.floor(thumbWidth / 2 + trackWidth * tickRatio);

  tick.style.left = `${tickLeft}px`;
  $(tick).toggleClass('recommended', ceval.threads === ceval.recommendedThreads);
}

function engineSelection(ctrl: ParentCtrl) {
  const ceval = ctrl.getCeval(),
    active = ceval.engines.active,
    engines = ceval.engines.supporting(ceval.opts.variant.key),
    external = ceval.engines.external;
  if (!engines?.length || !ceval.possible || !ceval.allowed()) return [];
  return [
    h('div.setting', [
      'Engine:',
      h(
        'select.select-engine',
        {
          hook: bind('change', e => {
            ceval.selectEngine((e.target as HTMLSelectElement).value);
            ctrl.redraw?.();
          }),
        },
        [
          ...engines.map(engine =>
            h('option', { attrs: { value: engine.id, selected: active?.id == engine.id } }, engine.name),
          ),
        ],
      ),
      external &&
        h('button.delete', {
          attrs: { ...dataIcon(Licon.X), title: 'Delete external engine' },
          hook: bind('click', e => {
            (e.currentTarget as HTMLElement).blur();
            if (confirm('Remove external engine?'))
              ceval.engines.deleteExternal(external.id).then(ok => ok && ctrl.redraw?.());
          }),
        }),
    ]),
    h('br'),
  ];
}
