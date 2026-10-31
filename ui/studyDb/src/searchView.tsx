import { licon } from 'lib/licon';
import { jsx, spinnerVdom, type VNode, type VNodeData } from 'lib/view';

import type { SearchTabContent } from './searchTabContent';
import { listData } from './studyListView';
import type { StudySearchOrder } from './types';

const orders: [StudySearchOrder, string][] = [
  ['relevant', 'Relevance'],
  ['hot', 'Hot'],
  ['newest', 'Newest'],
  ['oldest', 'Oldest'],
  ['updated', 'Recently updated'],
  ['popular', 'Most popular'],
  ['alphabetical', 'Alphabetical'],
];

const variants: [VariantKey, string][] = [
  ['standard', 'Standard'],
  ['chess960', 'Chess960'],
  ['crazyhouse', 'Crazyhouse'],
  ['antichess', 'Antichess'],
  ['atomic', 'Atomic'],
  ['horde', 'Horde'],
  ['kingOfTheHill', 'King of the Hill'],
  ['racingKings', 'Racing Kings'],
  ['threeCheck', 'Three-check'],
];

export function searchView(content: SearchTabContent, submit: () => void): VNode {
  const field = <Key extends string>(
    label: string,
    fields: Partial<Record<Key, string>>,
    key: Key,
    attrs: VNodeData['attrs'] = {},
  ) => (
    <label class="search-field">
      <span>{label}</span>
      <input
        attrs={{ type: 'text', name: key, maxlength: 200, ...attrs }}
        props={{ value: fields[key] ?? '' }}
        on={{
          input: (event: Event) => {
            fields[key] = (event.target as HTMLInputElement).value;
            content.onChange();
          },
        }}
      />
    </label>
  );
  const player = (slot: 1 | 2) => {
    const name = slot === 1 ? 'player1' : 'player2';
    const fideId = slot === 1 ? 'fideId1' : 'fideId2';
    const color = slot === 1 ? 'color1' : 'color2';
    return (
      <fieldset class="search-player" attrs={{ 'aria-label': `Player ${slot}` }}>
        <legend>
          <select
            attrs={{ name: color, 'aria-label': `Player ${slot} color` }}
            on={{
              change: (event: Event) => {
                const value = (event.target as HTMLSelectElement).value as Color | '';
                if (value) {
                  content.tags[color] = value;
                  content.tags[slot === 1 ? 'color2' : 'color1'] = value === 'white' ? 'black' : 'white';
                } else {
                  delete content.tags.color1;
                  delete content.tags.color2;
                }
                content.onChange();
              },
            }}>
            <option attrs={{ value: '', selected: !content.tags[color] }}>Either</option>
            <option attrs={{ value: 'white', selected: content.tags[color] === 'white' }}>White</option>
            <option attrs={{ value: 'black', selected: content.tags[color] === 'black' }}>Black</option>
          </select>
        </legend>
        {field('Name', content.tags, name)}
        {field('FIDE ID', content.tags, fideId, {
          inputmode: 'numeric',
          pattern: '[0-9]+',
        })}
      </fieldset>
    );
  };
  return (
    <div class="search" {...listData(content.id)}>
      <form
        class="search-controls"
        on={{
          submit: (event: Event) => {
            event.preventDefault();
            submit();
          },
        }}>
        <div class="search-main">
          <input
            attrs={{
              type: 'search',
              name: 'text',
              placeholder: 'Study or chapter text',
              'aria-label': 'Search names, descriptions, and topics',
              maxlength: 200,
            }}
            props={{ value: content.query }}
            on={{
              input: (event: Event) => {
                content.query = (event.target as HTMLInputElement).value;
                content.onChange();
              },
            }}
          />
          <select
            class="search-order"
            attrs={{ name: 'order', 'aria-label': 'Sort by', title: 'Sort by' }}
            on={{
              change: (event: Event) => {
                content.order = (event.target as HTMLSelectElement).value as StudySearchOrder;
                content.onChange();
              },
            }}>
            {orders.map(([value, label]) => (
              <option attrs={{ value, selected: content.order === value }}>{label}</option>
            ))}
          </select>
          <button
            class="search-submit"
            attrs={{
              type: 'submit',
              title: 'Search studies',
              'aria-label': 'Search studies',
              'data-icon': licon.Search,
              disabled: !content.canSubmit,
            }}
          />
        </div>
        <div class="search-fields">
          <div class="search-pair">
            <label class="search-field">
              <span>Variant</span>
              <select
                attrs={{ name: 'variant' }}
                on={{
                  change: (event: Event) => {
                    content.tags.variant = (event.target as HTMLSelectElement).value;
                    content.onChange();
                  },
                }}>
                <option attrs={{ value: '', selected: !content.tags.variant }}>Any variant</option>
                {variants.map(([value, label]) => (
                  <option attrs={{ value, selected: content.tags.variant === value }}>{label}</option>
                ))}
              </select>
            </label>
            {field('ECO', content.tags, 'eco', {
              pattern: '[A-Ea-e][0-9]{2}',
              maxlength: 3,
            })}
          </div>
          {field('Opening', content.tags, 'opening')}
          {field('Owner', content.study, 'owner')}
          {field('Event', content.tags, 'event')}
          <div class="search-players">
            {player(1)}
            {player(2)}
          </div>
        </div>
      </form>
      {(content.pending || content.message) && (
        <div class="status" attrs={{ role: 'status' }}>
          {content.pending ? spinnerVdom() : content.message}
        </div>
      )}
    </div>
  );
}
