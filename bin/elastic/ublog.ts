import type { JsonDoc, IndexSchema, Properties, Mapping } from './types.ts';

export function getIndexSchema(): IndexSchema {
  return {
    name: 'ublog',
    esPath: 'ublog',
    batchSize: 100,
    source: false,
    settings: { number_of_shards: 5, number_of_replicas: 0, refresh_interval: '10s' },
    properties,
    mapping,
  };
}

const properties: Properties = {
  text: { type: 'text' },
  language: { type: 'keyword', doc_values: false },
  likes: { type: 'short' },
  date: { type: 'date' },
  quality: { type: 'short' },
};

const mapping: Mapping = {
  collection: 'ublog_post',
  projection: {
    _id: 1,
    markdown: 1,
    title: 1,
    intro: 1,
    topics: 1,
    blog: 1,
    live: 1,
    lived: 1,
    likes: 1,
    language: 1,
    automod: 1,
  },
  mongoFilter: args => ({ 'lived.at': { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } }),
  operations: docs => ({
    toUpsert: docs
      .filter(post => post.live && post.automod?.quality !== 0)
      .map(post => {
        const topics = (post.topics ?? []).join(' ').replaceAll('Chess', '');
        const author = typeof post.blog === 'string' ? (post.blog.split(':')[1] ?? '') : '';
        const doc: JsonDoc = {
          text: `${post.title}\n${topics}\n${author}\n${post.intro}\n${post.markdown}`,
          likes: post.likes ?? 0,
          date: post.lived?.at instanceof Date ? post.lived.at.getTime() : 0,
        };
        if (post.language != null) doc.language = post.language;
        if (post.automod?.quality != null) doc.quality = post.automod.quality;
        return { id: post._id, doc };
      }),
    toDelete: docs.filter(post => !post.live || post.automod?.quality === 0).map(post => post._id),
  }),
};
