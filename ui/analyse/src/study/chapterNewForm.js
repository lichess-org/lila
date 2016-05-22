var m = require('mithril');
var storedProp = require('../util').storedProp;
var partial = require('chessground').util.partial;
var xhr = require('./studyXhr');
var dialog = require('./dialog');
var tours = require('./studyTour');

var concealChoices = [
  ['', "Reveal all moves at once"],
  ['1', "Conceal next moves (puzzle mode)"]
];
var fieldValue = function(e, id) {
  var el = e.target.querySelector('#chapter-' + id);
  return el ? el.value : null;
};

module.exports = {
  concealChoices: concealChoices,
  fieldValue: fieldValue,
  ctrl: function(send, chapters, setTab, root) {

    var vm = {
      variants: [],
      open: false,
      initial: m.prop(false),
      tab: storedProp('study.form.tab', 'init'),
      editorFen: m.prop(null)
    };

    var loadVariants = function() {
      if (!vm.variants.length) xhr.variants().then(function(vs) {
        vm.variants = vs;
        m.redraw();
      });
    };

    var open = function() {
      vm.open = true;
      loadVariants();
      vm.initial(false);
      if (lichess.once('insight-tour-chapter'))
        setTimeout(partial(tours.chapter, vm.tab), 1000);
    };
    var close = function() {
      vm.open = false;
    };

    return {
      vm: vm,
      open: open,
      root: root,
      openInitial: function() {
        open();
        vm.initial(true);
      },
      close: close,
      toggle: function() {
        if (vm.open) close();
        else open();
      },
      initial: vm.initial,
      submit: function(data) {
        data.initial = vm.initial();
        send("addChapter", data)
        close();
        setTab();
      },
      chapters: chapters,
      startTour: partial(tours.chapter, vm.tab)
    }
  },
  view: function(ctrl) {

    var activeTab = ctrl.vm.tab();
    var makeTab = function(key, name, title) {
      return m('a.hint--top', {
        class: key + (activeTab === key ? ' active' : ''),
        'data-hint': title,
        onclick: partial(ctrl.vm.tab, key),
      }, name);
    };
    var gameOrPgn = activeTab === 'game' || activeTab === 'pgn';

    return dialog.form({
      onClose: ctrl.close,
      content: [
        activeTab === 'edit' ? null : m('h2', [
          'New chapter',
          m('i.help', {
            'data-icon': '',
            onclick: ctrl.startTour
          })
        ]),
        m('form.chapter_form.material.form', {
          onsubmit: function(e) {
            ctrl.submit({
              name: fieldValue(e, 'name'),
              game: fieldValue(e, 'game'),
              variant: fieldValue(e, 'variant'),
              fen: fieldValue(e, 'fen') || (activeTab === 'edit' ? ctrl.vm.editorFen() : null),
              pgn: fieldValue(e, 'pgn'),
              orientation: fieldValue(e, 'orientation'),
              conceal: !!fieldValue(e, 'conceal')
            });
            e.stopPropagation();
            return false;
          }
        }, [
          m('div.form-group', [
            m('input#chapter-name', {
              required: true,
              minlength: 2,
              maxlength: 80,
              config: function(el, isUpdate) {
                if (!isUpdate && !el.value) {
                  el.value = 'Chapter ' + (ctrl.initial() ? 1 : (ctrl.chapters().length + 1));
                  el.select();
                  el.focus();
                }
              }
            }),
            m('label.control-label[for=chapter-name]', 'Name'),
            m('i.bar')
          ]),
          m('div.study_tabs', [
            makeTab('init', 'Init', 'Start from initial position'),
            makeTab('edit', 'Edit', 'Start from custom position'),
            makeTab('game', 'game', 'Load a lichess game'),
            makeTab('fen', 'FEN', 'Load a FEN position'),
            makeTab('pgn', 'PGN', 'Load a PGN game')
          ]),
          activeTab === 'edit' ? m('div.editor_wrap', {
            config: function(el, isUpdate, ctx) {
              if (isUpdate) return;
              $.when(
                lichess.loadScript('/assets/compiled/lichess.editor.js'),
                $.get('/editor.json', {
                  fen: ctrl.root.vm.node.fen
                })
              ).then(function(a, b) {
                var data = b[0];
                data.embed = true;
                data.options = {
                  inlineCastling: true,
                  onChange: function(fen) {
                    ctrl.vm.editorFen(fen);
                    m.redraw();
                  }
                };
                var editor = LichessEditor(el, data);
                ctrl.vm.editorFen(editor.getFen());
              });
            }
          }, m.trust(lichess.spinnerHtml)) : null,
          activeTab === 'game' ? m('div.form-group', [
            m('input#chapter-game', {
              placeholder: 'Game ID or URL'
            }),
            m('label.control-label[for=chapter-game]', 'From played or imported game'),
            m('i.bar')
          ]) : null,
          activeTab === 'fen' ? m('div.form-group.no-label', [
            m('input#chapter-fen', {
              placeholder: 'Initial FEN position'
            }),
            m('i.bar')
          ]) : null,
          activeTab === 'pgn' ? m('div.form-group.no-label', [
            m('textarea#chapter-pgn', {
              placeholder: 'Paste your PGN here, one game only'
            }),
            m('i.bar')
          ]) : null,
          m('div', [
            m('div.form-group.half', [
              m('select#chapter-variant', {
                  disabled: gameOrPgn
                }, gameOrPgn ? [
                  m('option', 'Automatic')
                ] :
                ctrl.vm.variants.map(function(v) {
                  return m('option', {
                    value: v.key
                  }, v.name)
                })),
              m('label.control-label[for=chapter-variant]', 'Variant'),
              m('i.bar')
            ]),
            m('div.form-group.half', [
              m('select#chapter-orientation', ['White', 'Black'].map(function(color) {
                return m('option', {
                  value: color.toLowerCase()
                }, color)
              })),
              m('label.control-label[for=chapter-orientation]', 'Orientation'),
              m('i.bar')
            ])
          ]),
          gameOrPgn ? m('div.form-group', [
            m('select#chapter-conceal', concealChoices.map(function(c) {
              return m('option', {
                value: c[0]
              }, c[1])
            })),
            m('label.control-label[for=chapter-conceal]', 'Progressive move display'),
            m('i.bar')
          ]) : null,
          dialog.button('Create chapter')
        ])
      ]
    });
  }
};
