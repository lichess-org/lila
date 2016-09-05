var m = require('mithril');
var partial = require('chessground').util.partial;
var simul = require('../simul');
var util = require('./util');
var button = require('./button');
var xhr = require('../xhr');

function maybeWithdrawButton(ctrl, applicant) {
  if (ctrl.userId === applicant.player.id) return m('a.thin.button', {
    onclick: partial(xhr.withdraw, ctrl)
  }, ctrl.trans('withdraw'));
}

function byRating(a, b) {
  return a.rating > b.rating
};

function randomButton(ctrl, candidates) {
  return candidates.length ? m('a.button.top_right.text', {
    'data-icon': 'E',
    onclick: function() {
      var randomCandidate = candidates[Math.floor(Math.random() * candidates.length)];
      xhr.accept(randomCandidate.player.id)(ctrl);
    }
  }, 'Accept random candidate') : null;
}

function startOrCancel(ctrl, accepted) {
  return accepted.length > 1 ?
    m('a.button.top_right.text.active', {
      'data-icon': 'G',
      onclick: partial(xhr.start, ctrl)
    }, 'Start') : m('a.button.top_right.text', {
      'data-icon': 'L',
      onclick: function() {
        if (confirm('Delete this simul?')) xhr.abort(ctrl);
      }
    }, ctrl.trans('cancel'));
}

module.exports = function(ctrl) {
  var candidates = simul.candidates(ctrl).sort(byRating);
  var accepted = simul.accepted(ctrl).sort(byRating);
  var isHost = simul.createdByMe(ctrl);
  return [
    ctrl.userId ? (
      simul.createdByMe(ctrl) ? [
        startOrCancel(ctrl, accepted),
        randomButton(ctrl, candidates)
      ] : (
        simul.containsMe(ctrl) ? m('a.button.top_right', {
          onclick: partial(xhr.withdraw, ctrl)
        }, ctrl.trans('withdraw')) : m('a.button.top_right.text', {
            'data-icon': 'G',
            onclick: function() {
              if (ctrl.data.variants.length === 1)
                xhr.join(ctrl.data.variants[0].key)(ctrl);
              else {
                $.modal($('#simul .join_choice'));
                $('#modal-wrap .join_choice a').click(function() {
                  $.modal.close();
                  xhr.join($(this).data('variant'))(ctrl);
                });
              }
            }
          },
          ctrl.trans('join'))
      )) : null,
    util.title(ctrl),
    simul.acceptedContainsMe(ctrl) ? m('div.instructions',
      'You have been selected! Hold still, the simul is about to begin.'
    ) : (
      (simul.createdByMe(ctrl) && ctrl.data.applicants.length < 6) ? m('div.instructions',
        'Share this page URL to let people enter the simul!'
      ) : null
    ),
    m('div.halves',
      m('div.half.candidates',
        m('table.slist.user_list',
          m('thead', m('tr', m('th', {
            colspan: 3
          }, [
            m('strong', candidates.length),
            ' candidate players'
          ]))),
          m('tbody', candidates.map(function(applicant) {
            var variant = util.playerVariant(ctrl, applicant.player);
            return m('tr', {
              key: applicant.player.id,
              class: ctrl.userId === applicant.player.id ? 'me' : ''
            }, [
              m('td', util.player(applicant.player)),
              m('td.variant.text', {
                'data-icon': variant.icon
              }, variant.name),
              m('td.action', isHost ? m('a.button.text', {
                'data-icon': 'E',
                onclick: function(e) {
                  xhr.accept(applicant.player.id)(ctrl);
                }
              }, 'Accept') : null)
            ])
          })))
      ),
      m('div.half.accepted', [
        m('table.slist.user_list',
          m('thead', [
            m('tr', m('th', {
              colspan: 3
            }, [
              m('strong', accepted.length),
              ' accepted players'
            ])), (simul.createdByMe(ctrl) && candidates.length && !accepted.length) ? m('tr.help',
              m('th',
                'Now you get to accept some players, then start the simul')) : null
          ]),
          m('tbody', accepted.map(function(applicant) {
            var variant = util.playerVariant(ctrl, applicant.player);
            return m('tr', {
              key: applicant.player.id,
              class: ctrl.userId === applicant.player.id ? 'me' : ''
            }, [
              m('td', util.player(applicant.player)),
              m('td.variant.text', {
                'data-icon': variant.icon
              }, variant.name),
              m('td.action', isHost ? m('a.button', {
                'data-icon': 'L',
                onclick: function(e) {
                  xhr.reject(applicant.player.id)(ctrl);
                }
              }) : null)
            ])
          })))
      ])
    ),
    m('blockquote.pull-quote', [
      m('p', ctrl.data.quote.text),
      m('footer', ctrl.data.quote.author)
    ]),
    m('div.join_choice.block_buttons', ctrl.data.variants.map(function(variant) {
      return m('a.button', {
        'data-variant': variant.key
      }, variant.name);
    }))
  ];
};
