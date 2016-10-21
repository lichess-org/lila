var m = require('mithril');
var dialog = require('./dialog');
var partial = require('chessground').util.partial;

var baseUrl = 'https://lichess.org/study/';

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
        m('h2', 'Share & export'),
        m('form.material.form', [
          m('div.form-group', [
            m('input.has-value', {
              readonly: true,
              value: baseUrl + studyId
            }),
            m('label.control-label', 'Study URL'),
            m('i.bar')
          ]),
          m('div.form-group', [
            m('input.has-value', {
              readonly: true,
              value: baseUrl + studyId + '#' + chapter.id
            }),
            m('label.control-label', 'Current chapter URL'),
            m('i.bar')
          ]),
          m('div.form-group', [
            m('input.has-value', {
              readonly: true,
              disabled: !ctrl.isPublic(),
              value: ctrl.isPublic() ? '<iframe width=600 height=371 src="' + baseUrl + 'embed/' + studyId + '/' + chapter.id + '" frameborder=0></iframe>' : 'Only public studies can be embedded.'
            }),
            m('a.form-help.text', {
              href: '/developers#embed-study',
              target: '_blank',
              'data-icon': ''
            }, 'Read more about embedding a study chapter'),
            m('label.control-label', 'Embed current chapter'),
            m('i.bar')
          ]),
          m('a.button.text.hint--top', {
            'data-icon': 'x',
            href: '/study/' + studyId + '.pgn'
          }, 'Download as PGN'),
        ])
      ]
    });
  }
};
