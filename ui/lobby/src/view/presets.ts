import { bind } from 'common/snabbdom';
import { clockShow, clockToPerf } from 'common/clock';
import { Hooks, VNode, h } from 'snabbdom';
import LobbyController from '../ctrl';
import { Preset } from '../interfaces';

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
  const clock = p.timeMode == 2 ? ctrl.trans.plural('nbDays', p.days) : clockShow(p.lim * 60, p.byo, p.inc || 0, p.per);
  const perf = p.ai
    ? ctrl.trans('aiNameLevelAiLevel', 'AI', p.ai)
    : p.timeMode == 2
      ? ctrl.trans.noarg('correspondence')
      : ctrl.trans.noargOrCapitalize(clockToPerf(p.lim * 60, p.byo, p.inc || 0, p.per));
  return h(
    'div',
    {
      attrs: { 'data-id': p.id },
    },
    [h('div.clock', clock), perf ? h('div.perf', perf) : null]
  );
}
