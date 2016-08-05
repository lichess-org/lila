var m = require('mithril');
var classSet = require('chessground').util.classSet;
var partial = require('chessground').util.partial;
var chapterNewForm = require('./chapterNewForm');
var chapterEditForm = require('./chapterEditForm');

var configIcon = m('i', {
  'data-icon': '%'
});

module.exports = {
  ctrl: function(initChapters, send, setTab, chapterConfig, root) {

    var list = m.prop(initChapters);

    var newForm = chapterNewForm.ctrl(send, list, setTab, root);
    var editForm = chapterEditForm.ctrl(send, chapterConfig);

    return {
      newForm: newForm,
      editForm: editForm,
      list: list,
      get: function(id) {
        return list().filter(function(c) {
          return c.id === id;
        })[0];
      },
      size: function() {
        return list().length;
      },
      rename: function(id, name) {
        send("renameChapter", {
          id: id,
          name: name
        });
        confing(null);
      },
      sort: function(ids) {
        send("sortChapters", ids);
      },
      firstChapterId: function() {
        return list()[0].id;
      }
    };
  },
  view: {
    main: function(ctrl) {

      var configButton = function(chapter, editing) {
        if (ctrl.members.canContribute()) return m('span.action.config', {
          onclick: function(e) {
            ctrl.chapters.editForm.toggle(chapter);
            e.stopPropagation();
          }
        }, configIcon);
      };
      var current = ctrl.currentChapter();

      return [
        m('div', {
          key: 'chapters',
          class: 'list chapters',
          config: function(el, isUpdate, ctx) {
            var newCount = ctrl.chapters.list().length;
            if (!isUpdate || !ctx.count || ctx.count !== newCount) {
              if (isUpdate || current.id !== ctrl.chapters.firstChapterId()) {
                $(el).scrollTo($(el).find('.active'), 200);
              }
            }
            ctx.count = newCount;
            if (ctrl.members.canContribute() && newCount > 1 && !ctx.sortable) {
              var makeSortable = function() {
                ctx.sortable = Sortable.create(el, {
                  onSort: function() {
                    ctrl.chapters.sort(ctx.sortable.toArray());
                  }
                });
                ctx.onunload = function() {
                  ctx.sortable.destroy();
                };
              };
              if (window.Sortable) makeSortable();
              else lichess.loadScript('/assets/javascripts/vendor/Sortable.min.js').done(makeSortable);
            }
          },
          onclick: function(e) {
            var id = e.target.getAttribute('data-id') || $(e.target).parents('div.chapter').data('id');
            id && ctrl.setChapter(id);
          }
        }, [
          ctrl.chapters.list().map(function(chapter) {
            var active = current && current.id === chapter.id;
            var editing = ctrl.chapters.editForm.isEditing(chapter.id);
            var attrs = {
              key: chapter.id,
              'data-id': chapter.id,
              class: classSet({
                elem: true,
                chapter: true,
                active: active,
                editing: editing
              })
            };
            return [
              m('div', attrs, [
                m('div.left', [
                  m('div.status', (active && ctrl.vm.loading) ? m.trust(lichess.spinnerHtml) : m('i', {
                    'data-icon': active ? 'J' : 'K'
                  })),
                  chapter.name
                ]),
                configButton(chapter, editing)
              ])
            ];
          })
        ]),
        ctrl.members.canContribute() ? m('i.add[data-icon=0]', {
          title: 'New chapter',
          'data-icon': 'O',
          onclick: ctrl.chapters.newForm.toggle
        }) : null
      ];
    }
  }
};
