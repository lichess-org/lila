import type AnalyseCtrl from '@/ctrl';
import { displayColumns } from 'lib/device';
import { hl, toggle, type LooseVNodes } from 'lib/view';

export const config = (ctrl: AnalyseCtrl): LooseVNodes => [
  displayColumns() > 1 && hl('h2', i18n.site.visualMotifs),
  toggle(
    {
      name: i18n.site.undefendedPieces,
      id: 'show-undefended',
      checked: ctrl.motif.undefended(),
      change: ctrl.motif.undefended,
    },
    ctrl.redraw,
  ),
  toggle(
    {
      name: i18n.site.pinnedPieces,
      id: 'show-pin',
      checked: ctrl.motif.pin(),
      change: ctrl.motif.pin,
    },
    ctrl.redraw,
  ),
  toggle(
    {
      name: i18n.site.checkableKing,
      id: 'show-checkable',
      checked: ctrl.motif.checkable(),
      change: ctrl.motif.checkable,
    },
    ctrl.redraw,
  ),
];
