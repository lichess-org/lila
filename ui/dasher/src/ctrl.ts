import { PingCtrl } from './ping';
import { LangsCtrl } from './langs';
import { SoundCtrl } from './sound';
import { BackgroundCtrl } from './background';
import { BoardCtrl } from './board';
import { PieceCtrl } from './piece';
import { LinksCtrl } from './links';
import { MaybeVNode, Redraw } from 'common/snabbdom';
import { DasherData, Mode, PaneCtrl } from './interfaces';
import { Prop, prop } from 'common';
import { trans } from 'common/i18n';
import { pubsub } from 'common/pubsub';

const defaultMode = 'links';

type ModeIndexed = { [key in Mode]: PaneCtrl };

export class DasherCtrl implements ModeIndexed {
  trans: Trans;
  ping: PingCtrl;
  langs: LangsCtrl;
  sound: SoundCtrl;
  background: BackgroundCtrl;
  board: BoardCtrl;
  piece: PieceCtrl;
  links: LinksCtrl;
  opts: { playing: boolean; zenable: boolean } = {
    playing: $('body').hasClass('playing'),
    zenable: $('body').hasClass('zenable'),
  };

  constructor(
    readonly data: DasherData,
    readonly redraw: Redraw,
  ) {
    this.trans = trans(data.i18n);
    this.ping = new PingCtrl(this);
    this.langs = new LangsCtrl(this);
    this.sound = new SoundCtrl(this);
    this.background = new BackgroundCtrl(this);
    this.board = new BoardCtrl(this);
    this.piece = new PieceCtrl(this);
    this.links = new LinksCtrl(this);
    pubsub.on('top.toggle.user_tag', () => this.setMode(defaultMode));
  }

  mode: Prop<Mode> = prop(defaultMode as Mode);
  render = (): MaybeVNode => this[this.mode()]?.render() || null;
  setMode = (m: Mode): void => {
    this.mode(m);
    this.redraw();
  };
  close = (): void => this.setMode(defaultMode);
}
