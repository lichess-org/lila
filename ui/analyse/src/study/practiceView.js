var m = require('mithril');
var classSet = require('common').classSet;

var firstRender = true;

function selector(practice) {
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
    practice.structure.map(function(section) {
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

  main: function(ctrl) {

    var current = ctrl.currentChapter();
    var practice = ctrl.practice;

    return [
      m('div.title', [
        m('i.practice.icon.' + practice.study.id),
        m('div.text', [
          m('h1', practice.study.name),
          m('em', practice.study.desc)
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
                class: 'status ' + (practice.completion[chapter.id] ? 'done' : 'ongoing')
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
        selector(ctrl.practice)
      ])
    ];
  }
};
