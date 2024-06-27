import type { BotInfo, Mapping, Mappings } from '../types';
import type { Editor } from './editor';
import type { Pane } from './pane';
import type { Setting } from './setting';
//import type { Panel } from './panel';
import type { ZerofishBotEditor } from '../zerofishBot';
//export type { Setting, Editor, Panel };

export interface BotInfoReader extends BotInfo {
  readonly [key: string]: any;
}

export interface EditorHost {
  readonly view: HTMLElement;
  readonly editor: Editor;
  readonly bot: ZerofishBotEditor;
  readonly botDefault: BotInfoReader;
  readonly cleanups: (() => void)[];
  update(): void;
}

export interface PaneInfo {
  type?: AnyType;
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  required?: boolean;
  requires?: string[];
  value?: string | number | boolean | Mapping;
}

export interface SelectInfo extends PaneInfo {
  type: 'selectSetting';
  value: string | undefined;
  choices: { name: string; value: string }[];
}

export interface TextareaInfo extends PaneInfo {
  type: 'textareaSetting';
  value: string;
  rows?: number;
}

export interface RangeInfo extends PaneInfo {
  type: 'rangeSetting';
  value: number;
  min: number;
  max: number;
  step: number;
}

export interface NumberInfo extends PaneInfo {
  type: 'numberSetting';
  value: number;
  min: number;
  max: number;
}

export interface TextInfo extends PaneInfo {
  type: 'textSetting';
  value: string;
}

export interface SelectorInfo extends PaneInfo {
  type: 'selectorPanel';
  value: Mapping;
}

export type AnyType =
  | 'radio'
  | 'toggleSetting'
  | 'selectSetting'
  | 'textSetting'
  | 'textareaSetting'
  | 'rangeSetting'
  | 'numberSetting'
  | 'selectorPanel';

export type AnyKey =
  | keyof SelectInfo
  | keyof TextInfo
  | keyof TextareaInfo
  | keyof RangeInfo
  | keyof NumberInfo
  | keyof SelectorInfo;

export type AnyInfo =
  | SelectInfo
  | TextInfo
  | TextareaInfo
  | RangeInfo
  | NumberInfo
  | SelectorInfo
  | AnyInfo[];

export interface Schema extends PaneInfo {
  [key: string]: AnyInfo | Schema | PropertyVal;
  type?: undefined | 'radio';
}

export type PaneArgs = { host: EditorHost; info: PaneInfo; parent?: Pane; autoEnable?: () => boolean };

export type ObjectSelector = 'bot' | 'default' | 'schema';

type PropertyVal = string | number | boolean | Mapping | PropertyVal[] | undefined;
