var m = require('mithril');

function user(u) {
  var rating = u.rating + (u.provisional ? '?' : '');
  var fullName = (u.title ? u.title + ' ' : '') + u.name;
  return {
    tag: 'a',
    attrs: {
      class: 'ulpt user_link',
      'data-href': '/@/' + u.name
    },
    children: [
      fullName,
      ' (' + rating + ')'
    ]
  };
}

function timeControl(c) {
  switch (c.type) {
    case 'unlimited':
      return 'Unlimited';
    case 'correspondence':
      return c.daysPerTurn + ' days';
    case 'clock':
      return c.show;
  }
}

function challenge(ctrl, dir) {
  return function(c) {
    return m('div', {
      class: 'challenge' + (c.declined ? ' declined' : ''),
    }, [
      m('i', {
        'data-icon': c.perf.icon
      }),
      m('div.content', [
        m('span.title', user(c.challenger)),
        m('span.desc', [
          c.rated ? 'Rated' : 'Casual',
          timeControl(c.timeControl),
          c.variant.name
        ].join(' '))
      ]),
      m('div.buttons', [
        m('form', {
          method: 'post',
          action: '/challenge/' + c.id + '/accept'
        }, m('button', {
          'type': 'submit',
          class: 'submit button accept',
          'data-icon': 'E'
        })),
        m('form', m('button', {
          'type': 'submit',
          class: 'submit button decline',
          'data-icon': 'L',
          onclick: function(e) {
            ctrl.decline(c.id);
            return false;
          }
        }))
      ])
    ]);
  };
}

function allChallenges(ctrl, d, nb) {
  return m('div', {
    class: 'challenges' +
      (ctrl.vm.reloading ? ' reloading' : '') +
      (nb > 3 ? ' many' : '')
  }, [
    d.in.map(challenge(ctrl, 'in')),
    d.out.map(challenge(ctrl, 'out')),
  ]);
}

function empty(ctrl, d) {
  return m('div.empty', 'No challenges.');
}

module.exports = function(ctrl) {
  if (ctrl.vm.initiating) return m('div.square-wrap', m('div.square-spin'));
  var d = ctrl.data;
  var nb = d.in.length + d.out.length;
  return nb ? allChallenges(ctrl, d, nb) : empty(ctrl);
};
