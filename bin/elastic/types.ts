import type { estypes } from '@elastic/elasticsearch';
import type { Db, Filter, Sort } from 'mongodb';

export type IndexName = 'forum' | 'ublog' | 'team' | 'study' | 'game';
export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };
export type JsonDoc = Record<string, JsonValue>;
export type MongoDoc = { _id: string } & Record<string, any>;
export type Properties = Record<string, estypes.MappingProperty>;
export type Operations = { toUpsert: { id: string; doc: JsonDoc }[]; toDelete: string[] };

export interface Context {
  args: Args;
  mongo: Db;
}

export interface IndexSchema {
  name: IndexName;
  esPath: string;
  batchSize: number;
  source: boolean;
  settings: estypes.IndicesIndexSettings & {
    number_of_shards?: number;
    number_of_replicas?: number;
    refresh_interval: string;
  };
  properties: Properties;
  mapping: Mapping;
}

export interface Mapping {
  collection: string;
  projection: Record<string, 1>;
  mongoFilter: (args: Args) => Filter<MongoDoc>;
  mongoSort?: Sort;
  operations: (docs: MongoDoc[], context: Context) => Promise<Operations> | Operations;
}

export interface Args {
  mode: 'watch' | 'backfill';
  indexes: IndexName[];
  esUri: string;
  mongoUri: string;
  metricsPort: number;
  interval: number;
  from: Date;
  to?: Date;
}
