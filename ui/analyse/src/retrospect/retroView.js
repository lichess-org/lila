var m = require('mithril');

module.exports = function(root) {
  var ctrl = root.retro;
  if (!ctrl || !ctrl.node) return;
  return m('div.retro_box', [
    m('div.player', [
      m('div.no-square', m('piece.king.' + ctrl.color)),
      m('div.instruction', [
        m('strong', ctrl.node.san + ' was played'),
        m('em', 'Find a better move for ' + ctrl.color)
      ])
    ]),
    m('button', 'Skip')
  ]);
};
