import * as licon from 'lib/licon';
import { text as xhrText } from 'lib/xhr';
import { requestIdleCallback } from 'lib';
import { spinnerHtml } from 'lib/view';
import { pubsub } from 'lib/pubsub';

// Thanks Steven Benner! - adapted from https://github.com/stevenbenner/jquery-powertip

const inCrosstable = (el: HTMLElement) => document.querySelector('.crosstable')?.contains(el);

const onPowertipPreRender = (id: string, preload?: (url: string) => void) => (el: HTMLAnchorElement) => {
  const url = (el.dataset.href || el.href).replace(/\?.+$/, '');
  if (preload) preload(url);
  xhrText(url + '/mini').then(html => {
    const el = document.getElementById(id) as HTMLElement;
    el.innerHTML = html;
    pubsub.emit('content-loaded', el);
  });
};

const uptA = (url: string, icon: string) => `<a class="btn-rack__btn" href="${url}" data-icon="${icon}"></a>`;

const userPowertip = (el: HTMLElement, pos?: PowerTip.Placement) =>
  $(el)
    .removeClass('ulpt')
    .powerTip({
      preRender: onPowertipPreRender('powerTip', (url: string) => {
        const u = url.slice(3);
        const name = el.dataset.name || $(el).html();
        $('#powerTip').html(
          '<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' +
            name +
            '</span></div></div><div class="upt__actions btn-rack">' +
            uptA('/@/' + u + '/tv', licon.AnalogTv) +
            uptA('/inbox/new?user=' + u, licon.BubbleSpeech) +
            uptA('/?user=' + u + '#friend', licon.Swords) +
            '<a class="btn-rack__btn relation-button" disabled></a></div>',
        );
      }),
      placement:
        pos || (el.getAttribute('data-pt-pos') as PowerTip.Placement) || (inCrosstable(el) ? 'n' : 's'),
    });

const gamePowertip = (el: HTMLElement) =>
  $(el)
    .removeClass('glpt')
    .powerTip({
      preRender: onPowertipPreRender('miniGame', () => spinnerHtml),
      placement: inCrosstable(el) ? 'n' : 'w',
      popupId: 'miniGame',
    });

function powerTipWith(el: HTMLElement, ev: Event, f: (el: HTMLElement) => void) {
  if ('ontouchstart' in window && !el.classList.contains('mobile-powertip')) return;
  f(el);
  $.powerTip.show(el, ev);
}

function onIdleForAll(par: HTMLElement, sel: string, f: (el: HTMLElement) => void) {
  requestIdleCallback(
    () => Array.prototype.forEach.call(par.querySelectorAll(sel), (el: HTMLElement) => f(el)), // do not codegolf to `f`
    800,
  );
}

function $as<T>(cashOrHtml: Cash | string): T {
  return (typeof cashOrHtml === 'string' ? $(cashOrHtml) : cashOrHtml)[0] as T;
}

const powertip: LichessPowertip = {
  watchMouse() {
    document.body.addEventListener('mouseover', e => {
      const t = e.target as HTMLElement;
      if (t.classList.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (t.classList.contains('glpt')) powerTipWith(t, e, gamePowertip);
    });
  },
  manualGameIn(parent: HTMLElement) {
    onIdleForAll(parent, '.glpt', gamePowertip);
  },
  manualGame: gamePowertip,
  manualUser: userPowertip,
  manualUserIn(parent: HTMLElement) {
    onIdleForAll(parent, '.ulpt', userPowertip);
  },
};

export default powertip;

interface WithTooltip extends HTMLElement {
  displayController: DisplayController;
  hasActiveHover: boolean;
  forcedOpen: boolean;
}

const session: { [key: string]: any; scoped: { [key: string]: any } } = {
  // for each popupId
  scoped: {
    // isTipOpen: false,
    // isClosing: false,
    // tipOpenImminent: false,
    // activeHover: null,
    // desyncTimeout: null,
    // delayInProgress: false,
  },
  currentX: 0,
  currentY: 0,
  previousX: 0,
  previousY: 0,
  mouseTrackingActive: false,
  delayInProgress: false,
  windowWidth: 0,
  windowHeight: 0,
  scrollTop: 0,
  scrollLeft: 0,
};

const Collision = {
  none: 0,
  top: 1,
  bottom: 2,
  left: 4,
  right: 8,
};

$.fn.powerTip = function (opts) {
  // don't do any work if there were no matched elements
  if (!this.length) {
    return this;
  }

  // extend options and instantiate TooltipController
  const options = Object.assign({}, defaults, opts) as Options,
    tipController = new TooltipController(options);

  // hook mouse and viewport dimension tracking, causes layout reflow
  requestIdleCallback(() => initTracking());

  // setup the elements
  this.each((_, el: WithTooltip) => {
    const $this = $(el);

    // handle repeated powerTip calls on the same element by destroying the
    // original instance hooked to it and replacing it with this call
    if ('displayController' in el) {
      $.powerTip.destroy(el);
    }

    // create hover controllers for each element
    el.displayController = new DisplayController($this, options, tipController);
  });

  // attach events to matched elements if the manual options is not enabled
  this.on({
    // mouse events
    mouseenter: function (event) {
      $.powerTip.show(this, event);
    },
    mouseleave: function () {
      $.powerTip.hide(this);
    },
  });

  return this;
};

interface Options extends PowerTip.Options {
  defaultSize: [number, number];
}

const defaults: Options = {
  popupId: 'powerTip',
  intentSensitivity: 7,
  intentPollInterval: 150,
  closeDelay: 150,
  placement: 'n',
  smartPlacement: true,
  defaultSize: [260, 120],
  offset: 10,
};

const smartPlacementLists: { [key: string]: string[] } = {
  n: ['n', 'ne', 'nw', 's', 'se', 'sw', 'e', 'w'],
  e: ['e', 'ne', 'se', 'w', 'nw', 'sw', 'n', 's'],
  s: ['s', 'se', 'sw', 'n', 'ne', 'nw', 'e', 'w'],
  w: ['w', 'nw', 'sw', 'e', 'ne', 'se', 'n', 's'],
  nw: ['nw', 'w', 'sw', 'n', 's', 'se', 'nw', 'e'],
  ne: ['ne', 'e', 'se', 'n', 's', 'sw', 'ne', 'w'],
  sw: ['sw', 'w', 'nw', 's', 'n', 'ne', 'sw', 'e'],
  se: ['se', 'e', 'ne', 's', 'n', 'nw', 'se', 'w'],
};

/**
 * Public API
 */

$.powerTip = {
  show(element: WithTooltip, event?: PointerEvent) {
    if (event) {
      trackMouse(event);
      session.previousX = event.pageX;
      session.previousY = event.pageY;
      element.displayController.show();
    } else {
      element.displayController.show(true, true);
    }
    return element;
  },

  reposition(element: WithTooltip) {
    element.displayController.resetPosition();
    return element;
  },

  hide(element: WithTooltip, immediate?: boolean) {
    element.displayController.hide(immediate);
    return element;
  },

  destroy(element: Partial<WithTooltip>) {
    element.displayController?.hide(true);
  },
};

// csscoordinates.js

type Coords = {
  left: number | 'auto';
  right: number | 'auto';
  top: number | 'auto';
  bottom: number | 'auto';
};

function cssCoordinates(): Coords {
  return { left: 'auto', top: 'auto', right: 'auto', bottom: 'auto' };
}

// displaycontroller.js

class DisplayController {
  scoped: { [key: string]: any } = {};
  hoverTimer?: Timeout;
  el: WithTooltip;

  constructor(
    readonly element: Cash,
    readonly options: Options,
    readonly tipController: TooltipController,
  ) {
    this.el = $as(element);
    this.scoped = session.scoped[options.popupId!];
    // expose the methods
  }

  show(immediate?: boolean, forceOpen?: boolean) {
    this.cancel();
    if (!this.el.hasActiveHover) {
      if (!immediate) {
        this.scoped.tipOpenImminent = true;
        this.hoverTimer = setTimeout(() => {
          this.hoverTimer = undefined;
          this.checkForIntent();
        }, this.options.intentPollInterval);
      } else {
        if (forceOpen) {
          this.el.forcedOpen = true;
        }
        this.tipController.showTip(this.element);
      }
    }
  }

  hide(disableDelay?: boolean) {
    this.cancel();
    this.scoped.tipOpenImminent = false;
    if (this.el.hasActiveHover) {
      this.el.forcedOpen = false;
      if (!disableDelay) {
        this.scoped.delayInProgress = true;
        this.hoverTimer = setTimeout(() => {
          this.hoverTimer = undefined;
          this.tipController.hideTip(this.element);
          session.delayInProgress = false;
        }, this.options.closeDelay);
      } else {
        this.tipController.hideTip(this.element);
      }
    }
  }

  checkForIntent() {
    // calculate mouse position difference
    const xDifference = Math.abs(session.previousX - session.currentX),
      yDifference = Math.abs(session.previousY - session.currentY),
      totalDifference = xDifference + yDifference;

    // check if difference has passed the sensitivity threshold
    if (totalDifference < (this.options.intentSensitivity ?? 0)) {
      this.tipController.showTip(this.element);
    } else {
      // try again
      session.previousX = session.currentX;
      session.previousY = session.currentY;
      this.show();
    }
  }

  cancel() {
    clearTimeout(this.hoverTimer);
    this.scoped.delayInProgress = false;
  }

  resetPosition() {
    this.tipController.resetPosition(this.element);
  }
}

// placementcalculator.js

function placementCalculator() {
  return {
    compute(
      element: Cash,
      placement: PowerTip.Placement,
      tipWidth: number,
      tipHeight: number,
      offset: number,
    ) {
      placement = site.powertip.forcePlacementHook?.(element[0]!) ?? placement;

      const placementBase = placement.split('-')[0], // ignore 'alt' for corners
        coords = cssCoordinates(),
        position = getHtmlPlacement(element, placementBase);

      // calculate the appropriate x and y position in the document
      switch (placement) {
        case 'n':
          coords.left = position.left - tipWidth / 2;
          coords.bottom = session.windowHeight - position.top + offset;
          break;
        case 'e':
          coords.left = position.left + offset;
          coords.top = position.top - tipHeight / 2;
          break;
        case 's':
          coords.left = position.left - tipWidth / 2;
          coords.top = position.top + offset;
          break;
        case 'w':
          coords.top = position.top - tipHeight / 2;
          coords.right = session.windowWidth - position.left + offset;
          break;
        case 'nw':
          coords.bottom = session.windowHeight - position.top + offset;
          coords.right = session.windowWidth - position.left - 20;
          break;
        case 'ne':
          coords.left = position.left - 20;
          coords.bottom = session.windowHeight - position.top + offset;
          break;
        case 'sw':
          coords.top = position.top + offset;
          coords.right = session.windowWidth - position.left - 20;
          break;
        case 'se':
          coords.left = position.left - 20;
          coords.top = position.top + offset;
          break;
      }
      return coords;
    },
  };

  function getHtmlPlacement(element: Cash, placement: string) {
    const objectOffset = element.offset()!,
      objectWidth = element.outerWidth(),
      objectHeight = element.outerHeight();
    let left = 0,
      top = 0;

    // calculate the appropriate x and y position in the document
    switch (placement) {
      case 'n':
        left = objectOffset.left + objectWidth / 2;
        top = objectOffset.top;
        break;
      case 'e':
        left = objectOffset.left + objectWidth;
        top = objectOffset.top + objectHeight / 2;
        break;
      case 's':
        left = objectOffset.left + objectWidth / 2;
        top = objectOffset.top + objectHeight;
        break;
      case 'w':
        left = objectOffset.left;
        top = objectOffset.top + objectHeight / 2;
        break;
      case 'nw':
        left = objectOffset.left;
        top = objectOffset.top;
        break;
      case 'ne':
        left = objectOffset.left + objectWidth;
        top = objectOffset.top;
        break;
      case 'sw':
        left = objectOffset.left;
        top = objectOffset.top + objectHeight;
        break;
      case 'se':
        left = objectOffset.left + objectWidth;
        top = objectOffset.top + objectHeight;
        break;
    }

    return { left, top };
  }
}

// tooltipcontroller.js

class TooltipController {
  scoped: { [key: string]: any };
  tipElement: Cash;
  placementCalculator = placementCalculator();

  constructor(readonly options: Options) {
    this.tipElement = $('#' + options.popupId);
    if (!session.scoped[options.popupId!]) session.scoped[options.popupId!] = {};
    this.scoped = session.scoped[options.popupId!];
    // build and append tooltip div if it does not already exist
    if (this.tipElement.length === 0) {
      const tip = document.createElement('div');
      tip.id = options.popupId!;
      this.tipElement = $(tip);
      $('body').append(this.tipElement);
    }

    // if we want to be able to mouse onto the tooltip then we need to attach
    // hover events to the tooltip that will cancel a close request on hover and
    // start a new close request on mouseleave
    this.tipElement.on({
      mouseenter: () => {
        if (this.scoped.activeHover) {
          this.scoped.activeHover[0].displayController.cancel();
        }
      },
      mouseleave: () => {
        if (this.scoped.activeHover) {
          this.scoped.activeHover[0].displayController.hide();
        }
      },
    });
  }

  showTip(element: Cash) {
    $as<WithTooltip>(element).hasActiveHover = true;
    this.doShowTip(element);
  }

  doShowTip(element: Cash) {
    // it is possible, especially with keyboard navigation, to move on to
    // another element with a tooltip during the queue to get to this point
    // in the code. if that happens then we need to not proceed or we may
    // have the fadeout callback for the last tooltip execute immediately
    // after this code runs, causing bugs.
    if (!$as<WithTooltip>(element).hasActiveHover) return;

    // if the tooltip is open and we got asked to open another one then the
    // old one is still in its fadeOut cycle, so wait and try again
    if (this.scoped.isTipOpen) {
      if (!this.scoped.isClosing) {
        this.hideTip(this.scoped.activeHover);
      }
      setTimeout(() => {
        this.doShowTip(element);
      }, 100);
      return;
    }

    this.tipElement.empty();

    // trigger powerTipPreRender event
    if (this.options.preRender) {
      this.options.preRender($as(element));
    }

    this.scoped.activeHover = element;
    this.scoped.isTipOpen = true;

    // set tooltip position
    this.resetPosition(element);

    this.tipElement.show();

    // start desync polling
    if (!this.scoped.desyncTimeout) {
      this.scoped.desyncTimeout = setInterval(() => this.closeDesyncedTip(), 500);
    }
  }

  hideTip(element: Cash) {
    // reset session
    this.scoped.isClosing = true;
    this.scoped.activeHover = null;
    this.scoped.isTipOpen = false;

    // stop desync polling
    this.scoped.desyncTimeout = clearInterval(this.scoped.desyncTimeout);

    // reset element state
    $as<WithTooltip>(element).hasActiveHover = false;
    $as<WithTooltip>(element).forcedOpen = false;

    // fade out
    this.tipElement.hide();
    const coords = cssCoordinates();

    // reset session and tooltip element
    this.scoped.isClosing = false;
    this.tipElement.removeClass();

    // support mouse-follow and fixed position tips at the same time by
    // moving the tooltip to the last cursor location after it is hidden
    coords.top = session.currentY + this.options.offset!;
    coords.left = session.currentX + this.options.offset!;
    this.tipElement.css(coords);
  }

  resetPosition(element: Cash) {
    if (this.options.smartPlacement) {
      let priorityList = smartPlacementLists[this.options.placement!];
      if ($as<WithTooltip>(element).classList.contains('mobile-powertip'))
        priorityList = [...priorityList, 's']; // so that 's' is used in case all are incorrectly judged as collisions on phones
      // iterate over the priority list and use the first placement option
      // that does not collide with the view port. If they all collide
      // then the last placement in the list will be used.
      $.each(priorityList, (_, pos: PowerTip.Placement) => {
        // place tooltip and find collisions
        const collisions = getViewportCollisions(
          this.placeTooltip(element, pos),
          this.tipElement.outerWidth() || this.options.defaultSize[0],
          this.tipElement.outerHeight() || this.options.defaultSize[1],
        );
        // continue/break if there were/weren't collisions (cash loop mechanism):
        return collisions !== Collision.none;
      });
    } else {
      // if we're not going to use the smart placement feature then just
      // compute the coordinates and do it
      this.placeTooltip(element, this.options.placement!);
    }
  }

  placeTooltip(element: Cash, placement: PowerTip.Placement) {
    let iterationCount = 0,
      tipWidth,
      tipHeight,
      coords = cssCoordinates();

    // set the tip to 0,0 to get the full expanded width
    coords.top = 0;
    coords.left = 0;
    this.tipElement.css(coords);

    // to support elastic tooltips we need to check for a change in the
    // rendered dimensions after the tooltip has been positioned
    do {
      // grab the current tip dimensions
      tipWidth = this.tipElement.outerWidth() || this.options.defaultSize[0];
      tipHeight = this.tipElement.outerHeight() || this.options.defaultSize[1];

      // get placement coordinates
      coords = this.placementCalculator.compute(
        element,
        placement,
        tipWidth,
        tipHeight,
        this.options.offset!,
      );

      // place the tooltip
      this.tipElement.css(coords);
    } while (
      // sanity check: limit to 5 iterations, and...
      ++iterationCount <= 5 &&
      // try again if the dimensions changed after placement
      (tipWidth !== this.tipElement.outerWidth() || tipHeight !== this.tipElement.outerHeight())
    );

    return coords;
  }

  closeDesyncedTip() {
    let isDesynced = false;
    // It is possible for the mouse cursor to leave an element without
    // firing the mouseleave or blur event. This most commonly happens when
    // the element is disabled under mouse cursor. If this happens it will
    // result in a desynced tooltip because the tooltip was never asked to
    // close. So we should periodically check for a desync situation and
    // close the tip if such a situation arises.
    if (this.scoped.isTipOpen && !this.scoped.isClosing && !this.scoped.delayInProgress) {
      // user moused onto another tip or active hover is disabled
      if (this.scoped.activeHover[0].hasActiveHover === false || this.scoped.activeHover.is(':disabled')) {
        isDesynced = true;
      } else {
        // hanging tip - have to test if mouse position is not over the
        // active hover and not over a tooltip set to let the user
        // interact with it.
        // for keyboard navigation: this only counts if the element does
        // not have focus.
        // for tooltips opened via the api: we need to check if it has
        // the forcedOpen flag.
        if (
          !isMouseOver(this.scoped.activeHover) &&
          !this.scoped.activeHover.is(':focus') &&
          !this.scoped.activeHover[0].forcedOpen
        ) {
          if (!isMouseOver(this.tipElement)) {
            isDesynced = true;
          }
        }
      }

      if (isDesynced) {
        // close the desynced tip
        this.hideTip(this.scoped.activeHover);
      }
    }
  }
}

// utility.js

function initTracking() {
  if (!session.mouseTrackingActive) {
    session.mouseTrackingActive = true;
    const $window = $(window);

    // grab the current viewport dimensions on load
    session.scrollLeft = window.scrollX;
    session.scrollTop = window.scrollY;
    session.windowWidth = $window.width();
    session.windowHeight = $window.height();

    // hook mouse move tracking
    document.addEventListener('mousemove', trackMouse);

    // hook viewport dimensions tracking
    window.addEventListener(
      'resize',
      function () {
        session.windowWidth = $window.width();
        session.windowHeight = $window.height();
      },
      { passive: true },
    );

    window.addEventListener(
      'scroll',
      function () {
        const x = window.scrollX,
          y = window.scrollY;
        if (x !== session.scrollLeft) {
          session.currentX += x - session.scrollLeft;
          session.scrollLeft = x;
        }
        if (y !== session.scrollTop) {
          session.currentY += y - session.scrollTop;
          session.scrollTop = y;
        }
      },
      { passive: true },
    );
  }
}

function trackMouse(event: PointerEvent) {
  session.currentX = event.pageX;
  session.currentY = event.pageY;
}

function isMouseOver(element: Cash) {
  const elementPosition = element.offset()!;
  return (
    session.currentX >= elementPosition.left &&
    session.currentX <= elementPosition.left + element.outerWidth() &&
    session.currentY >= elementPosition.top &&
    session.currentY <= elementPosition.top + element.outerHeight()
  );
}

function getViewportCollisions(coords: Coords, elementWidth: number, elementHeight: number) {
  const viewportTop = session.scrollTop,
    viewportLeft = session.scrollLeft,
    viewportBottom = viewportTop + session.windowHeight,
    viewportRight = viewportLeft + session.windowWidth;
  let collisions = Collision.none;
  if (
    coords.top < viewportTop ||
    Math.abs(Number(coords.bottom) - session.windowHeight) - elementHeight < viewportTop
  ) {
    collisions |= Collision.top;
  }
  if (
    Number(coords.top) + elementHeight > viewportBottom ||
    Math.abs(Number(coords.bottom) - session.windowHeight) > viewportBottom
  ) {
    collisions |= Collision.bottom;
  }
  if (coords.left < viewportLeft || Number(coords.right) + elementWidth > viewportRight) {
    collisions |= Collision.left;
  }
  if (Number(coords.left) + elementWidth > viewportRight || coords.right < viewportLeft) {
    collisions |= Collision.right;
  }
  return collisions;
}
