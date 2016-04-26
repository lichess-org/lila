var m = require('mithril');
var classSet = require('chessground').util.classSet;
var partial = require('chessground').util.partial;
var xhr = require('./studyXhr');

function onEnter(action) {
  return function(el, isUpdate) {
    if (!isUpdate) $(el).keypress(function(e) {
      if (e.which == 10 || e.which == 13) action($(this).val(), this);
    })
  };
}

module.exports = {
  ctrl: function(chapters, send) {

    var vm = {
      confing: null, // which chapter is being configured by us
      creating: null, // name of the chapter we're creating
      variants: []
    };

    return {
      vm: vm,
      list: function() {
        return chapters;
      },
      set: function(cs) {
        chapters = cs;
      },
      get: function(id) {
        return chapters.find(function(c) {
          return c.id === id;
        });
      },
      create: function(data) {
        send("addChapter", data);
        vm.creating = null;
      },
      rename: function(id, name) {
        send("renameChapter", {
          id: id,
          name: name
        });
        vm.confing = null;
      },
      delete: function(id) {
        send("deleteChapter", id);
        vm.confing = null;
      },
      loadVariants: function() {
        if (!vm.variants.length) xhr.variants().then(function(vs) {
          vm.variants = vs;
          m.redraw();
        });
      }
    };
  },
  view: {
    main: function(ctrl) {

      var ownage = ctrl.members.isOwner();

      var configButton = function(chapter, confing) {
        if (ownage) return m('span.action.config', {
          onclick: function(e) {
            ctrl.chapters.vm.confing = confing ? null : chapter.id;
            e.stopPropagation();
          }
        }, m('i', {
          'data-icon': '%'
        }));
      };

      var chapterConfig = function(chapter) {
        return m('div.config', [
          m('input', {
            value: chapter.name,
            config: onEnter(partial(ctrl.chapters.rename, chapter.id))
          }),
          m('div.delete', m('a.button.text[data-icon=q]', {
            onclick: function() {
              if (ctrl.chapters.list().length < 2)
                alert('There cannot be less than one chapter.');
              else if (confirm('Delete  ' + chapter.name + '?'))
                ctrl.chapters.delete(chapter.id);
            }
          }, 'Delete this chapter'))
        ]);
      };

      var create = function() {
        return m('div.create', [
          m('input', {
            class: 'list_input',
            config: onEnter(function(name, el) {
              ctrl.chapters.vm.creating = name;
              ctrl.chapters.loadVariants();
              el.value = '';
              m.redraw();
            }),
            placeholder: 'Add a new chapter'
          })
        ]);
      };

      return [
        m('div', {
          class: 'list chapters' + (ownage ? ' ownage' : ''),
          config: function(el, isUpdate) {
            if (!isUpdate) $(el).scrollTo($(el).find('.active'), {
              offsetTop: 200
            });
          }
        }, [
          ctrl.chapters.list().map(function(chapter) {
            var confing = ctrl.chapters.vm.confing === chapter.id;
            var active = ctrl.position().chapterId === chapter.id;
            var attrs = {
              class: classSet({
                elem: true,
                chapter: true,
                active: active,
                confing: confing
              }),
              onclick: function() {
                ctrl.setChapter(chapter.id);
              }
            };
            return [
              m('div', attrs, [
                m('div.left', [
                  m('div.status',
                    (active && ctrl.vm.loading) ? m.trust(lichess.spinnerHtml) : m('i', {
                      'data-icon': active ? 'J' : 'K'
                    })),
                  chapter.name
                ]),
                m('div.right', [
                  configButton(chapter, confing)
                ])
              ]),
              confing ? chapterConfig(chapter) : null
            ];
          })
        ]),
        ownage ? create() : null
      ];
    },
    form: function(ctrl, name) {
      return m('div.lichess_overboard.study_overboard', {
        config: function(el, isUpdate) {
          if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
        }
      }, [
        m('a.close.icon[data-icon=L]', {
          onclick: function() {
            ctrl.vm.creating = null;
          }
        }),
        m('h2', 'New chapter'),
        m('form.material.form', {
          onsubmit: function(e) {
            ctrl.create({
              name: name,
              game: e.target.querySelector('#chapter-game').value,
              variant: e.target.querySelector('#chapter-variant').value,
              fen: e.target.querySelector('#chapter-fen').value,
              orientation: e.target.querySelector('#chapter-orientation').value
            });
            e.stopPropagation();
            return false;
          }
        }, [
          m('div.game.form-group', [
            m('input#chapter-name', {
              value: name
            }),
            m('label.control-label[for=chapter-name]', 'Name'),
            m('i.bar')
          ]),
          m('div.game.form-group', [
            m('input#chapter-game', {
              placeholder: 'Game ID or URL'
            }),
            m('label.control-label[for=chapter-game]', 'Load existing game?'),
            m('i.bar')
          ]),
          m('div.game.form-group', [
            m('select#chapter-variant', ctrl.vm.variants.map(function(v) {
              return m('option', {
                value: v.key
              }, v.name)
            })),
            m('label.control-label[for=chapter-variant]', 'Variant'),
            m('i.bar')
          ]),
          m('div.game.form-group', [
            m('input#chapter-fen', {
              placeholder: 'Initial position'
            }),
            m('label.control-label[for=chapter-fen]', 'From FEN position'),
            m('i.bar')
          ]),
          m('div.game.form-group', [
            m('select#chapter-orientation', ['White', 'Black'].map(function(color) {
              return m('option', {
                value: color.toLowerCase()
              }, color)
            })),
            m('label.control-label[for=chapter-orientation]', 'Orientation'),
            m('i.bar')
          ]),
          m('div.button-container',
            m('button.submit.button.text[type=submit][data-icon=E]', 'Create chapter')
          )
        ])
      ]);
    }
  }
};
