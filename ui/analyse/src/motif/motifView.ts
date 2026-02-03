import type AnalyseCtrl from '@/ctrl';
import { displayColumns } from 'lib/device';
import { hl, cmnToggleWrap, type LooseVNodes } from 'lib/view';

export const config = (ctrl: AnalyseCtrl): LooseVNodes => [
  displayColumns() > 1 && hl('h2', i18n.site.visualMotifs),
  cmnToggleWrap({
    id: 'show-undefended',
    name: i18n.site.undefendedPieces,
    checked: ctrl.motif.undefended(),
    change: ctrl.motif.undefended,
    redraw: ctrl.redraw,
  }),
  cmnToggleWrap({
    id: 'show-pin',
    name: i18n.site.pinnedPieces,
    checked: ctrl.motif.pin(),
    change: ctrl.motif.pin,
    redraw: ctrl.redraw,
  }),
  cmnToggleWrap({
    id: 'show-checkable',
    name: i18n.site.checkableKing,
    checked: ctrl.motif.checkable(),
    change: ctrl.motif.checkable,
    redraw: ctrl.redraw,
  }),
];
