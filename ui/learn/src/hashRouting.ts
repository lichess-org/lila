export const BASE_LEARN_PATH = '/learn';

export const hashNavigate = (stageId?: number, levelId?: number) => {
  let hashPath = '';
  if (typeof stageId === 'number') hashPath += `/${stageId}`;
  if (typeof levelId === 'number') hashPath += `/${levelId}`;
  window.location.hash = hashPath;
};

export const hashHref = (stageId?: number, levelId?: number) => {
  let href = BASE_LEARN_PATH;
  if (typeof stageId === 'number') {
    href += `#/${stageId}`;
    if (typeof levelId === 'number') href += '/' + levelId;
  }
  return href;
};

export const extractHashParameters = (): {
  stageId: number | null;
  levelId: number | null;
} => {
  const hash = window.location.hash;
  if (!hash) return { stageId: null, levelId: null };
  const parts = hash.split('/');
  const nanToNull = (n: number) => (isNaN(n) ? null : n);
  return { stageId: nanToNull(parseInt(parts[1], 10)), levelId: nanToNull(parseInt(parts[2], 10)) };
};
