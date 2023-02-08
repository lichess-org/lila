import { patch } from './view/util';
import makeBoot from './boot';
import makeStart from './start';
import LichessChat from 'chat';
import { Chessground } from 'chessground';

export { patch };

export const start = makeStart(patch);

export const boot = makeBoot(start);

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = LichessChat;

(window as any).LichessAnalyse = { start, boot }; // esbuild
