var m = require('mithril');
var xhr = require('./xhr');

module.exports = {
  ctrl: function(opts) {
    var id = opts.id;
    var vm = {
      text: m.prop(null)
    };
    var doPost = $.fp.debounce(function() {
      xhr.setNote(id, vm.text());
    }, 1000);
    return {
      id: id,
      vm: vm,
      trans: opts.trans,
      fetch: function() {
        xhr.getNote(id).then(function(t) {
          vm.text(t || '');
          m.redraw();
        });
      },
      post: function(text) {
        vm.text(text);
        doPost();
      }
    }
  },
  view: function(ctrl) {
    var text = ctrl.vm.text();
    if (text === null) return m('div.loading', {
      config: function(el, isUpdate) {
        if (!isUpdate) ctrl.fetch();
      }
    }, m.trust(lichess.spinnerHtml));
    return m('textarea', {
      placeholder: ctrl.trans('typePrivateNotesHere'),
      config: function(el, isUpdate) {
        if (isUpdate) return;
        $(el).val(text).on('change keyup paste', function() {
          ctrl.post($(el).val());
        });
      }
    });
  }
};
