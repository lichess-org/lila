import * as licon from 'common/licon';
import { h, type VNode } from 'snabbdom';
import type { LearnCtrl } from './ctrl';
import { type Stage, categs } from './stage/list';
import type { StageProgress } from './learn';
import * as scoring from './score';
import { assetUrl } from './util';
import { mapSideView } from './mapSideView';
import { hashHref } from './hashRouting';
import { runView } from './run/runView';

export const view = (ctrl: LearnCtrl): VNode => (ctrl.inStage() ? runView(ctrl) : mapView(ctrl));

type Status = 'future' | 'done' | 'ongoing';

const mapView = (ctrl: LearnCtrl) =>
  h('div.learn.learn--map', [
    h('div.learn__side', mapSideView(ctrl)),
    h('div.learn__main.learn-stages', [
      ...categs.map(categ =>
        h('div.categ', [
          h('h2', categ.name),
          h(
            'div.categ_stages',
            categ.stages.map(stage => {
              const stageProgress = ctrl.data.stages[stage.key];
              const complete = ctrl.isStageIdComplete(stage.id);
              const prevComplete = ctrl.isStageIdComplete(stage.id - 1);
              const status: Status = complete ? 'done' : prevComplete || stageProgress ? 'ongoing' : 'future';
              const title = stage.title;
              return h(
                `a.stage.${status}.${titleVerbosityClass(title)}`,
                { attrs: { href: hashHref(stage.id) } },
                [
                  status !== 'future' ? ribbon(ctrl, stage, status, stageProgress) : undefined,
                  h('img', { attrs: { src: stage.image } }),
                  h('div.text', [h('h3', title), h('p.subtitle', stage.subtitle)]),
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
  return progress[0] ? progress.join(' / ') : i18n.learn.play;
};

const ribbon = (ctrl: LearnCtrl, s: Stage, status: Exclude<Status, 'future'>, stageProgress: StageProgress) =>
  h(
    'span.ribbon-wrapper',
    h(
      `span.ribbon.${status}`,
      status === 'ongoing' ? ongoingStr(ctrl, s) : makeStars(scoring.getStageRank(s, stageProgress.scores)),
    ),
  );

function whatNext(ctrl: LearnCtrl) {
  const makeStage = (href: string, img: string, title: string, subtitle: string, done?: boolean) => {
    const transTitle = title;
    return h(`a.stage.done.${titleVerbosityClass(transTitle)}`, { attrs: { href: href } }, [
      done ? h('span.ribbon-wrapper', h('span.ribbon.done', makeStars(1))) : null,
      h('img', { attrs: { src: assetUrl + 'images/learn/' + img + '.svg' } }),
      h('div.text', [h('h3', transTitle), h('p.subtitle', subtitle)]),
    ]);
  };
  const userId = ctrl.data._id;
  return h('div.categ.what_next', [
    h('h2', i18n.learn.whatNext),
    h('p', i18n.learn.youKnowHowToPlayChess),
    h('div.categ_stages', [
      userId
        ? makeStage(
            '/@/' + userId,
            'beams-aura',
            i18n.learn.register,
            i18n.learn.getAFreeLichessAccount,
            true,
          )
        : makeStage('/signup', 'beams-aura', i18n.learn.register, i18n.learn.getAFreeLichessAccount),
      makeStage('/practice', 'robot-golem', i18n.learn.practice, i18n.learn.learnCommonChessPositions),
      makeStage('/training', 'bullseye', i18n.learn.puzzles, i18n.learn.exerciseYourTacticalSkills),
      makeStage(
        '/video?tags=beginner',
        'tied-scroll',
        i18n.learn.videos,
        i18n.learn.watchInstructiveChessVideos,
      ),
      makeStage('/#hook', 'sword-clash', i18n.learn.playPeople, i18n.learn.opponentsFromAroundTheWorld),
      makeStage('/#ai', 'vintage-robot', i18n.learn.playMachine, i18n.learn.testYourSkillsWithTheComputer),
    ]),
  ]);
}
