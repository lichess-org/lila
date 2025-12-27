import type { CevalHandler } from '../types';
import type { CevalCtrl } from '../ctrl';
import { fewerCores } from '../util';
import { isChrome } from '@/device';
import { type VNode, onInsert, bind, dataIcon, hl, rangeConfig, confirm } from '@/view';
import * as Licon from '@/licon';
import { onClickAway } from '@/index';
import { clamp } from '@/algo';

const allSearchTicks: number[] = [2, 4, 6, 8, 10, 12, 15, 20, 30, Number.POSITIVE_INFINITY];

export function renderCevalSettings(ctrl: CevalHandler): VNode | null {
  const ceval = ctrl.ceval,
    minThreads = ceval.engines.current()?.minThreads ?? 1,
    maxThreads = ceval.maxThreads,
    engCtrl = ctrl.ceval.engines,
    searchTicks = allSearchTicks.filter(x => x * 1000 <= ceval.engines.maxMovetime());

  let observer: ResizeObserver;

  function clickThreads(x = ceval.recommendedThreads) {
    ceval.setThreads(x);
    ctrl.startCeval();
    ceval.opts.redraw();
  }

  function threadsTick(dir: 'up' | 'down') {
    return hl(`div.arrow-${dir}`, { hook: bind('click', () => clickThreads()) });
  }

  function searchTick() {
    const millis = ceval.storedMovetime();
    return clamp(
      allSearchTicks.findIndex(tickSecs => tickSecs * 1000 >= millis),
      { min: 0, max: searchTicks.length - 1 },
    );
  }

  return !ceval.showEnginePrefs()
    ? null
    : hl(
        'div#ceval-settings-anchor',
        hl(
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
            engineSelection(ctrl),
            (id => {
              return hl('div.setting', { attrs: { title: 'Set time to evaluate fresh positions' } }, [
                hl('label', { attrs: { for: id } }, 'Search time'),
                hl('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 0,
                    max: searchTicks.length - 1,
                    step: 1,
                    'aria-valuetext': i18n.site.nbSeconds(searchTicks[searchTick()]),
                  },
                  hook: rangeConfig(searchTick, n => {
                    ceval.storedMovetime(searchTicks[n] * 1000);
                    ctrl.startCeval();
                    ceval.opts.redraw();
                  }),
                }),
                hl(
                  'div.range_value',
                  isFinite(searchTicks[searchTick()]) ? `${searchTicks[searchTick()]}s` : '∞',
                ),
              ]);
            })('engine-search-ms'),
            (id => {
              const max = 5;
              return hl(
                'div.setting',
                { attrs: { title: 'Set number of evaluation lines and move arrows on the board' } },
                [
                  hl('label', { attrs: { for: id } }, i18n.site.multipleLines),
                  hl('input#' + id, {
                    attrs: { type: 'range', min: 0, max, step: 1 },
                    hook: rangeConfig(
                      () => ceval.storedPv(),
                      (pvs: number) => {
                        ceval.storedPv(pvs);
                        ctrl.clearCeval?.();
                        ceval.opts.redraw();
                      },
                    ),
                  }),
                  hl('div.range_value', `${ceval.storedPv()} / ${max}`),
                ],
              );
            })('analyse-multipv'),
            maxThreads > minThreads &&
              (id => {
                return hl(
                  'div.setting',
                  {
                    attrs: {
                      title:
                        fewerCores() && !ceval.engines.external()
                          ? 'More threads will use more battery for better analysis'
                          : "Set this below your CPU's thread count\nThe ticks mark a good safe choice",
                    },
                  },
                  [
                    hl('label', { attrs: { for: id } }, 'Threads'),
                    hl('span', [
                      hl('input#' + id, {
                        attrs: {
                          type: 'range',
                          min: minThreads,
                          max: maxThreads,
                          step: 1,
                        },
                        hook: rangeConfig(() => ceval.threads, clickThreads),
                      }),
                      hl(
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
                        !ceval.engines.external() && [threadsTick('up'), threadsTick('down')],
                      ),
                    ]),
                    hl('div.range_value', `${ceval.threads} / ${maxThreads}`),
                  ],
                );
              })('analyse-threads'),
            (id =>
              hl('div.setting', { attrs: { title: 'Higher values may improve performance' } }, [
                hl('label', { attrs: { for: id } }, i18n.site.memory),
                hl('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 4,
                    max: Math.floor(Math.log2(engCtrl.current()?.maxHash ?? 4)),
                    step: 1,
                    disabled: ceval.maxHash <= 16,
                    'aria-valuetext': `${ceval.hashSize} megabytes`,
                  },
                  hook: rangeConfig(
                    () => Math.floor(Math.log2(ceval.hashSize)),
                    v => {
                      ceval.setHashSize(Math.pow(2, v));
                      ctrl.startCeval();
                      ceval.opts.redraw();
                    },
                  ),
                }),

                hl('div.range_value', formatHashSize(ceval.hashSize)),
              ]))('analyse-memory'),
          ],
        ),
      );
}

function formatHashSize(v: number) {
  return v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB';
}

function setupTick(v: VNode, ceval: CevalCtrl) {
  const tick = v.elm as HTMLElement;
  const parentSpan = tick.parentElement!;
  const minThreads = ceval.engines.current()?.minThreads ?? 1;
  const thumbWidth = isChrome() ? 17 : 19; // it is what it is
  const trackWidth = parentSpan.querySelector('input')!.offsetWidth - thumbWidth;
  const tickRatio = (ceval.recommendedThreads - minThreads) / (ceval.maxThreads - minThreads);
  const tickLeft = Math.floor(thumbWidth / 2 + trackWidth * tickRatio);

  tick.style.left = `${tickLeft}px`;
  $(tick).toggleClass('recommended', ceval.threads === ceval.recommendedThreads);
}

function engineSelection(ctrl: CevalHandler) {
  const ceval = ctrl.ceval,
    active = ceval.engines.current(),
    engines = ceval.engines.supporting(ceval.opts.variant.key),
    external = ceval.engines.external();
  return [
    hl('div.setting', [
      hl('label', { attrs: { for: 'select-engine' } }, 'Engine:'),
      hl(
        'select#select-engine',
        {
          hook: bind('change', e => {
            ceval.selectEngine((e.target as HTMLSelectElement).value);
            ceval.opts.redraw();
          }),
        },
        engines.map(engine =>
          hl('option', { attrs: { value: engine.id, selected: active?.id === engine.id } }, engine.name),
        ),
      ),
      external &&
        hl('button.delete', {
          attrs: { ...dataIcon(Licon.X), title: 'Delete external engine' },
          hook: bind('click', async e => {
            (e.currentTarget as HTMLElement).blur();
            if (await confirm('Remove external engine?'))
              ceval.engines.deleteExternal(external.id).then(ok => ok && ceval.opts.redraw());
          }),
        }),
    ]),
    hl('br'),
  ];
}
