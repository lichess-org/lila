import type { IndexSchema, Properties, Mapping } from './types.ts';

export function getIndexSchema(): IndexSchema {
  return {
    name: 'team',
    esPath: 'team',
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
  na: { type: 'text', boost: 10, analyzer: 'english' }, // name
  de: { type: 'text', boost: 2, analyzer: 'english' }, // description
  nbm: { type: 'short' }, // member count
};

const mapping: Mapping = {
  collection: 'team',
  projection: { _id: 1, name: 1, description: 1, nbMembers: 1, enabled: 1 },
  mongoFilter: args => ({
    $or: [
      { createdAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
      { updatedAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
      { erasedAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
    ],
  }),
  operations: docs => ({
    toUpsert: docs
      .filter(t => t.enabled)
      .map(t => ({ id: t._id, doc: { na: t.name, de: t.description, nbm: t.nbMembers ?? 0 } })),
    toDelete: docs.filter(t => !t.enabled).map(t => t._id),
  }),
};
