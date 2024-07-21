import type { BotInfo, Operator, Book, Sound as NamedSound } from '../types';
import type { PaneCtrl } from './paneCtrl';
import type { Pane } from './pane';
import type { AssetType, DevAssetDb } from './devAssetDb';
import type { ZerofishBot } from '../zerofishBot';

type Sound = Omit<NamedSound, 'name'>;

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
  readonly assetDb: DevAssetDb;
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
  value?: string | number | boolean | Operator | Sound;
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
}

export interface SoundInfo extends PaneInfo {
  value: Sound;
}

export interface SoundsInfo extends SoundInfo {
  type: 'sounds';
  value: Sound;
}

export interface OperatorInfo extends PaneInfo {
  type: 'operator';
  value: Operator;
}

export type AnyType =
  | 'group'
  | 'books'
  | 'sound'
  | 'sounds'
  | 'operator'
  | 'radioGroup'
  | 'toggle'
  | 'select'
  | 'text'
  | 'textarea'
  | 'range'
  | 'number';

export type AnyKey =
  | keyof SelectInfo
  | keyof TextInfo
  | keyof TextareaInfo
  | keyof RangeInfo
  | keyof NumberInfo
  | keyof BooksInfo
  | keyof SoundInfo
  | keyof SoundsInfo
  | keyof OperatorInfo;

export type AnyInfo =
  | SelectInfo
  | TextInfo
  | TextareaInfo
  | RangeInfo
  | NumberInfo
  | BooksInfo
  | SoundInfo
  | SoundsInfo
  | OperatorInfo
  | AnyInfo[];

export type PropertyValue = Operator | Sound | Book[] | Sound[] | string | number | boolean | undefined;

export interface Schema extends PaneInfo {
  [key: string]: Schema | AnyInfo | Operator | string | string[] | number | boolean | undefined;
  type?: undefined | 'radioGroup' | 'group';
}

export type PaneArgs = { host: HostView; info: PaneInfo; parent?: Pane };

export type ObjectSelector = 'bot' | 'default' | 'schema';
