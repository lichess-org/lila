/*!
 * hcg-tooltip v1.0.0 modified for Lichess.
 * @see https://www.html-code-generator.com/javascript/tooltip-library
 * @license MIT
 */

'use strict';

const INSTANCES: TooltipInstance[] = [];
const LIVE_DELEGATES: TooltipDelegate[] = [];
const liveOptOutElements: TooltipElement[] = [];
const DATA_TOOLTIP_ATTR = 'data-hcg-tooltip';
const TOOLTIP_FADE_DURATION = 150;

let activeInstance: TooltipInstance | null = null;

const DEFAULTS: TooltipOptions = {
  className: null,
  delayIn: 0,
  delayOut: 0,
  fallback: '',
  gravity: 'n',
  html: false,
  live: false,
  offset: 0,
  opacity: 0.8,
  title: 'title',
  trigger: 'hover',
};

function maybeCall(value: TooltipOptionValue, context: TooltipElement): string | number | boolean {
  return typeof value === 'function' ? value.call(context, context) : value;
}

function isElementInDOM(element: Element | null): element is TooltipElement {
  if (element?.nodeType !== 1) return false;
  return element.isConnected;
}

function toElements(target: string | TooltipElement | ArrayLike<Node> | null | undefined): TooltipElement[] {
  if (!target) return [];
  if (typeof target === 'string') {
    return Array.from(document.querySelectorAll<HTMLElement>(target));
  }
  if ('nodeType' in target && target.nodeType === 1) return [target];
  if ('length' in target) {
    return Array.prototype.slice.call(target).filter(function (node): node is TooltipElement {
      return node?.nodeType === 1;
    });
  }
  return [];
}

function mergeOptions(overrides?: Partial<TooltipOptions>): TooltipOptions {
  const merged = Object.assign({}, DEFAULTS);
  if (!overrides) return merged;
  Object.keys(overrides).forEach(function (key) {
    if (overrides[key] !== undefined) merged[key] = overrides[key];
  });
  return merged;
}

function normalizeGravity(value: TooltipOptionValue): string {
  const gravity = String(value == null ? '' : value).toLowerCase();
  return /^(n|s|e|w|nw|ne|sw|se)$/.test(gravity) ? gravity : String(DEFAULTS.gravity);
}

function applyCustomClass(
  tipElement: TooltipTipElement,
  className: TooltipOptions['className'],
  targetElement: TooltipElement,
) {
  const classValue = className == null ? '' : maybeCall(className, targetElement);
  if (!classValue) return;
  String(classValue)
    .trim()
    .split(/\s+/)
    .forEach(function (classToken) {
      if (classToken) tipElement.classList.add(classToken);
    });
}

function elementOffset(element: TooltipElement) {
  const rect = element.getBoundingClientRect();
  return {
    top: rect.top + window.scrollY,
    left: rect.left + window.scrollX,
    width: element.offsetWidth,
    height: element.offsetHeight,
  };
}

function nudgeIntoViewport(position: { top: number; left: number }, width: number, height: number) {
  const edgeInset = 4;
  const minLeft = window.scrollX + edgeInset;
  const minTop = window.scrollY + edgeInset;
  const maxLeft = Math.max(minLeft, window.scrollX + window.innerWidth - width - edgeInset);
  const maxTop = Math.max(minTop, window.scrollY + window.innerHeight - height - edgeInset);

  return {
    top: Math.min(Math.max(position.top, minTop), maxTop),
    left: Math.min(Math.max(position.left, minLeft), maxLeft),
  };
}

function dataAttributeToDatasetKey(attributeName: string) {
  return attributeName.slice(5).replace(/-([a-z])/g, function (_match: string, letter: string) {
    return letter.toUpperCase();
  });
}

function readElementAttribute(element: TooltipElement, attributeName: string) {
  if (attributeName === 'title') return element.dataset.originalTitle;
  if (attributeName.startsWith('data-')) {
    return element.dataset[dataAttributeToDatasetKey(attributeName)];
  }
  return element.getAttribute(attributeName);
}

function hideOtherActiveTooltip(exceptInstance: TooltipInstance | null) {
  if (activeInstance && activeInstance !== exceptInstance) {
    activeInstance.hoverState = 'out';
    activeInstance.clearTimers();
    activeInstance.hide();
  }
}

function hasLiveOptOut(element: TooltipElement) {
  return liveOptOutElements.includes(element);
}

function addLiveOptOut(element: TooltipElement) {
  if (!liveOptOutElements.includes(element)) {
    liveOptOutElements.push(element);
  }
}

function removeLiveOptOut(element: TooltipElement) {
  const index = liveOptOutElements.indexOf(element);
  if (index !== -1) liveOptOutElements.splice(index, 1);
}

function TooltipInstance(this: TooltipInstance, element: TooltipElement, options: TooltipOptions) {
  this.element = element;
  this.options = options;
  this.enabled = true;
  this.hoverState = null;
  this.tipElement = null;
  this.showTimer = null;
  this.hideTimer = null;
  this.boundEnterHandler = this.onEnter.bind(this);
  this.boundLeaveHandler = this.onLeave.bind(this);
  this.fixTitle();
}

TooltipInstance.prototype.fixTitle = function () {
  const element = this.element;
  if (element.title || !('originalTitle' in element.dataset)) {
    element.dataset.originalTitle = element.title || '';
    element.removeAttribute('title');
  }
};

TooltipInstance.prototype.getTitle = function () {
  const element = this.element;
  const options = this.options;
  let titleText;
  this.fixTitle();
  if (typeof options.title === 'string') {
    const attributeName = options.title === 'title' ? 'data-original-title' : options.title;
    titleText = readElementAttribute(element, attributeName);
  } else if (typeof options.title === 'function') {
    titleText = maybeCall(options.title, element);
  }
  titleText = String(titleText == null ? '' : titleText).trim();
  return titleText || options.fallback;
};

TooltipInstance.prototype.createTipElement = function () {
  if (!this.tipElement) {
    const tipElement = document.createElement('div') as TooltipTipElement;
    tipElement.className = 'hcg-tooltip';
    tipElement.setAttribute('role', 'tooltip');
    tipElement.bodyElement = document.createElement('div');
    tipElement.bodyElement.className = 'hcg-tooltip-body';
    tipElement.appendChild(tipElement.bodyElement);
    tipElement.arrowElement = document.createElement('div');
    tipElement.arrowElement.className = 'hcg-tooltip-arrow';
    tipElement.appendChild(tipElement.arrowElement);
    this.tipElement = tipElement;
  }
  this.tipElement._hcgTooltipPointee = this.element;
  return this.tipElement;
};

TooltipInstance.prototype.detachTip = function () {
  const tipElement = this.tipElement;
  if (!tipElement) return;
  if (tipElement.parentNode) tipElement.parentNode.removeChild(tipElement);
  this.tipElement = null;
  if (activeInstance === this) activeInstance = null;
};

TooltipInstance.prototype.show = function () {
  const titleText = this.getTitle();
  if (!titleText || !this.enabled) return;

  hideOtherActiveTooltip(this);

  const tipElement = this.createTipElement();
  const options = this.options;
  if (options.html) tipElement.bodyElement.innerHTML = titleText;
  else tipElement.bodyElement.textContent = titleText;

  tipElement.className = 'hcg-tooltip';
  tipElement.style.setProperty('--hcg-tooltip-opacity', String(options.opacity));
  tipElement.style.top = '0';
  tipElement.style.left = '0';
  tipElement.style.visibility = 'hidden';
  tipElement.style.display = 'block';
  document.body.appendChild(tipElement);

  const targetPosition = elementOffset(this.element);
  const tipWidth = tipElement.offsetWidth;
  const tipHeight = tipElement.offsetHeight;
  const gravity = normalizeGravity(maybeCall(options.gravity, this.element));
  let tooltipPosition;
  const offset = options.offset;

  switch (gravity.charAt(0)) {
    case 'n':
      tooltipPosition = {
        top: targetPosition.top + targetPosition.height + offset,
        left: targetPosition.left + targetPosition.width / 2 - tipWidth / 2,
      };
      break;
    case 's':
      tooltipPosition = {
        top: targetPosition.top - tipHeight - offset,
        left: targetPosition.left + targetPosition.width / 2 - tipWidth / 2,
      };
      break;
    case 'e':
      tooltipPosition = {
        top: targetPosition.top + targetPosition.height / 2 - tipHeight / 2,
        left: targetPosition.left - tipWidth - offset,
      };
      break;
    case 'w':
      tooltipPosition = {
        top: targetPosition.top + targetPosition.height / 2 - tipHeight / 2,
        left: targetPosition.left + targetPosition.width + offset,
      };
      break;
    default:
      tooltipPosition = {
        top: targetPosition.top + targetPosition.height + offset,
        left: targetPosition.left + targetPosition.width / 2 - tipWidth / 2,
      };
  }

  if (gravity.length === 2) {
    const secondary = gravity.charAt(1);
    const primary = gravity.charAt(0);
    if (primary === 'n' || primary === 's') {
      if (secondary === 'w') {
        tooltipPosition.left = targetPosition.left + targetPosition.width / 2 - 15;
      } else if (secondary === 'e') {
        tooltipPosition.left = targetPosition.left + targetPosition.width / 2 - tipWidth + 15;
      }
    } else if (secondary === 'n') {
      tooltipPosition.top = targetPosition.top + targetPosition.height / 2 - 15;
    } else if (secondary === 's') {
      tooltipPosition.top = targetPosition.top + targetPosition.height / 2 - tipHeight + 15;
    }
  }

  const originalTooltipPosition = tooltipPosition;
  tooltipPosition = nudgeIntoViewport(tooltipPosition, tipWidth, tipHeight);

  tipElement.style.top = tooltipPosition.top + 'px';
  tipElement.style.left = tooltipPosition.left + 'px';
  tipElement.classList.add('hcg-tooltip--' + gravity);
  applyCustomClass(tipElement, options.className, this.element);
  tipElement.arrowElement.className = 'hcg-tooltip-arrow hcg-tooltip-arrow-' + gravity.charAt(0);
  tipElement.arrowElement.style.transform = `translate(${originalTooltipPosition.left - tooltipPosition.left}px, ${
    originalTooltipPosition.top - tooltipPosition.top
  }px)`;
  tipElement.style.visibility = 'visible';
  void tipElement.offsetWidth;
  tipElement.classList.add('hcg-tooltip--visible');
  activeInstance = this;
};

TooltipInstance.prototype.hide = function () {
  const tipElement = this.tipElement;
  if (!tipElement) {
    if (activeInstance === this) activeInstance = null;
    return;
  }
  tipElement.classList.remove('hcg-tooltip--visible');
  if (activeInstance === this) activeInstance = null;
  setTimeout(() => {
    if (this.tipElement === tipElement && !tipElement.classList.contains('hcg-tooltip--visible')) {
      this.detachTip();
    }
  }, TOOLTIP_FADE_DURATION);
};

TooltipInstance.prototype.clearTimers = function () {
  if (this.showTimer != null) {
    clearTimeout(this.showTimer);
    this.showTimer = null;
  }
  if (this.hideTimer != null) {
    clearTimeout(this.hideTimer);
    this.hideTimer = null;
  }
};

TooltipInstance.prototype.schedule = function (
  hoverState: 'in' | 'out',
  callback: () => void,
  delay: number,
) {
  const instance = this;
  this.hoverState = hoverState;
  this.clearTimers();
  if (delay === 0) {
    callback.call(this);
  } else {
    const timerProperty = hoverState === 'in' ? 'showTimer' : 'hideTimer';
    this[timerProperty] = setTimeout(function () {
      if (instance.hoverState === hoverState) callback.call(instance);
    }, delay);
  }
};

TooltipInstance.prototype.onEnter = function () {
  this.schedule('in', this.show, this.options.delayIn);
};

TooltipInstance.prototype.onLeave = function () {
  this.schedule('out', this.hide, this.options.delayOut);
};

TooltipInstance.prototype.enable = function () {
  this.enabled = true;
  return this;
};

TooltipInstance.prototype.disable = function () {
  this.enabled = false;
  this.schedule('out', this.hide, 0);
  return this;
};

TooltipInstance.prototype.toggleEnabled = function () {
  this.enabled = !this.enabled;
  if (!this.enabled) this.schedule('out', this.hide, 0);
  return this;
};

TooltipInstance.prototype.destroy = function () {
  this.clearTimers();
  this.hide();
  this.detachTip();
  const element = this.element;
  if (this.liveBound) addLiveOptOut(element);
  if (element._hcgTooltip === this) delete element._hcgTooltip;
  unbindTriggers(element, this);
  const index = INSTANCES.indexOf(this);
  if (index !== -1) INSTANCES.splice(index, 1);
};

function getTriggerEvents(trigger: string, live: boolean) {
  if (trigger === 'focus') {
    return live ? { enter: 'focusin', leave: 'focusout' } : { enter: 'focus', leave: 'blur' };
  }
  return live ? { enter: 'mouseover', leave: 'mouseout' } : { enter: 'mouseenter', leave: 'mouseleave' };
}

function unbindTriggers(element: TooltipElement, instance: TooltipInstance) {
  ['hover', 'focus'].forEach(function (trigger) {
    const events = getTriggerEvents(trigger, false);
    element.removeEventListener(events.enter, instance.boundEnterHandler);
    element.removeEventListener(events.leave, instance.boundLeaveHandler);
  });
}

function getInstance(
  element: TooltipElement,
  options: Partial<TooltipOptions> | undefined,
  source: 'direct' | 'live',
) {
  if (source === 'live' && hasLiveOptOut(element)) return null;
  if (element._hcgTooltip) {
    if (options !== undefined) {
      const existingInstance = element._hcgTooltip;
      if (source === 'direct' || (source === 'live' && existingInstance.liveBound)) {
        existingInstance.options = mergeOptions(options);
      }
    }
    if (source === 'direct') {
      element._hcgTooltip.liveBound = false;
      removeLiveOptOut(element);
    }
    return element._hcgTooltip;
  }
  const TooltipInstanceConstructor = TooltipInstance as unknown as new (
    element: TooltipElement,
    options: TooltipOptions,
  ) => TooltipInstance;
  const instance = new TooltipInstanceConstructor(element, mergeOptions(options));
  instance.liveBound = source === 'live';
  element._hcgTooltip = instance;
  INSTANCES.push(instance);
  return instance;
}

function bindInstance(element: TooltipElement, options: TooltipOptions) {
  removeLiveOptOut(element);
  const instance = getInstance(element, options, 'direct');
  if (!instance) return null;
  unbindTriggers(element, instance);
  if (options.trigger === 'manual') return instance;
  const events = getTriggerEvents(options.trigger, false);
  element.addEventListener(events.enter, instance.boundEnterHandler);
  element.addEventListener(events.leave, instance.boundLeaveHandler);
  return instance;
}

function liveMatchTarget(node: EventTarget | null, selector: string): TooltipElement | null {
  const element = node instanceof Element ? node : null;
  return element?.closest(selector) as TooltipElement | null;
}

function removeLiveDelegateListeners(delegate: TooltipDelegate) {
  const events = getTriggerEvents(delegate.trigger, true);
  if (delegate.onEnter) document.removeEventListener(events.enter, delegate.onEnter);
  if (delegate.onLeave) document.removeEventListener(events.leave, delegate.onLeave, true);
}

function removeLiveDelegatesForSelector(selector: string) {
  for (let index = LIVE_DELEGATES.length - 1; index >= 0; index--) {
    if (LIVE_DELEGATES[index].selector === selector) {
      removeLiveDelegateListeners(LIVE_DELEGATES[index]);
      LIVE_DELEGATES.splice(index, 1);
    }
  }
}

function createLiveDelegate(selector: string, options: TooltipOptions) {
  const existingDelegate = LIVE_DELEGATES.find(function (delegate) {
    return delegate.selector === selector && delegate.trigger === options.trigger;
  });

  if (existingDelegate) {
    existingDelegate.options = mergeOptions(options);

    document.querySelectorAll<HTMLElement>(selector).forEach(function (element) {
      const tooltipElement = element as TooltipElement;
      const instance = tooltipElement._hcgTooltip;

      if (instance?.liveBound) {
        instance.options = mergeOptions(options);
      }
    });

    return existingDelegate;
  }

  removeLiveDelegatesForSelector(selector);

  const events = getTriggerEvents(options.trigger, true);

  const delegate = {
    selector,
    options: mergeOptions(options),
    trigger: options.trigger,

    onEnter(event: MouseEvent | FocusEvent) {
      const target = liveMatchTarget(event.target, selector);
      if (!target) return;

      const relatedTarget = event.relatedTarget;
      if (liveMatchTarget(relatedTarget, selector) === target) {
        return;
      }

      if (activeInstance && activeInstance.element !== target) {
        hideOtherActiveTooltip(null);
      }

      const instance = getInstance(target, delegate.options, 'live');
      if (instance) {
        if (delegate.trigger === 'hover') {
          target.addEventListener('mouseenter', instance.boundEnterHandler);
          target.addEventListener('mouseleave', instance.boundLeaveHandler);
        }
        instance.onEnter();
      }
    },

    onLeave(event: MouseEvent | FocusEvent) {
      const target = liveMatchTarget(event.target, selector);
      if (!target) return;

      const relatedTarget = event.relatedTarget;
      if (liveMatchTarget(relatedTarget, selector) === target) {
        return;
      }

      const instance = target._hcgTooltip;
      if (instance) {
        instance.onLeave();
      }
    },
  } satisfies TooltipDelegate;

  if (options.trigger !== 'manual') {
    document.addEventListener(events.enter, delegate.onEnter);
    document.addEventListener(events.leave, delegate.onLeave, true);
  }

  LIVE_DELEGATES.push(delegate);

  return delegate;
}

function parseDataNumber(value: string | undefined, useFloat: boolean): number | undefined {
  if (value == null || value === '') return undefined;
  const number = (useFloat ? parseFloat : parseInt)(value, 10);
  return isNaN(number) ? undefined : number;
}

function optionsFromDataset(element: TooltipElement): Partial<TooltipOptions> {
  const dataset = element.dataset;
  if (!('hcgTooltip' in dataset)) return {};

  const options: Partial<TooltipOptions> = {};
  const inlineText = dataset.hcgTooltip;
  if (inlineText && String(inlineText).trim()) {
    options.title = DATA_TOOLTIP_ATTR;
  }
  if (dataset.hcgTooltipGravity) options.gravity = dataset.hcgTooltipGravity;
  if (dataset.hcgTooltipTrigger) options.trigger = dataset.hcgTooltipTrigger;
  if (dataset.hcgTooltipClass) options.className = dataset.hcgTooltipClass;
  if (dataset.hcgTooltipFallback != null) options.fallback = dataset.hcgTooltipFallback;

  const delayIn = parseDataNumber(dataset.hcgTooltipDelayIn, false);
  if (delayIn !== undefined) options.delayIn = delayIn;

  const delayOut = parseDataNumber(dataset.hcgTooltipDelayOut, false);
  if (delayOut !== undefined) options.delayOut = delayOut;

  const offset = parseDataNumber(dataset.hcgTooltipOffset, false);
  if (offset !== undefined) options.offset = offset;

  const opacity = parseDataNumber(dataset.hcgTooltipOpacity, true);
  if (opacity !== undefined) options.opacity = opacity;

  if ('hcgTooltipHtml' in dataset) {
    const htmlValue = dataset.hcgTooltipHtml;
    options.html = !htmlValue || htmlValue === 'true' || htmlValue === '1';
  }

  return options;
}

function elementsWithDataTooltip(scope: Element | Document): TooltipElement[] {
  const elements: TooltipElement[] = [];
  if (scope instanceof HTMLElement && scope.hasAttribute(DATA_TOOLTIP_ATTR)) {
    elements.push(scope);
  }
  if (scope.querySelectorAll) {
    elements.push(...scope.querySelectorAll<HTMLElement>('[' + DATA_TOOLTIP_ATTR + ']'));
  }
  return elements;
}

function initFromData(
  root: string | Element | Document | null | undefined,
  overrides?: Partial<TooltipOptions>,
) {
  const scope = root == null ? document : typeof root === 'string' ? document.querySelector(root) : root;
  if (!scope?.querySelectorAll) return [];

  return elementsWithDataTooltip(scope).map(function (element) {
    return bindInstance(element, mergeOptions(Object.assign({}, overrides, optionsFromDataset(element))));
  });
}

function hcgTooltip(
  target: string | TooltipElement | ArrayLike<Node> | null | undefined,
  options: true | string | Partial<TooltipOptions> = {},
) {
  if (options === true) {
    const element = typeof target === 'string' ? document.querySelector<HTMLElement>(target) : target;
    const tooltipElement = element && 'nodeType' in element ? (element as TooltipElement) : null;
    return tooltipElement?._hcgTooltip ? tooltipElement._hcgTooltip : null;
  }

  if (typeof options === 'string') {
    const elements = toElements(target);
    elements.forEach(function (element) {
      const instance = element._hcgTooltip;
      const method = instance && (instance as unknown as Record<string, unknown>)[options];
      if (typeof method === 'function') method.call(instance);
    });
    return elements.length === 1
      ? (elements[0]?._hcgTooltip ?? null)
      : elements.map(element => element._hcgTooltip ?? null);
  }

  const mergedOptions = mergeOptions(options);

  if (mergedOptions.live) {
    if (typeof target !== 'string') {
      console.warn('hcg-tooltip: live mode requires a selector string.');
      return [];
    }
    createLiveDelegate(target, mergedOptions);
    return [];
  }

  const instances = toElements(target).map(function (element) {
    return bindInstance(element, mergedOptions);
  });
  return instances.length === 1 ? instances[0] : instances;
}

hcgTooltip.defaults = DEFAULTS;
hcgTooltip.get = function (target: string | TooltipElement) {
  const element = (
    typeof target === 'string' ? document.querySelector<HTMLElement>(target) : target
  ) as TooltipElement | null; // oxlint-disable-line typescript/no-unnecessary-type-assertion
  return element?._hcgTooltip ? element._hcgTooltip : null;
};
hcgTooltip.initFromData = initFromData;

function syncInstanceAfterTipRemoval(tipElement: TooltipTipElement, targetElement: TooltipElement | null) {
  let instance: TooltipInstance | null = null;
  if (targetElement?._hcgTooltip?.tipElement === tipElement) {
    instance = targetElement._hcgTooltip;
  } else {
    for (let index = 0; index < INSTANCES.length; index++) {
      if (INSTANCES[index].tipElement === tipElement) {
        instance = INSTANCES[index];
        break;
      }
    }
  }
  if (!instance) return;
  instance.tipElement = null;
  instance.hoverState = 'out';
  instance.clearTimers();
  if (activeInstance === instance) activeInstance = null;
}

hcgTooltip.revalidate = function () {
  document.querySelectorAll<HTMLDivElement>('.hcg-tooltip').forEach(function (element) {
    const tipElement = element as TooltipTipElement;
    const targetElement = tipElement._hcgTooltipPointee ?? null;
    if (targetElement && isElementInDOM(targetElement)) return;
    if (tipElement.parentNode) tipElement.parentNode.removeChild(tipElement);
    syncInstanceAfterTipRemoval(tipElement, targetElement);
  });
};

hcgTooltip.destroyAll = function () {
  activeInstance = null;
  LIVE_DELEGATES.slice().forEach(function (delegate) {
    removeLiveDelegateListeners(delegate);
  });
  LIVE_DELEGATES.length = 0;
  INSTANCES.slice().forEach(function (instance) {
    instance.destroy();
  });
  liveOptOutElements.length = 0;
};

export default hcgTooltip;
