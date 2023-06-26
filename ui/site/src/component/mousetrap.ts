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
  callback: Callback;
  modifiers: string[];
  action: Action;
  seq?: string;
  level?: number;
  combo: string;
}

/**
 * mapping of special keycodes to their corresponding keys
 *
 * everything in this dictionary cannot use keypress events
 * so it has to be here to map to the correct keycodes for
 * keyup/keydown events
 */
const _MAP: Record<string, string> = {
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
  _MAP[111 + i] = 'f' + i;
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
  _MAP[i + 96] = i.toString();
}

/**
 * mapping for special characters so they can support
 *
 * this dictionary is only used incase you want to bind a
 * keyup or keydown event to one of these keys
 */
const _KEYCODE_MAP: Record<string, string> = {
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
const _SPECIAL_ALIASES: Record<string, string> = {
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
function _characterFromEvent(e: KeyboardEvent): string {
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
  if (_MAP[e.which]) {
    return _MAP[e.which];
  }

  if (_KEYCODE_MAP[e.which]) {
    return _KEYCODE_MAP[e.which];
  }

  // if it is not in the special map

  // with keydown and keyup events the character seems to always
  // come in as an uppercase character whether you are pressing shift
  // or not.  we should make sure it is always lowercase for comparisons
  return String.fromCharCode(e.which).toLowerCase();
}

/**
 * checks if two arrays are equal
 */
function _modifiersMatch(modifiers1: string[], modifiers2: string[]): boolean {
  return modifiers1.sort().join(',') === modifiers2.sort().join(',');
}

/**
 * takes a key event and figures out what the modifiers are
 */
function _eventModifiers(e: KeyboardEvent): string[] {
  const modifiers: string[] = [];
  if (e.shiftKey) modifiers.push('shift');
  if (e.altKey) modifiers.push('alt');
  if (e.ctrlKey) modifiers.push('ctrl');
  if (e.metaKey) modifiers.push('meta');
  return modifiers;
}

/**
 * determines if the keycode specified is a modifier key or not
 */
function _isModifier(key: string): boolean {
  return key == 'shift' || key == 'ctrl' || key == 'alt' || key == 'meta';
}

/**
 * variable to store the flipped version of _MAP from above
 * needed to check if we should use keypress or not when no action
 * is specified
 */
let _REVERSE_MAP: Record<string, string> | undefined;

/**
 * reverses the map lookup so that we can look for specific keys
 * to see what can and can't use keypress
 *
 * @return {Object}
 */
function _getReverseMap(): Record<string, string> {
  if (!_REVERSE_MAP) {
    _REVERSE_MAP = {};
    for (const key in _MAP) {
      // pull out the numeric keypad from here cause keypress should
      // be able to detect the keys from the character
      if (parseInt(key, 10) > 95 && parseInt(key, 10) < 112) {
        continue;
      }

      if (_MAP.hasOwnProperty(key)) {
        _REVERSE_MAP[_MAP[key]] = key;
      }
    }
  }
  return _REVERSE_MAP;
}

/**
 * picks the best action based on the key combination
 */
function _pickBestAction(key: string, modifiers: string[], action?: Action): Action {
  // if no action was picked in we should try to pick the one
  // that we think would work best for this key
  if (!action) {
    action = _getReverseMap()[key] ? 'keydown' : 'keypress';
  }

  // modifier keys don't work as expected with keypress,
  // switch to keydown
  if (action == 'keypress' && modifiers.length) {
    action = 'keydown';
  }

  return action;
}

/**
 * Converts from a string key combination to an array
 *
 * @param  {string} combination like "command+shift+l"
 * @return {Array}
 */
function _keysFromString(combination: string): string[] {
  if (combination === '+') {
    return ['+'];
  }

  combination = combination.replace(/\+{2}/g, '+plus');
  return combination.split('+');
}

/**
 * Gets info for a specific key combination
 */
function _getKeyInfo(combination: string, action?: Action): KeyInfo {
  let key: string;
  const modifiers: string[] = [];

  // take the keys from this pattern and figure out what the actual
  // pattern is all about
  const keys = _keysFromString(combination);

  for (let i = 0; i < keys.length; ++i) {
    key = keys[i];

    // normalize key names
    if (_SPECIAL_ALIASES[key]) {
      key = _SPECIAL_ALIASES[key];
    }

    // if this key is a modifier then add it to the list of modifiers
    if (_isModifier(key)) {
      modifiers.push(key);
    }
  }

  // depending on what the key combination is
  // we will try to pick the best event for it
  action = _pickBestAction(key!, modifiers, action);

  return {
    key: key!,
    modifiers,
    action,
  };
}

export default class Mousetrap {
  /**
   * a list of all the callbacks setup via Mousetrap.bind()
   */
  private _callbacks: Record<string, CallbackEntry[]> = {};

  /**
   * keeps track of what level each sequence is at since multiple
   * sequences can start out with the same sequence
   */
  private _sequenceLevels: Record<string, number> = {};

  /**
   * variable to store the setTimeout call
   */
  private _resetTimer: number | undefined;

  /**
   * temporary state where we will ignore the next keyup
   */
  private _ignoreNextKeyup: boolean | string = false;

  /**
   * temporary state where we will ignore the next keypress
   */
  private _ignoreNextKeypress = false;

  /**
   * are we currently inside of a sequence?
   * type of action ("keyup" or "keydown" or "keypress") or false
   */
  private _nextExpectedAction: boolean | string = false;

  constructor(targetElement?: HTMLElement | Document) {
    targetElement = targetElement || document;

    // start!
    targetElement.addEventListener('keypress', e => this._handleKeyEvent(e as KeyboardEvent));
    targetElement.addEventListener('keydown', e => this._handleKeyEvent(e as KeyboardEvent));
    targetElement.addEventListener('keyup', e => this._handleKeyEvent(e as KeyboardEvent));
  }

  /**
   * resets all sequence counters except for the ones passed in
   */
  private _resetSequences(doNotReset?: Record<string, number>): void {
    doNotReset = doNotReset || {};

    let activeSequences = false;

    for (const key in this._sequenceLevels) {
      if (doNotReset[key]) {
        activeSequences = true;
        continue;
      }
      this._sequenceLevels[key] = 0;
    }

    if (!activeSequences) {
      this._nextExpectedAction = false;
    }
  }

  /**
   * finds all callbacks that match based on the keycode, modifiers,
   * and action
   */
  private _getMatches(
    character: string,
    modifiers: string[],
    e: Partial<KeyboardEvent>,
    sequenceName?: string,
    combination?: string,
    level?: number
  ): CallbackEntry[] {
    var i;
    var callback;
    var matches = [];
    const action = e.type;

    // if there are no events related to this keycode
    if (!this._callbacks[character]) {
      return [];
    }

    // if a modifier key is coming up on its own we should allow it
    if (action == 'keyup' && _isModifier(character)) {
      modifiers = [character];
    }

    // loop through all callbacks for the key that was pressed
    // and see if any of them match
    for (i = 0; i < this._callbacks[character].length; ++i) {
      callback = this._callbacks[character][i];

      // if a sequence name is not specified, but this is a sequence at
      // the wrong level then move onto the next match
      if (!sequenceName && callback.seq && this._sequenceLevels[callback.seq] != callback.level) {
        continue;
      }

      // if the action we are looking for doesn't match the action we got
      // then we should keep going
      if (action != callback.action) {
        continue;
      }

      // if this is a keypress event and the meta key and control key
      // are not pressed that means that we need to only look at the
      // character, otherwise check the modifiers as well
      //
      // chrome will not fire a keypress if meta or control is down
      // safari will fire a keypress if meta or meta+shift is down
      // firefox will fire a keypress if meta or control is down
      if (
        (action == 'keypress' && !e.metaKey && !e.ctrlKey) ||
        _modifiersMatch(modifiers, callback.modifiers)
      ) {
        // when you bind a combination or sequence a second time it
        // should overwrite the first one.  if a sequenceName or
        // combination is specified in this call it does just that
        //
        // @todo make deleting its own method?
        var deleteCombo = !sequenceName && callback.combo == combination;
        var deleteSequence = sequenceName && callback.seq == sequenceName && callback.level == level;
        if (deleteCombo || deleteSequence) {
          this._callbacks[character].splice(i, 1);
        }

        matches.push(callback);
      }
    }

    return matches;
  }

  /**
   * actually calls the callback function
   *
   * always prevent default and stop propogation on the event
   */
  private _fireCallback(callback: Callback, e: KeyboardEvent, combo: string): void {
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
  private _handleKey(character: string, modifiers: string[], e: KeyboardEvent): void {
    var callbacks = this._getMatches(character, modifiers, e);
    var doNotReset: Record<string, number> = {};
    var maxLevel = 0;
    var processedSequenceCallback = false;

    // Calculate the maxLevel for sequences so we can only execute the longest callback sequence
    for (let i = 0; i < callbacks.length; ++i) {
      if (callbacks[i].seq) {
        maxLevel = Math.max(maxLevel, callbacks[i].level || 0);
      }
    }

    // loop through matching callbacks for this key event
    for (const callback of callbacks) {
      // fire for all sequence callbacks
      // this is because if for example you have multiple sequences
      // bound such as "g i" and "g t" they both need to fire the
      // callback for matching g cause otherwise you can only ever
      // match the first one
      if (callback.seq) {
        // only fire callbacks for the maxLevel to prevent
        // subsequences from also firing
        //
        // for example 'a option b' should not cause 'option b' to fire
        // even though 'option b' is part of the other sequence
        //
        // any sequences that do not match here will be discarded
        // below by the _resetSequences call
        if (callback.level != maxLevel) {
          continue;
        }

        processedSequenceCallback = true;

        // keep a list of which sequences were matches for later
        doNotReset[callback.seq] = 1;
        this._fireCallback(callback.callback, e, callback.combo);
        continue;
      }

      // if there were no sequence matches but we are still here
      // that means this is a regular match so we should fire that
      if (!processedSequenceCallback) {
        this._fireCallback(callback.callback, e, callback.combo);
      }
    }

    // if the key you pressed matches the type of sequence without
    // being a modifier (ie "keyup" or "keypress") then we should
    // reset all sequences that were not matched by this event
    //
    // this is so, for example, if you have the sequence "h a t" and you
    // type "h e a r t" it does not match.  in this case the "e" will
    // cause the sequence to reset
    //
    // modifier keys are ignored because you can have a sequence
    // that contains modifiers such as "enter ctrl+space" and in most
    // cases the modifier key will be pressed before the next key
    //
    // also if you have a sequence such as "ctrl+b a" then pressing the
    // "b" key will trigger a "keypress" and a "keydown"
    //
    // the "keydown" is expected when there is a modifier, but the
    // "keypress" ends up matching the _nextExpectedAction since it occurs
    // after and that causes the sequence to reset
    //
    // we ignore keypresses in a sequence that directly follow a keydown
    // for the same character
    var ignoreThisKeypress = e.type == 'keypress' && this._ignoreNextKeypress;
    if (e.type == this._nextExpectedAction && !_isModifier(character) && !ignoreThisKeypress) {
      this._resetSequences(doNotReset);
    }

    this._ignoreNextKeypress = processedSequenceCallback && e.type == 'keydown';
  }

  /**
   * handles a keydown event
   *
   * @param {Event} e
   * @returns void
   */
  private _handleKeyEvent(e: KeyboardEvent): void {
    // normalize e.which for key events
    // @see http://stackoverflow.com/questions/4285627/javascript-keycode-vs-charcode-utter-confusion
    if (typeof e.which !== 'number') {
      (e as any).which = e.keyCode;
    }

    var character = _characterFromEvent(e);

    // no character found then stop
    if (!character) {
      return;
    }

    // need to use === for the character check because the character can be 0
    if (e.type == 'keyup' && this._ignoreNextKeyup === character) {
      this._ignoreNextKeyup = false;
      return;
    }

    this._handleKey.call(this, character, _eventModifiers(e), e);
  }

  /**
   * called to set a 1 second timeout on the specified sequence
   *
   * this is so after each key press in the sequence you have 1 second
   * to press the next key before you have to start over
   *
   * @returns void
   */
  private _resetSequenceTimer() {
    clearTimeout(this._resetTimer);
    this._resetTimer = setTimeout(() => this._resetSequences(), 1000);
  }

  /**
   * binds a key sequence to an event
   */
  private _bindSequence(combo: string, keys: string[], callback: Callback, action?: Action): void {
    // start off by adding a sequence level record for this combination
    // and setting the level to 0
    this._sequenceLevels[combo] = 0;

    /**
     * callback to increase the sequence level for this sequence and reset
     * all other sequences that were active
     */
    const _increaseSequence = (nextAction: string) => {
      return () => {
        this._nextExpectedAction = nextAction;
        ++this._sequenceLevels[combo];
        this._resetSequenceTimer();
      };
    };

    /**
     * wraps the specified callback inside of another function in order
     * to reset all sequence counters as soon as this sequence is done
     *
     * @param {Event} e
     * @returns void
     */
    const _callbackAndReset = (e: KeyboardEvent) => {
      this._fireCallback(callback, e, combo);

      // we should ignore the next key up if the action is key down
      // or keypress.  this is so if you finish a sequence and
      // release the key the final key will not trigger a keyup
      if (action !== 'keyup') {
        this._ignoreNextKeyup = _characterFromEvent(e);
      }

      // weird race condition if a sequence ends with the key
      // another sequence begins with
      setTimeout(() => this._resetSequences(), 10);
    };

    // loop through keys one at a time and bind the appropriate callback
    // function.  for any key leading up to the final one it should
    // increase the sequence. after the final, it should reset all sequences
    //
    // if an action is specified in the original bind call then that will
    // be used throughout.  otherwise we will pass the action that the
    // next key in the sequence should match.  this allows a sequence
    // to mix and match keypress and keydown events depending on which
    // ones are better suited to the key provided
    for (let i = 0; i < keys.length; ++i) {
      var isFinal = i + 1 === keys.length;
      var wrappedCallback = isFinal
        ? _callbackAndReset
        : _increaseSequence(action || _getKeyInfo(keys[i + 1]).action);
      this._bindSingle(keys[i], wrappedCallback, action, combo, i);
    }
  }

  /**
   * binds a single keyboard combination
   */
  private _bindSingle(
    combination: string,
    callback: Callback,
    action?: Action,
    sequenceName?: string,
    level?: number
  ): void {
    // make sure multiple spaces in a row become a single space
    combination = combination.replace(/\s+/g, ' ');

    var sequence = combination.split(' ');
    var info;

    // if this pattern is a sequence of keys then run through this method
    // to reprocess each pattern one key at a time
    if (sequence.length > 1) {
      this._bindSequence(combination, sequence, callback, action);
      return;
    }

    info = _getKeyInfo(combination, action);

    // make sure to initialize array if this is the first time
    // a callback is added for this key
    this._callbacks[info.key] = this._callbacks[info.key] || [];

    // remove an existing match if there is one
    this._getMatches(info.key, info.modifiers, { type: info.action }, sequenceName, combination, level);

    // add this call back to the array
    // if it is a sequence put it at the beginning
    // if not put it at the end
    //
    // this is important because the way these are processed expects
    // the sequence ones to come first
    this._callbacks[info.key][sequenceName ? 'unshift' : 'push']({
      callback,
      modifiers: info.modifiers,
      action: info.action,
      seq: sequenceName,
      level: level,
      combo: combination,
    });
  }

  /**
   * binds multiple combinations to the same callback
   */
  private _bindMultiple(combinations: string[], callback: Callback, action?: Action): void {
    combinations.forEach(c => this._bindSingle(c, callback, action));
  }

  /**
   * binds an event to mousetrap
   *
   * can be a single key, a combination of keys separated with +,
   * an array of keys, or a sequence of keys separated by spaces
   *
   * be sure to list the modifier keys first to make sure that the
   * correct key ends up getting bound (the last key in the pattern)
   */
  bind(keys: string | string[], callback: Callback, action?: Action): Mousetrap {
    this._bindMultiple.call(this, keys instanceof Array ? keys : [keys], callback, action);
    return this;
  }
}
