import { GlyphCtrl } from './studyGlyph';

export const bind = (ctrl: GlyphCtrl) => {
  const kbd = window.Mousetrap;
  if (!kbd) return;
  kbd
    .bind(['1'], () => {
      ctrl.toggleGlyph(1);
      ctrl.redraw();
    })
    .bind(['2'], () => {
      ctrl.toggleGlyph(2);
      ctrl.redraw();
    })
    .bind(['3'], () => {
      ctrl.toggleGlyph(3);
      ctrl.redraw();
    })
    .bind(['4'], () => {
      ctrl.toggleGlyph(4);
      ctrl.redraw();
    })
    .bind(['5'], () => {
      ctrl.toggleGlyph(5);
      ctrl.redraw();
    })
    .bind(['6'], () => {
      ctrl.toggleGlyph(6);
      ctrl.redraw();
    });
};
