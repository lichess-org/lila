import { lstatSync } from 'fs';

export default {
  '*.{json,scss,ts}': files => {
    const regularFiles = files.filter(f => !lstatSync(f).isSymbolicLink());
    return regularFiles.length ? `prettier --write ${regularFiles.join(' ')}` : 'true';
  },
};
