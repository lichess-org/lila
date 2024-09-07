import { boot } from './boot';
import Mousetrap from './mousetrap';
import { spinnerHtml } from 'common/spinner';
import { randomToken } from 'common/algo';
import powertip from './powertip';
import * as assets from './asset';
import makeLog from './log';
import pubsub from './pubsub';
import { unload, redirect, reload } from './reload';
import announce from './announce';
import { trans, displayLocale } from 'common/i18n';
import sound from './sound';
import watchers from './watchers';
import { Chessground } from 'chessground';

const s = window.site;
// s.load, s.quantity, s.siteI18n are initialized in layout.scala embedded script tags

s.mousetrap = new Mousetrap(document);
s.sri = randomToken();
s.powertip = powertip;
s.spinnerHtml = spinnerHtml;
s.asset = assets;
s.pubsub = pubsub;
s.unload = unload;
s.redirect = redirect;
s.reload = reload;
s.watchers = watchers;
s.announce = announce;
s.trans = trans(s.siteI18n);
s.displayLocale = displayLocale;
s.sound = sound;
s.contentLoaded = (parent?: HTMLElement) => pubsub.emit('content-loaded', parent);
s.blindMode = document.body.classList.contains('blind-mode');
s.makeChat = data => site.asset.loadEsm('chat', { init: { el: document.querySelector('.mchat')!, ...data } });
s.makeChessground = (element: HTMLElement, config?: CgConfig) => Chessground(element, config);
s.log = makeLog();
s.load.then(boot);
