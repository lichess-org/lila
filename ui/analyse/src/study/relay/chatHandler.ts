import { BroadcastChatHandler, Line } from 'chat/src/interfaces';
import AnalyseCtrl from '../../ctrl';
import { VNode, h } from 'snabbdom';
import { bind } from 'common/snabbdom';

export function broadcastChatHandler(ctrl: AnalyseCtrl): BroadcastChatHandler {
  // '\ue666' was arbitrarily chosen from the unicode private use area to separate the text from the chapterId and ply
  const separator = '\ue666';

  const encodeMsg = (text: string): string => {
    text = cleanMsg(text);
    if (ctrl.study?.relay && !ctrl.study.relay.tourShow()) {
      const chapterId = ctrl.study.currentChapter().id;
      const ply = ctrl.study.currentNode().ply;
      const newText = text + separator + chapterId + separator + ply;
      if (newText.length <= 140) {
        text = newText;
      }
    }
    return text;
  };

  const cleanMsg = (msg: string): string => {
    if (msg.includes(separator) && ctrl.study?.relay) {
      return msg.split(separator)[0];
    }
    return msg;
  };

  const jumpToMove = (msg: string): void => {
    if (msg.includes(separator) && ctrl.study?.relay) {
      const segs = msg.split(separator);
      if (segs.length == 3) {
        const [_, chapterId, ply] = segs;
        if (ctrl.study.vm.chapterId != chapterId || ctrl.study.vm.loading) {
          ctrl.study.vm.nextPly = parseInt(ply);
          ctrl.study.setChapter(chapterId);
        } else {
          if (ctrl.study.relay.tourShow()) {
            ctrl.study.relay.tourShow(false);
          }
          ctrl.jumpToMain(parseInt(ply));
        }
      }
    }
  };

  const canJumpToMove = (msg: string): string | null => {
    if (msg.includes(separator) && ctrl.study?.relay) {
      const segs = msg.split(separator);
      if (segs.length == 3) {
        const [_, chapterId, ply] = segs;
        return `${chapterId}#${ply}`;
      }
    }
    return null;
  };

  const jumpButton = (line: Line): VNode | null => {
    const msgPly = canJumpToMove(line.t);
    return msgPly
      ? h(
          'button.jump',
          {
            hook: bind('click', () => jumpToMove(line.t)),
            attrs: {
              title: `Jump to move ${msgPly}`,
            },
          },
          '#',
        )
      : null;
  };

  return {
    encodeMsg,
    cleanMsg,
    jumpToMove,
    jumpButton,
  };
}
