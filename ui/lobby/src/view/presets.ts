import { clockShow, clockToPerf } from 'common/clock';
import { bind } from 'common/snabbdom';
import { i18n, i18nFormat, i18nPluralSame } from 'i18n';
import { i18nPerf } from 'i18n/perf';
import { engineName } from 'shogi/engine-name';
import { type Hooks, type VNode, type VNodes, h } from 'snabbdom';
import type LobbyController from '../ctrl';
import type { Hook, Preset, PresetOpts, Seek } from '../interfaces';

export function render(ctrl: LobbyController): VNodes {
  return ctrl.allPresets
    .map(p => presetButton(p, ctrl))
    .concat(
      h(
        'div.custom',
        {
          attrs: { 'data-id': 'custom' },
        },
        i18n('custom'),
      ),
    );
}

export function presetHooks(ctrl: LobbyController): Hooks {
  return bind(
    'click',
    e => {
      const id =
        (e.target as HTMLElement).getAttribute('data-id') ||
        ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('data-id');
      if (id === 'custom') $('.config_hook').trigger('mousedown');
      else {
        const preset = ctrl.allPresets.find(p => p.id === id);
        if (preset) ctrl.clickPreset(preset);
      }
    },
    ctrl.redraw,
  );
}

function presetPerf(p: Preset): Perf {
  return p.timeMode == 2 ? 'correspondence' : clockToPerf(p.lim * 60, p.byo, p.inc, p.per);
}

function presetButton(p: Preset, ctrl: LobbyController): VNode {
  const clock =
      p.timeMode == 2
        ? i18nPluralSame('nbDays', p.days)
        : clockShow(p.lim * 60, p.byo, p.inc, p.per),
    perf = presetPerf(p),
    perfName = p.ai ? 'AI - ' + i18nFormat('levelX', p.ai).toLowerCase() : i18nPerf(perf),
    isReady =
      !!p.ai ||
      (p.timeMode == 2
        ? ctrl.data.seeks.some(s => isSameSeek(p, s, ctrl))
        : ctrl.data.hooks.some(h => isSameHook(h, clock, perf, ctrl))),
    attrs = {
      'data-id': p.id,
    };
  if (p.ai) attrs['title'] = engineName('standard', undefined, p.ai);

  return h(
    'div' + (p.ai === 1 && ctrl.presetOpts.isNewPlayer ? '.highlight' : ''),
    {
      attrs,
      class: {
        highlight: p.ai === 1 && ctrl.presetOpts.isNewPlayer,
        disabled: ctrl.currentPresetId === p.id || (p.timeMode === 2 && !ctrl.data.me),
      },
    },
    [
      h('div.clock', clock),
      h('div.perf', perfName),
      isReady
        ? h('i.check-mark', {
            attrs: { 'data-icon': 'E', title: 'Ready to play' },
          })
        : null,
    ],
  );
}

function isSameSeek(p: Preset, s: Seek, ctrl: LobbyController): boolean {
  return (
    s.days === p.days &&
    !s.variant &&
    s.mode === 1 &&
    ctrl.data.me &&
    s.username !== ctrl.data.me.username &&
    ratingInRange('correspondence', s.rr, s.rating, ctrl.presetOpts)
  );
}

function isSameHook(h: Hook, clk: string, perf: Perf, ctrl: LobbyController): boolean {
  return (
    h.clock === clk &&
    (h.ra === 1 || ctrl.isAnon) &&
    !h.variant &&
    h.sri !== window.lishogi.sri &&
    ratingInRange(perf, h.rr, h.rating, ctrl.presetOpts)
  );
}

function ratingInRange(
  perf: Perf,
  range: string | undefined,
  theirRating: number | undefined,
  opts: PresetOpts,
): boolean {
  const myPerf = opts.ratings?.[perf],
    myRating = myPerf && !myPerf.clueless ? myPerf.rating : undefined;
  if (myRating && range) {
    const parsed = range.split('-').map(s => Number.parseInt(s));
    if (parsed.length !== 2) return true;
    return (
      myRating >= parsed[0] &&
      myRating <= parsed[1] &&
      (!theirRating ||
        (theirRating >= myRating - opts.ratingDiff && theirRating <= myRating + opts.ratingDiff))
    );
  } else return true;
}
