import m = require('mithril');
let util = require('../util');
let stages = require('../stage/list');
let scoring = require('../score');

function renderInStage(ctrl) {
  return m('div.learn__side-map', [
    m('div.stages', [
      m(
        'a.back',
        {
          href: '/',
          config: m.route,
        },
        [
          m('img', {
            src: util.assetUrl + 'images/learn/brutal-helm.svg',
          }),
          ctrl.trans.noarg('menu'),
        ]
      ),
      stages.categs.map(function (categ, categId) {
        return m(
          'div.categ',
          {
            class: categId == ctrl.categId() ? 'active' : '',
          },
          [
            m(
              'h2',
              {
                onclick: function () {
                  ctrl.categId(categId);
                },
              },
              ctrl.trans.noarg(categ.name)
            ),
            m(
              'div.categ_stages',
              categ.stages.map(function (s) {
                let result = ctrl.data.stages[s.key];
                let status = s.id === ctrl.active() ? 'active' : result ? 'done' : 'future';
                return m(
                  'a',
                  {
                    class: 'stage ' + status,
                    href: '/' + s.id,
                    config: m.route,
                  },
                  [
                    m('img', {
                      src: s.image,
                    }),
                    m('span', ctrl.trans.noarg(s.title)),
                  ]
                );
              })
            ),
          ]
        );
      }),
    ]),
  ]);
}

function renderHome(ctrl) {
  let progress = ctrl.progress();
  return m('div.learn__side-home', [
    m('i.fat'),
    m('h1', ctrl.trans.noarg('learnChess')),
    m('h2', ctrl.trans.noarg('byPlaying')),
    m('div.progress', [
      m('div.text', ctrl.trans('progressX', progress + '%')),
      m('div.bar', {
        style: {
          width: progress + '%',
        },
      }),
    ]),
    m('div.actions', [
      progress > 0
        ? m(
            'a.confirm',
            {
              onclick: function () {
                if (confirm(ctrl.trans.noarg('youWillLoseAllYourProgress'))) ctrl.reset();
              },
            },
            ctrl.trans.noarg('resetMyProgress')
          )
        : null,
    ]),
  ]);
}

module.exports = function (opts, trans) {
  return {
    controller: function () {
      let categId = m.prop(0);
      m.redraw.strategy('diff');
      return {
        data: opts.storage.data,
        categId: categId,
        active: function () {
          return opts.stageId;
        },
        inStage: function () {
          return opts.route === 'run';
        },
        setStage: function (stage) {
          categId(stages.stageIdToCategId(stage.id));
        },
        progress: function () {
          let max = stages.list.length * 10;
          let data = opts.storage.data.stages;
          let total = Object.keys(data).reduce(function (t, key) {
            let rank = scoring.getStageRank(stages.byKey[key], data[key].scores);
            if (rank === 1) return t + 10;
            if (rank === 2) return t + 8;
            return t + 5;
          }, 0);
          return Math.round((total / max) * 100);
        },
        reset: opts.storage.reset,
        trans: trans,
      };
    },
    view: function (ctrl) {
      if (ctrl.inStage()) return renderInStage(ctrl);
      else return renderHome(ctrl);
    },
  };
};
