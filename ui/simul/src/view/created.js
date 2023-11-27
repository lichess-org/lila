var m = require('mithril');
var simul = require('../simul');
var util = require('./util');
var text = require('../text');
var xhr = require('../xhr');

function byName(a, b) {
  return a.player.name > b.player.name;
}

function randomButton(ctrl, candidates) {
  return candidates.length
    ? m(
        'a.button.text',
        {
          'data-icon': 'E',
          onclick: function () {
            var randomCandidate = candidates[Math.floor(Math.random() * candidates.length)];
            xhr.accept(randomCandidate.player.id)(ctrl);
          },
        },
        'Accept random candidate'
      )
    : null;
}

function startOrCancel(ctrl, accepted) {
  return accepted.length > 1
    ? m(
        'a.button.button-green.text',
        {
          'data-icon': 'G',
          onclick: function () {
            xhr.start(ctrl);
          },
        },
        'Start'
      )
    : m(
        'a.button.button-red.text',
        {
          'data-icon': 'L',
          onclick: function () {
            if (confirm('Delete this simul?')) xhr.abort(ctrl);
          },
        },
        ctrl.trans('cancel')
      );
}

module.exports = function (ctrl) {
  var candidates = simul.candidates(ctrl).sort(byName);
  var accepted = simul.accepted(ctrl).sort(byName);
  var isHost = simul.createdByMe(ctrl);
  return [
    m('div.box__top', [
      util.title(ctrl),
      m('div.box__top__actions', [
        ctrl.userId
          ? simul.createdByMe(ctrl)
            ? [startOrCancel(ctrl, accepted), randomButton(ctrl, candidates)]
            : simul.containsMe(ctrl)
              ? m(
                  'a.button',
                  {
                    onclick: function () {
                      xhr.withdraw(ctrl);
                    },
                  },
                  ctrl.trans('withdraw')
                )
              : m(
                  'a.button.text' + (ctrl.teamBlock ? '.disabled' : ''),
                  {
                    disabled: ctrl.teamBlock,
                    'data-icon': 'G',
                    onclick: ctrl.teamBlock
                      ? undefined
                      : () => {
                          if (ctrl.data.variants.length === 1) xhr.join(ctrl, ctrl.data.variants[0].key);
                          else {
                            $.modal($('.simul .continue-with'));
                            $('#modal-wrap .continue-with a').click(function () {
                              $.modal.close();
                              xhr.join(ctrl, $(this).data('variant'));
                            });
                          }
                        },
                  },
                  ctrl.teamBlock ? ctrl.trans('mustBeInTeam', ctrl.data.team.name) : ctrl.trans('join')
                )
          : m(
              'a.button.text',
              {
                'data-icon': 'G',
                href: '/login?referrer=' + window.location.pathname,
              },
              ctrl.trans('signIn')
            ),
      ]),
    ]),
    text.view(ctrl),
    simul.acceptedContainsMe(ctrl)
      ? m('p.instructions', 'You have been selected! Hold still, the simul is about to begin.')
      : simul.createdByMe(ctrl) && ctrl.data.applicants.length < 6
        ? m('p.instructions', 'Share this page URL to let people enter the simul!')
        : null,
    m(
      'div.halves',
      m(
        'div.half.candidates',
        m(
          'table.slist.slist-pad',
          m(
            'thead',
            m(
              'tr',
              m(
                'th',
                {
                  colspan: 3,
                },
                [m('strong', candidates.length), ' candidate players']
              )
            )
          ),
          m(
            'tbody',
            candidates.map(function (applicant) {
              var variant = util.playerVariant(ctrl, applicant.player);
              return m(
                'tr',
                {
                  key: applicant.player.id,
                  class: ctrl.userId === applicant.player.id ? 'me' : '',
                },
                [
                  m('td', util.player(applicant.player)),
                  m('td.variant', {
                    'data-icon': variant.icon,
                  }),
                  m(
                    'td.action',
                    isHost
                      ? m('a.button', {
                          'data-icon': 'E',
                          title: 'Accept',
                          onclick: function () {
                            xhr.accept(applicant.player.id)(ctrl);
                          },
                        })
                      : null
                  ),
                ]
              );
            })
          )
        )
      ),
      m('div.half.accepted', [
        m(
          'table.slist.user_list',
          m('thead', [
            m(
              'tr',
              m(
                'th',
                {
                  colspan: 3,
                },
                [m('strong', accepted.length), ' accepted players']
              )
            ),
            simul.createdByMe(ctrl) && candidates.length && !accepted.length
              ? m('tr.help', m('th', 'Now you get to accept some players, then start the simul'))
              : null,
          ]),
          m(
            'tbody',
            accepted.map(function (applicant) {
              var variant = util.playerVariant(ctrl, applicant.player);
              return m(
                'tr',
                {
                  key: applicant.player.id,
                  class: ctrl.userId === applicant.player.id ? 'me' : '',
                },
                [
                  m('td', util.player(applicant.player)),
                  m('td.variant', {
                    'data-icon': variant.icon,
                  }),
                  m(
                    'td.action',
                    isHost
                      ? m('a.button.button-red', {
                          'data-icon': 'L',
                          onclick: function () {
                            xhr.reject(applicant.player.id)(ctrl);
                          },
                        })
                      : null
                  ),
                ]
              );
            })
          )
        ),
      ])
    ),
    m('blockquote.pull-quote', [m('p', ctrl.data.quote.text), m('footer', ctrl.data.quote.author)]),
    m(
      'div.continue-with.none',
      ctrl.data.variants.map(function (variant) {
        return m(
          'a.button',
          {
            'data-variant': variant.key,
          },
          ctrl.trans(variant.key)
        );
      })
    ),
  ];
};
