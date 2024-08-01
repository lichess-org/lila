import type { BotInfo, Operator, Book, SoundEvent, Sound as NamedSound } from '../types';
import type { PaneCtrl } from './paneCtrl';
import type { Pane } from './pane';
import type { AssetType, DevRepo } from './devRepo';
import type { ZerofishBot } from '../zerofishBot';

export type Sound = Omit<NamedSound, 'key'>;

export interface BotInfoReader extends BotInfo {
  readonly [key: string]: any;
}

export interface ZerofishBotEditor extends ZerofishBot {
  [key: string]: any;
  disabled: Set<string>;
}

export interface HostView {
  readonly view: HTMLElement;
  readonly ctrl: PaneCtrl;
  readonly bot: ZerofishBotEditor;
  readonly defaultBot: BotInfoReader;
  readonly assetDb: DevRepo;
  readonly cleanups: (() => void)[];
  update(): void;
}

export interface Template<T extends object = any> {
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
  required?: boolean;
  requires?: Requirement;
  value?: string | number | boolean | Operator;
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

export type SoundsInfo = BaseSoundsInfo & {
  [key in SoundEvent]: SoundEventInfo;
};

export interface OperatorInfo extends PaneInfo {
  type: 'operator';
  value: Operator;
}

export type InfoKey =
  | keyof SelectInfo
  | keyof TextInfo
  | keyof TextareaInfo
  | keyof RangeInfo
  | keyof NumberInfo
  | keyof BooksInfo
  | keyof SoundEventInfo
  | keyof OperatorInfo;

export type AnyInfo =
  | SelectInfo
  | TextInfo
  | TextareaInfo
  | RangeInfo
  | NumberInfo
  | BooksInfo
  | SoundsInfo
  | SoundEventInfo
  | OperatorInfo
  | AnyInfo[];

type ExtractType<T> = T extends { type: infer U } ? U : never;

export type InfoType = ExtractType<AnyInfo> | 'group' | 'radioGroup';

export type PropertyValue = Operator | Book[] | Sound[] | string | number | boolean | undefined;

export type SchemaValue = Schema | AnyInfo | PropertyValue | Requirement | string[];

export interface Schema extends PaneInfo {
  [key: string]: SchemaValue;
  type?: undefined | 'group' | 'radioGroup';
}

export type PaneArgs = { host: HostView; info: PaneInfo; parent?: Pane };

export type ObjectSelector = 'bot' | 'default' | 'schema';

export type Requirement = string | { and: Requirement[] } | { or: Requirement[] };
