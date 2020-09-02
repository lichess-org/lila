import StrongSocket from "./component/socket";
import { requestIdleCallback, once, debounce, makeChat, formAjax, numberFormat, escapeHtml } from "./component/functions";
import { hasTouchEvents, sri, spinnerHtml, info } from "./component/intro";
import { isCol1 } from "./component/is-col1";
import { storage, tempStorage } from "./component/storage";
import powertip from "./component/powertip";
import { assetUrl, soundUrl, loadCss, loadCssPath, jsModule, loadScript, hopscotch, slider } from "./component/assets";
import widget from "./component/widget";
import idleTimer from "./component/idle-timer";
import pubsub from "./component/pubsub";
import { unload, redirect, reload } from "./component/reload";
import announce from "./component/announce";
import trans from "./component/trans";
import sound from "./component/sound";
import soundBox from "./component/soundbox";
import userAutocomplete from "./component/user-autocomplete";
import miniBoard from "./component/mini-board";
import miniGame from "./component/mini-game";
import timeago from "./component/timeago";
import modal from "./component/modal";

export default function() {
  return {
    StrongSocket,
    requestIdleCallback,
    hasTouchEvents,
    sri,
    info,
    isCol1,
    storage,
    tempStorage,
    once,
    debounce,
    powertip,
    widget,
    spinnerHtml,
    assetUrl,
    soundUrl,
    loadCss,
    loadCssPath,
    jsModule,
    loadScript,
    hopscotch,
    slider,
    makeChat,
    formAjax,
    numberFormat,
    idleTimer,
    pubsub,
    unload,
    redirect,
    reload,
    escapeHtml,
    announce,
    trans,
    sound,
    soundBox,
    userAutocomplete,
    miniBoard,
    miniGame,
    timeago,
    modal
  };
}
