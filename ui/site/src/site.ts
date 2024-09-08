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
import { Chessground } from 'chessground';

const site = window.site;
// site.load, site.quantity, site.siteI18n are initialized in layout.scala embedded script tags
// site.manifest is fetched immediately from the server
// site.info, site.debug are populated by ui/build
// site.socket, site.quietMode, site.analysis are set elsewhere but available here
site.sri = randomToken();
site.displayLocale = displayLocale;
site.blindMode = document.body.classList.contains('blind-mode');
site.mousetrap = new Mousetrap(document);
site.powertip = powertip;
site.asset = assets;
site.pubsub = pubsub;
site.unload = unload;
site.redirect = redirect;
site.reload = reload;
site.announce = announce;
site.trans = trans(site.siteI18n);
site.sound = sound;
site.makeChessground = (element: HTMLElement, config?: CgConfig) => Chessground(element, config);
site.log = makeLog();
site.load.then(boot);
