import { winningChances } from 'ceval';
import { DrawShape } from 'shogiground/draw';
import { opposite } from 'shogiground/util';
import { Role, isDrop } from 'shogiops/types';
import { makeSquareName, parseUsi } from 'shogiops/util';
import AnalyseCtrl from './ctrl';

function pieceDrop(
  key: Key,
  role: Role,
  color: Color,
  brush: 'engine' | 'engineAlt' | 'engineThreat' | 'engineThreatAlt'
): DrawShape {
  return {
    orig: key,
    dest: key,
    piece: {
      color,
      role,
      scale: 0.8,
    },
    brush: brush,
  };
}

export function makeShapesFromUsi(
  color: Color,
  usi: Usi,
  brush: 'engine' | 'engineAlt' | 'engineThreat' | 'engineThreatAlt'
): DrawShape[] {
  const move = parseUsi(usi)!;
  const to = makeSquareName(move.to);
  if (isDrop(move)) return [{ orig: to, dest: to, brush }, pieceDrop(to, move.role, color, brush)];

  return [
    {
      orig: makeSquareName(move.from),
      dest: to,
      description: move.promotion ? '+' : undefined,
      brush,
    },
  ];
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color = ctrl.node.sfen.includes(' w') ? 'gote' : 'sente';
  const rcolor = opposite(color);

  if (ctrl.practice) {
    if (ctrl.practice.hovering()) return makeShapesFromUsi(color, ctrl.practice.hovering().usi, 'engine');
    const hint = ctrl.practice.hinting();

    if (hint) {
      if (hint.mode === 'move') return makeShapesFromUsi(color, hint.usi, 'engine');
      else {
        const move = parseUsi(hint.usi)!;
        return [
          {
            orig: isDrop(move) ? { color, role: move.role } : makeSquareName(move.from),
            dest: isDrop(move) ? { color, role: move.role } : makeSquareName(move.from),
            brush: 'engine',
          },
        ];
      }
    }
    return [];
  }
  const instance = ctrl.getCeval();
  const hovering = instance.hovering();
  const { eval: nEval, sfen: nSfen, ceval: nCeval, threat: nThreat } = ctrl.node;

  let shapes: DrawShape[] = [];
  if (ctrl.retro && ctrl.retro.showBadNode()) {
    return makeShapesFromUsi(color, ctrl.retro.showBadNode().usi, 'engineThreat');
  }
  if (hovering && hovering.sfen === nSfen) shapes = shapes.concat(makeShapesFromUsi(color, hovering.usi, 'engine'));
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval?.best) shapes = shapes.concat(makeShapesFromUsi(rcolor, nEval.best, 'engine'));
    if (!hovering && parseInt(instance.multiPv())) {
      const curNodeBest = instance.enabled() && nCeval,
        nextBest = curNodeBest ? nCeval.pvs[0].moves[0] : ctrl.nextNodeBest();
      if (nextBest) shapes = shapes.concat(makeShapesFromUsi(color, nextBest, curNodeBest ? 'engine' : 'engineThreat'));
      if (instance.enabled() && nCeval && nCeval.pvs[1] && !(ctrl.threatMode() && nThreat && nThreat.pvs.length > 2)) {
        nCeval.pvs.forEach((pv, i) => {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, nCeval.pvs[0], pv);
          if (shift >= 0 && shift < 0.2) {
            shapes = shapes.concat(makeShapesFromUsi(color, pv.moves[0], i > 0 ? 'engineAlt' : 'engine'));
          }
        });
      }
    }
  }
  if (instance.enabled() && ctrl.threatMode() && nThreat) {
    const [pv0, ...pv1s] = nThreat.pvs;

    shapes = shapes.concat(makeShapesFromUsi(rcolor, pv0.moves[0], 'engineThreat'));

    pv1s.forEach(pv => {
      const shift = winningChances.povDiff(rcolor, pv, pv0);
      if (shift >= 0 && shift < 0.2) {
        shapes = shapes.concat(makeShapesFromUsi(rcolor, pv.moves[0], 'engineThreatAlt'));
      }
    });
  }
  if (ctrl.showMoveAnnotation() && ctrl.showComputer()) {
    const { usi, glyphs } = ctrl.node;
    if (usi && glyphs && glyphs.length > 0) {
      const glyph = glyphs[0],
        svg = glyphToSvg[glyph.symbol],
        move = parseUsi(usi);
      if (svg && move) {
        shapes = shapes.concat({
          orig: makeSquareName(move.to),
          dest: makeSquareName(move.to),
          customSvg: svg,
          brush: '',
        });
      }
    }
  }
  return shapes;
}

const prependDropShadow = (svgBase: string) =>
  `
<defs>
  <filter id="shadow">
    <feDropShadow dx="4" dy="7" stdDeviation="5" flood-opacity="0.5" />
  </filter>
</defs>` + svgBase;
// NOTE:
//   Base svg was authored with Inkscape.
//   On Inkscape, by using "Object to Path", text is converted to path, which enables consistent layout on browser.
//   Inkscape's output includes unnecessary attributes which can be cleaned up with https://lean-svg.netlify.app.
//   Wrap it by `transform="translate(...) scale(...)"` so that it sits at the right top corner.
//   Small tweak (e.g. changing color, scaling size, etc...) can be done by directly modifying svg below.
const glyphToSvg = {
  // Inaccuracy
  '?!': prependDropShadow(`
<g transform="translate(77 -18) scale(0.4)">
  <circle style="fill:#56b4e9;filter:url(#shadow)" cx="50" cy="50" r="50" />
  <path fill="#fff" d="M37.734 21.947c-3.714 0-7.128.464-10.242 1.393-3.113.928-6.009 2.13-8.685 3.605l4.343 8.766c2.35-1.202 4.644-2.157 6.883-2.867a22.366 22.366 0 0 1 6.799-1.065c2.294 0 4.07.464 5.326 1.393 1.311.874 1.967 2.186 1.967 3.933 0 1.748-.546 3.277-1.639 4.588-1.038 1.257-2.786 2.758-5.244 4.506-2.786 2.021-4.751 3.961-5.898 5.819-1.147 1.857-1.721 4.15-1.721 6.88v2.952h10.568v-2.377c0-1.147.137-2.103.41-2.868.328-.764.93-1.557 1.803-2.376.874-.82 2.104-1.803 3.688-2.95 2.13-1.584 3.906-3.058 5.326-4.424 1.42-1.42 2.485-2.95 3.195-4.59.71-1.638 1.065-3.576 1.065-5.816 0-4.206-1.584-7.675-4.752-10.406-3.114-2.731-7.51-4.096-13.192-4.096zm24.745.819l2.048 39.084h9.75l2.047-39.084zM35.357 68.73c-1.966 0-3.632.52-4.998 1.557-1.365.983-2.047 2.732-2.047 5.244 0 2.404.682 4.152 2.047 5.244 1.366 1.038 3.032 1.557 4.998 1.557 1.912 0 3.55-.519 4.916-1.557 1.366-1.092 2.05-2.84 2.05-5.244 0-2.512-.684-4.26-2.05-5.244-1.365-1.038-3.004-1.557-4.916-1.557zm34.004 0c-1.966 0-3.632.52-4.998 1.557-1.365.983-2.049 2.732-2.049 5.244 0 2.404.684 4.152 2.05 5.244 1.365 1.038 3.03 1.557 4.997 1.557 1.912 0 3.55-.519 4.916-1.557 1.366-1.092 2.047-2.84 2.047-5.244 0-2.512-.681-4.26-2.047-5.244-1.365-1.038-3.004-1.557-4.916-1.557z"/>
</g>
`),

  // Mistake
  '?': prependDropShadow(`
<g transform="translate(77 -18) scale(0.4)">
  <circle style="fill:#e69f00;filter:url(#shadow)" cx="50" cy="50" r="50" />
  <path fill="#fff" d="M40.436 60.851q0-4.66 1.957-7.83 1.958-3.17 6.712-6.619 4.195-2.983 5.967-5.127 1.864-2.237 1.864-5.22 0-2.983-2.237-4.475-2.144-1.585-6.06-1.585-3.915 0-7.737 1.212t-7.83 3.263l-4.941-9.975q4.568-2.517 9.881-4.101 5.314-1.585 11.653-1.585 9.695 0 15.008 4.661 5.407 4.661 5.407 11.839 0 3.822-1.212 6.619-1.212 2.796-3.635 5.22-2.424 2.33-6.06 5.034-2.703 1.958-4.195 3.356-1.491 1.398-2.05 2.703-.467 1.305-.467 3.263v2.703H40.436zm-1.492 18.924q0-4.288 2.33-5.966 2.331-1.771 5.687-1.771 3.263 0 5.594 1.771 2.33 1.678 2.33 5.966 0 4.102-2.33 5.966-2.331 1.772-5.594 1.772-3.356 0-5.686-1.772-2.33-1.864-2.33-5.966z"/>
</g>
`),

  // Blunder
  '??': prependDropShadow(`
<g transform="translate(77 -18) scale(0.4)">
  <circle style="fill:#df5353;filter:url(#shadow)" cx="50" cy="50" r="50" />
  <path fill="#fff" d="M31.8 22.22c-3.675 0-7.052.46-10.132 1.38-3.08.918-5.945 2.106-8.593 3.565l4.298 8.674c2.323-1.189 4.592-2.136 6.808-2.838a22.138 22.138 0 0 1 6.728-1.053c2.27 0 4.025.46 5.268 1.378 1.297.865 1.946 2.16 1.946 3.89s-.541 3.242-1.622 4.539c-1.027 1.243-2.756 2.73-5.188 4.458-2.756 2-4.7 3.918-5.836 5.755-1.134 1.837-1.702 4.107-1.702 6.808v2.92h10.457v-2.35c0-1.135.135-2.082.406-2.839.324-.756.918-1.54 1.783-2.35.864-.81 2.079-1.784 3.646-2.918 2.107-1.568 3.863-3.026 5.268-4.376 1.405-1.405 2.46-2.92 3.162-4.541.703-1.621 1.054-3.54 1.054-5.755 0-4.161-1.568-7.592-4.702-10.294-3.08-2.702-7.43-4.052-13.05-4.052zm38.664 0c-3.675 0-7.053.46-10.133 1.38-3.08.918-5.944 2.106-8.591 3.565l4.295 8.674c2.324-1.189 4.593-2.136 6.808-2.838a22.138 22.138 0 0 1 6.728-1.053c2.27 0 4.026.46 5.269 1.378 1.297.865 1.946 2.16 1.946 3.89s-.54 3.242-1.62 4.539c-1.027 1.243-2.757 2.73-5.189 4.458-2.756 2-4.7 3.918-5.835 5.755-1.135 1.837-1.703 4.107-1.703 6.808v2.92h10.457v-2.35c0-1.135.134-2.082.404-2.839.324-.756.918-1.54 1.783-2.35.865-.81 2.081-1.784 3.648-2.918 2.108-1.568 3.864-3.026 5.269-4.376 1.405-1.405 2.46-2.92 3.162-4.541.702-1.621 1.053-3.54 1.053-5.755 0-4.161-1.567-7.592-4.702-10.294-3.08-2.702-7.43-4.052-13.05-4.052zM29.449 68.504c-1.945 0-3.593.513-4.944 1.54-1.351.973-2.027 2.703-2.027 5.188 0 2.378.676 4.108 2.027 5.188 1.35 1.027 3 1.54 4.944 1.54 1.892 0 3.512-.513 4.863-1.54 1.35-1.08 2.026-2.81 2.026-5.188 0-2.485-.675-4.215-2.026-5.188-1.351-1.027-2.971-1.54-4.863-1.54zm38.663 0c-1.945 0-3.592.513-4.943 1.54-1.35.973-2.026 2.703-2.026 5.188 0 2.378.675 4.108 2.026 5.188 1.351 1.027 2.998 1.54 4.943 1.54 1.891 0 3.513-.513 4.864-1.54 1.351-1.08 2.027-2.81 2.027-5.188 0-2.485-.676-4.215-2.027-5.188-1.35-1.027-2.973-1.54-4.864-1.54z"/>
</g>
`),

  // Interesting move
  '!?': prependDropShadow(`
<g transform="translate(77 -18) scale(0.4)">
  <circle style="fill:#ea45d8;filter:url(#shadow)" cx="50" cy="50" r="50"/>
    <path fill="#fff" d="M60.823 58.9q0-4.098 1.72-6.883 1.721-2.786 5.9-5.818 3.687-2.622 5.243-4.506 1.64-1.966 1.64-4.588t-1.967-3.933q-1.885-1.393-5.326-1.393t-6.8 1.065q-3.36 1.065-6.883 2.868l-4.343-8.767q4.015-2.212 8.685-3.605 4.67-1.393 10.242-1.393 8.521 0 13.192 4.097 4.752 4.096 4.752 10.405 0 3.36-1.065 5.818-1.066 2.458-3.196 4.588-2.13 2.048-5.326 4.424-2.376 1.72-3.687 2.95-1.31 1.229-1.802 2.376-.41 1.147-.41 2.868v2.376h-10.57zm-1.311 16.632q0-3.77 2.048-5.244 2.049-1.557 4.998-1.557 2.868 0 4.916 1.557 2.049 1.475 2.049 5.244 0 3.605-2.049 5.244-2.048 1.556-4.916 1.556-2.95 0-4.998-1.556-2.048-1.64-2.048-5.244zM36.967 61.849h-9.75l-2.049-39.083h13.847zM25.004 75.532q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244z" vector-effect="non-scaling-stroke"/>
</g>
`),

  // Good move
  '!': prependDropShadow(`
<g transform="translate(77 -18) scale(0.4)">
  <circle style="fill:#22ac38;filter:url(#shadow)" cx="50" cy="50" r="50" />
  <path fill="#fff" d="M54.967 62.349h-9.75l-2.049-39.083h13.847zM43.004 76.032q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244z" vector-effect="non-scaling-stroke"/>
</g>
`),

  // Brilliant move
  '!!': prependDropShadow(`
<g transform="translate(77 -18) scale(0.4)">
  <circle style="fill:#168226;filter:url(#shadow)" cx="50" cy="50" r="50" />
  <path fill="#fff" d="M71.967 62.349h-9.75l-2.049-39.083h13.847zM60.004 76.032q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244zM37.967 62.349h-9.75l-2.049-39.083h13.847zM26.004 76.032q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244z" vector-effect="non-scaling-stroke"/>
</g>
`),
};
