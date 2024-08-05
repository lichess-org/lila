import * as licon from 'common/licon';
import { h, VNode } from 'snabbdom';
import { LearnCtrl } from './ctrl';
import { Stage } from './stage/list';
import * as stages from './stage/list';
import { StageProgress } from './learn';
import * as scoring from './score';
import * as util from './util';
import { mapSideView } from './mapSideView';
import { hashHref } from './hashRouting';
import { runView } from './run/runView';

export const view = (ctrl: LearnCtrl): VNode => (ctrl.inStage() ? runView(ctrl) : mapView(ctrl));

type Status = 'future' | 'done' | 'ongoing';

const mapView = (ctrl: LearnCtrl) =>
  h('div.learn.learn--map', [
    h('div.learn__side', mapSideView(ctrl)),
    h('div.learn__main.learn-stages', [
      ...stages.categs.map(categ =>
        h('div.categ', [
          h('h2', ctrl.trans.noarg(categ.name)),
          h(
            'div.categ_stages',
            categ.stages.map(stage => {
              const stageProgress = ctrl.data.stages[stage.key];
              const complete = ctrl.isStageIdComplete(stage.id);
              const prevComplete = ctrl.isStageIdComplete(stage.id - 1);
              const status: Status = complete ? 'done' : prevComplete || stageProgress ? 'ongoing' : 'future';
              const title = ctrl.trans.noarg(stage.title);
              return h(
                `a.stage.${status}.${titleVerbosityClass(title)}`,
                { attrs: { href: hashHref(stage.id) } },
                [
                  status != 'future' ? ribbon(ctrl, stage, status, stageProgress) : undefined,
                  h('img', { attrs: { src: stage.image } }),
                  h('div.text', [h('h3', title), h('p.subtitle', ctrl.trans.noarg(stage.subtitle))]),
                ],
              );
            }),
          ),
        ]),
      ),
      whatNext(ctrl),
    ]),
  ]);

const titleVerbosityClass = (title: string) => (title.length > 13 ? (title.length > 18 ? 'vvv' : 'vv') : '');

const makeStars = (rank: scoring.Rank): VNode[] =>
  Array(4 - rank).fill(h('i', { attrs: { 'data-icon': licon.Star } }));

const ongoingStr = (ctrl: LearnCtrl, s: Stage): string => {
  const progress = ctrl.stageProgress(s);
  return progress[0] ? progress.join(' / ') : ctrl.trans.noarg('play');
};

const ribbon = (ctrl: LearnCtrl, s: Stage, status: Exclude<Status, 'future'>, stageProgress: StageProgress) =>
  h(
    'span.ribbon-wrapper',
    h(
      `span.ribbon.${status}`,
      status == 'ongoing' ? ongoingStr(ctrl, s) : makeStars(scoring.getStageRank(s, stageProgress.scores)),
    ),
  );

function whatNext(ctrl: LearnCtrl) {
  const makeStage = (href: string, img: string, title: string, subtitle: string, done?: boolean) => {
    const transTitle = ctrl.trans.noarg(title);
    return h(
      `a.stage.done.${titleVerbosityClass(transTitle)}`,
      {
        attrs: { href: href },
      },
      [
        done ? h('span.ribbon-wrapper', h('span.ribbon.done', makeStars(1))) : null,
        h('img', { attrs: { src: util.assetUrl + 'images/learn/' + img + '.svg' } }),
        h('div.text', [h('h3', transTitle), h('p.subtitle', ctrl.trans.noarg(subtitle))]),
      ],
    );
  };
  const userId = ctrl.data._id;
  return h('div.categ.what_next', [
    h('h2', ctrl.trans.noarg('whatNext')),
    h('p', ctrl.trans.noarg('youKnowHowToPlayChess')),
    h('div.categ_stages', [
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
