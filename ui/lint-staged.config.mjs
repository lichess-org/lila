import { lstatSync } from 'node:fs';

function filterSymbolicLinks(files) {
  return files.filter(f => !lstatSync(f).isSymbolicLink());
}

export default {
  // NOTE: these patterns must stay in sync with bin/git-hooks/pre-commit!
  '*.{json,ts,mts,mjs}': files => {
    const regularFiles = filterSymbolicLinks(files);
    return regularFiles.length
      ? `oxlint --type-aware --config=../.oxlintrc.json --tsconfig=tsconfig.base.json --fix ${regularFiles.join(' ')} && oxfmt`
      : 'true';
  },
  '*.scss': files => {
    const regularFiles = filterSymbolicLinks(files);
    return regularFiles.length ? `stylelint ${regularFiles.join(' ')} --fix && oxfmt` : 'true';
  },
};
