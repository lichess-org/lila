import m from '../mithrilFix';
import * as util from '../util';
import * as stages from '../stage/list';
import * as scoring from '../score';
import { LearnOpts, LearnProgress } from '../main';

export interface SideCtrl {
  trans: Trans;
  categId: _mithril.MithrilBasicProperty<number>;
  data: LearnProgress;
  reset(): void;
  active(): number | null;
  inStage(): boolean;
  setStage(stage: stages.Stage): void;
  progress(): number;
}

function renderInStage(ctrl: SideCtrl) {
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
      ...stages.categs.map(function (categ, categId) {
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
                const result = ctrl.data.stages[s.key];
                const status = s.id === ctrl.active() ? 'active' : result ? 'done' : 'future';
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

function renderHome(ctrl: SideCtrl) {
  const progress = ctrl.progress();
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

export default function (opts: LearnOpts, trans: Trans) {
  return {
    controller: function (): SideCtrl {
      const categId = m.prop(0);
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
        setStage: function (stage: stages.Stage) {
          categId(stages.stageIdToCategId(stage.id) || categId());
        },
        progress: function () {
          const max = stages.list.length * 10;
          const data = opts.storage.data.stages;
          const total = Object.keys(data).reduce(function (t, key) {
            const rank = scoring.getStageRank(stages.byKey[key], data[key].scores);
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
    view: function (ctrl: SideCtrl) {
      if (ctrl.inStage()) return renderInStage(ctrl);
      else return renderHome(ctrl);
    },
  };
}
