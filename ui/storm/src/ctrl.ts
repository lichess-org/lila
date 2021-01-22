import { Api as CgApi } from 'chessground/api';
import { prop, Prop } from 'common';
import { StormOpts, StormData, StormPuzzle } from './interfaces';


export default class StormCtrl {

  data: StormData;
  trans: Trans;
  ground = prop<CgApi | undefined>(undefined) as Prop<CgApi>;

  constructor(readonly opts: StormOpts, readonly redraw: () => void) {
    this.data = opts.data;
    this.trans = lichess.trans(opts.i18n);
  }

  makeCgOpts = (): CgConfig => {
    const node = vm.node;
    const color: Color = node.ply % 2 === 0 ? 'white' : 'black';
    const dests = chessgroundDests(position());
    const nextNode = vm.node.children[0];
    const canMove = vm.mode === 'view' ||
      (color === vm.pov && (!nextNode || nextNode.puzzle == 'fail'));
    const movable = canMove ? {
      color: dests.size > 0 ? color : undefined,
      dests
    } : {
        color: undefined,
        dests: new Map(),
      };
    const config = {
      fen: node.fen,
      orientation: vm.pov,
      turnColor: color,
      movable: movable,
      premovable: {
        enabled: false
      },
      check: !!node.check,
      lastMove: uciToLastMove(node.uci)
    };
    if (node.ply >= vm.initialNode.ply) {
      if (vm.mode !== 'view' && color !== vm.pov && !nextNode) {
        config.movable.color = vm.pov;
        config.premovable.enabled = true;
      }
    }
    vm.cgConfig = config;
    return config;
  }
}
