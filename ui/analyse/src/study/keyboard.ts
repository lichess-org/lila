import { GlyphCtrl } from "./studyGlyph";

export const bind = (ctrl: GlyphCtrl) => {
    const kbd = window.Mousetrap;
    if (!kbd) return;
    kbd
        .bind(['shift+g'], () => {
            ctrl.toggleGlyph(1);
            ctrl.redraw();
        })
        ;
}