import { BroadcastChatHandler } from 'chat/src/interfaces';
import AnalyseCtrl from '../../ctrl';

export function broadcastChatHandler(ctrl: AnalyseCtrl): BroadcastChatHandler {
  // '\ue666' was arbitrarily chosen from the unicode private use area to separate the text from the chapterId and ply
  const separator = '\ue666';

  const encodeMsg = (text: string): string => {
    text = cleanMsg(text);
    if (ctrl.study?.relay && !ctrl.study.relay.tourShow()) {
      const chapterId = ctrl.study.currentChapter().id;
      const ply = ctrl.study.currentNode().ply;
      text = text + separator + chapterId + separator + ply;
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
        ctrl.study.setChapter(chapterId);
        setTimeout(() => ctrl.jumpToMain(parseInt(ply)), 100);
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

  return {
    encodeMsg,
    cleanMsg,
    jumpToMove,
    canJumpToMove,
  };
}
