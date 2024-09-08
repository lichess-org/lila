import { boot } from './boot';
import Mousetrap from './mousetrap';
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

const site = window.site;
// site.load, site.quantity, site.siteI18n are initialized in layout.scala embedded script tags
// site.manifest is fetched from the server and available from site
// site.info, site.debug are built in the ui/build process
// site.socket, site.quietMode, site.analysis are set elsewhere and available from site

site.mousetrap = new Mousetrap(document);
site.sri = randomToken();
site.powertip = powertip;
site.asset = assets;
site.pubsub = pubsub;
site.unload = unload;
site.redirect = redirect;
site.reload = reload;
site.watchers = watchers;
site.announce = announce;
site.trans = trans(site.siteI18n);
site.displayLocale = displayLocale;
site.sound = sound;
site.contentLoaded = (parent?: HTMLElement) => pubsub.emit('content-loaded', parent);
site.blindMode = document.body.classList.contains('blind-mode');
site.makeChat = 
  data => site.asset.loadEsm('chat', { init: { el: document.querySelector('.mchat')!, ...data } });
site.makeChessground = (element: HTMLElement, config?: CgConfig) => Chessground(element, config);
site.log = makeLog();
site.load.then(boot);
