import type { Book, SoundEvent, Sound as NamedSound } from 'lib/bot/types';
import type { Requirement, Filter } from 'lib/bot/filter';
import type { Pane } from './pane';
import type { AssetType } from './devAssets';
import type { EditDialog } from './editDialog';

export type Sound = Omit<NamedSound, 'key'>;

export type { Requirement };

export interface Template<T extends object> {
  min: Record<keyof T, number>;
  max: Record<keyof T, number>;
  step: Record<keyof T, number>;
  value: Record<keyof T, number>;
}

export interface PaneInfo {
  type?: InfoType;
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  toggle?: boolean;
  requires?: Requirement;
  value?: string | number | boolean | Filter;
  assetType?: AssetType;
}

export interface SelectInfo extends PaneInfo {
  type: 'select';
  value?: string;
  choices?: { name: string; value: string }[];
}

export interface TextareaInfo extends PaneInfo {
  type: 'textarea';
  value?: string;
  placeholder?: string;
  rows?: number;
}

export interface NumberInfo extends PaneInfo {
  type: 'number' | 'range';
  value?: number;
  min: number;
  max: number;
}

export interface RangeInfo extends NumberInfo {
  type: 'range';
  step: number;
}

export interface TextInfo extends PaneInfo {
  type: 'text';
  value?: string;
  placeholder?: string;
}

export interface BooksInfo extends PaneInfo {
  type: 'books';
  template: Template<{ weight: number }>;
}

export interface SoundEventInfo extends PaneInfo {
  type: 'soundEvent';
}

interface BaseSoundsInfo extends PaneInfo {
  type: 'sounds';
  template: Template<Sound>;
}

export type SoundsInfo = BaseSoundsInfo & Record<SoundEvent, SoundEventInfo>;

export interface FilterInfo extends PaneInfo {
  type: 'filter';
  value: Filter;
}

export type InfoKey =
  | keyof SelectInfo
  | keyof TextInfo
  | keyof TextareaInfo
  | keyof RangeInfo
  | keyof NumberInfo
  | keyof BooksInfo
  | keyof SoundEventInfo
  | keyof FilterInfo;

export type AnyInfo =
  | SelectInfo
  | TextInfo
  | TextareaInfo
  | RangeInfo
  | NumberInfo
  | BooksInfo
  | SoundsInfo
  | SoundEventInfo
  | FilterInfo
  | AnyInfo[];

type ExtractType<T> = T extends { type: infer U } ? U : never;

type InfoType = ExtractType<AnyInfo> | 'group' | 'radioGroup';

export type PropertyValue = Filter | Book[] | Sound[] | string | number | boolean | undefined;

type SchemaValue = Schema | AnyInfo | PropertyValue | Requirement | string[];

export interface Schema extends PaneInfo {
  [key: string]: SchemaValue;
  type?: 'group' | 'radioGroup';
}

export type PaneArgs = { host: EditDialog; info: PaneInfo & Record<string, any>; parent?: Pane };

export type PropertySource = 'scratch' | 'local' | 'server' | 'schema';
