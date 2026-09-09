import { repeater, blurIfPrimaryClick } from 'lib';
import { renderEval, view as cevalView } from 'lib/ceval';
import { displayColumns, isTouchDevice } from 'lib/device';
import { licon, type LiconValue } from 'lib/licon';
import { addPointerListeners } from 'lib/pointer';
import { type VNode, type LooseVNode, onInsert, hl } from 'lib/view';

import type AnalyseCtrl from '../ctrl';

type Action = 'first' | 'prev' | 'next' | 'last' | 'opening-explorer' | 'menu' | 'analysis' | 'engine-mode';

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
          hold: e => holdControl(ctrl, e),
        }),
      ),
    },
    [
      hl('div.jumps', [
        jumpButton(licon.JumpFirst, 'first', canJumpPrev),
        jumpButton(licon.LessThan, 'prev', canJumpPrev),
        jumpButton(licon.GreaterThan, 'next', canJumpNext),
        jumpButton(licon.JumpLast, 'last', ctrl.node !== ctrl.mainline[ctrl.mainline.length - 1]),
      ]),
      ctrl.study?.practice
        ? hl('button.fbt', {
            attrs: { title: i18n.site.analysis, 'data-act': 'analysis', 'data-icon': licon.Microscope },
          })
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
      ctrl.study?.practice
        ? hl('div.noop')
        : hl('button.fbt', {
            class: { active: ctrl.activeControlBarTool() === 'action-menu' },
            attrs: { title: i18n.site.menu, 'data-act': 'menu', 'data-icon': licon.Hamburger },
          }),
    ],
  );
}

const renderPracticeTab = (ctrl: AnalyseCtrl): LooseVNode =>
  hl('button.fbt', {
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

function renderMobileCevalTab(ctrl: AnalyseCtrl): LooseVNode {
  const engineMode = ctrl.activeControlMode() || 'ceval',
    ev = ctrl.allowedEval() || undefined,
    evalstr = ev?.cp !== undefined ? renderEval(ev.cp) : ev?.mate ? '#' + ev.mate : '',
    active = ctrl.activeControlMode() && !ctrl.activeControlBarTool(),
    latent = ctrl.activeControlMode() && !!ctrl.activeControlBarTool();

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
        evalstr && ctrl.showEvaluation() && !ctrl.isGamebook() && hl('eval', evalstr),
      ],
      engineMode === 'practice' && evalstr && hl('eval', evalstr),
      engineMode === 'retro' && ctrl.retro?.completion().join('/'),
    ],
  );
}

function holdControl(ctrl: AnalyseCtrl, e: PointerEvent) {
  if (!(e.target instanceof HTMLElement)) return;
  const action = e.target.closest<HTMLElement>('[data-act]')?.dataset.act as Action;
  if (action === 'prev' || action === 'next') {
    repeater(() => {
      ctrl.navigate[action]();
      ctrl.redraw();
    });
  } else clickControl(ctrl, e);
}

function clickControl(ctrl: AnalyseCtrl, e: PointerEvent) {
  if (!(e.target instanceof HTMLElement)) return;
  const action = e.target.closest<HTMLElement>('[data-act]')?.dataset.act as Action;
  if (!action) return;
  if (action === 'prev') ctrl.navigate.prev();
  else if (action === 'next') ctrl.navigate.next();
  else if (action === 'first') ctrl.navigate.first();
  else if (action === 'last') ctrl.navigate.last();
  else if (action === 'opening-explorer') ctrl.toggleExplorer();
  else if (action === 'menu') ctrl.toggleActionMenu();
  else if (action === 'analysis') window.open(ctrl.study?.practice?.analysisUrl(), '_blank');
  else if (action === 'engine-mode' && !e.target.closest<HTMLElement>('.cmn-toggle')) {
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
  blurIfPrimaryClick(e);
  ctrl.redraw();
}

const jumpButton = (icon: LiconValue, effect: string, enabled: boolean): VNode =>
  hl('button.fbt.move', { attrs: { disabled: !enabled, 'data-act': effect, 'data-icon': icon } });

const isMobileUi = (): boolean => displayColumns() === 1 && isTouchDevice();
