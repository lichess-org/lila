import * as licon from 'common/licon';
import { h } from 'snabbdom';
import { LearnCtrl } from './ctrl';
import { Stage, categs } from '../stage/list';
import { StageProgress } from '../learn';
import * as scoring from '../score';

export const view = (ctrl: LearnCtrl) => {
  return h('div', stageListView(ctrl));
};

const stageListView = (ctrl: LearnCtrl) => {
  return h('div.learn.learn--map', [
    // h('div.learn__side', ctrl.opts.side.view()),
    h('div.learn__main.learn-stages', [
      ...categs.map(function (categ) {
        return h('div.categ', [
          h('h2', ctrl.trans.noarg(categ.name)),
          h(
            'div.categ_stages',
            categ.stages.map(stage => {
              const stageProgress = ctrl.data.stages[stage.key];
              const complete = ctrl.isStageIdComplete(stage.id);
              const prevComplete = ctrl.isStageIdComplete(stage.id - 1);
              let status = 'future';
              if (complete) status = 'done';
              else if (prevComplete || stageProgress) status = 'ongoing';
              const title = ctrl.trans.noarg(stage.title);
              return h(
                `a.stage.${status}${titleVerbosityClass(title)}`,
                {
                  href: '/' + stage.id,
                  // config: m.route,
                },
                [
                  ribbon(ctrl, stage, status, stageProgress),
                  h('img', { attrs: { src: stage.image } }),
                  h('div.text', [h('h3', title), h('p.subtitle', ctrl.trans.noarg(stage.subtitle))]),
                ],
              );
            }),
          ),
        ]);
      }),
      // whatNext(ctrl),
    ]),
  ]);
};

function titleVerbosityClass(title: string) {
  return title.length > 13 ? (title.length > 18 ? ' vvv' : ' vv') : '';
}

// import m from '../mithrilFix';
// import * as util from '../util';
// import { MapCtrl } from './mapMain';
// import { StageProgress } from '../learn';

function makeStars(nb: number) {
  const stars = [];
  for (let i = 0; i < 4 - nb; i++) stars.push(h('i', { attrs: { 'data-icon': licon.Star } }));
  return stars;
}

function ribbon(ctrl: LearnCtrl, s: Stage, status: string, stageProgress: StageProgress) {
  if (status === 'future') return;
  let content;
  if (status === 'ongoing') {
    const p = ctrl.stageProgress(s);
    content = p[0] ? p.join(' / ') : ctrl.trans.noarg('play');
  } else content = makeStars(scoring.getStageRank(s, stageProgress.scores));
  if (status === 'future') return;
  return h('span.ribbon-wrapper', h(`span.ribbon.${status}`, content));
}

// function whatNext(ctrl: MapCtrl) {
//   const makeStage = function (href: string, img: string, title: string, subtitle: string, done?: boolean) {
//     const transTitle = ctrl.trans.noarg(title);
//     return m(
//       'a',
//       {
//         class: 'stage done' + titleVerbosityClass(transTitle),
//         href: href,
//       },
//       [
//         done ? m('span.ribbon-wrapper', m('span.ribbon.done', makeStars(1))) : null,
//         m('img', {
//           src: util.assetUrl + 'images/learn/' + img + '.svg',
//         }),
//         m('div.text', [m('h3', transTitle), m('p.subtitle', ctrl.trans.noarg(subtitle))]),
//       ],
//     );
//   };
//   const userId = ctrl.data._id;
//   return m('div.categ.what_next', [
//     m('h2', ctrl.trans.noarg('whatNext')),
//     m('p', ctrl.trans.noarg('youKnowHowToPlayChess')),
//     m('div.categ_stages', [
//       userId
//         ? makeStage('/@/' + userId, 'beams-aura', 'register', 'getAFreeLichessAccount', true)
//         : makeStage('/signup', 'beams-aura', 'register', 'getAFreeLichessAccount'),
//       makeStage('/practice', 'robot-golem', 'practice', 'learnCommonChessPositions'),
//       makeStage('/training', 'bullseye', 'puzzles', 'exerciseYourTacticalSkills'),
//       makeStage('/video', 'tied-scroll', 'videos', 'watchInstructiveChessVideos'),
//       makeStage('/#hook', 'sword-clash', 'playPeople', 'opponentsFromAroundTheWorld'),
//       makeStage('/#ai', 'vintage-robot', 'playMachine', 'testYourSkillsWithTheComputer'),
//     ]),
//   ]);
// }
