import { h } from 'snabbdom';
import { categories } from '../categories';
import LearnCtrl from '../ctrl';
import { Stage } from '../interfaces';
import { average } from '../util';

function calcPercentage(ctrl: LearnCtrl): number {
  const max = ctrl.stages.map(s => s.levels.length).reduce((a, b) => a + b, 0) * 3,
    keys = Object.keys(ctrl.progress.progress.stages),
    total: number = keys
      .map(k => ((ctrl.progress.progress.stages[k]?.scores || []) as number[]).reduce((a, b) => a + b, 0))
      .reduce((a, b) => a + b, 0);

  return Math.round((total / max) * 100);
}

function makeStars(start: number) {
  const stars = [];
  for (let i = 0; i < start; i++)
    stars.push(
      h('i', {
        attrs: {
          'data-icon': 't',
        },
      })
    );
  return stars;
}

function ribbon(ctrl: LearnCtrl, s: Stage, status: string, res: number[]) {
  if (status === 'future') return;
  let content;
  if (status === 'ongoing') {
    content = res.length > 0 ? [res.length, s.levels.length].join(' / ') : ctrl.trans.noarg('play');
  } else content = makeStars(Math.floor(average(res)));
  return h('div.ribbon.' + status, h('div.ribbon-inner', content));
}

function side(ctrl: LearnCtrl) {
  const progress = calcPercentage(ctrl);
  return h('div.learn__side-home' + (progress === 100 ? '.done' : ''), [
    h('i.fat'),
    h('h1', ctrl.trans.noarg('learnShogi')),
    h('h2', ctrl.trans.noarg('byPlaying')),
    h('div.progress', [
      h('div.text', ctrl.trans('progressX', progress + '%')),
      h('div.bar', {
        style: {
          width: progress + '%',
        },
      }),
    ]),
    h('div.actions', [
      progress > 0
        ? h(
            'a.confirm',
            {
              on: {
                click: () => {
                  if (confirm(ctrl.trans.noarg('youWillLoseAllYourProgress'))) ctrl.reset();
                },
              },
            },
            ctrl.trans.noarg('resetMyProgress')
          )
        : null,
    ]),
  ]);
}

function whatNext(ctrl: LearnCtrl) {
  const makeStage = (href: string, img: string, title: I18nKey, subtitle: I18nKey, done?: boolean) => {
    const transTitle = ctrl.trans.noarg(title);
    return h(
      'a.stage' + titleVerbosityClass(transTitle) + (done ? '.done' : ''),
      {
        attrs: {
          href: href,
        },
      },
      [
        done ? h('div.ribbon.done', h('div.ribbon-inner', makeStars(3))) : null,
        h('div.stage-img.' + img),
        h('div.text', [h('h3', transTitle), h('p.subtitle', ctrl.trans.noarg(subtitle))]),
      ]
    );
  };
  const userId = ctrl.progress.progress._id;
  return h('div.categ.what_next', [
    h('h2', ctrl.trans.noarg('whatNext')),
    h('p', ctrl.trans.noarg('youKnowHowToPlayShogi')),
    h('div.categ_stages', [
      userId
        ? makeStage('/@/' + userId, 'beams-aura', 'register', 'getAFreeLishogiAccount', true)
        : makeStage('/signup', 'beams-aura', 'register', 'getAFreeLishogiAccount'),
      makeStage('/resources', 'king', 'shogiResources', 'curatedShogiResources'),
      makeStage('/training', 'bullseye', 'puzzles', 'exerciseYourTacticalSkills'),
      makeStage('/#hook', 'sword-clash', 'playPeople', 'opponentsFromAroundTheWorld'),
      makeStage('/#ai', 'vintage-robot', 'playMachine', 'testYourSkillsWithTheComputer'),
    ]),
  ]);
}

function titleVerbosityClass(title: string) {
  return title.length > 13 ? (title.length > 18 ? '.vvv' : '.vv') : '';
}

export default function (ctrl: LearnCtrl) {
  let prevComplete = true;
  return h('div.main.learn.learn--map', [
    h('div.learn__side', side(ctrl)),
    h('div.learn__main.learn-stages', [
      ...categories.map(categ => {
        return h('div.categ', [
          h('h2', ctrl.trans.noarg(categ.key)),
          h(
            'div.categ_stages',
            categ.stages.map(s => {
              const res = ctrl.progress.get(s.key).filter(s => s > 0);

              const complete = res.length === s.levels.length;
              let status = 'future';
              if (complete) {
                status = 'done';
              } else if (prevComplete || res.length > 0) {
                status = 'ongoing';
                prevComplete = false;
              }

              const title = ctrl.trans.noarg(s.title);
              return h(
                'a.stage.' +
                  status +
                  (s.id === 1 && Object.keys(ctrl.progress.progress.stages).length === 0 ? '.first' : ''),
                {
                  on: {
                    click: () => {
                      ctrl.setLesson(s.id);
                      ctrl.redraw();
                    },
                  },
                },
                [
                  ribbon(ctrl, s, status, res),
                  h('div.stage-img.' + s.key),
                  h('div.text' + titleVerbosityClass(title), [
                    h('h3', title),
                    h('p.subtitle', ctrl.trans.noarg(s.subtitle)),
                  ]),
                ]
              );
            })
          ),
        ]);
      }),
      whatNext(ctrl),
    ]),
  ]);
}
