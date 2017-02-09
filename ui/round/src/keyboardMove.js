var m = require('mithril');

module.exports = {
  ctrl: function(root) {
    var cg = root.chessground;
    var focus = m.prop(false);
    return {
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
            lichessKeyboardMove({
              input: el,
              focus: ctrl.focus,
              select: ctrl.select
            });
          });
        },
      }),
      ctrl.focus() ?
      m('em', 'Enter coordinates to select squares. Press escape to cancel, press / to focus chat') :
      m('strong', 'Press <enter> to focus')
    ]);
  }
};
