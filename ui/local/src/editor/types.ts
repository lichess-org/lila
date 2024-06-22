import type { BotInfo, Mapping } from '../types';
import type { SettingCtrl } from './settings';
import type { SettingView } from './setting';
import type { ZerofishBotEditor } from '../zerofishBot';
export type { SettingView as SettingNode, SettingCtrl as SettingGroup };

export interface BotInfoReader extends BotInfo {
  readonly [key: string]: any;
}

export interface SettingHost {
  readonly view: HTMLElement;
  readonly settings: SettingCtrl;
  readonly bot: ZerofishBotEditor;
  readonly botDefault: BotInfoReader;
  update(): void;
}

export interface BaseInfo {
  type?: 'select' | 'text' | 'textarea' | 'range' | 'number' | 'mapping' | 'radioGroup';
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  require?: string[] | boolean;
  radioGroup?: string;
  value?: string | number | boolean | Mapping;
}

export interface SelectInfo extends BaseInfo {
  type: 'select';
  value: string | undefined;
  choices: { name: string; value: string }[];
}

export interface TextareaInfo extends BaseInfo {
  type: 'textarea';
  value: string;
  rows?: number;
}

export interface RangeInfo extends BaseInfo {
  type: 'range';
  value: number;
  min: number;
  max: number;
  step: number;
}

export interface NumberInfo extends BaseInfo {
  type: 'number';
  value: number;
  min: number;
  max: number;
}

export interface TextInfo extends BaseInfo {
  type: 'text';
  value: string;
}

export interface MappingInfo extends BaseInfo {
  type: 'mapping';
  value: Mapping;
}

export type AnyKey =
  | keyof SelectInfo
  | keyof TextInfo
  | keyof TextareaInfo
  | keyof RangeInfo
  | keyof NumberInfo
  | keyof MappingInfo;

export type AnyInfo = SelectInfo | TextInfo | TextareaInfo | RangeInfo | NumberInfo | MappingInfo;

export interface BotSchema extends BaseInfo {
  [key: string]: AnyInfo | BotSchema | PropertyVal;
  type?: undefined | 'radioGroup';
}

export type NodeArgs = { host: SettingHost; info: BaseInfo };

type PropertyVal = string | number | boolean | Mapping | PropertyVal[] | undefined;
