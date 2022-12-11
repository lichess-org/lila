import m from '../mithrilFix';
import * as util from '../util';
import * as scoring from '../score';
import * as stages from '../stage/list';
import { MapCtrl } from './mapMain';
import { StageProgress } from '../main';

function makeStars(nb: number) {
  const stars = [];
  for (let i = 0; i < 4 - nb; i++)
    stars.push(
      m('i', {
        'data-icon': '',
      })
    );
  return stars;
}

function ribbon(ctrl: MapCtrl, s: stages.Stage, status: string, res: StageProgress) {
  if (status === 'future') return;
  let content;
  if (status === 'ongoing') {
    const p = ctrl.stageProgress(s);
    content = p[0] ? p.join(' / ') : ctrl.trans.noarg('play');
  } else content = makeStars(scoring.getStageRank(s, res.scores));
  if (status === 'future') return;
  return m(
    'span.ribbon-wrapper',
    m(
      'span.ribbon',
      {
        class: status,
      },
      content
    )
  );
}

function whatNext(ctrl: MapCtrl) {
  const makeStage = function (href: string, img: string, title: string, subtitle: string, done?: boolean) {
    const transTitle = ctrl.trans.noarg(title);
    return m(
      'a',
      {
        class: 'stage done' + titleVerbosityClass(transTitle),
        href: href,
      },
      [
        done ? m('span.ribbon-wrapper', m('span.ribbon.done', makeStars(1))) : null,
        m('img', {
          src: util.assetUrl + 'images/learn/' + img + '.svg',
        }),
        m('div.text', [m('h3', transTitle), m('p.subtitle', ctrl.trans.noarg(subtitle))]),
      ]
    );
  };
  const userId = ctrl.data._id;
  return m('div.categ.what_next', [
    m('h2', ctrl.trans.noarg('whatNext')),
    m('p', ctrl.trans.noarg('youKnowHowToPlayChess')),
    m('div.categ_stages', [
      userId
        ? makeStage('/@/' + userId, 'beams-aura', 'register', 'getAFreeLichessAccount', true)
        : makeStage('/signup', 'beams-aura', 'register', 'getAFreeLichessAccount'),
      makeStage('/practice', 'robot-golem', 'practice', 'learnCommonChessPositions'),
      makeStage('/training', 'bullseye', 'puzzles', 'exerciseYourTacticalSkills'),
      makeStage('/video', 'tied-scroll', 'videos', 'watchInstructiveChessVideos'),
      makeStage('/#hook', 'sword-clash', 'playPeople', 'opponentsFromAroundTheWorld'),
      makeStage('/#ai', 'vintage-robot', 'playMachine', 'testYourSkillsWithTheComputer'),
    ]),
  ]);
}

function titleVerbosityClass(title: string) {
  return title.length > 13 ? (title.length > 18 ? ' vvv' : ' vv') : '';
}

export default function (ctrl: MapCtrl) {
  return m('div.learn.learn--map', [
    m('div.learn__side', ctrl.opts.side.view()),
    m('div.learn__main.learn-stages', [
      ...stages.categs.map(function (categ) {
        return m('div.categ', [
          m('h2', ctrl.trans.noarg(categ.name)),
          m(
            'div.categ_stages',
            categ.stages.map(function (s) {
              const res = ctrl.data.stages[s.key];
              const complete = ctrl.isStageIdComplete(s.id);
              const prevComplete = ctrl.isStageIdComplete(s.id - 1);
              let status = 'future';
              if (complete) status = 'done';
              else if (prevComplete || res) status = 'ongoing';
              const title = ctrl.trans.noarg(s.title);
              return m(
                'a',
                {
                  class: 'stage ' + status + titleVerbosityClass(title),
                  href: '/' + s.id,
                  config: m.route,
                },
                [
                  ribbon(ctrl, s, status, res),
                  m('img', {
                    src: s.image,
                  }),
                  m('div.text', [m('h3', title), m('p.subtitle', ctrl.trans.noarg(s.subtitle))]),
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
