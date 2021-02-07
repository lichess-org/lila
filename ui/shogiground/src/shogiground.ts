import { Api, start } from './api'
import { Config, configure } from './config'
import { State, defaults } from './state'

import { renderWrap } from './wrap';
import * as events from './events'
import { render, updateBounds } from './render';
import * as svg from './svg';
import * as util from './util';

export function Shogiground(element: HTMLElement, config?: Config): Api {

  const state = defaults() as State;

  configure(state, config || {});

  function redrawAll(): void {
    const prevUnbind = state.dom && state.dom.unbind; /* eslint-disable-line */
    // compute bounds from existing board element if possible
    // this allows non-square boards from CSS to be handled (for 3D)
    const relative = state.viewOnly && !state.drawable.visible,
      elements = renderWrap(element, state, relative),
      bounds = util.memo(() => elements.board.getBoundingClientRect()),
      redrawNow = (skipSvg?: boolean): void => {
        render(state);
        if (!skipSvg && elements.svg) svg.renderSvg(state, elements.svg);
      },
      boundsUpdated = (): void => {
        bounds.clear();
        updateBounds(state);
        if (elements.svg) svg.renderSvg(state, elements.svg);
      };
    state.dom = {
      elements,
      bounds,
      redraw: debounceRedraw(redrawNow),
      redrawNow,
      unbind: prevUnbind,
      relative
    };
    state.drawable.prevSvgHash = '';
    redrawNow(false);
    events.bindBoard(state, boundsUpdated);
    if (!prevUnbind) state.dom.unbind = events.bindDocument(state, boundsUpdated);
    state.events.insert && state.events.insert(elements);
  }
  redrawAll();

  return start(state, redrawAll);
}

function debounceRedraw(redrawNow: (skipSvg?: boolean) => void): () => void {
  let redrawing = false;
  return () => {
    if (redrawing) return;
    redrawing = true;
    requestAnimationFrame(() => {
      redrawNow();
      redrawing = false;
    });
  };
}
