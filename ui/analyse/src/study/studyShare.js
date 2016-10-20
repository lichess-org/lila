var m = require('mithril');
var dialog = require('./dialog');
var partial = require('chessground').util.partial;

module.exports = {
  ctrl: function(data, currentChapter) {
    var open = m.prop(false);
    return {
      open: open,
      toggle: function() {
        open(!open());
      },
      studyId: data.id,
      chapter: currentChapter,
      isPublic: function() {
        return data.visibility === 'public';
      }
    }
  },
  view: function(ctrl) {
    if (!ctrl.open()) return;
    var studyId = ctrl.studyId;
    var chapter = ctrl.chapter();
    return dialog.form({
      onClose: function() {
        ctrl.open(false);
      },
      content: [
        m('h2', 'Share and embed'),
        m('form.material.form', [
          m('div.form-group', [
            m('input.has-value', {
              readonly: true,
              value: 'https://lichess.org/study/' + studyId
            }),
            m('label.control-label', 'Study URL'),
            m('i.bar')
          ]),
          m('div.form-group', [
            m('input.has-value', {
              readonly: true,
              value: 'https://lichess.org/study/' + studyId + '#' + chapter.id
            }),
            m('label.control-label', 'Current chapter URL'),
            m('i.bar')
          ]),
          m('div.form-group', [
            m('input.has-value', {
              readonly: true,
              value: ctrl.isPublic() ? '<iframe width=600 height=371 src="https://en.lichess.org/study/embed/' + studyId + '/' + chapter.id + '" frameborder=0></iframe>' : 'Only public studies can be embedded.'
            }),
            m('label.control-label', 'Embed current chapter'),
            m('i.bar')
          ])
        ])
      ]
    });
  }
};
