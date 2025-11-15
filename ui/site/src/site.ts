import { boot } from './boot';
import Mousetrap from './mousetrap';
import { randomToken } from 'lib/algo';
import powertip from './powertip';
import * as assets from './asset';
import { unload, redirect, reload } from './reload';
import { display as announceDisplay } from './announce';
import { displayLocale } from 'lib/i18n';
import sound from './sound';
import { api } from 'lib/api';
import { loadPolyfills } from './polyfill';
import { addWindowHandlers } from './domHandlers';

const site = window.site;
// site.load is initialized in site.inline.ts (body script)
// site.manifest is fetched
// site.info, site.debug are populated by ui/build
// site.quietMode is set elsewhere
// window.lichess is initialized in ui/api/src/api.ts
site.sri = randomToken();
site.displayLocale = displayLocale;
site.blindMode = document.body.classList.contains('blind-mode');
site.mousetrap = new Mousetrap(document);
site.powertip = powertip;
site.asset = assets;
site.unload = unload;
site.redirect = redirect;
site.reload = reload;
site.announce = announceDisplay;
site.sound = sound;
(window as any).lichess = api;
loadPolyfills();
addWindowHandlers();
site.load.then(boot);
