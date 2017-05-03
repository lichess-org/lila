import { PingCtrl, ctrl as pingCtrl } from './ping'
import { LangsCtrl, ctrl as langsCtrl } from './langs'
import { Redraw, Prop, prop } from './util'
import { get } from './xhr'

export interface DasherCtrl {
  mode: Prop<Mode>
  setMode: (m: Mode) => void
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

export function makeCtrl(opts: DasherOpts, redraw: Redraw): DasherCtrl {

  let mode: Prop<Mode> = prop('links' as Mode);
  let data: Prop<DasherData | undefined> = prop(undefined);
  let trans: Prop<Trans> = prop(window.lichess.trans({}));

  function setMode(m: Mode) {
    mode(m);
    redraw();
  }
  function close() { setMode('links'); }

  const ping = pingCtrl(trans, redraw);
  const langs = langsCtrl('', [], redraw, close);

  function update(d: DasherData) {
    if (data()) trans(window.lichess.trans(d.i18n));
    data(d);
    redraw();
  }

  get('/dasher').then(update);

  return {
    mode,
    setMode,
    data,
    trans,
    ping,
    langs,
    opts,
  };
};
