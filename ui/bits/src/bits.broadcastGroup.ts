import Tagify, { type InvalidTagEventData } from '@yaireo/tagify';
import Sortable from 'sortablejs';

import { myUserId, throttle } from 'lib';
import { json as xhrJson } from 'lib/xhr';

type CurrentPageResults = {
  tour: BroadcastByUser;
}[];

type BroadcastByUser = { id: string; name: string; tier?: number; communityOwner?: LightUser };
type BroadcastTag = { value: string; name: string };

export default function initModule(opts: { studyAdmin: boolean; broadcaster: boolean }): void {
  const textArea = document.querySelector<HTMLTextAreaElement>('#form3-grouping_info_tours');
  if (!textArea) return;
  let myBroadcasts: BroadcastByUser[] | null = null;
  const fromServer = textArea.value
    .trim()
    .split(/\n/)
    .map(t => t.trim())
    .filter(t => t.length > 0);

  const makeTourTagify = (el: HTMLTextAreaElement) =>
    new Tagify<BroadcastTag>(el, {
      enforceWhitelist: true,
      keepInvalidTags: true, // Persist bad tags so that users can hover and see the reason why it was rejected
      duplicates: false,
      dropdown: {
        enabled: 0,
        mapValueTo: 'name',
        searchKeys: ['name'],
      },
      tagTextProp: 'name',
      maxTags: 50,
      originalInputValueFormat: tags => tags.map(t => t.value).join('\n'),
      readonly: textArea.disabled,
    });

  const group = makeTourTagify(textArea);

  group.removeAllTags();
  const preExisting = fromServer.map<BroadcastTag>(t => ({ value: t.slice(-8), name: t.slice(0, -9) }));
  group.whitelist = preExisting;
  group.addTags(preExisting);

  Sortable.create(group.DOM.scope, {
    filter: '.tagify__input',
    onEnd: () => {
      group.updateValueByDOMTags();
    },
  });

  const fetchMyBroadcasts = async () => {
    if (myBroadcasts === null) {
      group.loading(true);
      const resp = await xhrJson(`/api/broadcast/by/${myUserId()}`);
      const results = resp['currentPageResults'] as CurrentPageResults;
      myBroadcasts = results.map(t => t.tour).filter(t => opts.broadcaster || !t.tier); // Fetching our own broadcasts so no need to check for communityOwner
      group.whitelist = group.whitelist.map(t => (typeof t === 'string' ? { value: t, name: t } : t));
      group.whitelist = group.whitelist.concat(
        myBroadcasts
          .map(b => ({ value: b.id, name: b.name }))
          .filter(
            fetched =>
              !group.whitelist.some(
                existing => typeof existing !== 'string' && existing.value === fetched.value,
              ),
          ),
      );
      group.loading(false);
      group.dropdown.show();
    }
  };

  const doFetchBroadcast = async (id: string) =>
    (await xhrJson(`/api/broadcast/${id}`))?.tour as BroadcastByUser;
  const fetchBroadcastAndVerify = throttle(
    3000,
    async (invalidData: InvalidTagEventData<BroadcastTag>, broadcastId: string) => {
      group.loading(true);
      const replaceErrTag = (msg: string) =>
        group.replaceTag(invalidData.tag, {
          ...invalidData.data,
          // @ts-ignore
          __isValid: msg,
        });
      doFetchBroadcast(broadcastId)
        .then(
          tour => {
            if (
              !opts.studyAdmin ||
              (!opts.broadcaster && !tour.tier && tour.communityOwner?.id !== myUserId())
            )
              replaceErrTag('Insufficient permissions to group this broadcast');
            else {
              const newTag = { value: tour.id, name: tour.name };
              group.whitelist = [
                ...(group.whitelist as BroadcastTag[]).filter(t => t.value !== tour.id),
                newTag,
              ];
              group.replaceTag(invalidData.tag, newTag);
            }
          },
          () => {
            replaceErrTag('Failed to fetch broadcast details.');
          },
        )
        .finally(() => {
          group.loading(false);
        });
    },
  );

  group.on('focus', fetchMyBroadcasts);
  group.on('invalid', e => {
    const data = e.detail.data;
    const validIDRegex = /(?:^|(?:broadcast(?:\/.*)?\/))(\w{8})(?:\/edit)?$/; // 8 character ID OR Tour URL OR edit URL
    const match = data?.name.match(validIDRegex);
    if (data?.name && match) fetchBroadcastAndVerify(e.detail, match[1]);
  });

  const plusButton = document.createElement('button');
  plusButton.type = 'button';
  plusButton.className = 'button button-thin';
  plusButton.textContent = 'Add another score group';
  const plusWrap = document.createElement('div');
  plusWrap.className = 'relay-score-group-plus';
  plusWrap.appendChild(plusButton);

  const inputWithoutTagify = $('[id*="_scoreGroups_"]').last().parent().clone();
  plusButton.addEventListener('mousedown', () => {
    const lastEl = $('[id*="_scoreGroups_"]').last().parent();
    const newLast = inputWithoutTagify.clone();
    const textArea = newLast.find('textarea');
    const newIndex = $('[id*="_scoreGroups_"]').length;
    if (newIndex >= 10) return;
    textArea.attr('id', `form3-grouping_scoreGroups_${newIndex}`);
    textArea.attr('name', `grouping.scoreGroups[${newIndex}]`);
    textArea.val('');
    const label = newLast.find('label');
    label.attr('for', `form3-grouping_scoreGroups_${newIndex}`);
    label.text(`Score Group ${newIndex + 1}`);
    makeSgTagify(textArea[0] as HTMLTextAreaElement);
    newLast.insertAfter(lastEl);
  });

  $('[id*="_scoreGroups_"]').last().parent()[0]?.insertAdjacentElement('afterend', plusWrap);

  const makeSgTagify = (el: HTMLTextAreaElement) => {
    const sgTagify = makeTourTagify(el);
    sgTagify.settings = {
      ...sgTagify.settings,
      enforceWhitelist: true,
      keepInvalidTags: false,
      whitelist: group.value,
      originalInputValueFormat: tags => tags.map(t => t.value).join(','),
    };
    const preExisting = el.value
      .split(',')
      .map(id => group.whitelist.find(t => typeof t !== 'string' && t.value === id))
      .filter(t => typeof t === 'object');
    sgTagify.removeAllTags();
    sgTagify.addTags(preExisting);
    group.on('add', () => {
      sgTagify.whitelist = group.value;
    });
    group.on('remove', tag => {
      if (tag.detail.data?.value) sgTagify.removeTags(tag.detail.data.value);
    });
  };

  $('[id*="_scoreGroups_"]').each((_, el) => makeSgTagify(el as HTMLTextAreaElement));
}
