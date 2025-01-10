import { i18n, i18nFormat } from 'i18n';
import { type VNode, h } from 'snabbdom';
import { categories } from '../categories';
import type LearnCtrl from '../ctrl';
import type { Stage } from '../interfaces';
import { average } from '../util';

function calcPercentage(ctrl: LearnCtrl): number {
  const max = ctrl.stages.map(s => s.levels.length).reduce((a, b) => a + b, 0) * 3,
    keys = Object.keys(ctrl.progress.progress.stages),
    total: number = keys
      .map(k =>
        ((ctrl.progress.progress.stages[k]?.scores || []) as number[]).reduce((a, b) => a + b, 0),
      )
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
      }),
    );
  return stars;
}

function ribbon(s: Stage, status: string, res: number[]) {
  if (status === 'future') return;
  let content;
  if (status === 'ongoing') {
    content = res.length > 0 ? [res.length, s.levels.length].join(' / ') : i18n('learn:play');
  } else content = makeStars(Math.floor(average(res)));
  return h('div.ribbon.' + status, h('div.ribbon-inner', content));
}

function side(ctrl: LearnCtrl) {
  const progress = calcPercentage(ctrl);
  return h('div.learn__side-home' + (progress === 100 ? '.done' : ''), [
    h('i.fat'),
    h('h1', i18n('learn:learnShogi')),
    h('h2', i18n('learn:byPlaying')),
    h('div.progress', [
      h('div.text', i18nFormat('learn:progressX', progress + '%')),
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
                  if (confirm(i18n('learn:youWillLoseAllYourProgress'))) ctrl.reset();
                },
              },
            },
            i18n('learn:resetMyProgress'),
          )
        : null,
    ]),
  ]);
}

function whatNext(ctrl: LearnCtrl) {
  const makeStage = (
    href: string,
    img: string,
    title: string,
    subtitle: string,
    done?: boolean,
  ) => {
    return h(
      'a.stage' + titleVerbosityClass(title) + (done ? '.done' : ''),
      {
        attrs: {
          href: href,
        },
      },
      [
        done ? h('div.ribbon.done', h('div.ribbon-inner', makeStars(3))) : null,
        h('div.stage-img.' + img),
        h('div.text', [h('h3', title), h('p.subtitle', subtitle)]),
      ],
    );
  };
  const userId = ctrl.progress.progress._id;
  return h('div.categ.what_next', [
    h('h2', i18n('learn:whatNext')),
    h('p', i18n('learn:youKnowHowToPlayShogi')),
    h('div.categ_stages', [
      userId
        ? makeStage(
            '/@/' + userId,
            'beams-aura',
            i18n('learn:register'),
            i18n('learn:getAFreeLishogiAccount'),
            true,
          )
        : makeStage(
            '/signup',
            'beams-aura',
            i18n('learn:register'),
            i18n('learn:getAFreeLishogiAccount'),
          ),
      makeStage(
        '/resources',
        'king',
        i18n('learn:shogiResources'),
        i18n('learn:curatedShogiResources'),
      ),
      makeStage(
        '/training',
        'bullseye',
        i18n('learn:puzzles'),
        i18n('learn:exerciseYourTacticalSkills'),
      ),
      makeStage(
        '/#hook',
        'sword-clash',
        i18n('learn:playPeople'),
        i18n('learn:opponentsFromAroundTheWorld'),
      ),
      makeStage(
        '/#ai',
        'vintage-robot',
        i18n('learn:playMachine'),
        i18n('learn:testYourSkillsWithTheComputer'),
      ),
    ]),
  ]);
}

function titleVerbosityClass(title: string) {
  return title.length > 13 ? (title.length > 18 ? '.vvv' : '.vv') : '';
}

export default function (ctrl: LearnCtrl): VNode {
  let prevComplete = true;
  return h('div.main.learn.learn--map', [
    h('div.learn__side', side(ctrl)),
    h('div.learn__main.learn-stages', [
      ...categories.map(categ => {
        return h('div.categ', [
          h('h2', categ.name),
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
              return h(
                'a.stage.' +
                  status +
                  (s.id === 1 && Object.keys(ctrl.progress.progress.stages).length === 0
                    ? '.first'
                    : ''),
                {
                  on: {
                    click: () => {
                      ctrl.setLesson(s.id);
                      ctrl.redraw();
                    },
                  },
                },
                [
                  ribbon(s, status, res),
                  h('div.stage-img.' + s.key),
                  h('div.text' + titleVerbosityClass(s.title), [
                    h('h3', s.title),
                    h('p.subtitle', s.subtitle),
                  ]),
                ],
              );
            }),
          ),
        ]);
      }),
      whatNext(ctrl),
    ]),
  ]);
}
