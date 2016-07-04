var m = require('mithril');
var util = require('../util');
var stages = require('../stage/list');

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
        enabled: function() {
          return opts.route === 'run';
        },
        setStage: function(stage) {
          categId(stages.stageIdToCategId(stage.id));
        }
      };
    },
    view: function(ctrl) {
      if (ctrl.enabled()) return m('div.learn.map', [
        m('div.stages', [
          m('a.home', {
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
  };
}
