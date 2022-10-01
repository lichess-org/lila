import { GlyphCtrl } from './studyGlyph';

export const bind = (ctrl: GlyphCtrl) => {
  const kbd = window.Mousetrap;
  if (!kbd) return;
  for (let i = 0; i < 7; i++) {
    kbd.bind([i.toString()], () => {
      ctrl.toggleGlyph(i);
      ctrl.redraw();
    });
  }
};
