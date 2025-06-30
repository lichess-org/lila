import { h, type VNode } from 'snabbdom';
import { type LichessStorage, storage } from '../storage';
import { renderSan, renderPieceStyle, renderPrefixStyle } from './render';

export interface Setting<A> {
  choices: Choices<A>;
  get(): A;
  set(v: A): A;
}

type Choice<A> = [A, string];
type Choices<A> = Array<Choice<A>>;

interface Opts<A> {
  choices: Choices<A>;
  default: A;
  storage: LichessStorage;
}

export function makeSetting<A>(opts: Opts<A>): Setting<A> {
  return {
    choices: opts.choices,
    get: () => (opts.storage.get() as A) || opts.default,
    set(v: A) {
      opts.storage.set(v);
      return v;
    },
  };
}

export function renderSetting<A>(setting: Setting<A>, redraw: () => void): VNode {
  const v = setting.get();
  return h(
    'select',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLSelectElement).addEventListener('change', e => {
            setting.set((e.target as HTMLSelectElement).value as A);
            redraw();
          });
        },
      },
    },
    setting.choices.map(choice => {
      const [key, name] = choice;
      return h('option', { attrs: { value: '' + key, selected: key === v } }, name);
    }),
  );
}

const moveStyles = ['uci', 'san', 'literate', 'nato', 'anna'] as const;
export type MoveStyle = (typeof moveStyles)[number];
const pieceStyles = ['letter', 'white uppercase letter', 'name', 'white uppercase name'] as const;
export type PieceStyle = (typeof pieceStyles)[number];
const prefixStyles = ['letter', 'name', 'none'] as const;
export type PrefixStyle = (typeof prefixStyles)[number];
export type PositionStyle = 'before' | 'after' | 'none';
export type BoardStyle = 'plain' | 'table';

export function boardSetting(): Setting<BoardStyle> {
  return makeSetting<BoardStyle>({
    choices: [
      ['plain', 'plain: layout with no semantic rows or columns'],
      ['table', 'table: layout using a table with rank and file columns and row headers'],
    ],
    default: 'plain',
    storage: storage.make('nvui.boardLayout'),
  });
}

export function styleSetting(): Setting<MoveStyle> {
  return makeSetting<MoveStyle>({
    choices: moveStyles.map(s => [s, `${s}: ${renderSan('Nxf3', 'g1f3', s)}`]),
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: storage.make('nvui.moveNotation'),
  });
}

export function pieceSetting(): Setting<PieceStyle> {
  return makeSetting<PieceStyle>({
    choices: pieceStyles.map(p => [p, `${p}: ${renderPieceStyle('P', p)}`]),
    default: 'letter',
    storage: storage.make('nvui.pieceStyle'),
  });
}

export function prefixSetting(): Setting<PrefixStyle> {
  return makeSetting<PrefixStyle>({
    choices: prefixStyles.map(p => [p, `${p}: ${renderPrefixStyle('white', p)}`]),
    default: 'letter',
    storage: storage.make('nvui.prefixStyle'),
  });
}

export function positionSetting(): Setting<PositionStyle> {
  return makeSetting<PositionStyle>({
    choices: [
      ['before', 'before: c2: wp'],
      ['after', 'after: wp: c2'],
      ['none', 'none'],
    ],
    default: 'before',
    storage: storage.make('nvui.positionStyle'),
  });
}
