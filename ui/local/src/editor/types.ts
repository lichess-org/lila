import type { BotInfo, Mapping } from '../types';
import type { Editor } from './editor';
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
  update(): void;
}

export interface BaseInfo {
  type?: AnyType;
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  required?: boolean;
  requires?: string[];
  radio?: string;
  value?: string | number | boolean | Mapping;
}

export interface SelectInfo extends BaseInfo {
  type: 'selectSetting';
  value: string | undefined;
  choices: { name: string; value: string }[];
}

export interface TextareaInfo extends BaseInfo {
  type: 'textareaSetting';
  value: string;
  rows?: number;
}

export interface RangeInfo extends BaseInfo {
  type: 'rangeSetting';
  value: number;
  min: number;
  max: number;
  step: number;
}

export interface NumberInfo extends BaseInfo {
  type: 'numberSetting';
  value: number;
  min: number;
  max: number;
}

export interface TextInfo extends BaseInfo {
  type: 'textSetting';
  value: string;
}

export interface MappingInfo extends BaseInfo {
  type: 'mappingPanel';
  value: Mapping;
}

export type AnyType =
  | 'radio'
  | 'selectSetting'
  | 'textSetting'
  | 'textareaSetting'
  | 'rangeSetting'
  | 'numberSetting'
  | 'mappingPanel';

export type AnyKey =
  | keyof SelectInfo
  | keyof TextInfo
  | keyof TextareaInfo
  | keyof RangeInfo
  | keyof NumberInfo
  | keyof MappingInfo;

export type AnyInfo = SelectInfo | TextInfo | TextareaInfo | RangeInfo | NumberInfo | MappingInfo | AnyInfo[];

export interface Schema extends BaseInfo {
  [key: string]: AnyInfo | Schema | PropertyVal;
  type?: undefined | 'radio';
}

export type PaneArgs = { host: EditorHost; info: BaseInfo };

export type ObjectSelector = 'bot' | 'default' | 'schema';

type PropertyVal = string | number | boolean | Mapping | PropertyVal[] | undefined;
