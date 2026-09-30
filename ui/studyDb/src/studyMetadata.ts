import type { StudyData } from './types';

export function mergeStudyMetadata(cached: StudyData | undefined, incoming: StudyData): StudyData {
  return {
    ...cached,
    ...incoming,
    metadataRevision: cached?.metadataRevision,
    chapters:
      incoming.chapters?.map(chapter => ({
        ...cached?.chapters?.find(existing => existing.id === chapter.id),
        ...chapter,
      })) ?? cached?.chapters,
  };
}

export function matchesMetadata(value: unknown, query: string): boolean {
  if (value === null || value === undefined) return false;
  if (typeof value === 'object')
    return Object.entries(value).some(([key, value]) =>
      key === 'tags' && Array.isArray(value)
        ? value.some(([, tagValue]) => matchesMetadata(tagValue, query))
        : matchesMetadata(value, query),
    );
  return String(value).toLowerCase().includes(query);
}
