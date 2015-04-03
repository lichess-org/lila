var m = require('mithril');
var partial = require('chessground').util.partial;
var simul = require('../simul');
var util = require('./util');
var button = require('./button');
var xhr = require('../xhr');

function header(ctrl) {
  var sim = ctrl.data;
  return [
    m('th.large', 'Simul in preparation'),
    ctrl.userId ? m('th',
      simul.containsMe(ctrl) ? [
        (simul.createdByMe(ctrl) && simul.accepted().length > 1) ?
        m('button.button.right', {
          onclick: partial(xhr.start, ctrl)
        }, 'Start') : null,
        button.withdraw(ctrl)
      ] : button.join(ctrl)
    ) : m('th')
  ];
}

module.exports = function(ctrl) {
  var candidates = simul.candidates(ctrl);
  var accepted = simul.accepted(ctrl);
  var isHost = simul.createdByMe(ctrl);
  return [
    util.title(ctrl),
    m('table.slist.user_list.started.candidates',
      m('thead', m('tr', m('th', {
        colspan: 3
      }, candidates.length + ' candidate players'))),
      m('tbody', candidates.map(function(applicant) {
        return m('tr', [
          m('td', util.player(applicant.player)),
          m('td', util.playerVariantName(ctrl, applicant.player)),
          m('td', isHost ? m('button.button', {
            onclick: partial(xhr.accept(applicant.player.id), ctrl)
          }, 'Accept') : null)
        ])
      }))),
    m('table.slist.user_list.started.accepted',
      m('thead', m('tr', m('th', {
        colspan: 3
      }, accepted.length + ' accepted players'))),
      m('tbody', accepted.map(function(applicant) {
        return m('tr', [
          m('td', util.player(applicant.player)),
          m('td', util.playerVariantName(ctrl, applicant.player)),
          m('td', isHost ? m('button.button', {
            onclick: partial(xhr.reject(applicant.player.id), ctrl)
          }, 'Reject') : null)
        ])
      }))),
  ];
};
