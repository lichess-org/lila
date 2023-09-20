import StrongSocket from './component/socket';
import Mousetrap from './component/mousetrap';
import { requestIdleCallback, escapeHtml } from './component/functions';
import once from './component/once';
import { spinnerHtml } from 'common/spinner';
import sri from './component/sri';
import { storage, tempStorage } from './component/storage';
import powertip from './component/powertip';
import clockWidget from './component/clock-widget';
import {
  assetUrl,
  loadCss,
  loadCssPath,
  jsModule,
  loadIife,
  hopscotch,
  userComplete,
  loadEsm,
} from './component/assets';
import idleTimer from './component/idle-timer';
import pubsub from './component/pubsub';
import { unload, redirect, reload } from './component/reload';
import announce from './component/announce';
import { trans } from './component/trans';
import sound from './component/sound';
import { mic } from './component/mic';
import * as miniBoard from 'common/mini-board';
import * as miniGame from './component/mini-game';
import { format as timeago, formatter as dateFormat } from './component/timeago';
import watchers from './component/watchers';
import { Chessground } from 'chessground';

declare const __debug__: boolean;

export default () => {
  const l = window.lichess;
  l.debug = __debug__;
  l.StrongSocket = StrongSocket;
  l.mousetrap = new Mousetrap(document);
  l.requestIdleCallback = requestIdleCallback;
  l.sri = sri;
  l.storage = storage;
  l.tempStorage = tempStorage;
  l.once = once;
  l.powertip = powertip;
  l.clockWidget = clockWidget;
  l.spinnerHtml = spinnerHtml;
  l.assetUrl = assetUrl;
  l.loadCss = loadCss;
  l.loadCssPath = loadCssPath;
  l.jsModule = jsModule;
  l.loadIife = loadIife;
  l.loadEsm = loadEsm;
  l.hopscotch = hopscotch;
  l.userComplete = userComplete;
  l.idleTimer = idleTimer;
  l.pubsub = pubsub;
  l.unload = unload;
  l.redirect = redirect;
  l.reload = reload;
  l.watchers = watchers;
  l.escapeHtml = escapeHtml;
  l.announce = announce;
  l.trans = trans;
  l.sound = sound;
  l.mic = mic;
  l.miniBoard = miniBoard;
  l.miniGame = miniGame;
  l.timeago = timeago;
  l.dateFormat = dateFormat;
  l.contentLoaded = (parent?: HTMLElement) => pubsub.emit('content-loaded', parent);
  l.blindMode = document.body.classList.contains('blind-mode');
  l.makeChat = data => lichess.loadEsm('chat', { init: { el: document.querySelector('.mchat')!, ...data } });
  l.makeChessground = Chessground;
};
