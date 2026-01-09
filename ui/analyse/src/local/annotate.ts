import { evalWinningChances } from 'lib/ceval/winningChances';
import * as co from 'chessops';
import { ucisToNodes, signEval } from 'lib/tree/util';
import { randomId } from 'lib/algo';

// modules/tree/.../Advice.scala

type Totals = { [key in Grade]: number };
type Info = { cp?: number; mate?: number; line?: San[]; best?: string };
type Grade = 'blunder' | 'mistake' | 'inaccuracy';

const lostMateText = 'Lost forced checkmate sequence';
const giftMateText = 'Checkmate is now unavoidable';
const glyphs = {
  mistake: { id: 2, symbol: '?', name: 'Mistake' },
  blunder: { id: 4, symbol: '??', name: 'Blunder' },
  inaccuracy: { id: 6, symbol: '?!', name: 'Dubious move' },
};

export function annotate(nodes: Tree.Node[]): { infos: Info[]; totals: { [c in Color]: Totals } } {
  const totals = {
    white: { inaccuracy: 0, mistake: 0, blunder: 0 },
    black: { inaccuracy: 0, mistake: 0, blunder: 0 },
  };
  const infos: Info[] = [];

  for (let i = 1; i < nodes.length; i++) {
    const [from, to] = [nodes[i - 1], nodes[i]];
    const { cp: fromCp, mate: fromMate } = signEval(from);
    const { cp: toCp, mate: toMate } = signEval(to);
    const loss = Math.abs(evalWinningChances(signEval(from)) + evalWinningChances(signEval(to)));

    let grade: Grade | undefined;
    let prefix: string | undefined;

    if (toCp !== undefined && Number(fromMate) > 0) {
      [grade, prefix] = [toCp > 999 ? 'inaccuracy' : toCp > 700 ? 'mistake' : 'blunder', lostMateText];
    } else if (fromCp !== undefined && Number(toMate) < 0) {
      [grade, prefix] = [fromCp < -999 ? 'inaccuracy' : fromCp < -700 ? 'mistake' : 'blunder', giftMateText];
    } else if (loss > 0.1) {
      grade = loss > 0.3 ? 'blunder' : loss > 0.2 ? 'mistake' : 'inaccuracy';
    }

    if (grade && from.eval?.best !== to.uci) {
      infos.push(addLine(from, grade, prefix));
      totals[from.ply % 2 === 0 ? 'white' : 'black'][grade]++;
    } else {
      infos.push({ cp: nodes[i].eval?.cp, mate: nodes[i].eval?.mate });
    }
  }
  return { infos, totals };
}

function addLine(
  parent: Tree.Node,
  grade: 'blunder' | 'mistake' | 'inaccuracy',
  prefix: string = grade.charAt(0).toUpperCase() + grade.slice(1),
) {
  const bad = parent.children[0];
  const chess = co.Chess.fromSetup(co.fen.parseFen(parent.fen).unwrap()).unwrap();
  const move = co.parseUci(parent.eval!.best!);
  const san = co.san.makeSan(chess, move!);
  const ucis = parent.eval!.pvs[0].moves.split(' ').slice(0, 12);
  bad.comments = [{ id: randomId().slice(0, 4), by: 'lichess', text: `${prefix}. ${san} was best.` }];
  bad.glyphs = [glyphs[grade]];
  parent.children.push(
    ucisToNodes(ucis, chess, bad.ply).map(good =>
      Object.assign({}, { clock: bad.clock }, good, { comp: true, eval: undefined }),
    )[0],
  );
  return {
    cp: bad.eval?.cp,
    mate: bad.eval?.mate,
    best: parent.eval?.best,
    line: ucis.map(uci => co.san.makeSanAndPlay(chess, co.parseUci(uci)!)),
  };
}
