var m = require('mithril');
var classSet = require('chessground').util.classSet;
var partial = require('chessground').util.partial;
var util = require('../util');
var chapterNewForm = require('./chapterNewForm');
var chapterEditForm = require('./chapterEditForm');

var configIcon = m('i.action.config', {
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
      },
      toggleNewForm: function() {
        if (newForm.vm.open || list().length < 64) newForm.toggle();
        else alert("You have reached the limit of 64 chapters per study. Please create a new study.");
      }
    };
  },
  view: {
    main: function(ctrl) {

      var configButton = ctrl.members.canContribute() ? configIcon : null;
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
            } else if (ctrl.vm.loading && ctx.loadingId !== ctrl.vm.nextChapterId) {
              ctx.loadingId = ctrl.vm.nextChapterId;
              $(el).scrollTo($(el).find('.loading'), 200);
            }
            ctx.count = newCount;
            if (ctrl.members.canContribute() && newCount > 1 && !ctx.sortable) {
              var makeSortable = function() {
                ctx.sortable = Sortable.create(el, {
                  draggable: '.draggable',
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
            if (!isUpdate)
              el.addEventListener('click', function(e) {
                var id = e.target.parentNode.getAttribute('data-id') || e.target.getAttribute('data-id');
                if (!id) return;
                if (e.target.classList.contains('config'))
                  ctrl.chapters.editForm.toggle(ctrl.chapters.get(id));
                else ctrl.setChapter(id);
              });
          }
        }, [
          ctrl.chapters.list().map(function(chapter, i) {
            var editing = ctrl.chapters.editForm.isEditing(chapter.id);
            var loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId;
            var active = !ctrl.vm.loading && current && current.id === chapter.id;
            return [
              m('div', {
                key: chapter.id,
                'data-id': chapter.id,
                class: 'elem chapter draggable ' + classSet({
                  active: active,
                  editing: editing,
                  loading: loading
                })
              }, [
                m('span.status', i + 1),
                m('h3', chapter.name),
                configButton
              ])
            ];
          }),
          ctrl.members.canContribute() ? m('div', {
              key: 'new-chapter',
              class: 'elem chapter add',
              config: util.bindOnce('click', ctrl.chapters.toggleNewForm)
            },
            m('span.status', m('i[data-icon=O]')),
            m('h3.add_text', 'Add a new chapter')
          ) : null
        ])
      ];
    }
  }
};
