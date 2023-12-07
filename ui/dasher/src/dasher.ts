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

export interface DasherOpts {
  playing: boolean;
  zenable: boolean;
}

export default class DasherCtrl {
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
  opts = {
    playing: $('body').hasClass('playing'),
    zenable: $('body').hasClass('zenable'),
  };

  constructor(
    readonly data: DasherData,
    readonly redraw: Redraw,
  ) {
    this.trans = lichess.trans(data.i18n);
    lichess.pubsub.on('top.toggle.user_tag', () => this.setMode(defaultMode));
    this.ping = new PingCtrl(this.trans, this.redraw);
    this.subs = {
      langs: new LangsCtrl(this.data.lang, this.trans, this.close),
      sound: new SoundCtrl(this.data.sound.list, this.trans, this.redraw, this.close),
      background: new BackgroundCtrl(this.data.background, this.trans, this.redraw, this.close),
      board: new BoardCtrl(this.data.board, this.trans, this.redraw, this.close),
      theme: new ThemeCtrl(
        this.data.theme,
        this.trans,
        () => (this.data.board.is3d ? 'd3' : 'd2'),
        this.redraw,
        this.close,
      ),
      piece: new PieceCtrl(
        this.data.piece,
        this.trans,
        () => (this.data.board.is3d ? 'd3' : 'd2'),
        this.redraw,
        this.close,
      ),
    };
  }

  mode: Prop<Mode> = prop(defaultMode as Mode);

  setMode = (m: Mode) => {
    this.mode(m);
    this.redraw();
  };
  close = () => this.setMode(defaultMode);
}
