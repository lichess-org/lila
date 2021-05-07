var m = require('mithril');

var shareStates = ['nobody', 'friends only', 'everybody'];

module.exports = function (ctrl) {
  var shareText = 'Shared with ' + shareStates[ctrl.user.shareId] + '.';
  return m('div.info.box', [
    m('div.top', [
      m(
        'a.username.user-link.insight-ulpt',
        {
          href: '/@/' + ctrl.user.name,
        },
        ctrl.user.name
      ),
    ]),
    m('div.content', [
      m('p', ['Insights over ', m('strong', ctrl.user.nbGames), ' rated games.']),
      m(
        'p.share',
        ctrl.own
          ? m(
              'a',
              {
                href: '/account/preferences/privacy',
                target: '_blank',
                rel: 'noopener',
              },
              shareText
            )
          : shareText
      ),
    ]),
    m('div.refresh', {
      config: function (e, isUpdate) {
        if (isUpdate) return;
        var $ref = $('.insight-stale');
        if ($ref.length) {
          $(e).html($ref.show());
          lichess.refreshInsightForm();
        }
      },
    }),
  ]);
};
