import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { PresetCtrl, PresetOpts, Preset, PresetGroups } from './interfaces'
import { bind } from './util'

function splitIt(s: string): Preset {
  const parts = s.split('/');
  return {
    key: parts[0],
    text: parts[1]
  }
}

const groups: PresetGroups = {
  start: [
    'hi/Hello', 'gl/Good luck', 'hf/Have fun!', 'u2/You too!'
  ].map(splitIt),
  end: [
    'gg/Good game', 'wp/Well played', 'ty/Thank you', 'gtg/I\'ve got to go', 'bye/Bye!'
  ].map(splitIt)
}

export function presetCtrl(opts: PresetOpts): PresetCtrl {

  let group = opts.initialGroup;

  let said: string[] = [];

  return {
    group: () => group,
    said: () => said,
    setGroup(p) {
      if (p !== group) {
        group = p;
        if (!p) said = [];
        opts.redraw();
      }
    },
    post(preset) {
      if (!group) return;
      const sets = groups[group];
      if (!sets) return;
      if (said.indexOf(preset.key) !== -1) return;
      opts.post(preset.text);
      said.push(preset.key);
    }
  }
}

export function presetView(ctrl: PresetCtrl): VNode | undefined {
  const group = ctrl.group();
  if (!group) return;
  const sets = groups[group];
  const said = ctrl.said();
  return (sets && said.length < 2) ? h('div.presets', sets.map((p: Preset) => {
    const disabled = said.indexOf(p.key) !== -1;
    return h('span.hint--top', {
      class: {
        disabled
      },
      attrs: {
        'data-hint': p.text,
        disabled
      },
      hook: bind('click', () => { !disabled && ctrl.post(p) })
    }, p.key);
  })) : undefined;
}
