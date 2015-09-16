var m = require('mithril');
var partial = require('chessground').util.partial;
var pgnExport = require('../pgnExport');

module.exports = function(ctrl) {
  var fctrl = ctrl.forecast;
  var cNodes = ctrl.analyse.getNodesAfterPly(ctrl.vm.path, fctrl.gamePly());
  var isCandidate = fctrl.isCandidate(cNodes);
  return m('div.forecast', [
    m('div.box', [
      m('div.top', 'Conditional premoves'),
      m('div.list', fctrl.list().map(function(nodes, i) {
        return m('div.entry', {
          'data-icon': 'G',
          class: 'text'
        }, [
          m('a', {
            class: 'del',
            onclick: partial(fctrl.removeIndex, i)
          }, 'x'),
          m.trust(pgnExport.renderNodesHtml(nodes))
        ])
      })),
      m('button', {
        class: 'add button text',
        'data-icon': isCandidate ? 'O' : "",
        'disabled': !isCandidate,
      }, isCandidate ? [
        m('span', 'Add current variation'),
        m('span', m.trust(pgnExport.renderNodesHtml(cNodes)))
      ] : [
        m('span', 'Play a variation to create'),
        m('span', 'conditional premoves')
      ])
    ]),
    m('div.back_to_game',
      m('a', {
        class: 'button text',
        href: '/' + ctrl.data.game.id + '/' + ctrl.data.player.id,
        'data-icon': 'i'
      }, ctrl.trans('backToGame'))
    )
  ]);
};
