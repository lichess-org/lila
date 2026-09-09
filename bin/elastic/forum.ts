import type { JsonDoc, IndexSchema, Properties, Mapping } from './types.ts';

const forumMaxPostLength = 5_000;

export function getIndexSchema(): IndexSchema {
  return {
    name: 'forum',
    esPath: 'forum',
    batchSize: 100,
    source: false,
    settings: {
      number_of_shards: 5,
      number_of_replicas: 0,
      refresh_interval: '10s',
    },
    properties,
    mapping,
  };
}

const properties: Properties = {
  bo: { type: 'text', boost: 2, analyzer: 'english' }, // body
  to: { type: 'text', boost: 5, analyzer: 'english' }, // topic
  ti: { type: 'keyword', doc_values: false }, // topic ID
  au: { type: 'keyword', doc_values: false }, // author
  tr: { type: 'boolean', doc_values: false }, // troll
  da: { type: 'date' }, // date
};

const mapping: Mapping = {
  collection: 'f_post',
  projection: { _id: 1, text: 1, topicId: 1, troll: 1, createdAt: 1, userId: 1, erasedAt: 1 },
  mongoFilter: args => ({
    $or: [
      { createdAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
      { updatedAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
      { erasedAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
    ],
  }),
  operations: async (docs, { mongo: lichess }) => {
    docs = docs.filter(p => typeof p.text === 'string' && p.text.length <= forumMaxPostLength);
    const topics = lichess.collection('f_topic');
    const topicIds = [
      ...new Set(
        docs
          .filter(p => !p.erasedAt)
          .map(p => p.topicId)
          .filter(Boolean),
      ),
    ];
    const topicMap = Object.fromEntries(
      (await topics.find({ _id: { $in: topicIds } }, { projection: { _id: 1, name: 1 } }).toArray()).map(
        t => [t._id, t.name],
      ),
    );
    return {
      toUpsert: docs
        .filter(p => !p.erasedAt)
        .flatMap(p => {
          const topicName = topicMap[p.topicId];
          if (!topicName) return [];
          const doc: JsonDoc = { bo: p.text, to: topicName, ti: p.topicId, au: p.userId, tr: !!p.troll };
          if (p.createdAt instanceof Date) doc.da = p.createdAt.getTime();
          return [{ id: p._id, doc }];
        }),
      toDelete: docs.filter(p => p.erasedAt).map(p => p._id),
    };
  },
};
