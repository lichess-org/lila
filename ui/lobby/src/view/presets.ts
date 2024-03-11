import { bind } from 'common/snabbdom';
import { clockShow, clockToPerf } from 'common/clock';
import { Hooks, VNode, h } from 'snabbdom';
import LobbyController from '../ctrl';
import { Hook, Preset, PresetOpts, Seek } from '../interfaces';
import { engineName } from 'common/engineName';

export function render(ctrl: LobbyController) {
  return ctrl.allPresets
    .map(p => presetButton(p, ctrl))
    .concat(
      h(
        'div.custom',
        {
          attrs: { 'data-id': 'custom' },
        },
        ctrl.trans.noarg('custom')
      )
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
    ctrl.redraw
  );
}

function presetButton(p: Preset, ctrl: LobbyController): VNode {
  const clock = p.timeMode == 2 ? ctrl.trans.plural('nbDays', p.days) : clockShow(p.lim * 60, p.byo, p.inc || 0, p.per),
    perf = p.ai
      ? 'AI - ' + ctrl.trans('levelX', p.ai).toLowerCase()
      : p.timeMode == 2
        ? ctrl.trans.noarg('correspondence')
        : ctrl.trans.noargOrCapitalize(clockToPerf(p.lim * 60, p.byo, p.inc || 0, p.per)),
    isReady =
      !!p.ai ||
      (p.timeMode == 2
        ? ctrl.data.seeks.some(s => isSameSeek(p, s, ctrl))
        : ctrl.data.hooks.some(h => isSameHook(h, clock, ctrl))),
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
      h('div.perf', perf),
      isReady
        ? h('i.check-mark', {
            attrs: { 'data-icon': 'E', title: 'Ready to play' },
          })
        : null,
    ]
  );
}

function isSameSeek(p: Preset, s: Seek, ctrl: LobbyController): boolean {
  return (
    s.days === p.days &&
    !s.variant &&
    s.mode === 1 &&
    ctrl.data.me &&
    s.username !== ctrl.data.me.username &&
    ratingInRange(s.rr, s.rating, ctrl.presetOpts)
  );
}

function isSameHook(h: Hook, clk: string, ctrl: LobbyController): boolean {
  return (
    h.clock === clk &&
    (h.ra === 1 || ctrl.isAnon) &&
    !h.variant &&
    h.sri !== window.lishogi.sri &&
    ratingInRange(h.rr, h.rating, ctrl.presetOpts)
  );
}

function ratingInRange(range: string | undefined, theirRating: number | undefined, opts: PresetOpts): boolean {
  const myRating = opts.rating;
  if (myRating && range) {
    const parsed = range.split('-').map(s => parseInt(s));
    if (parsed.length !== 2) return true;
    return (
      myRating >= parsed[0] &&
      myRating <= parsed[1] &&
      (!theirRating || (theirRating >= myRating - opts.ratingDiff && theirRating <= myRating + opts.ratingDiff))
    );
  } else return true;
}
