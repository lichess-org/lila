/// <reference types="lishogi" />
import { Hooks } from 'snabbdom/hooks';
import { Puzzle } from './interfaces';
export declare function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks;
export declare function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks;
export declare const getNow: () => number;
export declare const uciToLastMove: (uci: string) => [Key, Key];
export declare const puzzlePov: (puzzle: Puzzle) => 'white' | 'black';
export declare const loadSound: (file: string, volume?: number | undefined, delay?: number | undefined) => () => any;
export declare const sound: {
  move: (take: boolean) => any;
  good: () => any;
  wrong: () => any;
  end: () => any;
};
