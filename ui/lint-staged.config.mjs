import { lstatSync } from 'node:fs';

function filterSymbolicLinks(files) {
  return files.filter(f => !lstatSync(f).isSymbolicLink());
}

export default {
  // NOTE: these patterns must stay in sync with bin/git-hooks/pre-commit!
  'ui/*.{json,ts,mts,mjs}': files => {
    const regularFiles = filterSymbolicLinks(files);
    return regularFiles.length
      ? `oxlint --type-aware --config=.oxlintrc.json --tsconfig=./ui/tsconfig.base.json --fix ${regularFiles.join(' ')} && oxfmt`
      : 'true';
  },
  'ui/*.scss': files => {
    const regularFiles = filterSymbolicLinks(files);
    return regularFiles.length ? `stylelint ${regularFiles.join(' ')} --fix && oxfmt` : 'true';
  },
};
