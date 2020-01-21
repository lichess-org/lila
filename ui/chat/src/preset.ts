import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind } from './util'
import { Redraw } from './interfaces'

export interface PresetCtrl {
  group(): string | undefined
  said(): string[]
  setGroup(group: string | undefined): void
  post(preset: Preset): void
  trans: Trans
}

export type PresetKey = string
export type PresetText = string

export interface Preset {
  key: PresetKey
  text: PresetText
}

export interface PresetGroups {
  start: Preset[]
  end: Preset[]
  [key: string]: Preset[]
}

export interface PresetOpts {
  initialGroup?: string
  redraw: Redraw
  post(text: string): void
  trans: Trans
}

function presetGroups(trans: Trans): PresetGroups {
  return {
    start: [
      trans('hiHello'), trans('glGoodLuck'), trans('hfHaveFun'), trans('u2YouToo')
    ].map(splitIt),
    end: [
      trans('ggGoodGame'), trans('wpWellPlayed'), trans('tyThankYou'), trans('gtgIveGotToGo'), trans('byeBye')
    ].map(splitIt)}
}

export function presetCtrl(opts: PresetOpts): PresetCtrl {

  let group: string | undefined = opts.initialGroup;

  let trans = opts.trans;

  let groups = presetGroups(trans);

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
      opts.post(preset.text);
      said.push(preset.key);
    },
    trans
  }
}

export function presetView(ctrl: PresetCtrl): VNode | undefined {
  const group = ctrl.group();
  if (!group) return;
  const groups = presetGroups(ctrl.trans);
  const sets = groups[group];
  const said = ctrl.said();
  return (sets && said.length < 2) ? h('div.mchat__presets', sets.map((p: Preset) => {
    const disabled = said.includes(p.key);
    return h('span', {
      class: {
        disabled
      },
      attrs: {
        title: p.text,
        disabled
      },
      hook: bind('click', () => { !disabled && ctrl.post(p) })
    }, p.key);
  })) : undefined;
}

function splitIt(s: string): Preset {
  const parts = s.split('/');
  return {
    key: parts[0],
    text: parts[1]
  }
}
