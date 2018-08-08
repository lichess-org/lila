import { Api, start } from './api'
import { Config, configure } from './config'
import { State, defaults } from './state'

import renderWrap from './wrap';
import * as events from './events'
import render from './render';
import * as svg from './svg';
import * as util from './util';

export function Draughtsground(element: HTMLElement, config?: Config): Api {

  const state = defaults() as State;

  configure(state, config || {});

  function redrawAll() {
    let prevUnbind = state.dom && state.dom.unbind;
    // first ensure the cg-board-wrap class is set
    // so bounds calculation can use the CSS width/height values
    // add that class yourself to the element before calling draughtsground
    // for a slight performance improvement! (avoids recomputing style)
    element.classList.add('cg-board-wrap');
    // compute bounds from existing board element if possible
    // this allows non-square boards from CSS to be handled (for 3D)
    const bounds = util.memo(() => element.getBoundingClientRect());
    const relative = state.viewOnly && !state.drawable.visible;
    const elements = renderWrap(element, state, relative ? undefined : bounds());
    const redrawNow = (skipSvg?: boolean) => {
      render(state);
      if (!skipSvg && elements.svg) svg.renderSvg(state, elements.svg);
    };
    state.dom = {
      elements: elements,
      bounds: bounds,
      redraw: debounceRedraw(redrawNow),
      redrawNow: redrawNow,
      unbind: prevUnbind,
      relative
    };
    state.drawable.prevSvgHash = '';
    redrawNow(false);
    events.bindBoard(state);
    if (!prevUnbind) state.dom.unbind = events.bindDocument(state, redrawAll);
  }
  redrawAll();

  const api = start(state, redrawAll);

  return api;
};

function debounceRedraw(redrawNow: (skipSvg?: boolean) => void): () => void {
  let redrawing = false;
  return () => {
    if (redrawing) return;
    redrawing = true;
    util.raf(() => {
      redrawNow();
      redrawing = false;
    });
  };
}
