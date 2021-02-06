import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { bind } from './util';
import { Redraw } from './interfaces';

export interface PresetCtrl {
  group(): string | undefined;
  said(): string[];
  setGroup(group: string | undefined): void;
  post(preset: Preset): void;
}

export type PresetKey = string;
export type PresetText = string;

export interface Preset {
  key: PresetKey;
  text: PresetText;
}

export interface PresetGroups {
  start: Preset[];
  end: Preset[];
  [key: string]: Preset[];
}

export interface PresetOpts {
  initialGroup?: string;
  redraw: Redraw;
  post(text: string): boolean;
}

const groups: PresetGroups = {
  start: ['hi/Hello', 'gl/Good luck', 'hf/Have fun!', 'u2/You too!'].map(splitIt),
  end: ['gg/Good game', 'wp/Well played', 'ty/Thank you', "gtg/I've got to go", 'bye/Bye!'].map(splitIt),
};

export function presetCtrl(opts: PresetOpts): PresetCtrl {
  let group: string | undefined = opts.initialGroup;

  let said: string[] = [];

  return {
    group: () => group,
    said: () => said,
    setGroup(p: string | undefined) {
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
      if (said.includes(preset.key)) return;
      if (opts.post(preset.text)) said.push(preset.key);
    },
  };
}

export function presetView(ctrl: PresetCtrl): VNode | undefined {
  const group = ctrl.group();
  if (!group) return;
  const sets = groups[group];
  const said = ctrl.said();
  return sets && said.length < 2
    ? h(
        'div.mchat__presets',
        sets.map((p: Preset) => {
          const disabled = said.includes(p.key);
          return h(
            'span',
            {
              class: {
                disabled,
              },
              attrs: {
                title: p.text,
                disabled,
              },
              hook: bind('click', () => {
                !disabled && ctrl.post(p);
              }),
            },
            p.key
          );
        })
      )
    : undefined;
}

function splitIt(s: string): Preset {
  const parts = s.split('/');
  return {
    key: parts[0],
    text: parts[1],
  };
}
