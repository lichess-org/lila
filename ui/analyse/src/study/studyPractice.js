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

var readOnlyProp = function(value) {
  return function() {
    return value;
  };
};

module.exports = {

  ctrl: function(root, data) {

    root.vm.showAutoShapes = readOnlyProp(true);
    root.vm.showGauge = readOnlyProp(true);
    root.vm.showComputer = readOnlyProp(true);

    var feedback = m.prop();

    var victoryType = function() {
      return root.study.data.chapter.tags.filter(function(tag) {
        return tag[0] === 'Termination' && tag[1].toLowerCase() === 'draw';
      })[0] ? 'draw' : 'checkmate';
    };

    var complete = function(chapterId, nbMoves) {
      var former = data.completion[chapterId] || 999;
      if (nbMoves < former) {
        data.completion[chapterId] = nbMoves;
        xhr.practiceComplete(chapterId, nbMoves);
      }
      feedback({
        success: true,
        nbMoves: nbMoves,
        next: findNextOngoingChapter() || findNextChapter()
      });
    };

    var checkVictory = function() {
      var n = root.vm.node,
        vt = victoryType(),
        nbMoves = Math.ceil(n.ply / 2),
        isVictory;
      switch (vt) {
        case 'checkmate':
          isVictory = root.gameOver() === 'checkmate' && root.turnColor() !== root.bottomColor();
          break;
        case 'draw':
          isVictory = root.gameOver() === 'draw' || (
            nbMoves >= 15 && n.ceval && n.ceval.depth === 16 && Math.abs(n.ceval.cp) < 100
          );
      }
      if (isVictory) complete(root.study.currentChapter().id, nbMoves);
      else feedback({
        goal: vt,
        nbMoves: nbMoves,
        comment: root.tree.root.comments[0]
      });
    };

    var findNextOngoingChapter = function() {
      return root.study.data.chapters.filter(function(c) {
        return !data.completion[c.id];
      })[0];
    };
    var findNextChapter = function() {
      var chapters = root.study.data.chapters;
      var currentId = root.study.currentChapter().id;
      for (var i in chapters)
        if (chapters[i].id === currentId) return chapters[(i + 1) % chapters.length];
      return chapters[0];
    };

    return {
      onJump: checkVictory,
      onCeval: checkVictory,
      data: data,
      feedback: feedback
    };
  },

  view: {
    underboard: function(ctrl) {
      var fb = ctrl.practice.feedback();
      if (!fb) return;
      if (fb.success) return m('a.feedback.complete', {
        onclick: function() {
          ctrl.setChapter(fb.next.id);
        }
      }, [
        m('span', 'Success!'),
        'Next: ',
        m('strong', fb.next.name)
      ]);
      return m('div.feedback.ongoing', [
        m('div.goal', [
          'Your goal: ',
          m('strong',
            fb.goal === 'checkmate' ? 'Checkmate the opponent' :
            'Hold the draw for ' + (16 - fb.nbMoves) + ' more moves.')
        ]),
        fb.comment ? m('div.comment', fb.comment.text) : null
      ]);
    },
    main: function(ctrl) {

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
            var completion = data.completion[chapter.id] ? 'done' : 'ongoing';
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
                  'data-icon': ((loading || active) && completion === 'ongoing') ? 'G' : 'E',
                  class: 'status ' + completion
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
  }
};
