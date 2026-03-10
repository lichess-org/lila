import { type ChatPlugin } from 'lib/chat/interfaces';
import { hl, type VNode, spinnerVdom } from 'lib/view';

import { type ChapterId } from '../interfaces';
import { previewContent } from '../multiBoard';
import type StudyCtrl from '../studyCtrl';
import type { RelayRound } from './interfaces';

export class LiveboardPlugin implements ChatPlugin {
  key = 'liveboard';
  name = i18n.broadcast.liveboard;
  kidSafe = true;
  redraw: Redraw;

  constructor(
    readonly ctrl: StudyCtrl,
    readonly round: RelayRound,
    readonly isDisabled: () => boolean,
    private chapterId: ChapterId,
  ) {}

  setChapterId(id: ChapterId) {
    this.chapterId = id;
  }

  view(): VNode {
    const preview = this.ctrl.chapters.list.get(this.chapterId);
    if (!preview) return spinnerVdom();
    const cloudEval = this.ctrl.multiCloudEval?.thisIfShowEval();
    const orientation = this.ctrl.bottomColor();
    return hl(
      'div.chat-liveboard',
      hl(
        `span.mini-game.is2d.liveboard-chapter-${preview.id}.liveboard-orientation-${orientation}`,
        previewContent(preview, orientation, cloudEval, true, this.round),
      ),
    );
  }
}
