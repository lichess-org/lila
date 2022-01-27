import { PingCtrl, ctrl as pingCtrl } from './ping';
import { LangsCtrl, LangsData, ctrl as langsCtrl } from './langs';
import { SoundCtrl, ctrl as soundCtrl } from './sound';
import { BackgroundCtrl, BackgroundData, ctrl as backgroundCtrl } from './background';
import { BoardCtrl, BoardData, ctrl as boardCtrl } from './board';
import { ThemeCtrl, ThemeData, ctrl as themeCtrl } from './theme';
import { PieceCtrl, PieceData, ctrl as pieceCtrl } from './piece';
import { Redraw, Prop, prop } from './util';

export interface DasherData {
  user?: LightUser;
  lang: LangsData;
  sound: {
    list: string[];
  };
  background: BackgroundData;
  board: BoardData;
  theme: ThemeData;
  piece: PieceData;
  coach: boolean;
  streamer: boolean;
  i18n: I18nDict;
}

export type Mode = 'links' | 'langs' | 'sound' | 'background' | 'board' | 'theme' | 'piece';

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
    board: BoardCtrl;
    theme: ThemeCtrl;
    piece: PieceCtrl;
  };
  opts: DasherOpts;
}

export interface DasherOpts {
  playing: boolean;
}

export function makeCtrl(opts: DasherOpts, data: DasherData, redraw: Redraw): DasherCtrl {
  const trans = lichess.trans(data.i18n);

  const mode: Prop<Mode> = prop(defaultMode as Mode);

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
    board: boardCtrl(data.board, trans, redraw, close),
    theme: themeCtrl(data.theme, trans, () => (data.board.is3d ? 'd3' : 'd2'), redraw, setMode),
    piece: pieceCtrl(data.piece, trans, () => (data.board.is3d ? 'd3' : 'd2'), redraw, setMode),
  };

  lichess.pubsub.on('top.toggle.user_tag', () => setMode(defaultMode));

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
