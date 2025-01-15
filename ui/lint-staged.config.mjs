import { lstatSync } from 'fs';

export default {
  // NOTE: these patterns must stay in sync with bin/git-hooks/pre-commit!
  '*.{json,scss,ts}': files => {
    const regularFiles = files.filter(f => !lstatSync(f).isSymbolicLink());
    return regularFiles.length
      ? `prettier --config=ui/.prettierrc.json --write ${regularFiles.join(' ')}`
      : 'true';
  },
};
