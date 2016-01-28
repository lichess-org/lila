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
      m('span.progress', [rating, ratingDiff])
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

function challenge(c) {
  return m('div.challenge', [
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
    ])
  ]);
}

module.exports = function(ctrl) {
  if (ctrl.vm.initiating) return m('div.square-wrap', m('div.square-spin'));
  var d = ctrl.data;
  return m('div', {
    class: 'challenges ' + (ctrl.vm.reloading ? ' reloading' : '')
  }, [
    d.in.map(challenge),
    d.out.map(challenge),
  ])
};
