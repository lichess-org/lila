var m = require('mithril');

function provisional() {
  return m('span', {
    title: 'Not enough rated games have been played to establish a reliable rating.'
  }, '(provisional)');
}

function percentile(d) {
  return d.percentile === 0 ? '' : [
    ' Better than ',
    m('a', {
      href: '/stat/rating/distribution/' + d.stat.perfType.key
    }, [
      m('strong', d.percentile + '%'),
      ' of ' + d.stat.perfType.name + ' players.'
    ])
  ];
}

function progress(p) {
  if (p > 0) return m('green[data-icon=N]', p);
  else if (p < 0) return m('red[data-icon=M]', -p);
}

module.exports = function(d) {
  return [
    m('h2', [
      'Rating: ',
      m('strong', {
        title: 'Yes, ratings have decimal accuracy.'
      }, d.perf.glicko.rating),
      '. ',
      m('span.details', d.perf.glicko.provisional ? provisional() : percentile(d))
    ]),
    m('p', [
      'Progression over the last twelve games: ',
      m('span.progress', progress(d.perf.progress) || 'none'),
      '. ',
      'Rating deviation: ',
      m('strong', {
        title: 'Lower value means the rating is more stable. Above 110, the rating is considered provisional.'
      }, d.perf.glicko.deviation),
      '. '
    ])
  ];
};
