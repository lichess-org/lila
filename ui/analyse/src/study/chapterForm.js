var m = require('mithril');
var storedProp = require('../util').storedProp;
var partial = require('chessground').util.partial;
var xhr = require('./studyXhr');
var dialog = require('./dialog');

module.exports = {
  ctrl: function(send, chapters, setTab) {

    var vm = {
      variants: [],
      open: false,
      tab: storedProp('study.form.tab', 'blank'),
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
    };
    var close = function() {
      vm.open = false;
    };

    return {
      vm: vm,
      open: open,
      close: close,
      toggle: function() {
        if (vm.open) close();
        else open();
      },
      submit: function(data) {
        send("addChapter", data)
        close();
        setTab();
      },
      chapters: chapters
    }
  },
  view: function(ctrl) {

    var activeTab = ctrl.vm.tab();
    var makeTab = function(key, name) {
      return m('a', {
        class: key + (activeTab === key ? ' active' : ''),
        onclick: partial(ctrl.vm.tab, key),
      }, name);
    };
    var fieldValue = function(e, id) {
      var el = e.target.querySelector('#chapter-' + id);
      return el ? el.value : null;
    };

    return dialog.form({
      onClose: ctrl.close,
      button: 'Create chapter',
      content: [
        m('h2', 'New chapter'),
        m('form.material.form', {
          onsubmit: function(e) {
            ctrl.submit({
              name: fieldValue(e, 'name'),
              game: fieldValue(e, 'game'),
              variant: fieldValue(e, 'variant'),
              fen: fieldValue(e, 'fen'),
              pgn: fieldValue(e, 'pgn'),
              orientation: fieldValue(e, 'orientation')
            });
            e.stopPropagation();
            return false;
          }
        }, [
          m('div.game.form-group', [
            m('input#chapter-name', {
              value: 'Chapter ' + (ctrl.chapters().length + 1),
              config: function(el, isUpdate) {
                if (!isUpdate) el.focus();
              }
            }),
            m('label.control-label[for=chapter-name]', 'Name'),
            m('i.bar')
          ]),
          m('div.study_tabs', [
            makeTab('blank', 'Blank'),
            makeTab('game', 'game'),
            makeTab('fen', 'FEN'),
            makeTab('pgn', 'PGN')
          ]),
          activeTab === 'game' ? m('div.game.form-group', [
            m('input#chapter-game', {
              placeholder: 'Game ID or URL'
            }),
            m('label.control-label[for=chapter-game]', 'From played or imported game'),
            m('i.bar')
          ]) : null,
          activeTab === 'fen' ? m('div.game.form-group', [
            m('input#chapter-fen', {
              placeholder: 'Initial position'
            }),
            m('label.control-label[for=chapter-fen]', 'From FEN position'),
            m('i.bar')
          ]) : null,
          activeTab === 'pgn' ? m('div.game.form-group', [
            m('textarea#chapter-pgn', {
              placeholder: 'PGN tags and moves'
            }),
            m('label.control-label[for=chapter-pgn]', 'From PGN game'),
            m('i.bar')
          ]) : null,
          m('div', [
            m('div.game.form-group.half', [
              m('select#chapter-variant', {
                  disabled: activeTab === 'game'
                }, activeTab === 'game' ? [
                  m('option', 'Game variant')
                ] :
                ctrl.vm.variants.map(function(v) {
                  return m('option', {
                    value: v.key
                  }, v.name)
                })),
              m('label.control-label[for=chapter-variant]', 'Variant'),
              m('i.bar')
            ]),
            m('div.game.form-group.half', [
              m('select#chapter-orientation', ['White', 'Black'].map(function(color) {
                return m('option', {
                  value: color.toLowerCase()
                }, color)
              })),
              m('label.control-label[for=chapter-orientation]', 'Orientation'),
              m('i.bar')
            ])
          ])
        ])
      ]
    });
  }
};
