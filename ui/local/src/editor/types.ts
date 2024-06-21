import type { BotInfo, Mapping } from '../types';
import type { SettingGroup } from './settingGroup';
import type { SettingNode } from './settingNode';
import type { ZerofishBotEditor } from '../zerofishBot';
export type { SettingNode, SettingGroup };

export interface BotInfoReader extends BotInfo {
  readonly [key: string]: any;
}

export interface SettingHost {
  readonly view: HTMLElement;
  readonly settings: SettingGroup;
  readonly bot: ZerofishBotEditor;
  readonly botDefault: BotInfoReader;
  update(): void;
}

export interface BaseInfo {
  type?: 'select' | 'text' | 'textarea' | 'range' | 'number' | 'mapping' | 'disclosure';
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  require?: string[] | boolean;
  radioGroup?: string;
  value?: string | number | boolean | Mapping;
}

export interface DisclosureInfo extends BaseInfo {
  type: 'disclosure';
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

export interface BotSchema extends BaseInfo {
  [key: string]:
    | SelectInfo
    | TextInfo
    | TextareaInfo
    | RangeInfo
    | NumberInfo
    | MappingInfo
    | DisclosureInfo
    | BotSchema
    | PropertyVal;
  type?: undefined;
}

export type NodeArgs = { host: SettingHost; info: BaseInfo };

type PropertyVal = string | number | boolean | Mapping | PropertyVal[] | undefined;
