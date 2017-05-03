import { PingCtrl, ctrl as pingCtrl } from './ping'
import { LangsCtrl, ctrl as langsCtrl } from './langs'
import { Redraw, Prop, prop } from './util'
import { get } from './xhr'

export interface DasherData {
  user: LightUser
  lang: {
    current: string
    accepted: string[]
  }
  kid: boolean
  coach: boolean
  prefs: any
  i18n: any
}

export type Mode = 'links' | 'langs'

export interface DasherCtrl {
  mode: Prop<Mode>
  setMode: (m: Mode) => void
  data: DasherData
  trans: Trans
  ping: PingCtrl
  langs: LangsCtrl
  opts: DasherOpts
}

export interface DasherOpts {
  playing: boolean
}

export function makeCtrl(opts: DasherOpts, data: DasherData, redraw: Redraw): DasherCtrl {

  const trans = window.lichess.trans(data.i18n);

  let mode: Prop<Mode> = prop('links' as Mode);

  function setMode(m: Mode) {
    mode(m);
    redraw();
  }
  function close() { setMode('links'); }

  const ping = pingCtrl(trans, redraw);
  const langs = langsCtrl(data.lang, redraw, close);

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
