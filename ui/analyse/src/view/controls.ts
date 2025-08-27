import { renderEval, view as cevalView } from 'lib/ceval/ceval';
import { repeater } from 'lib';
import * as licon from 'lib/licon';
import { type VNode, type LooseVNode, onInsert, hl } from 'lib/snabbdom';
import { displayColumns, isTouchDevice } from 'lib/device';
import { addPointerListeners } from 'lib/pointer';
import * as control from '../control';
import type AnalyseCtrl from '../ctrl';
import { info } from 'lib/view/dialogs';

type Action =
  | 'first'
  | 'prev'
  | 'next'
  | 'last'
  | 'scrub'
  | 'opening-explorer'
  | 'menu'
  | 'analysis'
  | 'mobile-mode';

type MobileMode = 'ceval' | 'practice' | 'retro';

export function renderControls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0],
    withScrub = isTouchDevice();

  return hl(
    'div.analyse__controls.analyse-controls',
    {
      hook: onInsert(el =>
        addPointerListeners(el, {
          click: e => clickControl(ctrl, e),
          hscrub: withScrub ? dx => scrubControl(ctrl, dx) : undefined,
          hold: withScrub ? undefined : e => holdControl(ctrl, e),
        }),
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
            renderMobileCevalTab(ctrl),
            hl('button.fbt', {
              attrs: {
                title: i18n.site.openingExplorerAndTablebase,
                'data-act': 'opening-explorer',
                'data-icon': licon.Book,
              },
              class: {
                hidden: !ctrl.explorer.allowed() || (!!ctrl.retro && displayColumns() > 1),
                active: ctrl.activeTool() === 'opening-explorer',
              },
            }),
            displayColumns() > 1 && renderPracticeTab(ctrl),
          ],
      hl('div.jumps', [
        displayColumns() > 1 && jumpButton(licon.JumpFirst, 'first', canJumpPrev),
        jumpButton(licon.LessThan, 'prev', canJumpPrev),
        displayColumns() === 1 && hl('i.scrub', { attrs: { 'data-act': 'scrub' } }, licon.InfoCircle),
        jumpButton(licon.GreaterThan, 'next', canJumpNext),
        displayColumns() > 1 &&
          jumpButton(licon.JumpLast, 'last', ctrl.node !== ctrl.mainline[ctrl.mainline.length - 1]),
      ]),
      [
        ctrl.studyPractice
          ? hl('div.noop')
          : hl('button.fbt', {
              class: { active: ctrl.activeTool() === 'action-menu' },
              attrs: { title: i18n.site.menu, 'data-act': 'menu', 'data-icon': licon.Hamburger },
            }),
      ],
    ],
  );
}

function renderPracticeTab(ctrl: AnalyseCtrl): LooseVNode {
  return (
    !ctrl.retro &&
    hl('button.fbt', {
      attrs: {
        title: i18n.site.practiceWithComputer,
        'data-act': 'mobile-mode',
        'data-mode': 'practice',
        'data-icon': licon.Bullseye,
      },
      class: {
        active: !!ctrl.practice && !ctrl.activeTool(),
        latent: !!ctrl.practice && !!ctrl.activeTool(),
      },
    })
  );
}

function renderMobileCevalTab(ctrl: AnalyseCtrl): LooseVNode {
  if (displayColumns() > 1) return undefined;
  const mobileMode = !!ctrl.practice ? 'practice' : !!ctrl.retro ? 'retro' : 'ceval',
    ev = ctrl.node.ceval ?? (ctrl.showFishnetAnalysis() ? ctrl.node.eval : undefined),
    evalstr = ev?.cp !== undefined ? renderEval(ev.cp) : ev?.mate ? '#' + ev.mate : '',
    active = ctrl.activeMobileMode() && !ctrl.activeTool(),
    latent = ctrl.activeMobileMode() && !!ctrl.activeTool();

  return hl(
    'button.fbt',
    {
      key: 'mobile-mode',
      attrs: { 'data-act': 'mobile-mode', 'data-mode': mobileMode },
      class: { active, latent, computing: ctrl.ceval.isComputing },
    },
    [
      mobileMode === 'ceval' && [
        cevalView.renderCevalSwitch(ctrl),
        evalstr && ctrl.showAnalysis() && !ctrl.isGamebook() && hl('eval', evalstr),
      ],
      hl('div.bar'),
      mobileMode === 'retro' && ctrl.retro?.completion().join('/'),
    ],
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
  //else if (action === 'line') ctrl.userJumpIfCan(ctrl.idbTree.nextLine(), true);
  else if (action === 'next') control.next(ctrl);
  else if (action === 'first') control.first(ctrl);
  else if (action === 'last') control.last(ctrl);
  else if (action === 'scrub') helpScrub();
  else if (action === 'opening-explorer') ctrl.toggleExplorer();
  else if (action === 'menu') ctrl.toggleActionMenu();
  else if (action === 'analysis') window.open(ctrl.studyPractice!.analysisUrl(), '_blank');
  else if (action === 'mobile-mode' && !e.target.closest<HTMLElement>('.switch')) {
    const mode = e.target.dataset.mode as MobileMode;
    if (ctrl.activeTool()) {
      ctrl.explorer.enabled(false);
      ctrl.actionMenu(false);
      if (ctrl.showCeval() || mode !== 'ceval') return ctrl.redraw();
    }
    if (mode === 'practice') ctrl.togglePractice();
    else if (mode === 'retro') ctrl.toggleRetro();
    else if (!ctrl.showCeval()) ctrl.cevalEnabled(true);
    else ctrl.showCevalProp(false);
  }
  ctrl.redraw();
}

let last: number[] = [];

function scrubControl(ctrl: AnalyseCtrl, dx: number | 'pointerup') {
  if (dx === 'pointerup') {
    const v = last.slice(-3).reduce((a, b) => a + b, 0) / Math.min(last.length, 3);
    if (v > 16) control.last(ctrl);
    else if (v < -16) control.first(ctrl);
    last = [];
  } else {
    if (dx > 0) control.next(ctrl);
    else control.prev(ctrl);
    last.push(dx);
  }
  ctrl.redraw();
}

const jumpButton = (icon: string, effect: string, enabled: boolean): VNode =>
  hl('button.fbt.move', { class: { disabled: !enabled }, attrs: { 'data-act': effect, 'data-icon': icon } });

function helpScrub() {
  info(
    'Swipe left or right across the button bar to go to game start or end. ' +
      'Move your finger slowly to scrub through moves one by one.',
  );
}
