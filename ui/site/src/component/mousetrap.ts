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

const characterFromEvent = (e: KeyboardEvent): string => {
  if (e.type == 'keypress') {
    const character = String.fromCharCode(e.which);
    return e.shiftKey ? character : character.toLowerCase(); // ignore caps lock
  }
  return MAP[e.which] || KEYCODE_MAP[e.which] || String.fromCharCode(e.which).toLowerCase();
};

const modifiersMatch = (modifiers1: string[], modifiers2: string[]): boolean => {
  return modifiers1.sort().join(',') === modifiers2.sort().join(',');
};

const eventModifiers = (e: KeyboardEvent): string[] => {
  const modifiers: string[] = [];
  if (e.shiftKey) modifiers.push('shift');
  if (e.altKey) modifiers.push('alt');
  if (e.ctrlKey) modifiers.push('ctrl');
  if (e.metaKey) modifiers.push('meta');
  return modifiers;
};

const isModifier = (key: string): boolean => {
  return key == 'shift' || key == 'ctrl' || key == 'alt' || key == 'meta';
};

const getReverseMap = (() => {
  let REVERSE_MAP: Record<string, string> | undefined;
  return () => {
    if (!REVERSE_MAP) {
      REVERSE_MAP = {};
      for (const key in MAP) {
        if (parseInt(key, 10) > 95 && parseInt(key, 10) < 112) {
          // pull out the numeric keypad from here cause keypress should
          // be able to detect the keys from the character
          continue;
        }
        if (Object.prototype.hasOwnProperty.call(MAP, key)) {
          REVERSE_MAP[MAP[key]] = key;
        }
      }
    }
    return REVERSE_MAP;
  };
})();

const pickBestAction = (key: string, modifiers: string[], action?: Action): Action => {
  // if no action was picked in we should try to pick the one
  // that we think would work best for this key
  action = action || getReverseMap()[key] ? 'keydown' : 'keypress';

  // modifier keys don't work as expected with keypress, switch to keydown
  if (action == 'keypress' && modifiers.length) return 'keydown';

  return action;
};

const keysFromString = (combination: string): string[] => {
  if (combination === '+') return ['+'];
  return combination.replace(/\+{2}/g, '+plus').split('+');
};

/**
 * Gets info for a specific key combination
 */
const getKeyInfo = (combination: string, action?: Action): KeyInfo => {
  let key: string;
  const modifiers: string[] = [];

  for (key of keysFromString(combination)) {
    // normalize key names
    if (SPECIAL_ALIASES[key]) key = SPECIAL_ALIASES[key];

    // if this key is a modifier then add it to the list of modifiers
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
   * binds an event to mousetrap
   *
   * can be a single key, a combination of keys separated with +,
   * or an array of such combinations
   *
   * be sure to list the modifier keys first to make sure that the
   * correct key ends up getting bound (the last key in the pattern)
   */
  bind(combinations: string | string[], callback: Callback, action?: Action): Mousetrap {
    this.bindMultiple(combinations instanceof Array ? combinations : [combinations], callback, action);
    return this;
  }

  private bindMultiple(combinations: string[], callback: Callback, action?: Action): void {
    for (const combination of combinations) this.bindSingle(combination, callback, action);
  }

  private bindSingle(combination: string, callback: Callback, action?: Action): void {
    const info = getKeyInfo(combination, action);
    (this.bindings[info.key] = this.bindings[info.key] || []).push({
      combination,
      callback,
      modifiers: info.modifiers,
      action: info.action,
    });
  }

  private handleKeyEvent(e: KeyboardEvent): void {
    // normalize (see http://stackoverflow.com/questions/4285627/javascript-keycode-vs-charcode-utter-confusion)
    if (typeof e.which !== 'number') {
      (e as any).which = e.keyCode;
    }

    const el = e.target as HTMLElement;

    for (const binding of this.getMatches(e)) {
      if (
        binding.combination == 'esc' ||
        (el.tagName != 'INPUT' && el.tagName != 'SELECT' && el.tagName != 'TEXTAREA' && !el.isContentEditable)
      ) {
        binding.callback(e);
        e.preventDefault();
        e.stopPropagation();
      }
    }
  }

  private getMatches(e: KeyboardEvent): Binding[] {
    const character = characterFromEvent(e);
    const action = e.type;
    const modifiers = action == 'keyup' && isModifier(character) ? [character] : eventModifiers(e);
    return (this.bindings[character] || []).filter(
      binding =>
        action == binding.action &&
        // chrome will not fire a keypress if meta or control is down
        // safari will fire a keypress if meta or meta+shift is down
        // firefox will fire a keypress if meta or control is down
        ((action == 'keypress' && !e.metaKey && !e.ctrlKey) || modifiersMatch(modifiers, binding.modifiers))
    );
  }
}
