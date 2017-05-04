import { PingCtrl, ctrl as pingCtrl } from './ping'
import { LangsCtrl, ctrl as langsCtrl } from './langs'
import { SoundCtrl, ctrl as soundCtrl } from './sound'
import { BackgroundCtrl, BackgroundData, ctrl as backgroundCtrl } from './background'
import { BoardCtrl, BoardData, ctrl as boardCtrl } from './board'
import { Redraw, Prop, prop } from './util'
import { get } from './xhr'

export interface DasherData {
  user: LightUser
  lang: {
    current: string
    accepted: string[]
  }
  sound: {
    list: string[]
  }
  background: BackgroundData
  board: BoardData
  kid: boolean
  coach: boolean
  prefs: any
  i18n: any
}

export type Mode = 'links' | 'langs' | 'sound' | 'background' | 'board'

  export interface DasherCtrl {
    mode: Prop<Mode>
    setMode: (m: Mode) => void
    data: DasherData
    trans: Trans
    ping: PingCtrl
    subs: {
      langs: LangsCtrl
      sound: SoundCtrl
      background: BackgroundCtrl,
      board: BoardCtrl
    }
    opts: DasherOpts
  }

  export interface DasherOpts {
    playing: boolean
  }

  export function makeCtrl(opts: DasherOpts, data: DasherData, redraw: Redraw): DasherCtrl {

    const trans = window.lichess.trans(data.i18n);

    let mode: Prop<Mode> = prop('board' as Mode);

    function setMode(m: Mode) {
      mode(m);
      redraw();
    }
    function close() { setMode('links'); }

    const ping = pingCtrl(trans, redraw);

    const subs = {
      langs: langsCtrl(data.lang, redraw, close),
      sound: soundCtrl(data.sound.list, trans, redraw, close),
      background: backgroundCtrl(data.background, redraw, close),
      board: boardCtrl(data.board, redraw, close)
    };

    return {
      mode,
      setMode,
      data,
      trans,
      ping,
      subs,
      opts
    };
  };
