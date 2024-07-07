import { BroadcastChatHandler } from 'chat/src/interfaces';
import AnalyseCtrl from '../../ctrl';

export function broadcastChatHandler(ctrl: AnalyseCtrl): BroadcastChatHandler {
  const encodeMsg = (text: string): string => {
    if (ctrl.study?.relay && !ctrl.study.relay.tourShow()) {
      const chapterId = ctrl.study.currentChapter().id;
      const ply = ctrl.study.currentNode().ply;
      // '\ue666' was arbitrarily chosen from the unicode private use area to separate the text from the chapterId and ply
      text = text + '\ue666' + chapterId + '\ue666' + ply;
    }
    return text;
  };

  const cleanMsg = (msg: string): string => {
    if (msg.includes('\ue666') && ctrl.study?.relay) {
      return msg.split('\ue666')[0];
    }
    return msg;
  };

  const jumpToMove = (msg: string): void => {
    if (msg.includes('\ue666') && ctrl.study?.relay) {
      const segs = msg.split('\ue666');
      if (segs.length == 3) {
        const [_, chapterId, ply] = segs;
        ctrl.study.setChapter(chapterId);
        setTimeout(() => ctrl.jumpToMain(parseInt(ply)), 100);
      }
    }
  };

  const canJumpToMove = (msg: string): string | null => {
    if (msg.includes('\ue666') && ctrl.study?.relay) {
      const segs = msg.split('\ue666');
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
