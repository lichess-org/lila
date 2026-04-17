import { view as cevalView } from 'lib/ceval';
import { hl, type LooseVNode, type VNode } from 'lib/view';

import type AnalyseCtrl from '@/ctrl';
import type { ConcealOf } from '@/interfaces';
import { renderNextChapter } from '@/study/nextChapter';
import { backToLiveView } from '@/study/relay/relayView';
import type * as studyDeps from '@/study/studyDeps';
import { addChapterId, renderResult, type ViewContext } from '@/view/components';

import explorerView from '../explorer/explorerView';
import { view as forkView } from '../fork';
import practiceView from '../practice/practiceView';
import retroView from '../retrospect/retroView';
import { view as actionMenu } from './actionMenu';

export function renderTools({ ctrl, deps, concealOf, allowVideo }: ViewContext, embeddedVideo?: LooseVNode) {
  const showCeval = ctrl.isCevalAllowed() && ctrl.showCeval();
  return hl(addChapterId(ctrl.study, 'div.analyse__tools'), [
    allowVideo && embeddedVideo,
    showCeval && cevalView.renderCeval(ctrl),
    showCeval && !ctrl.retro?.isSolving() && !ctrl.practice && cevalView.renderPvs(ctrl),
    renderMoveList(ctrl, deps, concealOf),
    deps?.gbEdit.running(ctrl) ? deps?.gbEdit.render(ctrl) : undefined,
    backToLiveView(ctrl),
    forkView(ctrl, concealOf),
    retroView(ctrl) || explorerView(ctrl) || practiceView(ctrl),
    ctrl.actionMenu() && actionMenu(ctrl),
  ]);
}

const renderMoveList = (ctrl: AnalyseCtrl, deps?: typeof studyDeps, concealOf?: ConcealOf): VNode =>
  hl('div.analyse__moves.areplay', { hook: ctrl.treeView.hook() }, [
    hl('div', [ctrl.treeView.render(concealOf), renderResult(ctrl)]),
    !ctrl.practice && !deps?.gbEdit.running(ctrl) && renderNextChapter(ctrl),
  ]);
