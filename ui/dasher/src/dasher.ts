import { Prop, prop } from 'common/common';
import { BackgroundCtrl, BackgroundData, ctrl as backgroundCtrl } from './background';
import { CustomThemeCtrl, CustomThemeData, ctrl as customThemeCtrl } from './customTheme';
import { LangsCtrl, LangsData, ctrl as langsCtrl } from './langs';
import { NotationCtrl, NotationData, ctrl as notationCtrl } from './notation';
import { PieceCtrl, PieceSetData, ctrl as pieceCtrl } from './piece';
import { PingCtrl, ctrl as pingCtrl } from './ping';
import { SoundCtrl, SoundData, ctrl as soundCtrl } from './sound';
import { ThemeCtrl, ThemeData, ctrl as themeCtrl } from './theme';
import { Redraw } from './util';

export interface DasherData {
  user?: LightUser;
  lang: LangsData;
  notation: NotationData;
  sound: SoundData;
  background: BackgroundData;
  theme: ThemeData;
  customTheme: CustomThemeData;
  piece: PieceSetData;
  chuPiece: PieceSetData;
  kyoPiece: PieceSetData;
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
    sound: soundCtrl(data.sound, trans, redraw, close),
    background: backgroundCtrl(data.background, trans, redraw, close),
    theme: themeCtrl(data.theme, trans, redraw, setMode),
    customTheme: customThemeCtrl(data.customTheme, trans, redraw, setMode),
    notation: notationCtrl(data.notation, trans, redraw, close),
    piece: pieceCtrl(data.piece, data.chuPiece, data.kyoPiece, trans, redraw, close),
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
