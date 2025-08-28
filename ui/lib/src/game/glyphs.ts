import { parseUci, makeSquare, squareRank } from 'chessops/util';
import type { DrawShape } from '@lichess-org/chessground/draw';

// maximum number of glyphs to show for a given move
const maxGlyphs = 4;

export function annotationShapes(node: Tree.Node): DrawShape[] {
  const { uci, glyphs, san } = node;
  if (uci && san && glyphs) {
    return (
      glyphs
        .slice(0, maxGlyphs)
        .map((glyph: Tree.Glyph, idx: number) => {
          const move = parseUci(uci)!;
          const destSquare = san.startsWith('O-O') // castle, short or long
            ? squareRank(move.to) === 0 // white castle
              ? san.startsWith('O-O-O')
                ? 'c1'
                : 'g1'
              : san.startsWith('O-O-O')
                ? 'c8'
                : 'g8'
            : makeSquare(move.to);
          const symbol = glyph.symbol;
          const prerendered = glyphToSvg[symbol] ? glyphToSvg[symbol](idx) : undefined;
          return {
            orig: destSquare,
            brush: prerendered ? '' : undefined,
            customSvg: prerendered ? { html: prerendered } : undefined,
            label: prerendered ? undefined : { text: symbol, fill: 'purple' },
            // keep some purple just to keep feedback forum on their toes
          };
        })
        // needed so that the right-most (and first) glyph is at the top of the stack
        .reverse()
    );
  } else return [];
}

// NOTE:
//   Base svg was authored with Inkscape.
//   On Inkscape, by using "Object to Path", text is converted to path, which enables consistent layout on browser.
//   `composeGlyph` expects elements in a 0,0,100,100 view box
//
//   Note, if you use, markers, markers style="fill:context-stroke" is not supported yet by safari, so you have to edit the file manually to set the color
//
//   Inkscape's output includes unnecessary attributes which can be cleaned up with https://lean-svg.netlify.app.
//   Small tweak (e.g. changing color, scaling size, etc...) can be done by directly modifying svg below.
const composeGlyph = (fill: string, path: string) => (stackedNumber: number) =>
  `<defs><filter id="a"><feDropShadow dx="4" dy="7" flood-opacity=".5" stdDeviation="5"/></filter></defs><g transform="matrix(.4 0 0 .4 ${glyphStacktoPx(stackedNumber).x} ${glyphStacktoPx(stackedNumber).y})"><circle cx="50" cy="50" r="50" fill="${fill}" filter="url(#a)"/>${path}</g>`;

// the glyphs are laid-down from right to left
// with the first glyph being at the top right, then progressively to the left, until the top left
const glyphStacktoPx = (stack: number) => {
  return {
    x: 71 - (stack % maxGlyphs) * 28,
    y: -12,
  };
};

const whiteIsWinning = composeGlyph(
  '#bbb',
  '<path fill="none" stroke="#fff" stroke-width="7" d="M29 27v46M6 50h46m8 0h36"/>',
);

const blackIsWinning = composeGlyph(
  '#333',
  '<path fill="none" stroke="#fff" stroke-width="8" d="M71 27v46m23-23H48m-8 0H4"/>',
);

const glyphToSvg: Dictionary<(stackedNumber: number) => string> = {
  // Inaccuracy
  '?!': composeGlyph(
    '#56b4e9',
    '<path fill="#fff" d="M37.734 21.947c-3.714 0-7.128.464-10.242 1.393-3.113.928-6.009 2.13-8.685 3.605l4.343 8.766c2.35-1.202 4.644-2.157 6.883-2.867a22.366 22.366 0 0 1 6.799-1.065c2.294 0 4.07.464 5.326 1.393 1.311.874 1.967 2.186 1.967 3.933 0 1.748-.546 3.277-1.639 4.588-1.038 1.257-2.786 2.758-5.244 4.506-2.786 2.021-4.751 3.961-5.898 5.819-1.147 1.857-1.721 4.15-1.721 6.88v2.952h10.568v-2.377c0-1.147.137-2.103.41-2.868.328-.764.93-1.557 1.803-2.376.874-.82 2.104-1.803 3.688-2.95 2.13-1.584 3.906-3.058 5.326-4.424 1.42-1.42 2.485-2.95 3.195-4.59.71-1.638 1.065-3.576 1.065-5.816 0-4.206-1.584-7.675-4.752-10.406-3.114-2.731-7.51-4.096-13.192-4.096zm24.745.819 2.048 39.084h9.75l2.047-39.084zM35.357 68.73c-1.966 0-3.632.52-4.998 1.557-1.365.983-2.047 2.732-2.047 5.244 0 2.404.682 4.152 2.047 5.244 1.366 1.038 3.032 1.557 4.998 1.557 1.912 0 3.55-.519 4.916-1.557 1.366-1.092 2.05-2.84 2.05-5.244 0-2.512-.684-4.26-2.05-5.244-1.365-1.038-3.004-1.557-4.916-1.557zm34.004 0c-1.966 0-3.632.52-4.998 1.557-1.365.983-2.049 2.732-2.049 5.244 0 2.404.684 4.152 2.05 5.244 1.365 1.038 3.03 1.557 4.997 1.557 1.912 0 3.55-.519 4.916-1.557 1.366-1.092 2.047-2.84 2.047-5.244 0-2.512-.681-4.26-2.047-5.244-1.365-1.038-3.004-1.557-4.916-1.557z"/>',
  ),

  // Mistake
  '?': composeGlyph(
    '#e69f00',
    '<path fill="#fff" d="M40.436 60.851q0-4.66 1.957-7.83 1.958-3.17 6.712-6.619 4.195-2.983 5.967-5.127 1.864-2.237 1.864-5.22 0-2.983-2.237-4.475-2.144-1.585-6.06-1.585-3.915 0-7.737 1.212t-7.83 3.263l-4.941-9.975q4.568-2.517 9.881-4.101 5.314-1.585 11.653-1.585 9.695 0 15.008 4.661 5.407 4.661 5.407 11.839 0 3.822-1.212 6.619-1.212 2.796-3.635 5.22-2.424 2.33-6.06 5.034-2.703 1.958-4.195 3.356-1.491 1.398-2.05 2.703-.467 1.305-.467 3.263v2.703H40.436zm-1.492 18.924q0-4.288 2.33-5.966 2.331-1.771 5.687-1.771 3.263 0 5.594 1.771 2.33 1.678 2.33 5.966 0 4.102-2.33 5.966-2.331 1.772-5.594 1.772-3.356 0-5.686-1.772-2.33-1.864-2.33-5.966z"/>',
  ),

  // Blunder
  '??': composeGlyph(
    '#df5353',
    '<path fill="#fff" d="M31.8 22.22c-3.675 0-7.052.46-10.132 1.38-3.08.918-5.945 2.106-8.593 3.565l4.298 8.674c2.323-1.189 4.592-2.136 6.808-2.838a22.138 22.138 0 0 1 6.728-1.053c2.27 0 4.025.46 5.268 1.378 1.297.865 1.946 2.16 1.946 3.89s-.541 3.242-1.622 4.539c-1.027 1.243-2.756 2.73-5.188 4.458-2.756 2-4.7 3.918-5.836 5.755-1.134 1.837-1.702 4.107-1.702 6.808v2.92h10.457v-2.35c0-1.135.135-2.082.406-2.839.324-.756.918-1.54 1.783-2.35.864-.81 2.079-1.784 3.646-2.918 2.107-1.568 3.863-3.026 5.268-4.376 1.405-1.405 2.46-2.92 3.162-4.541.703-1.621 1.054-3.54 1.054-5.755 0-4.161-1.568-7.592-4.702-10.294-3.08-2.702-7.43-4.052-13.05-4.052zm38.664 0c-3.675 0-7.053.46-10.133 1.38-3.08.918-5.944 2.106-8.591 3.565l4.295 8.674c2.324-1.189 4.593-2.136 6.808-2.838a22.138 22.138 0 0 1 6.728-1.053c2.27 0 4.026.46 5.269 1.378 1.297.865 1.946 2.16 1.946 3.89s-.54 3.242-1.62 4.539c-1.027 1.243-2.757 2.73-5.189 4.458-2.756 2-4.7 3.918-5.835 5.755-1.135 1.837-1.703 4.107-1.703 6.808v2.92h10.457v-2.35c0-1.135.134-2.082.404-2.839.324-.756.918-1.54 1.783-2.35.865-.81 2.081-1.784 3.648-2.918 2.108-1.568 3.864-3.026 5.269-4.376 1.405-1.405 2.46-2.92 3.162-4.541.702-1.621 1.053-3.54 1.053-5.755 0-4.161-1.567-7.592-4.702-10.294-3.08-2.702-7.43-4.052-13.05-4.052zM29.449 68.504c-1.945 0-3.593.513-4.944 1.54-1.351.973-2.027 2.703-2.027 5.188 0 2.378.676 4.108 2.027 5.188 1.35 1.027 3 1.54 4.944 1.54 1.892 0 3.512-.513 4.863-1.54 1.35-1.08 2.026-2.81 2.026-5.188 0-2.485-.675-4.215-2.026-5.188-1.351-1.027-2.971-1.54-4.863-1.54zm38.663 0c-1.945 0-3.592.513-4.943 1.54-1.35.973-2.026 2.703-2.026 5.188 0 2.378.675 4.108 2.026 5.188 1.351 1.027 2.998 1.54 4.943 1.54 1.891 0 3.513-.513 4.864-1.54 1.351-1.08 2.027-2.81 2.027-5.188 0-2.485-.676-4.215-2.027-5.188-1.35-1.027-2.973-1.54-4.864-1.54z"/>',
  ),

  // Interesting move
  '!?': composeGlyph(
    '#ea45d8',
    '<path fill="#fff" d="M60.823 58.9q0-4.098 1.72-6.883 1.721-2.786 5.9-5.818 3.687-2.622 5.243-4.506 1.64-1.966 1.64-4.588t-1.967-3.933q-1.885-1.393-5.326-1.393t-6.8 1.065q-3.36 1.065-6.883 2.868l-4.343-8.767q4.015-2.212 8.685-3.605 4.67-1.393 10.242-1.393 8.521 0 13.192 4.097 4.752 4.096 4.752 10.405 0 3.36-1.065 5.818-1.066 2.458-3.196 4.588-2.13 2.048-5.326 4.424-2.376 1.72-3.687 2.95-1.31 1.229-1.802 2.376-.41 1.147-.41 2.868v2.376h-10.57zm-1.311 16.632q0-3.77 2.048-5.244 2.049-1.557 4.998-1.557 2.868 0 4.916 1.557 2.049 1.475 2.049 5.244 0 3.605-2.049 5.244-2.048 1.556-4.916 1.556-2.95 0-4.998-1.556-2.048-1.64-2.048-5.244zM36.967 61.849h-9.75l-2.049-39.083h13.847zM25.004 75.532q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244z" vector-effect="non-scaling-stroke"/>',
  ),

  // Good move
  '!': composeGlyph(
    '#22ac38',
    '<path fill="#fff" d="M54.967 62.349h-9.75l-2.049-39.083h13.847zM43.004 76.032q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244z" vector-effect="non-scaling-stroke"/>',
  ),

  // Brilliant move
  '!!': composeGlyph(
    '#168226',
    '<path fill="#fff" d="M71.967 62.349h-9.75l-2.049-39.083h13.847zM60.004 76.032q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244zM37.967 62.349h-9.75l-2.049-39.083h13.847zM26.004 76.032q0-3.77 2.049-5.244 2.048-1.557 4.998-1.557 2.867 0 4.916 1.557 2.048 1.475 2.048 5.244 0 3.605-2.048 5.244-2.049 1.556-4.916 1.556-2.95 0-4.998-1.556-2.049-1.64-2.049-5.244z" vector-effect="non-scaling-stroke"/>',
  ),

  // Correct move in a puzzle
  '✓': composeGlyph(
    '#22ac38',
    '<path fill="#fff" d="M87 32.8q0 2-1.4 3.2L51 70.6 44.6 77q-1.7 1.3-3.4 1.3-1.8 0-3.1-1.3L14.3 53.3Q13 52 13 50q0-2 1.3-3.2l6.4-6.5Q22.4 39 24 39q1.9 0 3.2 1.3l14 14L72.7 23q1.3-1.3 3.2-1.3 1.6 0 3.3 1.3l6.4 6.5q1.3 1.4 1.3 3.4z"/>',
  ),

  // Incorrect move in a puzzle
  '✗': composeGlyph(
    '#df5353',
    '<path fill="#fff" d="M79.4 68q0 1.8-1.4 3.2l-6.7 6.7q-1.4 1.4-3.5 1.4-1.9 0-3.3-1.4L50 63.4 35.5 78q-1.4 1.4-3.3 1.4-2 0-3.5-1.4L22 71.2q-1.4-1.4-1.4-3.3 0-1.7 1.4-3.5L36.5 50 22 35.4Q20.6 34 20.6 32q0-1.7 1.4-3.5l6.7-6.5q1.2-1.4 3.5-1.4 2 0 3.3 1.4L50 36.6 64.5 22q1.2-1.4 3.3-1.4 2.3 0 3.5 1.4l6.7 6.5q1.4 1.8 1.4 3.5 0 2-1.4 3.3L63.5 49.9 78 64.4q1.4 1.8 1.4 3.5z"/>',
  ),

  // 1st repetition when a game ends in 3 or 5fold repetition

  '①': composeGlyph(
    '#6e7781',
    '<defs id="defs1"><marker id="marker1" markerHeight="2.286" markerWidth="1.226" orient="auto" preserveAspectRatio="xMidYMid" refX=".613" refY="1.143" viewBox="0 0 1.226 2.286"><g id="g14-0" fill="#fff" stroke="#fff" stroke-opacity="1" transform="matrix(.25424 0 0 .27515 -15.365 -38.999)"><path id="path12-4" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".068" d="M60.547 143.301c.08-1.166.131-1.428.291-1.515.103-.056.283.033.61.301.291.24 1.062.949 1.886 1.735.608.58.868.82 1.058.978.099.082.236.207.305.278.207.212.428.574.495.81l.029.098-.063.091c-.211.306-1.015 1.106-2.592 2.578-1.122 1.046-1.511 1.353-1.719 1.354-.24 0-.299-.25-.36-1.524-.027-.559-.027-.803-.005-2.615.022-1.689.031-2.087.065-2.569z" paint-order="normal"/></g></marker><marker id="marker2" markerHeight="1.016" markerWidth=".681" orient="auto" preserveAspectRatio="xMidYMid" refX=".34" refY=".508" viewBox="0 0 .681 1.016"><path id="path13-0-8-4-6-1" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".023" d="M.67.018C.01.01.671.01.011.013c.007.06.052.068.171.185A.592.592 0 0 1 .32.446c.023.068-.068.205-.162.3C.105.797.036.954.018 1l.648.004z" paint-order="normal"/></marker></defs><g id="layer1" stroke-dasharray="none" stroke-linejoin="round" stroke-opacity="1" transform="translate(-98.385 -141.885)"><ellipse id="path1-7" cx="148.385" cy="191.885" fill="#6e7781" fill-opacity="1" stroke="#6e7781" stroke-width="4.717" rx="47.642" ry="47.642"/><text xml:space="preserve" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal;text-align:start" id="text2-6" x="134.688" y="212.334" fill="#fff" stroke="#fff" stroke-width="4.622" direction="ltr" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400" text-anchor="start" writing-mode="lr-tb"><tspan id="tspan2-0" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal" x="134.688" y="212.334" fill="#fff" stroke="#fff" stroke-dasharray="none" stroke-opacity="1" stroke-width="4.622" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400">1</tspan></text><path id="path20-0-5" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M138.401 228.14a37.605 37.605 0 0 1-27.27-41.37 37.605 37.605 0 0 1 37.414-32.489 37.605 37.605 0 0 1 37.137 32.804 37.605 37.605 0 0 1-27.62 41.139" paint-order="normal"/></g>',
  ),

  // 2nd repetition when a game ends in 3 or 5fold repetition
  '②': composeGlyph(
    '#6e7781',
    '<svg xmlns="http://www.w3.org/2000/svg" id="svg1" width="100" height="100" version="1.1" viewBox="0 0 100 100"><defs id="defs1"><marker id="marker1" markerHeight="2.286" markerWidth="1.226" orient="auto" preserveAspectRatio="xMidYMid" refX=".613" refY="1.143" viewBox="0 0 1.226 2.286"><g id="g14-0" fill="#fff" stroke="#fff" stroke-opacity="1" transform="matrix(.25424 0 0 .27515 -15.365 -38.999)"><path id="path12-4" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".068" d="M60.547 143.301c.08-1.166.131-1.428.291-1.515.103-.056.283.033.61.301.291.24 1.062.949 1.886 1.735.608.58.868.82 1.058.978.099.082.236.207.305.278.207.212.428.574.495.81l.029.098-.063.091c-.211.306-1.015 1.106-2.592 2.578-1.122 1.046-1.511 1.353-1.719 1.354-.24 0-.299-.25-.36-1.524-.027-.559-.027-.803-.005-2.615.022-1.689.031-2.087.065-2.569z" paint-order="normal"/></g></marker><marker id="marker2" markerHeight="1.016" markerWidth=".681" orient="auto" preserveAspectRatio="xMidYMid" refX=".34" refY=".508" viewBox="0 0 .681 1.016"><path id="path13-0-8-4-6-1" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".023" d="M.67.018C.01.01.671.01.011.013c.007.06.052.068.171.185A.592.592 0 0 1 .32.446c.023.068-.068.205-.162.3C.105.797.036.954.018 1l.648.004z" paint-order="normal"/></marker></defs><g id="layer1" stroke-dasharray="none" stroke-linejoin="round" stroke-opacity="1" transform="translate(-98.385 -141.885)"><ellipse id="path1-7" cx="148.385" cy="191.885" fill="#6e7781" fill-opacity="1" stroke="#6e7781" stroke-width="4.717" rx="47.642" ry="47.642"/><text xml:space="preserve" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal;text-align:start" id="text2-6" x="133.23" y="212.334" fill="#fff" stroke="#fff" stroke-width="4.622" direction="ltr" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400" text-anchor="start" writing-mode="lr-tb"><tspan id="tspan2-0" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal" x="133.23" y="212.334" fill="#fff" stroke="#fff" stroke-dasharray="none" stroke-opacity="1" stroke-width="4.622" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400">2</tspan></text><path id="path20-0-5" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M184.254 203.178a37.605 37.605 0 0 1-35.545 26.31 37.605 37.605 0 0 1-35.993-25.694" paint-order="normal"/><path id="path20-0-5-0" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M111.104 186.968a37.605 37.605 0 0 1 36.82-32.684 37.605 37.605 0 0 1 37.611 31.77" display="inline" paint-order="normal"/></g></svg>',
  ),

  // 3rd repetition when a game ends in 3 or 5fold repetition
  '③': composeGlyph(
    '#6e7781',
    '<defs id="defs1"><marker id="marker1" markerHeight="2.286" markerWidth="1.226" orient="auto" preserveAspectRatio="xMidYMid" refX=".613" refY="1.143" viewBox="0 0 1.226 2.286"><g id="g14-0" fill="#fff" stroke="#fff" stroke-opacity="1" transform="matrix(.25424 0 0 .27515 -15.365 -38.999)"><path id="path12-4" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".068" d="M60.547 143.301c.08-1.166.131-1.428.291-1.515.103-.056.283.033.61.301.291.24 1.062.949 1.886 1.735.608.58.868.82 1.058.978.099.082.236.207.305.278.207.212.428.574.495.81l.029.098-.063.091c-.211.306-1.015 1.106-2.592 2.578-1.122 1.046-1.511 1.353-1.719 1.354-.24 0-.299-.25-.36-1.524-.027-.559-.027-.803-.005-2.615.022-1.689.031-2.087.065-2.569z" paint-order="normal"/></g></marker><marker id="marker2" markerHeight="1.016" markerWidth=".681" orient="auto" preserveAspectRatio="xMidYMid" refX=".34" refY=".508" viewBox="0 0 .681 1.016"><path id="path13-0-8-4-6-1" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".023" d="M.67.018C.01.01.671.01.011.013c.007.06.052.068.171.185A.592.592 0 0 1 .32.446c.023.068-.068.205-.162.3C.105.797.036.954.018 1l.648.004z" paint-order="normal"/></marker></defs><g id="layer1" stroke-dasharray="none" stroke-linejoin="round" stroke-opacity="1" transform="translate(-98.385 -141.885)"><ellipse id="path1-7" cx="148.385" cy="191.885" fill="#6e7781" fill-opacity="1" stroke="#6e7781" stroke-width="4.717" rx="47.642" ry="47.642"/><text xml:space="preserve" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal;text-align:start" id="text2-6" x="132.66" y="211.973" fill="#fff" stroke="#fff" stroke-width="4.622" direction="ltr" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400" text-anchor="start" writing-mode="lr-tb"><tspan id="tspan2-0" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal" x="132.66" y="211.973" fill="#fff" stroke="#fff" stroke-dasharray="none" stroke-opacity="1" stroke-width="4.622" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400">3</tspan></text><path id="path20-0-5" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M113.523 205.982a37.605 37.605 0 0 1 1.385-31.225 37.605 37.605 0 0 1 24.512-19.392" paint-order="normal"/><path id="path20-0-5-0" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M173.971 219.444a37.605 37.605 0 0 1-50.362.73" display="inline" paint-order="normal"/><path id="path20-0-5-3" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M155.897 155.039a37.605 37.605 0 0 1 25.473 18.788 37.605 37.605 0 0 1 2.104 31.582" display="inline" paint-order="normal"/></g>',
  ),
  // 4th repetition when a game ends in 3 or 5fold repetition
  '④': composeGlyph(
    '#6e7781',
    '<defs id="defs1"><marker id="marker1" markerHeight="2.286" markerWidth="1.226" orient="auto" preserveAspectRatio="xMidYMid" refX=".613" refY="1.143" viewBox="0 0 1.226 2.286"><g id="g14-0" fill="#fff" stroke="#fff" stroke-opacity="1" transform="matrix(.25424 0 0 .27515 -15.365 -38.999)"><path id="path12-4" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".068" d="M60.547 143.301c.08-1.166.131-1.428.291-1.515.103-.056.283.033.61.301.291.24 1.062.949 1.886 1.735.608.58.868.82 1.058.978.099.082.236.207.305.278.207.212.428.574.495.81l.029.098-.063.091c-.211.306-1.015 1.106-2.592 2.578-1.122 1.046-1.511 1.353-1.719 1.354-.24 0-.299-.25-.36-1.524-.027-.559-.027-.803-.005-2.615.022-1.689.031-2.087.065-2.569z" paint-order="normal"/></g></marker><marker id="marker2" markerHeight="1.016" markerWidth=".681" orient="auto" preserveAspectRatio="xMidYMid" refX=".34" refY=".508" viewBox="0 0 .681 1.016"><path id="path13-0-8-4-6-1" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".023" d="M.67.018C.01.01.671.01.011.013c.007.06.052.068.171.185A.592.592 0 0 1 .32.446c.023.068-.068.205-.162.3C.105.797.036.954.018 1l.648.004z" paint-order="normal"/></marker></defs><g id="layer1" stroke-dasharray="none" stroke-linejoin="round" stroke-opacity="1" transform="translate(-98.385 -141.885)"><ellipse id="path1-7" cx="148.385" cy="191.885" fill="#6e7781" fill-opacity="1" stroke="#6e7781" stroke-width="4.717" rx="47.642" ry="47.642"/><text xml:space="preserve" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal;text-align:start" id="text2-6" x="133.577" y="212.251" fill="#fff" stroke="#fff" stroke-width="4.622" direction="ltr" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400" text-anchor="start" writing-mode="lr-tb"><tspan id="tspan2-0" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal" x="133.577" y="212.251" fill="#fff" stroke="#fff" stroke-dasharray="none" stroke-opacity="1" stroke-width="4.622" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400">4</tspan></text><path id="path20-0-5" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M116.892 212.435a37.605 37.605 0 0 1-1.568-38.467" paint-order="normal"/><path id="path20-0-5-0" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M126.122 161.58a37.605 37.605 0 0 1 41.57-1.964" display="inline" paint-order="normal"/><path id="path20-0-5-3" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M179.944 171.436a37.605 37.605 0 0 1 2.054 37.31" display="inline" paint-order="normal"/><path id="path20-0-5-9" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M169.252 223.17a37.605 37.605 0 0 1-41.083.424" display="inline" paint-order="normal"/></g>',
  ),

  // 5th repetition when a game ends in 3 or 5fold repetition
  '⑤': composeGlyph(
    '#6e7781',
    '<defs id="defs1"><marker id="marker1" markerHeight="2.286" markerWidth="1.226" orient="auto" preserveAspectRatio="xMidYMid" refX=".613" refY="1.143" viewBox="0 0 1.226 2.286"><g id="g14-0" fill="#fff" stroke="#fff" stroke-opacity="1" transform="matrix(.25424 0 0 .27515 -15.365 -38.999)"><path id="path12-4" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".068" d="M60.547 143.301c.08-1.166.131-1.428.291-1.515.103-.056.283.033.61.301.291.24 1.062.949 1.886 1.735.608.58.868.82 1.058.978.099.082.236.207.305.278.207.212.428.574.495.81l.029.098-.063.091c-.211.306-1.015 1.106-2.592 2.578-1.122 1.046-1.511 1.353-1.719 1.354-.24 0-.299-.25-.36-1.524-.027-.559-.027-.803-.005-2.615.022-1.689.031-2.087.065-2.569z" paint-order="normal"/></g></marker><marker id="marker2" markerHeight="1.016" markerWidth=".681" orient="auto" preserveAspectRatio="xMidYMid" refX=".34" refY=".508" viewBox="0 0 .681 1.016"><path id="path13-0-8-4-6-1" fill="#fff" fill-opacity="1" stroke="#fff" stroke-dasharray="none" stroke-linecap="butt" stroke-linejoin="round" stroke-opacity="1" stroke-width=".023" d="M.67.018C.01.01.671.01.011.013c.007.06.052.068.171.185A.592.592 0 0 1 .32.446c.023.068-.068.205-.162.3C.105.797.036.954.018 1l.648.004z" paint-order="normal"/></marker></defs><g id="layer1" stroke-dasharray="none" stroke-linejoin="round" stroke-opacity="1" transform="translate(-98.385 -141.885)"><ellipse id="path1-7" cx="148.385" cy="191.885" fill="#6e7781" fill-opacity="1" stroke="#6e7781" stroke-width="4.717" rx="47.642" ry="47.642"/><text xml:space="preserve" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal;text-align:start" id="text2-6" x="132.521" y="211.625" fill="#fff" stroke="#fff" stroke-width="4.622" direction="ltr" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400" text-anchor="start" writing-mode="lr-tb"><tspan id="tspan2-0" style="-inkscape-font-specification:\'Arial, Normal\';font-variant-ligatures:normal;font-variant-caps:normal;font-variant-numeric:normal;font-variant-east-asian:normal" x="132.521" y="211.625" fill="#fff" stroke="#fff" stroke-dasharray="none" stroke-opacity="1" stroke-width="4.622" font-family="Arial" font-size="56.9" font-stretch="normal" font-style="normal" font-variant="normal" font-weight="400">5</tspan></text><path id="path20-0-5" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M156.895 155.256a37.605 37.605 0 0 1 24.718 19.021" paint-order="normal"/><path id="path20-0-5-0" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M185.859 188.747a37.605 37.605 0 0 1-8.564 27.187" display="inline" paint-order="normal"/><path id="path20-0-5-3" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M166.31 224.943a37.605 37.605 0 0 1-34.413.74" display="inline" paint-order="normal"/><path id="path20-0-5-9" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M121.307 217.979a37.605 37.605 0 0 1-10.525-26.443" display="inline" paint-order="normal"/><path id="path20-0-5-1" fill="none" fill-opacity="1" stroke="#fff" stroke-linecap="butt" stroke-width="9.791" marker-end="url(#marker1)" marker-start="url(#marker2)" d="M114.093 176.455a37.605 37.605 0 0 1 23.563-20.611" display="inline" paint-order="normal"/></g>',
  ),

  // Only move
  '□': composeGlyph('#a04048', '<path stroke="#fff" stroke-width="7" fill="none" d="M30,30 H70 V70 H30 z"/>'),

  // Zugzwang
  '⨀': composeGlyph(
    '#9171f2',
    '<circle stroke="#fff" stroke-width="7" fill="none" cx="50" cy="50" r="45"/><circle stroke="#fff" stroke-width="7" fill="none" cx="50" cy="50" r="4"/>',
  ),

  // Equal position
  '=': composeGlyph(
    '#82c2ef',
    '<path stroke="#fff" stroke-width="7" fill="none" d="M 27,40 h 46 M 27,60 h 46"/>',
  ),

  // Unclear position
  '∞': composeGlyph(
    '#f5918f',
    '<path stroke="#fff" stroke-width="7" fill="none" d="M 40,40 A 14.14 14.14 0 1 0 40,60 L 60,40 A 14.14 14.14 0 1 1 60,60 L 40,40"/>',
  ),

  // White is slightly better
  '⩲': composeGlyph(
    '#999',
    '<path fill="none" stroke="#fff" stroke-width="7" d="M50 51V5M27 28h46M27 64h46M27 78h46"/>',
  ),

  // Black is slightly better
  '⩱': composeGlyph(
    '#555',
    '<path fill="none" stroke="#fff" stroke-width="7" d="M50 49v46M27 72h46M27 36h46M27 22h46"/>',
  ),

  // White is better
  '±': composeGlyph(
    '#aaa',
    '<path fill="none" stroke="#fff" stroke-width="7" d="M50 59V13M27 36h46M27 72h46"/>',
  ),

  // Black is better
  '∓': composeGlyph(
    '#444',
    '<path fill="none" stroke="#fff" stroke-width="7" d="M50 41v46M27 64h46M27 28h46"/>',
  ),

  // White is winning
  '+−': whiteIsWinning,
  '+-': whiteIsWinning,

  // Black is winning
  '−+': blackIsWinning,
  '-+': blackIsWinning,

  // Novelty
  N: composeGlyph(
    '#90c290',
    '<path fill="#fff" d="M21.7 85.7V14.3h10.4l38.1 59.1h.4q-.1-1.6-.25-4.8-.15-3.2-.3-7t-.15-7V14.3h8.4v71.4H67.8L29.6 26.4h-.4q.3 3.5.55 8.7.25 5.2.25 10.7v39.9h-8.3"/>',
  ),

  // Development
  '↑↑': composeGlyph(
    '#c87e9d',
    '<path fill="#fff" d="M32 29.2q-6.6 3.5-14.8 7.3v-4.4q9.3-8 13.7-16.7h2.2q4.4 8.7 13.7 16.7v4.4q-8.2-3.8-14.8-7.3v55.4m36-55.4q-6.6 3.5-14.8 7.3v-4.4q9.3-8 13.7-16.7h2.2q4.4 8.7 13.7 16.7v4.4q-8.2-3.8-14.8-7.3v55.4"/><path fill="none" stroke="#fff" stroke-width="7" d="M32 29.2v55.4m36-55.4v55.4"/>',
  ),

  // Initiative
  '↑': composeGlyph(
    '#2660a4',
    '<path fill="#fff" d="M50 29.2q-6.6 3.5-14.8 7.3v-4.4q9.3-8 13.7-16.7h2.2q4.4 8.7 13.7 16.7v4.4q-8.2-3.8-14.8-7.3"/><path fill="none" stroke="#fff" stroke-width="7" d="M50 29.2v55.4"/>',
  ),

  // Attack
  '→': composeGlyph(
    '#fb0e3d',
    '><path fill="#fff" d="M70.8 50q-3.5 6.6-7.3 14.8h4.4q8-9.3 16.7-13.7v-2.2q-8.7-4.4-16.7-13.7h-4.4q3.8 8.2 7.3 14.8"/><path fill="none" stroke="#fff" stroke-width="7" d="M15.4 50h55.4"/>',
  ),

  // Counterplay
  '⇆': composeGlyph(
    '#ff784f',
    '<path fill="#fff" d="M32.1 48.4q-8-9.3-16.7-14.8 8.7-5.5 16.7-14.8h4.4q-3.8 8.2-7.3 14.8 3.5 6.6 7.3 14.8h-4.4m38.7 18q-3.5-6.6-7.3-14.8h4.4q8 9.3 16.7 13.7v2.2q-8.7 4.4-16.7 13.7h-4.4q3.8-8.2 7.3-14.8"/><path fill="none" stroke="#fff" stroke-width="7" d="M29.2 33.6H80M20 66.4h50.8"/>',
  ),

  // Time trouble
  '⊕': composeGlyph(
    '#c2095a',
    '<circle cx="50" cy="50" r="25" fill="none" stroke="#fff" stroke-width="7"/><path fill="none" stroke="#fff" stroke-width="7" d="M50 25v50M25 50h50"/>',
  ),

  // With compensation
  '=∞': composeGlyph(
    '#180aae',
    '<path fill="none" stroke="#fff" stroke-width="7" d="M10 42h36M10 58h36m24-15.05a10 10 0 1 0 0 14.1l5-14.1a10 10 0 1 1 0 14.1l-5-14.1"/>',
  ),

  // With the idea
  '∆': composeGlyph(
    '#c8c831',
    '<path fill="#fff" d="M22.95 85.7v-5.5l22.5-65.9h9l22.6 66v5.4h-54.1m9.6-7.9h34.6l-12.5-37.2q-3.3-9.9-4.8-16.5-1.3 4.9-2.4 8.9-1.1 4-2.2 7.2l-12.7 37.6"/>',
  ),
};
