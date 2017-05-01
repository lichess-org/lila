import { h } from 'snabbdom'

export function ctrl(cg, step, redraw) {
  let focus = false;
  let handler;
  let preHandlerBuffer = step.fen;
  const select = function(key) {
    if (cg.state.selected === key) cg.cancelMove();
    else cg.selectSquare(key, true);
  };
  let usedSan = false;
  return {
    update(step) {
      if (handler) handler(step.fen, cg.state.movable.dests);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, cg.state.movable.dests);
    },
    hasFocus: () => focus,
    setFocus(v) {
      focus = v;
      redraw();
    },
    san(orig, dest) {
      usedSan = true;
      cg.cancelMove();
      select(orig);
      select(dest);
    },
    select: select,
    hasSelected: () => cg.state.selected,
    usedSan: usedSan
  };
};
export function render(ctrl) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: {
        spellcheck: false,
        autocomplete: false
      },
      hook: {
        insert: vnode => {
          window.lichess.loadScript('/assets/javascripts/keyboardMove.js').then(() => {
            ctrl.registerHandler(window.lichess.keyboardMove({
              input: vnode.elm,
              setFocus: ctrl.setFocus,
              select: ctrl.select,
              hasSelected: ctrl.hasSelected,
              san: ctrl.san
            }));
          });
        }
      }
    }),
    ctrl.hasFocus() ?
    h('em', 'Enter SAN (Nc3) or UCI (b1c3) moves, or type / to focus chat') :
    h('strong', 'Press <enter> to focus')
  ]);
};
