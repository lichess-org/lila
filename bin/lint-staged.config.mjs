import { lstat } from 'fs/promises';

async function filterSymlinks(files) {
  const checks = await Promise.all(
    files.map(async file =>
      lstat(file)
        .then(s => s && !s.isSymbolicLink() && file)
        .catch(() => null),
    ),
  );
  return checks.filter(Boolean);
}

export default {
  '*.{json,scss,ts}': async files => {
    const regularFiles = await filterSymlinks(files);
    return regularFiles.length ? `prettier --write ${regularFiles.join(' ')}` : 'true';
  },
};
