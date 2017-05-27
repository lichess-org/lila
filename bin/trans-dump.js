const fs = require('fs-extra');
const parseString = require('xml2js').parseString;

fs.readFile('translation/source/site.xml', { encoding: 'utf8' }).then(txt => {
  parseString(txt, (err, xml) => {
    const strings = xml.resources.string.map(e => e['$'].name);
    const plurals = xml.resources.plurals.map(e => e['$'].name);
    const keys = strings.concat(plurals);

    const keyList = keys.map(k => 'val `' + k + '` = new Translated("' + k + '")');

    const code = `// Generated with bin/trans-dump.js ${new Date()}
package lila.i18n

object I18nKeys {

def untranslated(message: String) = new Untranslated(message)

${keyList.join('\n')}
}
`
    fs.writeFile('modules/i18n/src/main/I18nKeys.scala', code);
  });
});
