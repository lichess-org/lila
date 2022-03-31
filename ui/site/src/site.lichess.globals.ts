import StrongSocket from './component/socket';
import { requestIdleCallback, escapeHtml } from './component/functions';
import makeChat from './component/chat';
import once from './component/once';
import spinnerHtml from './component/spinner';
import sri from './component/sri';
import { storage, tempStorage } from './component/storage';
import powertip from './component/powertip';
import {
  assetUrl,
  loadCss,
  loadCssPath,
  jsModule,
  loadScript,
  hopscotch,
  userComplete,
  loadModule,
  loadIife,
} from './component/assets';
import widget from './component/widget';
import idleTimer from './component/idle-timer';
import pubsub from './component/pubsub';
import { unload, redirect, reload } from './component/reload';
import announce from './component/announce';
import trans from './component/trans';
import sound from './component/sound';
import * as miniBoard from 'common/mini-board';
import * as miniGame from './component/mini-game';
import { format as timeago, formatter as dateFormat } from './component/timeago';
import watchers from './component/watchers';

export default () => {
  window.lichess = {
    ...window.lichess,
    StrongSocket,
    requestIdleCallback,
    sri,
    storage,
    tempStorage,
    once,
    powertip,
    widget,
    spinnerHtml,
    assetUrl,
    loadCss,
    loadCssPath,
    jsModule,
    loadScript,
    loadModule,
    loadIife,
    hopscotch,
    userComplete,
    makeChat,
    idleTimer,
    pubsub,
    unload,
    redirect,
    reload,
    watchers,
    escapeHtml,
    announce,
    trans,
    sound,
    miniBoard,
    miniGame,
    timeago,
    dateFormat,
    contentLoaded: (parent?: HTMLElement) => pubsub.emit('content-loaded', parent),
  };
};
