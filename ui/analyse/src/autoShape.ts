import { parseUci, makeSquare } from 'chessops/util';
import { isDrop } from 'chessops/types';
import { winningChances } from 'ceval';
import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { DrawShape } from 'chessground/draw';
import AnalyseCtrl from './ctrl';

function pieceDrop(key: cg.Key, role: cg.Role, color: Color): DrawShape {
  return {
    orig: key,
    piece: {
      color,
      role,
      scale: 0.8,
    },
    brush: 'green',
  };
}

export function makeShapesFromUci(color: Color, uci: Uci, brush: string, modifiers?: any): DrawShape[] {
  const move = parseUci(uci)!;
  const to = makeSquare(move.to);
  if (isDrop(move)) return [{ orig: to, brush }, pieceDrop(to, move.role, color)];

  const shapes: DrawShape[] = [
    {
      orig: makeSquare(move.from),
      dest: to,
      brush,
      modifiers,
    },
  ];
  if (move.promotion) shapes.push(pieceDrop(to, move.promotion, color));
  return shapes;
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color = ctrl.node.fen.includes(' w ') ? 'white' : 'black';
  const rcolor = opposite(color);
  if (ctrl.practice) {
    if (ctrl.practice.hovering()) return makeShapesFromUci(color, ctrl.practice.hovering().uci, 'green');
    const hint = ctrl.practice.hinting();
    if (hint) {
      if (hint.mode === 'move') return makeShapesFromUci(color, hint.uci, 'paleBlue');
      else
        return [
          {
            orig: hint.uci[1] === '@' ? hint.uci.slice(2, 4) : hint.uci.slice(0, 2),
            brush: 'paleBlue',
          },
        ];
    }
    return [];
  }
  const instance = ctrl.getCeval();
  const hovering = ctrl.explorer.hovering() || instance.hovering();
  const { eval: nEval = {} as Partial<Tree.ServerEval>, fen: nFen, ceval: nCeval, threat: nThreat } = ctrl.node;

  let shapes: DrawShape[] = [];
  if (ctrl.retro && ctrl.retro.showBadNode()) {
    return makeShapesFromUci(color, ctrl.retro.showBadNode().uci, 'paleRed', {
      lineWidth: 8,
    });
  }
  if (hovering && hovering.fen === nFen) shapes = shapes.concat(makeShapesFromUci(color, hovering.uci, 'paleBlue'));
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval.best) shapes = shapes.concat(makeShapesFromUci(rcolor, nEval.best, 'paleGreen'));
    if (!hovering) {
      let nextBest = ctrl.nextNodeBest();
      if (!nextBest && instance.enabled() && nCeval) nextBest = nCeval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeShapesFromUci(color, nextBest, 'paleBlue'));
      if (instance.enabled() && nCeval && nCeval.pvs[1] && !(ctrl.threatMode() && nThreat && nThreat.pvs.length > 2)) {
        nCeval.pvs.forEach(function (pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, nCeval.pvs[0], pv);
          if (shift >= 0 && shift < 0.2) {
            shapes = shapes.concat(
              makeShapesFromUci(color, pv.moves[0], 'paleGrey', {
                lineWidth: Math.round(12 - shift * 50), // 12 to 2
              })
            );
          }
        });
      }
    }
  }
  if (instance.enabled() && ctrl.threatMode() && nThreat) {
    const [pv0, ...pv1s] = nThreat.pvs;

    shapes = shapes.concat(makeShapesFromUci(rcolor, pv0.moves[0], pv1s.length > 0 ? 'paleRed' : 'red'));

    pv1s.forEach(function (pv) {
      const shift = winningChances.povDiff(rcolor, pv, pv0);
      if (shift >= 0 && shift < 0.2) {
        shapes = shapes.concat(
          makeShapesFromUci(rcolor, pv.moves[0], 'paleRed', {
            lineWidth: Math.round(11 - shift * 45), // 11 to 2
          })
        );
      }
    });
  }
  if (true) {
    // TODO: Maybe option to switch off? Especially for manually annotated glyph in the study?
    const { uci, glyphs } = ctrl.node;
    if (glyphs && glyphs.length > 0) {
      const glyph = glyphs[0]; // TODO: Can there be more than one glyph?
      const svg = glyphToSvg[glyph.symbol];
      if (svg) {
        const move = parseUci(uci!)!;
        shapes = shapes.concat({
          orig: makeSquare(move.to),
          customSvg: svg,
          brush: '',
        });
      }
    }
  }
  return shapes;
}

// NOTE:
//   Base svg was authored with Inkscape.
///  Inkscape's output includes unnecessary attributes so they are cleaned up manually.
//   On Inkscape, text is converted to path so that the layout becomes consistent.
//   Wrapping it by `transform="translate(70 2) scale(0.27)"` so that it sits at the right top corner.
const glyphToSvg = {
  // Inaccuracy
  '?!': `
<g transform="translate(70 2) scale(0.27)">
  <g>
    <circle style="fill:#0000ff" cx="50" cy="50" r="50" />
    <g style="fill:#ffffff;stroke-width:0.81934">
      <path d="m 29.622758,58.899124 q 0,-4.0967 1.720614,-6.882456 1.720614,-2.785756 5.899248,-5.817314 3.68703,-2.621888 5.243776,-4.50637 1.63868,-1.966416 1.63868,-4.588304 0,-2.621888 -1.966416,-3.932832 -1.884482,-1.392878 -5.32571,-1.392878 -3.441228,0 -6.800522,1.065142 -3.359294,1.065142 -6.882456,2.86769 L 18.80747,26.944864 q 4.014766,-2.212218 8.685004,-3.605096 4.670238,-1.392878 10.24175,-1.392878 8.521136,0 13.191374,4.0967 4.752172,4.0967 4.752172,10.405618 0,3.359294 -1.065142,5.817314 -1.065142,2.45802 -3.195426,4.588304 -2.130284,2.04835 -5.32571,4.424436 -2.376086,1.720614 -3.68703,2.949624 -1.310944,1.22901 -1.802548,2.376086 -0.40967,1.147076 -0.40967,2.86769 v 2.376086 H 29.622758 Z m -1.310944,16.632601 q 0,-3.768964 2.04835,-5.243776 2.04835,-1.556746 4.997974,-1.556746 2.86769,0 4.91604,1.556746 2.04835,1.474812 2.04835,5.243776 0,3.605096 -2.04835,5.243776 -2.04835,1.556746 -4.91604,1.556746 -2.949624,0 -4.997974,-1.556746 -2.04835,-1.63868 -2.04835,-5.243776 z" />
      <path d="M 74.276729,61.848748 H 64.526583 L 62.478233,22.76623 H 76.325078 Z M 62.314365,75.531725 q 0,-3.768964 2.04835,-5.243776 2.04835,-1.556746 4.997974,-1.556746 2.86769,0 4.91604,1.556746 2.048349,1.474812 2.048349,5.243776 0,3.605096 -2.048349,5.243776 -2.04835,1.556746 -4.91604,1.556746 -2.949624,0 -4.997974,-1.556746 -2.04835,-1.63868 -2.04835,-5.243776 z" />
    </g>
  </g>
</g>
`,

  // Mistake
  '?': `
<g transform="translate(70 2) scale(0.27)">
  <g>
    <circle style="fill:#ff9900;fill-opacity:1" cx="50" cy="50" r="50" />
    <g style="fill:#ffffff;stroke-width:0.932208">
      <path d="m 40.435856,60.851495 q 0,-4.661041 1.957637,-7.830548 1.957637,-3.169507 6.711897,-6.618677 4.194937,-2.983065 5.966132,-5.127144 1.864416,-2.237299 1.864416,-5.220365 0,-2.983065 -2.237299,-4.474598 -2.144079,-1.584754 -6.059353,-1.584754 -3.915273,0 -7.737326,1.21187 -3.822053,1.211871 -7.830548,3.262729 L 28.13071,24.495382 q 4.567819,-2.516962 9.881405,-4.101716 5.313586,-1.584753 11.6526,-1.584753 9.694964,0 15.008549,4.66104 5.406807,4.66104 5.406807,11.839042 0,3.822053 -1.21187,6.618677 -1.211871,2.796624 -3.635612,5.220365 -2.423741,2.33052 -6.059352,5.033923 -2.703403,1.957637 -4.194936,3.355949 -1.491533,1.398312 -2.050858,2.703403 -0.466104,1.305091 -0.466104,3.262728 v 2.703403 H 40.435856 Z m -1.491533,18.923822 q 0,-4.288156 2.33052,-5.966131 2.33052,-1.771195 5.686469,-1.771195 3.262728,0 5.593248,1.771195 2.33052,1.677975 2.33052,5.966131 0,4.101716 -2.33052,5.966132 -2.33052,1.771195 -5.593248,1.771195 -3.355949,0 -5.686469,-1.771195 -2.33052,-1.864416 -2.33052,-5.966132 z" />
    </g>
  </g>
</g>
`,

  // Blunder
  '??': `
<g transform="translate(70 2) scale(0.27)">
  <g>
    <circle style="fill:#ff0000" cx="50" cy="50" r="50" />
    <g transform="matrix(0.873744,0,0,0.873744,6.3966161,5.9077088)" style="fill:#ffffff;stroke-width:0.901384">
      <path d="m 20.764426,59.526365 q 0,-4.50692 1.892906,-7.571625 1.892907,-3.064706 6.489965,-6.399826 4.056228,-2.884429 5.768857,-4.957612 1.802768,-2.163322 1.802768,-5.04775 0,-2.884429 -2.163321,-4.326644 -2.073184,-1.532352 -5.858996,-1.532352 -3.785813,0 -7.481487,1.171799 -3.695674,1.171799 -7.571626,3.154844 L 8.8661574,24.37239 q 4.4167816,-2.433736 9.5546706,-3.966089 5.137888,-1.532353 11.267299,-1.532353 9.374393,0 14.512282,4.50692 5.228027,4.50692 5.228027,11.447576 0,3.695675 -1.171799,6.399827 -1.171799,2.704151 -3.515397,5.04775 -2.343599,2.25346 -5.858996,4.867473 -2.614014,1.892907 -4.056228,3.244983 -1.442215,1.352076 -1.983045,2.614013 -0.450692,1.261938 -0.450692,3.154844 v 2.614014 H 20.764426 Z M 19.322211,77.82446 q 0,-4.146366 2.25346,-5.768858 2.25346,-1.712629 5.498443,-1.712629 3.154844,0 5.408303,1.712629 2.25346,1.622492 2.25346,5.768858 0,3.966089 -2.25346,5.768857 -2.253459,1.71263 -5.408303,1.71263 -3.244983,0 -5.498443,-1.71263 -2.25346,-1.802768 -2.25346,-5.768857 z" />
      <path d="m 63.760378,59.526365 q 0,-4.50692 1.892907,-7.571625 1.892906,-3.064706 6.489964,-6.399826 4.056228,-2.884429 5.768858,-4.957612 1.802768,-2.163322 1.802768,-5.04775 0,-2.884429 -2.163322,-4.326644 -2.073183,-1.532352 -5.858996,-1.532352 -3.785812,0 -7.481487,1.171799 -3.695674,1.171799 -7.571625,3.154844 L 51.86211,24.37239 q 4.416781,-2.433736 9.55467,-3.966089 5.137889,-1.532353 11.2673,-1.532353 9.374393,0 14.512282,4.50692 5.228027,4.50692 5.228027,11.447576 0,3.695675 -1.171799,6.399827 -1.1718,2.704151 -3.515398,5.04775 -2.343598,2.25346 -5.858996,4.867473 -2.614013,1.892907 -4.056228,3.244983 -1.442214,1.352076 -1.983044,2.614013 -0.450692,1.261938 -0.450692,3.154844 v 2.614014 H 63.760378 Z M 62.318164,77.82446 q 0,-4.146366 2.25346,-5.768858 2.25346,-1.712629 5.498442,-1.712629 3.154844,0 5.408304,1.712629 2.25346,1.622492 2.25346,5.768858 0,3.966089 -2.25346,5.768857 -2.25346,1.71263 -5.408304,1.71263 -3.244982,0 -5.498442,-1.71263 -2.25346,-1.802768 -2.25346,-5.768857 z" />
    </g>
  </g>
</g>
`,
};
