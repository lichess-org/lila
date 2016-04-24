var m = require('mithril');
var classSet = require('chessground').util.classSet;
var partial = require('chessground').util.partial;

function onEnter(action) {
  return function(el, isUpdate) {
    if (!isUpdate) $(el).keypress(function(e) {
      if (e.which == 10 || e.which == 13) action($(this).val());
    })
  };
}

module.exports = {
  ctrl: function(chapters, send) {

    var vm = {
      confing: null // which chapter is being configured by us
    };

    return {
      vm: vm,
      list: function() {
        return chapters;
      },
      set: function(cs) {
        chapters = cs;
      },
      add: function(name) {
        send("addChapter", {
          name: name
        });
      },
      rename: function(id, name) {
        send("renameChapter", {
          id: id,
          name: name
        });
        vm.confing = null;
      }
    };
  },
  view: function(ctrl) {

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
        })
      ]);
    };

    var create = function() {
      return m('div.create', [
        m('input', {
          class: 'list_input',
          config: onEnter(ctrl.chapters.add),
          placeholder: 'Add a new chapter'
        })
      ]);
    };

    return m('div', {
      class: 'list chapters' + (ownage ? ' ownage' : '')
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
              m('span.status', m('i', {
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
      }),
      ownage ? create() : null
    ]);
  }
};
