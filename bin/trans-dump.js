const fs = require('fs-extra');
const parseString = require('xml2js').parseString;

const baseDir = 'translation/source';
const dbs = ['site', 'arena'];

function ucfirst(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

function keyListFrom(name) {
  const dbName = ucfirst(name);
  return fs.readFile(`${baseDir}/${name}.xml`, { encoding: 'utf8' }).then(txt => {
    return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
      const strings = (xml.resources.string || []).map(e => e['$'].name);
      const plurals = (xml.resources.plurals || []).map(e => e['$'].name);
      const keys = strings.concat(plurals);

      resolve(keys.map(k => 'val `' + k + '` = new Translated("' + k + '", ' + dbName + ')').join('\n'));
    }));
  });
}

Promise.all(dbs.map(keyListFrom)).then(([site, arena]) => {
  const code = `// Generated with bin/trans-dump.js
package lila.i18n

import I18nDb.{ ${dbs.map(ucfirst).join(', ')} }

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
