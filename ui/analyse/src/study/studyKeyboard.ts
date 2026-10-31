import { makeBinder } from '../keyboard';
import type StudyCtrl from './studyCtrl';

export default function studyKeyboard(ctrl: StudyCtrl): void {
  const kbd = makeBinder(ctrl.ctrl.reaper);

  for (const [key, button] of [
    ['d', 'comments'],
    ['g', 'glyphs'],
  ])
    kbd.bind(key, () =>
      $(`.study__buttons .${button}`).each(function (this: HTMLElement) {
        this.dispatchEvent(new MouseEvent('click'));
      }),
    );

  kbd.bind('p', ctrl.goToPrevChapter);
  kbd.bind('n', ctrl.goToNextChapter);

  // ! ? !! ?? !? ?! □ ⨀
  for (let i = 1; i < 9; i++) kbd.bind(i.toString(), () => ctrl.glyphForm.toggleGlyph(i === 8 ? 22 : i));
  // = ∞ ⩲ ⩱ ± ∓ +- -+
  for (let i = 1; i < 9; i++) kbd.bind(`shift+${i}`, () => ctrl.glyphForm.toggleGlyph(i === 1 ? 10 : 11 + i));
  // N ↑↑ ↑ → ⇆ ⊕ =∞ ∆
  const observationIds = [146, 32, 36, 40, 132, 138, 44, 140];
  for (let i = 1; i < 9; i++)
    kbd.bind(`ctrl+shift+${i}`, () => ctrl.glyphForm.toggleGlyph(observationIds[i - 1]));

  kbd.bind('mod+z', ctrl.undoShapeChange);

  kbd.bind('shift+s', ctrl.search.open);

  kbd.bind('shift+h', () => ctrl.toggleStudyFormIfAllowed());

  kbd.bind('shift+e', () => {
    if (!ctrl.members.canContribute()) return;
    ctrl.chapters.editForm.toggle(ctrl.currentChapter());
    ctrl.redraw();
  });

  kbd.bind('shift+n', () => {
    if (!ctrl.members.canContribute()) return;
    ctrl.chapters.toggleNewForm();
    ctrl.redraw();
  });

  kbd.bind('ctrl+b', event => {
    if (event.repeat) return;
    event.preventDefault();
    if (!ctrl.relay) site.asset.loadEsm('studyDb');
  });
}
