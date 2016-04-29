var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = {
  ctrl: function(send) {
    var open = m.prop(false);
    var invite = function(username) {
      send("invite", username);
      open(false);
    };
    return {
      open: open,
      invite: invite
    };
  },
  view: function(ctrl) {
    return m('div.lichess_overboard.study_overboard', [
      m('a.close.icon[data-icon=L]', {
        onclick: partial(ctrl.open, false)
      }),
      m('h2', 'Invite to the study'),
      m('input', {
        class: 'list_input',
        config: function(el, isUpdate) {
          if (isUpdate) return;
          lichess.userAutocomplete($(el), {
            onSelect: function(v) {
              ctrl.invite(v);
              $(el).typeahead('close');
              el.value = '';
              m.redraw();
            }
          });
        },
        placeholder: 'Invite someone'
      })
    ]);
  }
};
