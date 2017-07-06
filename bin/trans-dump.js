const fs = require('fs-extra');
const parseString = require('xml2js').parseString;

fs.readFile('translation/source/site.xml', { encoding: 'utf8' }).then(txt => {
  parseString(txt, (err, xml) => {
    const strings = xml.resources.string.map(e => e['$'].name);
    const plurals = xml.resources.plurals.map(e => e['$'].name);
    const keys = strings.concat(plurals);

    const keyList = keys.map(k => 'val `' + k + '` = new Translated("' + k + '", Site)');

    const code = `// Generated with bin/trans-dump.js
package lila.i18n

import I18nDb.Site

// format: OFF
object I18nKeys {

def apply(db: I18nDb.Ref)(message: String) = new Translated(message, db)

def arena(message: String) = new Translated(message, I18nDb.Arena)

def untranslated(message: String) = new Untranslated(message)

${keyList.join('\n')}
}
`
    fs.writeFile('modules/i18n/src/main/I18nKeys.scala', code);
  });
});
