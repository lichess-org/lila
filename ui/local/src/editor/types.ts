import type { BotInfo, Mapping } from '../types';
import type { SettingNode, SettingGroup } from './setting';

export type { SettingNode, SettingGroup };

export interface BotInfoReader extends BotInfo {
  readonly [key: string]: any;
}

export interface SettingHost {
  readonly view: HTMLElement;
  readonly settings: SettingGroup;
  readonly bot: BotInfoReader;
  readonly botDefault: BotInfoReader;
}

export interface BaseInfo {
  type?: 'select' | 'text' | 'textarea' | 'range' | 'number' | 'mapping';
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  require?: string[] | boolean;
  radioGroup?: string;
  hasPanel?: boolean;
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
/*
export interface Mapping {
  by: 'score' | 'moves';
  data: Point[];
  range: { min: number; max: number };
}
*/
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
    | BotSchema
    | PropertyVal;
  type?: undefined;
}

export const reservedKeys: (keyof BaseInfo)[] = [
  'type',
  'id',
  'class',
  'label',
  'title',
  'require',
  'radioGroup',
  'value',
];

type PropertyVal = string | number | boolean | Mapping | PropertyVal[] | undefined;
