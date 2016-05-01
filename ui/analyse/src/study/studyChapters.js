var m = require('mithril');
var classSet = require('chessground').util.classSet;
var partial = require('chessground').util.partial;
var chapterForm = require('./chapterForm');

function onEnter(action) {
  return function(el, isUpdate) {
    if (!isUpdate) $(el).keypress(function(e) {
      if (e.which == 10 || e.which == 13) action($(this).val(), this);
    })
  };
}

module.exports = {
  ctrl: function(chapters, send, setTab) {

    var vm = {
      confing: null // which chapter is being configured by us
    };

    var form = chapterForm.ctrl(send, function() {
      return chapters;
    }, setTab);

    return {
      vm: vm,
      form: form,
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

      return [
        m('div', {
          class: 'list chapters' + (ownage ? ' ownage' : ''),
          config: function(el, isUpdate, ctx) {
            if (!isUpdate || !ctx.count || ctx.count !== ctrl.chapters.list().length)
              $(el).scrollTo($(el).find('.active'), 200);
            ctx.count = ctrl.chapters.list().length;
          }
        }, [
          ctrl.chapters.list().map(function(chapter) {
            var confing = ctrl.chapters.vm.confing === chapter.id;
            var active = ctrl.currentChapter().id === chapter.id;
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
                  m('div.status', (active && ctrl.vm.loading) ? m.trust(lichess.spinnerHtml) : m('i', {
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
        ])
      ];
    }
  }
};
