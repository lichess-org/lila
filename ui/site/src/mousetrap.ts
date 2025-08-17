/**
 * Adapted from https://craig.is/killing/mice
 * Copyright 2012-2017 Craig Campbell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { isEquivalent } from 'lib/algo';

type Action = 'keypress' | 'keydown' | 'keyup';

type Callback = (e: KeyboardEvent) => void;

interface KeyInfo {
  key: string;
  modifiers: string[];
  action: Action;
}

interface Binding {
  combination: string;
  callback: Callback;
  modifiers: string[];
  action: Action;
}

const MAP: Record<string, string> = {
  8: 'backspace',
  9: 'tab',
  13: 'enter',
  16: 'shift',
  17: 'ctrl',
  18: 'alt',
  20: 'capslock',
  27: 'esc',
  32: 'space',
  33: 'pageup',
  34: 'pagedown',
  35: 'end',
  36: 'home',
  37: 'left',
  38: 'up',
  39: 'right',
  40: 'down',
  45: 'ins',
  46: 'del',
  91: 'meta',
  93: 'meta',
  224: 'meta',
};
for (let i = 1; i < 20; ++i) MAP[111 + i] = 'f' + i;
for (let i = 0; i <= 9; ++i) MAP[i + 96] = i.toString();

const KEYCODE_MAP: Record<string, string> = {
  106: '*',
  107: '+',
  109: '-',
  110: '.',
  111: '/',
  186: ';',
  187: '=',
  188: ',',
  189: '-',
  190: '.',
  191: '/',
  192: '`',
  219: '[',
  220: '\\',
  221: ']',
  222: "'",
};

const SPECIAL_ALIASES: Record<string, string> = {
  option: 'alt',
  command: 'meta',
  return: 'enter',
  escape: 'esc',
  plus: '+',
  mod: /Mac|iPod|iPhone|iPad/.test(navigator.platform) ? 'meta' : 'ctrl',
};

const keyFromEvent = (e: KeyboardEvent): string => {
  if (e.type === 'keypress') {
    const character = String.fromCharCode(e.which);
    return e.shiftKey ? character : character.toLowerCase(); // ignore caps lock
  }
  return MAP[e.which] || KEYCODE_MAP[e.which] || String.fromCharCode(e.which).toLowerCase();
};

const modifiersMatch = (a: string[], b: string[]): boolean => a.sort().join(',') === b.sort().join(',');

const eventModifiers = (e: KeyboardEvent): string[] => {
  const modifiers: string[] = [];
  if (e.shiftKey) modifiers.push('shift');
  if (e.altKey) modifiers.push('alt');
  if (e.ctrlKey) modifiers.push('ctrl');
  if (e.metaKey) modifiers.push('meta');
  return modifiers;
};

const isModifier = (key: string): boolean =>
  key === 'shift' || key === 'ctrl' || key === 'alt' || key === 'meta';

const getReverseMap = (() => {
  let REVERSE_MAP: Record<string, string> | undefined;
  return () => {
    if (!REVERSE_MAP) {
      REVERSE_MAP = {};
      for (const key in MAP) {
        if (parseInt(key, 10) > 95 && parseInt(key, 10) < 112) continue; // skip numeric keypad
        if (Object.prototype.hasOwnProperty.call(MAP, key)) {
          REVERSE_MAP[MAP[key]] = key;
        }
      }
    }
    return REVERSE_MAP;
  };
})();

const pickBestAction = (key: string, modifiers: string[], action?: Action): Action => {
  action = action || (getReverseMap()[key] ? 'keydown' : 'keypress');
  if (action === 'keypress' && modifiers.length) return 'keydown'; // modifiers incompatible with keypress
  return action;
};

const keysFromString = (combination: string): string[] =>
  combination === '+' ? ['+'] : combination.replace(/\+{2}/g, '+plus').split('+');

const getKeyInfo = (combination: string, action?: Action): KeyInfo => {
  let key: string;
  const modifiers: string[] = [];

  for (key of keysFromString(combination)) {
    key = SPECIAL_ALIASES[key] || key; // normalize
    if (isModifier(key)) modifiers.push(key);
  }

  return {
    key: key!,
    modifiers,
    action: pickBestAction(key!, modifiers, action),
  };
};

export default class Mousetrap {
  private bindings: Record<string, Binding[]> = {};

  constructor(targetElement?: HTMLElement | Document) {
    targetElement = targetElement || document;
    targetElement.addEventListener('keypress', e => this.handleKeyEvent(e as KeyboardEvent));
    targetElement.addEventListener('keydown', e => this.handleKeyEvent(e as KeyboardEvent));
    targetElement.addEventListener('keyup', e => this.handleKeyEvent(e as KeyboardEvent));
  }

  /**
   * Binds an event to mousetrap.
   *
   * Can be a single key, a combination of keys separated with +,
   * or an array of such combinations.
   *
   * When adding modifiers, list the actual key last.
   */
  bind = (
    combinations: string | string[],
    callback: Callback,
    action?: Action,
    multiple = true,
  ): Mousetrap => {
    for (const combo of Array.isArray(combinations) ? combinations : [combinations]) {
      const info = getKeyInfo(combo, action);
      this.bindings[info.key] ??= [];
      if (multiple || !this.bindings[info.key].some(b => isEquivalent(b.modifiers, info.modifiers)))
        this.bindings[info.key].push({
          combination: combo,
          callback,
          modifiers: info.modifiers,
          action: info.action,
        });
    }
    return this;
  };

  unbind = (key: string): void => {
    this.bindings[key]?.forEach((b, i) => {
      if (b.modifiers.length === 0) this.bindings[key].splice(i, 1);
    });
  };

  private handleKeyEvent = (e: KeyboardEvent) => {
    if (typeof e.which !== 'number') (e as any).which = e.keyCode; // normalize

    const el = e.target as HTMLElement;

    for (const binding of this.getMatches(e)) {
      if (
        binding.combination === 'esc' ||
        (el.tagName !== 'INPUT' &&
          el.tagName !== 'SELECT' &&
          el.tagName !== 'TEXTAREA' &&
          !el.isContentEditable &&
          !el.hasAttribute('trap-bypass'))
      ) {
        binding.callback(e);
        e.preventDefault();
        e.stopPropagation();
      }
    }
  };

  private getMatches = (e: KeyboardEvent): Binding[] => {
    const key = keyFromEvent(e);
    const action = e.type;
    const modifiers = action === 'keyup' && isModifier(key) ? [key] : eventModifiers(e);
    return (this.bindings[key] || []).filter(
      binding =>
        action === binding.action &&
        // Chrome will not fire a keypress if meta or control is down,
        // Safari will fire a keypress if meta or meta+shift is down,
        // Firefox will fire a keypress if meta or control is down
        ((action === 'keypress' && !e.metaKey && !e.ctrlKey) || modifiersMatch(modifiers, binding.modifiers)),
    );
  };
}
