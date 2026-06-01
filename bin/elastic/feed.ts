import type { IndexSchema, Properties, Mapping } from './types.ts';

export function getIndexSchema(): IndexSchema {
  return {
    name: 'feed',
    esPath: 'feed',
    batchSize: 100,
    source: false,
    settings: { number_of_shards: 1, number_of_replicas: 0, refresh_interval: '10s' },
    properties,
    mapping,
  };
}

const properties: Properties = {
  content: { type: 'text' },
  at: { type: 'date' },
};

const mapping: Mapping = {
  collection: 'daily_feed',
  projection: { _id: 1, content: 1, at: 1, public: 1 },
  mongoFilter: args => ({
    public: true,
    at: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) },
  }),
  operations: docs => ({
    toUpsert: docs
      .filter(update => update.public)
      .map(update => ({ id: update._id, doc: { content: update.content, at: update.at } })),
    toDelete: docs.filter(update => !update.public).map(update => update._id),
  }),
};
