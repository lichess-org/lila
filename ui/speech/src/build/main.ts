import { renderMoveOrDrop as enRender } from '../english';
import { renderMoveOrDrop as jpRender } from '../japanese';

function main(opts: LishogiSpeech): void {
  window.lishogi.sound.say(
    {
      en: opts.notation ? enRender(opts.notation) : 'Game start',
      jp: opts.notation ? jpRender(opts.notation) : '開始',
    },
    opts.cut
  );
}

window.lishogi.registerModule(__bundlename__, main);
