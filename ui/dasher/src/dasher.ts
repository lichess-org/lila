import { PingCtrl } from './ping';
import { LangsCtrl, LangsData } from './langs';
import { SoundCtrl } from './sound';
import { BackgroundCtrl, BackgroundData } from './background';
import { BoardCtrl, BoardData } from './board';
import { ThemeCtrl, ThemeData } from './theme';
import { PieceCtrl, PieceData } from './piece';
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
  zenable: boolean;
}

export function makeCtrl(data: DasherData, redraw: Redraw): DasherCtrl {
  const trans = lichess.trans(data.i18n);
  const opts = {
    playing: $('body').hasClass('playing'),
    zenable: $('body').hasClass('zenable'),
  };

  const mode: Prop<Mode> = prop(defaultMode as Mode);

  const setMode = (m: Mode) => {
    mode(m);
    redraw();
  };
  const close = () => setMode(defaultMode);

  const ping = new PingCtrl(trans, redraw);

  const subs = {
    langs: new LangsCtrl(data.lang, trans, close),
    sound: new SoundCtrl(data.sound.list, trans, redraw, close),
    background: new BackgroundCtrl(data.background, trans, redraw, close),
    board: new BoardCtrl(data.board, trans, redraw, close),
    theme: new ThemeCtrl(data.theme, trans, () => (data.board.is3d ? 'd3' : 'd2'), redraw, close),
    piece: new PieceCtrl(data.piece, trans, () => (data.board.is3d ? 'd3' : 'd2'), redraw, close),
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
