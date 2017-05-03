import { PingCtrl, ctrl as pingCtrl } from './ping'
import { LangsCtrl, ctrl as langsCtrl } from './langs'
import { Redraw, Prop, prop } from './util'
import { load } from './xhr'

export interface Ctrl {
  mode: Prop<Mode>
  data: Prop<DasherData>
  trans: Prop<Trans>
  ping: PingCtrl
  langs: LangsCtrl
  opts: DasherOpts
}

export type DasherData = any

export type Mode = 'links' | 'langs'

export interface DasherOpts {
  playing: boolean
}

export function makeCtrl(opts: DasherOpts, redraw: Redraw): Ctrl {

  let mode: Prop<Mode> = prop('links' as Mode);
  let data: Prop<DasherData | undefined> = prop(undefined);
  let trans: Prop<Trans> = prop(window.lichess.trans({}));

  const ping = pingCtrl(trans, redraw);
  const langs = langsCtrl(redraw);

  function update(d: DasherData) {
    data = d;
    if (d.i18n) trans = window.lichess.trans(d.i18n);
    redraw();
  }

  load().then(update);

  return {
    mode,
    data,
    trans,
    ping,
    langs,
    opts,
  };
};
