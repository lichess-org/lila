var m = require('mithril');

function user(u) {
  if (!u) return 'Open challenge';
  var rating = u.rating + (u.provisional ? '?' : '');
  var fullName = (u.title ? u.title + ' ' : '') + u.name;
  return {
    tag: 'a',
    attrs: {
      class: 'ulpt user_link',
      href: '/@/' + u.name
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

function inButtons(ctrl, c) {
  return [
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
  ];
}

function outButtons(ctrl, c) {
  return [
    m('div.owner', [
      m('span.waiting', 'waiting...'),
      m('a.view', {
        href: '/' + c.id
      }, 'View challenge')
    ]),
    m('form', {
      method: 'post',
      action: '/challenge/' + c.id + '/cancel'
    }, m('button', {
      'type': 'submit',
      class: 'submit button decline',
      'data-icon': 'L',
    })),
  ];
}

function challenge(ctrl, dir) {
  return function(c) {
    return m('div', {
      class: 'challenge' + ' ' + dir + (c.declined ? ' declined' : ''),
    }, [
      m('i', {
        'data-icon': c.perf.icon
      }),
      m('div.content', [
        m('span.title', user(dir === 'in' ? c.challenger : c.destUser)),
        m('span.desc', [
          c.rated ? 'Rated' : 'Casual',
          timeControl(c.timeControl),
          c.variant.name
        ].join(' '))
      ]),
      m('div.buttons', (dir === 'in' ? inButtons : outButtons)(ctrl, c))
    ]);
  };
}

function allChallenges(ctrl, d, nb) {
  return m('div', {
    class: 'challenges' +
      (ctrl.vm.reloading ? ' reloading' : '') +
      (nb > 3 ? ' many' : ''),
    config: function(el, iu, ctx) {
      var hash = ctrl.idsHash();
      if (ctx.hash !== hash) {
        $('body').trigger('lichess.content_loaded');
        ctx.hash = hash;
      }
    }
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
