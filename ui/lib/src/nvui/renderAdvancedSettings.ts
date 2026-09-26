import { renderSetting } from 'lib/nvui/setting';
import { type VNode, hl, noTrans } from 'lib/view';

import * as s from './setting';

export function renderAdvancedSettings(
  moveStyle: s.Setting<s.MoveStyle>,
  pageStyle: s.Setting<s.PageStyle>,
  pieceStyle: s.Setting<s.PieceStyle>,
  prefixStyle: s.Setting<s.PrefixStyle>,
  positionStyle: s.Setting<s.PositionStyle>,
  boardStyle: s.Setting<s.BoardStyle>,
  ctrl: { redraw: () => void },
): VNode[] {
  return [
    hl('h2', i18n.site.advancedSettings),
    hl('label', [noTrans('Move notation'), renderSetting(moveStyle, ctrl.redraw)]),
    hl('label', [noTrans('Page layout'), renderSetting(pageStyle, ctrl.redraw)]),
    hl('h3', noTrans('Board settings')),
    hl('label', [noTrans('Piece style'), renderSetting(pieceStyle, ctrl.redraw)]),
    hl('label', [noTrans('Piece prefix style'), renderSetting(prefixStyle, ctrl.redraw)]),
    hl('label', [noTrans('Show position'), renderSetting(positionStyle, ctrl.redraw)]),
    hl('label', [noTrans('Board layout'), renderSetting(boardStyle, ctrl.redraw)]),
  ];
}
