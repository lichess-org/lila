import { PingCtrl, ctrl as pingCtrl } from './ping';
import { LangsCtrl, LangsData, ctrl as langsCtrl } from './langs';
import { SoundCtrl, ctrl as soundCtrl } from './sound';
import { BackgroundCtrl, BackgroundData, ctrl as backgroundCtrl } from './background';
import { ThemeCtrl, ThemeData, ctrl as themeCtrl } from './theme';
import { CustomThemeCtrl, ctrl as customThemeCtrl, CustomThemeData } from './customTheme';
import { PieceCtrl, PieceData, ctrl as pieceCtrl } from './piece';
import { Redraw } from './util';
import { NotationCtrl, ctrl as notationCtrl, NotationData } from './notation';
import { prop, Prop } from 'common/common';

export interface DasherData {
  user?: LightUser;
  lang: LangsData;
  notation: NotationData;
  sound: {
    list: string[];
  };
  background: BackgroundData;
  theme: ThemeData;
  customTheme: CustomThemeData;
  piece: PieceData;
  inbox: boolean;
  coach: boolean;
  streamer: boolean;
  i18n: any;
}

export type Mode =
  | 'links'
  | 'langs'
  | 'sound'
  | 'background'
  | 'board'
  | 'notation'
  | 'theme'
  | 'customTheme'
  | 'piece';

const defaultMode = 'links';

export interface DasherCtrl {
  mode: Prop<Mode>;
  setMode(m: Mode): void;
  data: DasherData;
  trans: Trans;
  ping: PingCtrl;
  subs: {
    langs: LangsCtrl;
    sound: SoundCtrl;
    background: BackgroundCtrl;
    notation: NotationCtrl;
    theme: ThemeCtrl;
    customTheme: CustomThemeCtrl;
    piece: PieceCtrl;
  };
  opts: DasherOpts;
}

export interface DasherOpts {
  playing: boolean;
}

export function makeCtrl(opts: DasherOpts, data: DasherData, redraw: Redraw): DasherCtrl {
  const trans = window.lishogi.trans(data.i18n);

  let mode: Prop<Mode> = prop(defaultMode as Mode);

  function setMode(m: Mode) {
    mode(m);
    redraw();
  }
  function close() {
    setMode(defaultMode);
  }

  const ping = pingCtrl(trans, redraw);

  const subs = {
    langs: langsCtrl(data.lang, trans, close),
    sound: soundCtrl(data.sound.list, trans, redraw, close),
    background: backgroundCtrl(data.background, trans, redraw, close),
    theme: themeCtrl(data.theme, trans, redraw, setMode),
    customTheme: customThemeCtrl(data.customTheme, trans, redraw, setMode),
    notation: notationCtrl(data.notation, trans, redraw, close),
    piece: pieceCtrl(data.piece, trans, redraw, close),
  };

  window.lishogi.pubsub.on('top.toggle.user_tag', () => setMode(defaultMode));

  return {
    mode,
    setMode,
    data,
    trans,
    ping,
    subs,
    opts,
  };
}
