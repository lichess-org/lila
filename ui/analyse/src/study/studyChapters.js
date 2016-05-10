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
  ctrl: function(initChapters, send, setTab) {

    var confing = m.prop(null); // which chapter is being configured by us
    var list = m.prop(initChapters);

    var form = chapterForm.ctrl(send, list, setTab);

    return {
      confing: confing,
      form: form,
      list: list,
      get: function(id) {
        return list().find(function(c) {
          return c.id === id;
        });
      },
      rename: function(id, name) {
        send("renameChapter", {
          id: id,
          name: name
        });
        confing(null);
      },
      delete: function(id) {
        send("deleteChapter", id);
        confing(null);
      }
    };
  },
  view: {
    main: function(ctrl) {

      var ownage = ctrl.members.isOwner();

      var configButton = function(chapter, confing) {
        if (ownage) return m('span.action.config', {
          onclick: function(e) {
            ctrl.chapters.confing(confing ? null : chapter.id);
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
            var newCount = ctrl.chapters.list().length;
            if (!isUpdate || !ctx.count || ctx.count !== newCount)
              $(el).scrollTo($(el).find('.active'), 200);
            ctx.count = newCount;
          }
        }, [
          ctrl.chapters.list().map(function(chapter) {
            var confing = ctrl.chapters.confing() === chapter.id;
            var current = ctrl.currentChapter();
            var active = current && current.id === chapter.id;
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
