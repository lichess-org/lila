var m = require('mithril');
var classSet = require('common').classSet;
var xhr = require('./studyXhr');

var firstRender = true;

function selector(data) {
  if (!firstRender && m.redraw.strategy() === 'diff') return {
    subtree: 'retain'
  };
  firstRender = false;
  return m('select.selector', {
    onchange: function(e) {
      location.href = '/practice/' + e.target.value;
    }
  }, [
    m('option[disabled][selected]', 'Practice list'),
    data.structure.map(function(section) {
      return m('optgroup', {
        label: section.name
      }, section.studies.map(function(study) {
        return m('option', {
          value: '/' + section.id + '/' + study.slug + '/' + study.id
        }, study.name);
      }));
    })
  ]);
};

module.exports = {

  ctrl: function(root, data) {

    var complete = function(chapterId, nbMoves) {
      xhr.practiceComplete(chapterId, nbMoves);
      data.completion[chapterId] = Math.min(data.completion[chapterId] || 999, nbMoves);
    };

    var isVictory = function() {
      return root.gameOver() === 'checkmate' && root.turnColor() !== root.bottomColor();
    };

    var onJump = function() {
      if (isVictory()) complete(root.study.currentChapter().id, root.vm.node.ply);
    };

    return {
      onJump: onJump,
      data: data
    }
  },

  view: function(ctrl) {

    var current = ctrl.currentChapter();
    var data = ctrl.practice.data;

    return [
      m('div.title', [
        m('i.practice.icon.' + data.study.id),
        m('div.text', [
          m('h1', data.study.name),
          m('em', data.study.desc)
        ])
      ]),
      m('div', {
        key: 'chapters',
        class: 'list chapters',
        config: function(el, isUpdate) {
          if (!isUpdate)
            el.addEventListener('click', function(e) {
              var id = e.target.parentNode.getAttribute('data-id') || e.target.getAttribute('data-id');
              if (id) ctrl.setChapter(id);
            });
        }
      }, [
        ctrl.chapters.list().map(function(chapter, i) {
          var loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId;
          var active = !ctrl.vm.loading && current && current.id === chapter.id;
          return [
            m('div', {
              key: chapter.id,
              'data-id': chapter.id,
              class: 'elem chapter ' + classSet({
                active: active,
                loading: loading
              })
            }, [
              m('span', {
                'data-icon': 'E',
                class: 'status ' + (data.completion[chapter.id] ? 'done' : 'ongoing')
              }),
              m('h3', chapter.name)
            ])
          ];
        })
      ]),
      m('div.finally', [
        m('a.back', {
          'data-icon': 'I',
          href: '/practice',
          title: 'More practice'
        }),
        selector(data)
      ])
    ];
  }
};
