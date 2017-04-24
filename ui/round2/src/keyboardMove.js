var m = require('mithril');

module.exports = {
  ctrl: function(cg, step) {
    var focus = m.prop(false);
    var handler;
    var preHandlerBuffer = step.fen;
    var select = function(key) {
      if (cg.state.selected === key) cg.cancelMove();
      else cg.selectSquare(key, true);
    };
    var usedSan = false;
    return {
      update: function(step) {
        if (handler) handler(step.fen, cg.state.movable.dests);
        else preHandlerBuffer = step.fen;
      },
      registerHandler: function(h) {
        handler = h;
        if (preHandlerBuffer) handler(preHandlerBuffer, cg.state.movable.dests);
      },
      focus: focus,
      setFocus: function(v) {
        focus(v);
        m.redraw();
      },
      san: function(orig, dest) {
        usedSan = true;
        cg.cancelMove();
        select(orig);
        select(dest);
      },
      select: select,
      hasSelected: function() {
        return cg.state.selected;
      },
      usedSan: usedSan
    };
  },
  view: function(ctrl) {
    return m('div.keyboard-move', [
      m('input', {
        spellcheck: false,
        autocomplete: false,
        config: function(el, isUpdate) {
          if (!isUpdate) lichess.loadScript('/assets/javascripts/keyboardMove.js').then(function() {
            ctrl.registerHandler(lichessKeyboardMove({
              input: el,
              setFocus: ctrl.setFocus,
              select: ctrl.select,
              hasSelected: ctrl.hasSelected,
              san: ctrl.san
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
