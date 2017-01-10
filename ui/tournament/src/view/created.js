var m = require('mithril');
var pagination = require('../pagination');
var arena = require('./arena');
var header = require('./header');

module.exports = {
  main: function(ctrl) {
    var pag = pagination.players(ctrl);
    return [
      header(ctrl),
      arena.standing(ctrl, pag, 'created'),
      m('blockquote.pull-quote', [
          m('p', ctrl.data.quote.text),
          m('footer', ctrl.data.quote.author)
      ]),
      m('div.content_box_content', {
        config: function(el, isUpdate) {
          if (!isUpdate) $(el).html($('#tournament_faq').show());
        }
      })
    ];
  },
  side: function(ctrl) {
    return null;
  }
};
