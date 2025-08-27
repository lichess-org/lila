import { renderEval } from 'lib/ceval/ceval';
import { repeater } from 'lib';
import * as licon from 'lib/licon';
import { type VNode, type LooseVNode, onInsert, hl } from 'lib/snabbdom';
import { addPointerListeners, displayColumns } from 'lib/device';
import * as control from '../control';
import type AnalyseCtrl from '../ctrl';

type Action =
  | 'first'
  | 'prev'
  | 'next'
  | 'last'
  | 'opening-explorer'
  | 'menu'
  | 'analysis'
  | 'ceval-practice'
  | 'ceval';

export function renderControls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0],
    showingTool = ctrl.showingTool();

  return hl(
    'div.analyse__controls.analyse-controls',
    {
      hook: onInsert(el =>
        addPointerListeners(
          el,
          e => clickControl(ctrl, e),
          e => holdControl(ctrl, e),
        ),
      ),
    },
    [
      ctrl.studyPractice
        ? [
            hl('button.fbt', {
              attrs: { title: i18n.site.analysis, 'data-act': 'analysis', 'data-icon': licon.Microscope },
            }),
          ]
        : [
            hl('button.fbt', {
              attrs: {
                title: i18n.site.openingExplorerAndTablebase,
                'data-act': 'opening-explorer',
                'data-icon': licon.Book,
              },
              class: {
                hidden: !ctrl.explorer.allowed() || !!ctrl.retro,
                active: showingTool === 'opening-explorer',
              },
            }),
            ctrl.ceval.allowed() && [
              !(ctrl.isEmbed || ctrl.isGamebook()) &&
                hl('button.fbt', {
                  attrs: {
                    title: i18n.site.practiceWithComputer,
                    'data-act': 'ceval-practice',
                    'data-icon': licon.Bullseye,
                  },
                  class: {
                    hidden: !!ctrl.retro,
                    active: !!ctrl.practice && !showingTool,
                    latent: !!ctrl.practice && !!showingTool,
                  },
                }),
              renderMobileCevalTab(ctrl),
            ],
          ],
      hl('div.jumps', [
        jumpButton(licon.JumpFirst, 'first', canJumpPrev),
        jumpButton(licon.JumpPrev, 'prev', canJumpPrev),
        jumpButton(licon.JumpNext, 'next', canJumpNext),
        jumpButton(licon.JumpLast, 'last', ctrl.node !== ctrl.mainline[ctrl.mainline.length - 1]),
      ]),
      ctrl.studyPractice
        ? hl('div.noop')
        : hl('button.fbt', {
            class: { active: showingTool === 'action-menu' },
            attrs: { title: i18n.site.menu, 'data-act': 'menu', 'data-icon': licon.Hamburger },
          }),
    ],
  );
}

function renderMobileCevalTab(ctrl: AnalyseCtrl): LooseVNode {
  if (displayColumns() !== 1) return undefined;
  const cevalMode = ctrl.ceval.enabled() && !ctrl.practice,
    showingTool = ctrl.showingTool(),
    ev = ctrl.node.ceval ?? (ctrl.showComputer() ? ctrl.node.eval : undefined),
    evalstr = ev?.cp !== undefined ? renderEval(ev.cp) : ev?.mate ? '#' + ev.mate : '',
    active = cevalMode && !showingTool,
    latent = cevalMode && !!showingTool;
  return hl(
    'button.fbt',
    {
      attrs: { 'data-act': 'ceval', 'data-icon': licon.Stockfish },
      class: { active, latent },
    },
    evalstr &&
      (!cevalMode || latent) &&
      !ctrl.practice &&
      !ctrl.isGamebook() &&
      !ctrl.retro &&
      hl('eval', evalstr),
  );
}

function holdControl(ctrl: AnalyseCtrl, e: PointerEvent) {
  if (!(e.target instanceof HTMLElement)) return;
  const action = e.target.closest<HTMLElement>('[data-act]')?.dataset.act as Action;
  if (action === 'prev' || action === 'next') {
    repeater(() => {
      control[action](ctrl);
      ctrl.redraw();
    });
  } else clickControl(ctrl, e);
}

function clickControl(ctrl: AnalyseCtrl, e: PointerEvent) {
  if (!(e.target instanceof HTMLElement)) return;
  const action = e.target.closest<HTMLElement>('[data-act]')?.dataset.act as Action;
  if (!action) return;
  if (action === 'prev') control.prev(ctrl);
  else if (action === 'next') control.next(ctrl);
  else if (action === 'first') control.first(ctrl);
  else if (action === 'last') control.last(ctrl);
  else if (action === 'opening-explorer') ctrl.toggleExplorer();
  else if (action === 'menu') ctrl.toggleActionMenu();
  else if (action === 'analysis') {
    if (ctrl.studyPractice) window.open(ctrl.studyPractice.analysisUrl(), '_blank');
  } else ctrl.clickMobileCevalTab(action);
  ctrl.redraw();
}

const jumpButton = (icon: string, effect: string, enabled: boolean): VNode =>
  hl('button.fbt.move', { class: { disabled: !enabled }, attrs: { 'data-act': effect, 'data-icon': icon } });
