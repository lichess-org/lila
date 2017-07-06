const fs = require('fs-extra');
const parseString = require('xml2js').parseString;

const baseDir = 'translation/source';

function keyListFrom(file) {
  return fs.readFile(`${baseDir}/${file}`, { encoding: 'utf8' }).then(txt => {
    return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
      const strings = (xml.resources.string || []).map(e => e['$'].name);
      const plurals = (xml.resources.plurals || []).map(e => e['$'].name);
      const keys = strings.concat(plurals);

      resolve(keys.map(k => 'val `' + k + '` = new Translated("' + k + '", Site)').join('\n'));
    }));
  });
}

Promise.all([
  keyListFrom('site.xml'),
  keyListFrom('arena.xml')
]).then(([site, arena]) => {
  const code = `// Generated with bin/trans-dump.js
package lila.i18n

import I18nDb.Site

// format: OFF
object I18nKeys {

def untranslated(message: String) = new Untranslated(message)

object arena {
${arena}
}

${site}
}`;
  fs.writeFile('modules/i18n/src/main/I18nKeys.scala', code);
});
