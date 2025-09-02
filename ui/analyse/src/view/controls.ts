import { renderEval, view as cevalView } from 'lib/ceval/ceval';
import { repeater } from 'lib';
import * as licon from 'lib/licon';
import { type VNode, type LooseVNode, onInsert, hl } from 'lib/snabbdom';
import { displayColumns, isTouchDevice } from 'lib/device';
import { addPointerListeners } from 'lib/pointer';
import * as control from '../control';
import type AnalyseCtrl from '../ctrl';
import { domDialog } from 'lib/view/dialog';
import { myUserId } from 'lib/common';

type Action =
  | 'first'
  | 'prev'
  | 'next'
  | 'last'
  | 'scrub-help'
  | 'opening-explorer'
  | 'menu'
  | 'analysis'
  | 'engine-mode';

type EngineMode = 'ceval' | 'practice' | 'retro';

export function renderControls(ctrl: AnalyseCtrl) {
  const canJumpPrev = ctrl.path !== '',
    canJumpNext = !!ctrl.node.children[0];

  return hl(
    'div.analyse__controls.analyse-controls',
    {
      hook: onInsert(el =>
        addPointerListeners(el, {
          click: e => clickControl(ctrl, e),
          hscrub: isTouchDevice() ? dx => scrubControl(ctrl, dx) : undefined,
          hold: e => holdControl(ctrl, e),
        }),
      ),
    },
    [
      ctrl.study?.practice
        ? [
            hl('button.fbt', {
              attrs: { title: i18n.site.analysis, 'data-act': 'analysis', 'data-icon': licon.Microscope },
            }),
          ]
        : [
            displayColumns() === 1 && ctrl.isCevalAllowed() && renderMobileCevalTab(ctrl),
            hl('button.fbt', {
              attrs: {
                title: i18n.site.openingExplorerAndTablebase,
                'data-act': 'opening-explorer',
                'data-icon': licon.Book,
              },
              class: {
                hidden: !ctrl.explorer.allowed() || (!!ctrl.retro && !isMobileUi()),
                active: ctrl.activeControlBarTool() === 'opening-explorer',
              },
            }),
            displayColumns() > 1 && !ctrl.retro && !ctrl.ongoing && renderPracticeTab(ctrl),
          ],
      hl('div.jumps', [
        (!isMobileUi() || ctrl.study?.practice) && jumpButton(licon.JumpFirst, 'first', canJumpPrev),
        jumpButton(licon.LessThan, 'prev', canJumpPrev),
        isMobileUi() &&
          !scrubHelpAcknowledged() &&
          !ctrl.study?.practice &&
          hl('i.scrub-help', { attrs: { 'data-act': 'scrub-help' } }, licon.InfoCircle),
        jumpButton(licon.GreaterThan, 'next', canJumpNext),
        (!isMobileUi() || ctrl.study?.practice) &&
          jumpButton(licon.JumpLast, 'last', ctrl.node !== ctrl.mainline[ctrl.mainline.length - 1]),
      ]),
      [
        ctrl.study?.practice
          ? hl('div.noop')
          : hl('button.fbt', {
              class: { active: ctrl.activeControlBarTool() === 'action-menu' },
              attrs: { title: i18n.site.menu, 'data-act': 'menu', 'data-icon': licon.Hamburger },
            }),
      ],
    ],
  );
}

function renderPracticeTab(ctrl: AnalyseCtrl): LooseVNode {
  return hl('button.fbt', {
    attrs: {
      title: i18n.site.practiceWithComputer,
      'data-act': 'engine-mode',
      'data-mode': 'practice',
      'data-icon': licon.Bullseye,
    },
    class: {
      active: !!ctrl.practice && !ctrl.activeControlBarTool(),
      latent: !!ctrl.practice && !!ctrl.activeControlBarTool(),
    },
  });
}

function renderMobileCevalTab(ctrl: AnalyseCtrl): LooseVNode {
  const engineMode = !!ctrl.practice ? 'practice' : !!ctrl.retro ? 'retro' : 'ceval',
    ev = ctrl.node.ceval ?? (ctrl.showFishnetAnalysis() ? ctrl.node.eval : undefined),
    evalstr = ev?.cp !== undefined ? renderEval(ev.cp) : ev?.mate ? '#' + ev.mate : '',
    active = ctrl.activeControlBarMode() && !ctrl.activeControlBarTool(),
    latent = ctrl.activeControlBarMode() && !!ctrl.activeControlBarTool();

  return hl(
    'button.fbt',
    {
      key: 'engine-mode',
      attrs: { 'data-act': 'engine-mode', 'data-mode': engineMode },
      class: { active, latent, computing: ctrl.ceval.isComputing },
    },
    [
      engineMode === 'ceval' && [
        hl('div.bar'),
        cevalView.renderCevalSwitch(ctrl),
        evalstr && ctrl.showAnalysis() && !ctrl.isGamebook() && hl('eval', evalstr),
      ],
      engineMode === 'retro' && ctrl.retro?.completion().join('/'),
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
  else if (action === 'next') control.next(ctrl);
  else if (action === 'first') control.first(ctrl);
  else if (action === 'last') control.last(ctrl);
  else if (action === 'scrub-help') scrubHelp(ctrl);
  else if (action === 'opening-explorer') ctrl.toggleExplorer();
  else if (action === 'menu') ctrl.toggleActionMenu();
  else if (action === 'analysis') window.open(ctrl.study?.practice?.analysisUrl(), '_blank');
  else if (action === 'engine-mode' && !e.target.closest<HTMLElement>('.switch')) {
    const mode = e.target.dataset.mode as EngineMode;
    if (ctrl.activeControlBarTool()) {
      ctrl.explorer.enabled(false);
      ctrl.actionMenu(false);
      if (ctrl.showCeval() || mode !== 'ceval') return ctrl.redraw();
    }
    if (mode === 'practice') ctrl.togglePractice();
    else if (mode === 'retro') ctrl.toggleRetro();
    else ctrl.showCeval(!ctrl.showCeval());
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

function isMobileUi() {
  return displayColumns() === 1 && isTouchDevice();
}

function scrubHelp(ctrl: AnalyseCtrl) {
  domDialog({
    htmlText: $html`
      <p>
        Swipe left or right on the button bar below the board to go to game start or end.
      </p>
      <p>
        Move your finger slowly to scrub through moves one by one.
      </p>
      <button class="button">${i18n.site.ok}</button>`,
    actions: [{ selector: 'button', result: 'ok' }],
    noCloseButton: true,
    show: true,
  }).then(dlg => {
    scrubHelpAcknowledged(dlg.returnValue === 'ok');
    ctrl.redraw();
  });
}

function scrubHelpAcknowledged(ack?: boolean) {
  const key = `analyse.help.scrub-acknowledged.${myUserId() ?? 'anon'}`;
  if (ack === undefined) return !!localStorage.getItem(key);
  if (ack) localStorage.setItem(key, '1');
  return ack;
}
