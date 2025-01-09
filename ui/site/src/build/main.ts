import { tempStorage, storage } from '../storage';
import { pubsub } from '../pubsub';
import { announce } from '../announce';
import { render as tRender, format as tFormat, absolute as tAbsolute } from '../timeago';
import { createSound } from '../sound';
import { powertip } from '../powertip';
import { generateSri } from '../util';
import { redirect, reload } from '../navigation';
import { StrongSocket } from '../socket';
import { mousetrap } from '../mousetrap';
import { init, initSocket } from '../init';
import { loadInfiniteScroll } from '../infinite-scroll';
import { userAutocomplete } from '../user-autocomplete';
import { formToXhr, json, text, urlWithParams } from '../xhr';

const lishogi = window.lishogi;

lishogi.sri = generateSri();

lishogi.redirect = redirect;
lishogi.reload = reload;

window.lishogi.xhr = {
  text,
  json,
  formToXhr,
  urlWithParams,
};

lishogi.tempStorage = tempStorage;
lishogi.storage = storage;

lishogi.pubsub = pubsub;

lishogi.announce = announce;

lishogi.sound = createSound();

lishogi.timeago = {
  render: tRender,
  absolute: tAbsolute,
  format: tFormat,
};

lishogi.powertip = powertip;

lishogi.loadInfiniteScroll = loadInfiniteScroll;

lishogi.userAutocomplete = userAutocomplete;

lishogi.mousetrap = mousetrap;

lishogi.StrongSocket = StrongSocket;

(window.lishogi as any).modules = {};
window.lishogi.registerModule = (name: string, func: (...args: any[]) => any) => {
  window.lishogi.modules[name] = func;
};

lishogi.ready.then(() => {
  init();
  Object.keys(window.lishogi.modulesData).forEach(key => {
    console.log('daata:', window.lishogi.modulesData[key]);
    window.lishogi.modules[key]!(window.lishogi.modulesData[key]);
  });
  initSocket();
});
