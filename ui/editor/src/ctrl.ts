import { EditorConfig, EditorData, EditorOptions, Selection, Redraw, OpeningPosition } from './interfaces';
import * as editor from './editor';
import { read as fenRead } from 'chessground/fen';
import { Api as CgApi } from 'chessground/api';
import { prop, Prop } from 'common';

export default class EditorCtrl {
  cfg: EditorConfig;
  data: EditorData;
  options: EditorOptions;
  embed: boolean;
  trans: Trans;
  selected: Prop<Selection>;
  extraPositions?: OpeningPosition[];
  chessground: CgApi | undefined;
  positionIndex: { [boardFen: string]: number };
  redraw: Redraw;

  constructor(cfg: EditorConfig, redraw: Redraw) {
    this.cfg = cfg;
    this.data = editor.init(cfg);
    this.options = cfg.options || {};
    this.embed = cfg.embed;

    this.trans = window.lichess.trans(this.data.i18n);

    this.selected = prop('pointer');

    this.extraPositions = [{
      fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
      name: this.trans('startPosition')
    }, {
      fen: '8/8/8/8/8/8/8/8 w - -',
      name: this.trans('clearBoard')
    }, {
      fen: 'prompt',
      name: this.trans('loadPosition')
    }];

    this.positionIndex = {};
    if (cfg.positions) cfg.positions.forEach((p, i) => {
      this.positionIndex[p.fen.split(' ')[0]] = i;
    });

    window.Mousetrap.bind('f', (e) => {
      e.preventDefault();
      if (this.chessground) this.chessground.toggleOrientation();
      redraw();
    });

    this.redraw = redraw;
  }

  onChange(): void {
    this.options.onChange && this.options.onChange(this.computeFen());
    this.redraw();
  }

  computeFen(): string {
    return this.chessground ?
    editor.computeFen(this.data, this.chessground.getFen()) :
    this.cfg.fen;
  }

  bottomColor(): Color {
    return this.chessground ?
    this.chessground.state.orientation :
    this.options.orientation || 'white';
  }

  setColor(letter: 'w' | 'b'): void {
    this.data.color(letter);
    this.onChange();
  }

  setCastle(id, value): void {
    this.data.castles[id](value);
    this.onChange();
  }

  startPosition(): void {
    this.chessground.set({
      fen: 'start'
    });
    this.data.castles = editor.castlesAt(true);
    this.data.color('w');
    this.onChange();
  }

  clearBoard(): void {
    this.chessground.set({
      fen: '8/8/8/8/8/8/8/8'
    });
    this.data.castles = editor.castlesAt(false);
    this.onChange();
  }

  loadNewFen(fen: string | 'prompt'): void {
    if (fen === 'prompt') {
      fen = prompt('Paste FEN position').trim();
      if (!fen) return;
    }
    this.changeFen(fen);
  }

  changeFen(fen: string) {
    window.location.href = editor.makeUrl(this.data.baseUrl, fen);
  }

  changeVariant(variant: VariantKey): void {
    this.data.variant = variant;
    this.redraw();
  }

  positionLooksLegit(): boolean {
    const variant = this.data.variant;
    if (variant === 'antichess') return true;
    const pieces = this.chessground ? this.chessground.state.pieces : fenRead(this.cfg.fen);
    const kings = {
      white: 0,
      black: 0
    };
    for (const pos in pieces) {
      if (pieces[pos] && pieces[pos].role === 'king') kings[pieces[pos].color]++;
    }
    return kings.white === (variant !== "horde" ? 1 : 0) && kings.black === 1;
  }

  setOrientation = function(o: Color): void {
    this.options.orientation = o;
    if (this.chessground.state.orientation !== o)
    this.chessground.toggleOrientation();
    this.redraw();
  }
}
