import { announce } from '../announce';
import { loadInfiniteScroll } from '../infinite-scroll';
import { init, initSocket } from '../init';
import { mousetrap } from '../mousetrap';
import { redirect, reload } from '../navigation';
import { powertip } from '../powertip';
import { pubsub } from '../pubsub';
import { StrongSocket } from '../socket';
import { createSound } from '../sound';
import { storage, tempStorage } from '../storage';
import { absolute as tAbsolute, format as tFormat, render as tRender } from '../timeago';
import { userAutocomplete } from '../user-autocomplete';
import { generateSri } from '../util';
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
