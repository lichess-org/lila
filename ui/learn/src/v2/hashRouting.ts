export const BASE_LEARN_PATH = '/learn';

export const hashHref = (stageId?: number, levelId?: number) => {
  let href = BASE_LEARN_PATH;
  if (typeof stageId === 'number') {
    href += `#/${stageId}`;
    if (typeof levelId === 'number') href += '/' + levelId;
  }
  return href;
};

export const extractHashParameters = (): {
  stageId?: number;
  levelId?: number;
} => {
  const hash = window.location.hash;
  if (!hash) return {};
  const parts = hash.split('/');
  const nanToUndefined = (n: number) => (isNaN(n) ? undefined : n);
  return { stageId: nanToUndefined(parseInt(parts[1], 10)), levelId: nanToUndefined(parseInt(parts[2], 10)) };
};
