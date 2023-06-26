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

interface CallbackEntry {
  combo: string;
  callback: Callback;
  modifiers: string[];
  action: Action;
}

/**
 * mapping of special keycodes to their corresponding keys
 *
 * everything in this dictionary cannot use keypress events
 * so it has to be here to map to the correct keycodes for
 * keyup/keydown events
 */
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

/**
 * loop through the f keys, f1 to f19 and add them to the map
 * programatically
 */
for (let i = 1; i < 20; ++i) {
  MAP[111 + i] = 'f' + i;
}

/**
 * loop through to map numbers on the numeric keypad
 */
for (let i = 0; i <= 9; ++i) {
  // This needs to use a string cause otherwise since 0 is falsey
  // mousetrap will never fire for numpad 0 pressed as part of a keydown
  // event.
  //
  // @see https://github.com/ccampbell/mousetrap/pull/258
  MAP[i + 96] = i.toString();
}

/**
 * mapping for special characters so they can support
 *
 * this dictionary is only used incase you want to bind a
 * keyup or keydown event to one of these keys
 */
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

/**
 * this is a list of special strings you can use to map
 * to modifier keys when you specify your keyboard shortcuts
 */
const SPECIAL_ALIASES: Record<string, string> = {
  option: 'alt',
  command: 'meta',
  return: 'enter',
  escape: 'esc',
  plus: '+',
  mod: /Mac|iPod|iPhone|iPad/.test(navigator.platform) ? 'meta' : 'ctrl',
};

/**
 * takes the event and returns the key character
 */
const characterFromEvent = (e: KeyboardEvent): string => {
  // for keypress events we should return the character as is
  if (e.type == 'keypress') {
    const character = String.fromCharCode(e.which);

    // if the shift key is not pressed then it is safe to assume
    // that we want the character to be lowercase.  this means if
    // you accidentally have caps lock on then your key bindings
    // will continue to work
    //
    // the only side effect that might not be desired is if you
    // bind something like 'A' cause you want to trigger an
    // event when capital A is pressed caps lock will no longer
    // trigger the event.  shift+a will though.
    return e.shiftKey ? character : character.toLowerCase();
  }

  // for non keypress events the special maps are needed
  // with keydown and keyup events the character seems to always
  // come in as an uppercase character whether you are pressing shift
  // or not.  we should make sure it is always lowercase for comparisons
  return MAP[e.which] || KEYCODE_MAP[e.which] || String.fromCharCode(e.which).toLowerCase();
};

/**
 * checks if two arrays are equal
 */
const modifiersMatch = (modifiers1: string[], modifiers2: string[]): boolean => {
  return modifiers1.sort().join(',') === modifiers2.sort().join(',');
};

/**
 * takes a key event and figures out what the modifiers are
 */
const eventModifiers = (e: KeyboardEvent): string[] => {
  const modifiers: string[] = [];
  if (e.shiftKey) modifiers.push('shift');
  if (e.altKey) modifiers.push('alt');
  if (e.ctrlKey) modifiers.push('ctrl');
  if (e.metaKey) modifiers.push('meta');
  return modifiers;
};

/**
 * determines if the keycode specified is a modifier key or not
 */
const isModifier = (key: string): boolean => {
  return key == 'shift' || key == 'ctrl' || key == 'alt' || key == 'meta';
};

/**
 * reverses the map lookup so that we can look for specific keys
 * to see what can and can't use keypress
 */
const getReverseMap = (() => {
  /**
   * variable to store the flipped version of MAP from above
   * needed to check if we should use keypress or not when no action
   * is specified
   */
  let REVERSE_MAP: Record<string, string> | undefined;

  return () => {
    if (!REVERSE_MAP) {
      REVERSE_MAP = {};
      for (const key in MAP) {
        // pull out the numeric keypad from here cause keypress should
        // be able to detect the keys from the character
        if (parseInt(key, 10) > 95 && parseInt(key, 10) < 112) {
          continue;
        }

        if (MAP.hasOwnProperty(key)) {
          REVERSE_MAP[MAP[key]] = key;
        }
      }
    }
    return REVERSE_MAP;
  };
})();

/**
 * picks the best action based on the key combination
 */
const pickBestAction = (key: string, modifiers: string[], action?: Action): Action => {
  // if no action was picked in we should try to pick the one
  // that we think would work best for this key
  if (!action) {
    action = getReverseMap()[key] ? 'keydown' : 'keypress';
  }

  // modifier keys don't work as expected with keypress,
  // switch to keydown
  if (action == 'keypress' && modifiers.length) {
    action = 'keydown';
  }

  return action;
};

/**
 * Converts from a string key combination to an array
 */
const keysFromString = (combination: string): string[] => {
  if (combination === '+') {
    return ['+'];
  }

  combination = combination.replace(/\+{2}/g, '+plus');
  return combination.split('+');
};

/**
 * Gets info for a specific key combination
 */
const getKeyInfo = (combination: string, action?: Action): KeyInfo => {
  let key: string;
  const modifiers: string[] = [];

  for (key of keysFromString(combination)) {
    // normalize key names
    if (SPECIAL_ALIASES[key]) {
      key = SPECIAL_ALIASES[key];
    }

    // if this key is a modifier then add it to the list of modifiers
    if (isModifier(key)) {
      modifiers.push(key);
    }
  }

  return {
    key: key!,
    modifiers,
    action: pickBestAction(key!, modifiers, action), // depending on what the key combination is we will try to pick the best event for it
  };
};

export default class Mousetrap {
  /**
   * a list of all the callbacks setup via Mousetrap.bind()
   */
  private callbacks: Record<string, CallbackEntry[]> = {};

  constructor(targetElement?: HTMLElement | Document) {
    targetElement = targetElement || document;

    // start!
    targetElement.addEventListener('keypress', e => this.handleKeyEvent(e as KeyboardEvent));
    targetElement.addEventListener('keydown', e => this.handleKeyEvent(e as KeyboardEvent));
    targetElement.addEventListener('keyup', e => this.handleKeyEvent(e as KeyboardEvent));
  }

  /**
   * finds all callbacks that match based on the keycode, modifiers,
   * and action
   */
  private getMatches(character: string, modifiers: string[], e: Partial<KeyboardEvent>): CallbackEntry[] {
    const action = e.type;

    // if there are no events related to this keycode
    if (!this.callbacks[character]) {
      return [];
    }

    // if a modifier key is coming up on its own we should allow it
    if (action == 'keyup' && isModifier(character)) {
      modifiers = [character];
    }

    return this.callbacks[character].filter(
      callback =>
        // if the action we are looking for doesn't match the action we got
        // then we should keep going
        action == callback.action &&
        // if this is a keypress event and the meta key and control key
        // are not pressed that means that we need to only look at the
        // character, otherwise check the modifiers as well
        //
        // chrome will not fire a keypress if meta or control is down
        // safari will fire a keypress if meta or meta+shift is down
        // firefox will fire a keypress if meta or control is down
        ((action == 'keypress' && !e.metaKey && !e.ctrlKey) || modifiersMatch(modifiers, callback.modifiers))
    );
  }

  /**
   * actually calls the callback function
   *
   * always prevent default and stop propogation on the event
   */
  private fireCallback(callback: Callback, e: KeyboardEvent, combo: string): void {
    const el = e.target as HTMLElement;
    if (
      combo != 'esc' &&
      (el.tagName == 'INPUT' || el.tagName == 'SELECT' || el.tagName == 'TEXTAREA' || el.isContentEditable)
    )
      return;

    callback(e);
    e.preventDefault();
    e.stopPropagation();
  }

  /**
   * handles a character key event
   */
  private handleKey(character: string, modifiers: string[], e: KeyboardEvent): void {
    const callbacks = this.getMatches(character, modifiers, e);

    // loop through matching callbacks for this key event
    for (const callback of callbacks) {
      this.fireCallback(callback.callback, e, callback.combo);
    }
  }

  /**
   * handles a keydown event
   */
  private handleKeyEvent(e: KeyboardEvent): void {
    // normalize e.which for key events
    // @see http://stackoverflow.com/questions/4285627/javascript-keycode-vs-charcode-utter-confusion
    if (typeof e.which !== 'number') {
      (e as any).which = e.keyCode;
    }

    const character = characterFromEvent(e);

    // no character found then stop
    if (!character) {
      return;
    }

    this.handleKey(character, eventModifiers(e), e);
  }

  /**
   * binds a single keyboard combination
   */
  private bindSingle(combination: string, callback: Callback, action?: Action): void {
    const info = getKeyInfo(combination, action);

    // make sure to initialize array if this is the first time
    // a callback is added for this key
    this.callbacks[info.key] = this.callbacks[info.key] || [];

    // add this call back to the array
    this.callbacks[info.key].push({
      combo: combination,
      callback,
      modifiers: info.modifiers,
      action: info.action,
    });
  }

  /**
   * binds multiple combinations to the same callback
   */
  private bindMultiple(combinations: string[], callback: Callback, action?: Action): void {
    combinations.forEach(c => this.bindSingle(c, callback, action));
  }

  /**
   * binds an event to mousetrap
   *
   * can be a single key, a combination of keys separated with +,
   * or an array of keys
   *
   * be sure to list the modifier keys first to make sure that the
   * correct key ends up getting bound (the last key in the pattern)
   */
  bind(keys: string | string[], callback: Callback, action?: Action): Mousetrap {
    this.bindMultiple(keys instanceof Array ? keys : [keys], callback, action);
    return this;
  }
}
