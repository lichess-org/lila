var m = require('mithril');
var classSet = require('chessground').util.classSet;

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
      }
    };
  },
  view: function(ctrl) {

    var ownage = ctrl.members.isOwner();

    var configButton = function(chapter, confing) {
      if (ownage) return m('span.action.config', {
        onclick: function(e) {
          ctrl.chapters.vm.confing = confing ? null : chapter.id;
        }
      }, m('i', {
        'data-icon': '%'
      }));
    };

    var chapterConfig = function(chapter) {
      return m('div.config', [
        "config"
      ]);
    };

    var create = function() {
      return m('div.create', [
        m('input', {
          class: 'list_input',
          config: function(el, isUpdate) {
            if (isUpdate) return;
            $(el).keypress(function(e) {
              if (e.which == 10 || e.which == 13) ctrl.chapters.add($(this).val());
            })
          },
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
          })
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
