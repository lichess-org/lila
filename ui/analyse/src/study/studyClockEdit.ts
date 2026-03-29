import { h, type VNode } from 'snabbdom';

import { defined } from 'lib';
import type { TreePath } from 'lib/tree/types';

import type AnalyseCtrl from '../ctrl';
import renderClocks, { type ClockDataAtPath, type ClockSlotOpts, slotNameForIndex } from '../view/clocks';
import { renderClockEditInput } from './studyClockEditView';

/** Parse time string (H:MM:SS, MM:SS, M:SS, SS, optionally with tenths/hundredths) to centis. Returns undefined if invalid. */
export function parseTimeToCentis(str: string): number | undefined {
  const s = str.trim();
  if (s === '' || s === '--:--') return undefined;
  const parts = s.split(':').map(p => p.trim());
  if (parts.some(p => p === '')) return undefined;
  const integerSegment = /^\d+$/;
  const lastPart = parts[parts.length - 1];
  if (parts.slice(0, -1).some(p => !integerSegment.test(p))) return undefined;
  if (!/^\d+(\.\d+)?$/.test(lastPart) || lastPart.endsWith('.')) return undefined;
  const lastNum = parseFloat(lastPart);
  if (isNaN(lastNum) || lastNum < 0) return undefined;
  const wholeParts = parts.slice(0, -1).map(p => parseInt(p, 10));
  if (wholeParts.some(n => isNaN(n) || n < 0)) return undefined;
  const secSegment = Math.floor(lastNum);
  if (secSegment >= 60) return undefined;
  if (parts.length === 2 && parseInt(parts[0], 10) >= 60) return undefined;
  if (parts.length === 3 && parseInt(parts[1], 10) >= 60) return undefined;
  let seconds = 0;
  if (parts.length === 1) {
    seconds = lastNum;
  } else if (parts.length === 2) {
    seconds = parseInt(parts[0], 10) * 60 + lastNum;
  } else if (parts.length === 3) {
    seconds = parseInt(parts[0], 10) * 3600 + parseInt(parts[1], 10) * 60 + lastNum;
  } else {
    return undefined;
  }
  return Math.round(seconds * 100);
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;

/** Format centis for the edit input so round-trip matches. */
export function formatClockFromCentis(centis: number): string {
  if (centis <= 0) return '0:00:00';
  const date = new Date(centis * 10),
    hours = Math.floor(centis / 360000),
    mins = date.getUTCMinutes(),
    secs = date.getUTCSeconds(),
    remainder = centis % 100;
  const secStr =
    remainder === 0
      ? pad2(secs)
      : remainder % 10 === 0
        ? `${pad2(secs)}.${remainder / 10}`
        : `${pad2(secs)}.${remainder < 10 ? '0' + remainder : remainder}`;
  if (hours > 0) return `${hours}:${pad2(mins)}:${secStr}`;
  if (mins > 0) return `${mins}:${secStr}`;
  return remainder > 0
    ? remainder % 10 === 0
      ? `${secs}.${remainder / 10}`
      : `${secs}.${remainder < 10 ? '0' + remainder : remainder}`
    : `${secs}`;
}

export default class StudyClockEdit {
  error: boolean = false;
  shakeTrigger: number = 0;

  constructor(
    readonly slot: 'top' | 'bottom',
    readonly path: TreePath,
    public value: string,
    private redraw: Redraw,
  ) {}

  // Classify the current input into a control-flow object (clear / set / invalid) for submit logic.
  getCommitAction = (): { kind: 'clear' } | { kind: 'set'; centis: number } | { kind: 'invalid' } => {
    const val = this.value.trim();
    if (val === '') {
      return { kind: 'clear' };
    }
    const parsed = parseTimeToCentis(val);
    if (defined(parsed) && parsed >= 0) {
      return { kind: 'set', centis: parsed };
    }
    return { kind: 'invalid' };
  };

  setValue = (value: string) => {
    this.value = value;
    this.error = false;
    this.redraw();
  };

  setEditError = (error: boolean) => {
    this.error = error;
    if (error) this.shakeTrigger++;
    this.redraw();
  };
}

export function getClockEditSlotOpts(
  ctrl: AnalyseCtrl,
  path: TreePath,
  data: ClockDataAtPath,
): ClockSlotOpts | undefined {
  const study = ctrl.study;
  if (!study || study.relay) return undefined;
  if (
    path !== ctrl.path ||
    !study.vm.mode.write ||
    !study.members.canContribute() ||
    !ctrl.tree.pathIsMainline(path)
  ) {
    return undefined;
  }

  const clockEdit = study.clockEdit;
  // If an edit session is open for a different path, the user navigated away.
  // Commit/close the old session immediately ("closeNow" semantics). setTimeout() avoids
  // committing while we're still in the middle of rendering slot opts.
  if (clockEdit && clockEdit.path !== path) {
    setTimeout(() => study.commitClockEdit({ whenInvalid: 'closeNow' }), 0);
  }

  // Root clocks are not editable; don't inject any study clock-edit UI at ply 0.
  if (data.ply === 0) {
    return undefined;
  }

  // Editable slot is the side who just moved (not the side to move / active clock).
  const editableSlot = slotNameForIndex(data.whitePov, data.isWhiteTurn ? 1 : 0);

  const slotOpts: ClockSlotOpts = {};
  for (const index of [0, 1]) {
    const slot = slotNameForIndex(data.whitePov, index as 0 | 1);
    const centi = data.centis[index];
    const editable = editableSlot === slot;
    const isEditing = !!clockEdit && clockEdit.path === path && clockEdit.slot === slot;
    const empty = !defined(centi);

    const classes: string[] = [];
    if (editable && !isEditing) classes.push('analyse__clock--editable');
    if (isEditing) classes.push('analyse__clock--editing');

    let content = undefined;
    if (isEditing && clockEdit) {
      content = renderClockEditInput(clockEdit, {
        onInput: v => clockEdit.setValue(v),
        onKeydown: e => {
          if (e.key === 'Enter') study.commitClockEdit();
          else if (e.key === 'Escape') study.closeClockEdit();
        },
        onBlur: () => study.commitClockEdit({ whenInvalid: 'closeAfterFeedback' }),
      });
    } else if (editable && empty) {
      content = h('span.analyse__clock-placeholder', {}, ['--:--']);
    }

    const onClick =
      editable && !clockEdit
        ? () => study.openClockEdit(slot, path, defined(centi) ? formatClockFromCentis(centi) : '')
        : undefined;

    slotOpts[slot] = {
      classes,
      title: editable && !isEditing ? i18n.study.addOrEditClockTime : undefined,
      content,
      onClick,
    };
  }
  return slotOpts;
}

/** Renders clocks with editable slot opts when applicable (e.g. in study). */
export function renderEditableClocks(
  ctrl: AnalyseCtrl,
  path: TreePath = ctrl.path,
): [VNode, VNode] | undefined {
  return renderClocks(ctrl, path, data => getClockEditSlotOpts(ctrl, path, data));
}
