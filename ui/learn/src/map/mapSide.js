var m = require('mithril');
var util = require('../util');
var stages = require('../stage/list');
var scoring = require('../score');

function renderInStage(ctrl) {
  return m('div.map', [
    m('div.stages', [
      m('a.back', {
        href: '/',
        config: m.route
      }, [
        'Menu'
      ]),
      stages.categs.map(function(categ, categId) {
        return m('div.categ', {
          class: categId == ctrl.categId() ? 'active' : ''
        }, [
          m('h2', {
            onclick: function() {
              ctrl.categId(categId);
            }
          }, categ.name),
          m('div.categ_stages',
            categ.stages.map(function(s) {
              var result = ctrl.data.stages[s.key];
              var status = s.id === ctrl.active() ? 'active' : (result ? 'done' : 'future')
              return m('a', {
                class: 'stage ' + status,
                href: '/' + s.id,
                config: m.route
              }, [
                m('img', {
                  src: s.image
                }),
                m('h2', s.title)
              ]);
            }))
        ]);
      })
    ])
  ]);
}

function renderHome(ctrl) {
  var progress = ctrl.progress();
  return m('div.home', [
    m('i.fat'),
    m('h1', 'Learn chess'),
    m('h2', 'by playing!'),
    m('div.progress', [
      m('div.text', 'Progress: ' + progress + '%'),
      m('div.bar', {
        style: {
          width: progress + '%'
        }
      })
    ])
  ]);
}

module.exports = function(opts) {
  return {
    controller: function() {
      var categId = m.prop(0);
      return {
        data: opts.storage.data,
        categId: categId,
        active: function() {
          return opts.stageId;
        },
        inStage: function() {
          return opts.route === 'run';
        },
        setStage: function(stage) {
          categId(stages.stageIdToCategId(stage.id));
        },
        progress: function() {
          var max = stages.list.length * 10;
					var data = opts.storage.data.stages;
          var total = Object.keys(data).reduce(function(t, key) {
						var rank = scoring.getStageRank(stages.byKey[key], data[key].scores);
						if (rank === 1) return t + 10;
						if (rank === 2) return t + 8;
						return t + 5;
          }, 0);
          return Math.round(total / max * 100);
        }
      };
    },
    view: function(ctrl) {
      if (ctrl.inStage()) return renderInStage(ctrl);
      else return renderHome(ctrl);
    }
  };
}
