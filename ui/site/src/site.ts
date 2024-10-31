import { boot } from './boot';
import Mousetrap from './mousetrap';
import { randomToken } from 'common/algo';
import powertip from './powertip';
import * as assets from './asset';
import { unload, redirect, reload } from './reload';
import announce from './announce';
import { displayLocale } from 'common/i18n';
import sound from './sound';
import { api } from './api';
import { pubsub } from 'common/pubsub';

const site = window.site;
// site.load is initialized in layout.scala embedded script tags
// site.manifest is fetched
// site.info, site.debug are populated by ui/build
// site.socket, site.quietMode, site.analysis are set elsewhere
site.sri = randomToken();
site.displayLocale = displayLocale;
site.blindMode = document.body.classList.contains('blind-mode');
site.mousetrap = new Mousetrap(document);
site.powertip = powertip;
site.asset = assets;
site.unload = unload;
site.redirect = redirect;
site.reload = reload;
site.announce = announce;
site.sound = sound;
site.load.then(boot);

// public API
(window as any).lichess = api(pubsub);
