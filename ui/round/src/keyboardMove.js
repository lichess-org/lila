var m = require('mithril');

module.exports = {
  ctrl: function(cg, step) {
    var focus = m.prop(false);
    var handler;
    var preHandlerBuffer = step.fen;
    return {
      update: function(step) {
        if (handler) handler(step.fen, cg.data.movable.dests);
        else preHandlerBuffer = step.fen;
      },
      registerHandler: function(h) {
        handler = h;
        if (preHandlerBuffer) handler(preHandlerBuffer, cg.data.movable.dests);
      },
      focus: focus,
      select: function(key) {
        if (cg.data.selected === key) return cg.cancelMove();
        cg.selectSquare(key, true);
      },
      cancel: cg.cancelMove
    };
  },
  view: function(ctrl) {
    return m('div.keyboard-move', [
      m('input', {
        config: function(el, isUpdate) {
          if (!isUpdate) lichess.loadScript('/assets/javascripts/keyboardMove.js').then(function() {
            ctrl.registerHandler(lichessKeyboardMove({
              input: el,
              focus: ctrl.focus,
              select: ctrl.select,
              cancel: ctrl.cancel
            }));
          });
        },
      }),
      ctrl.focus() ?
      m('em', 'Enter SAN (Nc3) or UCI (b1c3) moves, or type / to focus chat') :
      m('strong', 'Press <enter> to focus')
    ]);
  }
};
