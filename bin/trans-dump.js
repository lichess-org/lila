const fs = require('fs').promises;
const parseString = require('xml2js').parseString;

const baseDir = 'translation/source';
const dbs = 'site arena emails learn activity coordinates study clas contact patron coach broadcast streamer tfa settings preferences team perfStat search tourname faq lag swiss puzzle puzzleTheme challenge'.split(' ');

function ucfirst(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

function xmlName(name) {
  return name == 'clas' ? 'class' : name;
}

function keyListFrom(name) {
  return fs.readFile(`${baseDir}/${xmlName(name)}.xml`, { encoding: 'utf8' }).then(txt => {
    return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
      const strings = (xml.resources.string || []).map(e => e['$'].name);
      const plurals = (xml.resources.plurals || []).map(e => e['$'].name);
      const keys = strings.concat(plurals);
      resolve({
        name: name,
        code: keys.map(k => 'val `' + k + '` = new I18nKey("' + (name == 'site' ? '' : xmlName(name) + ':') + k + '")').join('\n') + '\n',
      });
    }));
  });
}

Promise.all(dbs.map(keyListFrom)).then(objs => {
  function dbCode(obj) {
    return obj.name === 'site' ?
      obj.code :
      `object ${obj.name} {\n${obj.code}}\n`;
  }
  const code = `// Generated with bin/trans-dump.js
package lila.i18n

// format: OFF
object I18nKeys {

${objs.map(dbCode).join('\n')}
}
`;

  return fs.writeFile('modules/i18n/src/main/I18nKeys.scala', code);
});
