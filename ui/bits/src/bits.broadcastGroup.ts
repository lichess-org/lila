import Tagify, { type InvalidTagEventData } from '@yaireo/tagify';
import Sortable from 'sortablejs';

import { myUserId, throttle } from 'lib';
import { json as xhrJson } from 'lib/xhr';

type CurrentPageResults = {
  tour: BroadcastByUser;
}[];

type BroadcastByUser = { id: string; name: string; tier?: number; communityOwner?: LightUser };
type BroadcastTag = { value: string; name: string };

export default function initModule(opts: { studyadmin: boolean; broadcaster: boolean }): void {
  const textArea = document.querySelector<HTMLTextAreaElement>('#form3-grouping_info_tours');
  if (!textArea) return;
  let myBroadcasts: BroadcastByUser[] | null = null;
  const fromServer = textArea.value
    .trim()
    .split(/\n/)
    .map(t => t.trim())
    .filter(t => t.length > 0);

  const tagify = new Tagify<BroadcastTag>(textArea, {
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
    texts: { notAllowed: 'Invalid broadcast ID or insufficient permissions' },
  });

  tagify.removeAllTags();
  const preExisting = fromServer.map<BroadcastTag>(t => ({ value: t.slice(-8), name: t.slice(0, -9) }));
  if (preExisting.length) {
    tagify.whitelist = preExisting;
    tagify.addTags(preExisting);
  }

  Sortable.create(tagify.DOM.scope, {
    filter: '.tagify__input',
    onEnd: () => {
      tagify.updateValueByDOMTags();
    },
  });

  const fetchMyBroadcasts = async () => {
    if (myBroadcasts === null) {
      tagify.loading(true);
      const resp = await xhrJson(`/api/broadcast/by/${myUserId()}`);
      const results = resp['currentPageResults'] as CurrentPageResults;
      myBroadcasts = results.map(t => t.tour).filter(t => opts.broadcaster || !t.tier);
      tagify.whitelist = tagify.whitelist.map(t => (typeof t === 'string' ? { value: t, name: t } : t));
      tagify.whitelist = tagify.whitelist.concat(
        myBroadcasts
          .map(b => ({ value: b.id, name: b.name }))
          .filter(b => !tagify.whitelist.some(t => typeof t !== 'string' && t.value === b.value)),
      );
      tagify.loading(false);
      tagify.dropdown.show();
    }
  };

  const doFetchBroadcast = async (id: string) =>
    (await xhrJson(`/api/broadcast/${id}`))?.tour as BroadcastByUser;
  const fetchBroadcastAndVerify = throttle(
    3000,
    async (invalidData: InvalidTagEventData<BroadcastTag>, broadcastId: string) => {
      tagify.loading(true);
      const replaceErrTag = (msg: string) =>
        tagify.replaceTag(invalidData.tag, {
          ...invalidData.data,
          // @ts-ignore
          __isValid: msg,
        });
      doFetchBroadcast(broadcastId)
        .then(
          tour => {
            if (
              !opts.studyadmin ||
              (!opts.broadcaster && !tour.tier && tour.communityOwner?.id !== myUserId())
            )
              replaceErrTag('Insufficient permissions to group this broadcast');
            else {
              const newTag = { value: tour.id, name: tour.name };
              tagify.whitelist = [
                ...(tagify.whitelist as BroadcastTag[]).filter(t => t.value !== tour.id),
                newTag,
              ];
              tagify.replaceTag(invalidData.tag, newTag);
            }
          },
          () => {
            replaceErrTag('Failed to fetch broadcast details.');
          },
        )
        .finally(() => {
          tagify.loading(false);
        });
    },
  );

  tagify.on('focus', fetchMyBroadcasts);
  tagify.on('invalid', e => {
    const data = e.detail.data;
    const validIDRegex = /(?:^|(?:broadcast(?:\/.*)?\/))(\w{8})(?:\/edit)?$/; // 8 character ID OR Tour URL OR edit URL
    const match = data?.name.match(validIDRegex);
    if (data?.name && match) fetchBroadcastAndVerify(e.detail, match[1]);
  });
}
