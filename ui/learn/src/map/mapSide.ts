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
  partial_reset(stageKey: string): void;
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
    m(
      'div.actions',
      progress > 0
        ? [
            m(
              'label',
              {
                for: 'reset-categ-dropdown',
              },
              ctrl.trans.noarg('resetMyProgress')
            ),
            m(
              'select',
              {
                name: 'reset-categ-dropdown',
                id: 'reset-categ-dropdown',
                oninput: (value: Event | null) => {
                  const target = value?.target as HTMLSelectElement;
                  const question = target.selectedOptions[0].getAttribute('question');
                  if (question) {
                    if (target.value !== '' && confirm(question)) {
                      if (target.value === 'ALL') {
                        ctrl.reset();
                      } else {
                        ctrl.partial_reset(target.value);
                      }
                    }
                    target.value = '';
                  }
                },
              },
              ...[
                ...[
                  m(
                    'option',
                    {
                      value: '',
                    },
                    ctrl.trans.noarg('choose')
                  ),
                  m(
                    'option',
                    {
                      value: 'ALL',
                      question: ctrl.trans.noarg('youWillLoseAllYourProgress'),
                    },
                    ctrl.trans.noarg('everything')
                  ),
                ],
                ...[
                  ...stages.categs.map(function (categ) {
                    return categ.stages.map(function (s) {
                      return ctrl.data.stages[s.key]
                        ? m(
                            'option',
                            {
                              value: s.key,
                              question: ctrl.trans('resetMyProgressStage', ctrl.trans.noarg(s.title)),
                            },
                            ctrl.trans.noarg(s.title)
                          )
                        : undefined;
                    });
                  }),
                ],
              ]
            ),
          ]
        : null
    ),
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
        partial_reset: opts.storage.partial_reset,
        trans: trans,
      };
    },
    view: function (ctrl: SideCtrl) {
      if (ctrl.inStage()) return renderInStage(ctrl);
      else return renderHome(ctrl);
    },
  };
}
