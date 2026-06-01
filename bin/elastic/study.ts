import type { Collection } from 'mongodb';

import type { JsonDoc, IndexSchema, Mapping, Properties, MongoDoc } from './types.ts';

export function getIndexSchema(): IndexSchema {
  return {
    name: 'study',
    esPath: 'study_with_chapters',
    batchSize: 100,
    source: true,
    settings: {
      number_of_shards: 5,
      number_of_replicas: 0,
      refresh_interval: '10s',
      analysis: {
        analyzer: {
          english_with_chess_synonyms: {
            type: 'custom',
            tokenizer: 'standard',
            filter: [
              'english_possessive_stemmer',
              'lowercase',
              'english_stop',
              'chess_synonyms_filter',
              'english_stemmer',
            ],
          },
        },
        filter: {
          english_stop: { type: 'stop', stopwords: '_english_' },
          english_stemmer: { type: 'stemmer', language: 'english' },
          english_possessive_stemmer: { type: 'stemmer', language: 'possessive_english' },
          chess_synonyms_filter: { type: 'synonym', synonyms_path: 'synonyms/chess_synonyms.txt' },
        },
      },
    },
    properties,
    mapping,
  };
}

const properties: Properties = {
  name: {
    type: 'text',
    analyzer: 'english',
    search_analyzer: 'english_with_chess_synonyms',
    fields: { raw: { type: 'keyword', normalizer: 'lowercase' } },
  },
  description: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
  owner: { type: 'keyword', doc_values: false },
  members: { type: 'keyword', doc_values: false },
  topics: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
  chapters: {
    type: 'nested',
    dynamic: 'false',
    properties: {
      id: { type: 'keyword', doc_values: false },
      name: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
      description: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
      tags: {
        type: 'nested',
        dynamic: 'false',
        properties: {
          variant: { type: 'keyword', doc_values: false },
          event: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
          white: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
          black: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
          whiteFideId: { type: 'keyword', doc_values: false },
          blackFideId: { type: 'keyword', doc_values: false },
          eco: { type: 'keyword', doc_values: false },
          opening: { type: 'text', analyzer: 'english', search_analyzer: 'english_with_chess_synonyms' },
        },
      },
    },
  },
  likes: { type: 'integer' },
  public: { type: 'boolean' },
  rank: { type: 'date', format: 'yyyy-MM-dd HH:mm:ss' },
  createdAt: { type: 'date', format: 'yyyy-MM-dd HH:mm:ss' },
  updatedAt: { type: 'date', format: 'yyyy-MM-dd HH:mm:ss' },
};

const mapping: Mapping = {
  collection: 'study',
  projection: {
    _id: 1,
    name: 1,
    members: 1,
    ownerId: 1,
    visibility: 1,
    topics: 1,
    likes: 1,
    rank: 1,
    createdAt: 1,
    updatedAt: 1,
    from: 1,
    description: 1,
  },
  mongoFilter: args => ({
    $or: [
      { createdAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
      { updatedAt: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) } },
    ],
  }),
  operations: async (docs, { mongo: lichess }) => {
    docs = docs.filter(study => !study.from?.startsWith('relay'));
    const chapters = lichess.collection<MongoDoc>('study_chapter_flat');
    const chapterMap = await chaptersByStudyIds(
      chapters,
      docs.map(study => study._id),
    );
    return {
      toUpsert: docs.map(study => ({ id: study._id, doc: toStudyDoc(study, chapterMap.get(study._id)) })),
      toDelete: [],
    };
  },
};

async function chaptersByStudyIds(chapters: Collection<MongoDoc>, ids: string[]) {
  const map = new Map<string, JsonDoc[]>();
  if (ids.length === 0) return map;
  for (const chapter of await chapters.find({ studyId: { $in: ids } }).toArray()) {
    const list = map.get(chapter.studyId) ?? [];
    list.push(chapter);
    map.set(chapter.studyId, list);
  }
  return map;
}

function toStudyDoc(study: JsonDoc, chapters: JsonDoc[] | undefined) {
  const doc: JsonDoc = {
    name: study.name,
    owner: study.ownerId,
    members: Object.keys(study.members ?? {}),
    topics: study.topics ?? [],
    likes: study.likes ?? 0,
    public: study.visibility === 'public',
  };
  if (study.description && String(study.description) && study.description !== '-')
    doc.description = study.description;
  if (chapters) doc.chapters = chapters.map(toChapterDoc);
  const rank = formatDateTime(study.rank);
  if (rank) doc.rank = rank;
  const createdAt = formatDateTime(study.createdAt);
  if (createdAt) doc.createdAt = createdAt;
  const updatedAt = formatDateTime(study.updatedAt);
  if (updatedAt) doc.updatedAt = updatedAt;
  return doc;
}

function toChapterDoc(chapter: JsonDoc) {
  const tagMap = Object.fromEntries(
    (Array.isArray(chapter.tags) ? chapter.tags : [])
      .map(tag => String(tag).split(/:(.*)/s, 2))
      .filter(([name, value]: string[]) => name && value)
      .map(([name, value]: string[]) => [name, value]),
  );
  const tags: JsonDoc = {};
  if (tagMap.Variant != null) tags.variant = tagMap.Variant;
  if (tagMap.Event != null) tags.event = tagMap.Event;
  if (tagMap.White != null) tags.white = tagMap.White;
  if (tagMap.Black != null) tags.black = tagMap.Black;
  if (tagMap.WhiteFideId != null) tags.whiteFideId = tagMap.WhiteFideId;
  if (tagMap.BlackFideId != null) tags.blackFideId = tagMap.BlackFideId;
  if (tagMap.ECO != null) tags.eco = tagMap.ECO;
  if (tagMap.Opening != null) tags.opening = tagMap.Opening;
  const source: JsonDoc = { id: chapter._id };
  if (chapter.name && String(chapter.name)) source.name = chapter.name;
  if (chapter.description && String(chapter.description) && chapter.description !== '-')
    source.description = chapter.description;
  if (Object.keys(tags).length > 0) source.tags = tags;
  return source;
}

function formatDateTime(date: unknown) {
  if (!(date instanceof Date)) return undefined;
  return date.toISOString().slice(0, 19).replace('T', ' ');
}
