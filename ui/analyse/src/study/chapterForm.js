var m = require('mithril');
var storedProp = require('../util').storedProp;
var partial = require('chessground').util.partial;
var xhr = require('./studyXhr');

module.exports = {
  ctrl: function(send, getChapters) {

    var vm = {
      variants: [],
      open: false,
      name: null,
      tab: storedProp('study.form.tab', 'blank'),
    };

    var loadVariants = function() {
      if (!vm.variants.length) xhr.variants().then(function(vs) {
        vm.variants = vs;
        m.redraw();
      });
    };
    loadVariants();

    var close = function() {
      vm.open = false;
      vm.name = null;
    };

    return {
      vm: vm,
      open: function(name) {
        vm.open = true;
        vm.name = name;
        loadVariants();
      },
      close: close,
      submit: function(data) {
        send("addChapter", data)
        close();
      },
      getChapters: getChapters
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
    var autofocus = function(el, isUpdate) {
      if (!isUpdate) el.focus();
    };
    var fieldValue = function(e, id) {
      var el = e.target.querySelector('#chapter-' + id);
      return el ? el.value : null;
    };

    return m('div.lichess_overboard.study_overboard', {
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
      }
    }, [
      m('a.close.icon[data-icon=L]', {
        onclick: ctrl.close
      }),
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
            value: ctrl.vm.name || 'Chapter ' + ctrl.getChapters().length
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
            placeholder: 'Game ID or URL',
            config: autofocus
          }),
          m('label.control-label[for=chapter-game]', 'From played or imported game'),
          m('i.bar')
        ]) : null,
        activeTab === 'fen' ? m('div.game.form-group', [
          m('input#chapter-fen', {
            placeholder: 'Initial position',
            config: autofocus
          }),
          m('label.control-label[for=chapter-fen]', 'From FEN position'),
          m('i.bar')
        ]) : null,
        activeTab === 'pgn' ? m('div.game.form-group', [
          m('textarea#chapter-pgn', {
            placeholder: 'PGN tags and moves',
            config: autofocus
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
        ]),
        m('div.button-container',
          m('button.submit.button[type=submit]', 'Create chapter')
        )
      ])
    ]);
  }
};
