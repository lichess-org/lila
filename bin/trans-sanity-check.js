const fs = require('fs-extra');
const parseString = require('xml2js').parseString;

const baseDir = 'translation/source';
const destDir = 'translation/dest';
const dbs = ['site', 'arena', 'emails', 'learn', 'activity', 'coordinates'];

function checkAgainstRegexes(orig, tran, prefix, e, filename) {
  const regexes = [/lichess/i, /lichess\.org/i, /O-O/, /O-O-O/, /%s/, /%\d\$s/]
  regexes.forEach(r => {
    if (r.test(orig) && !r.test(tran)) {
      console.log(`${prefix}${e['$'].name} contains <${orig.match(r)[0]}> in the source but not in ${filename}`);
    }
  });
}

function compareDestToSource(src, name) {
  fs.readdir(`${destDir}/${name}`, { encoding: 'utf8' }).then(filenames => {
    Promise.all(filenames.map((filename, index, resolve, reject) =>  {
      fs.readFile(`${destDir}/${name}/${filename}`, { encoding: 'utf8' }).then(txt => {
        return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
          const strings = (xml.resources.string || []);
          const prefix = name === 'site' ? '' : `${name}:`
          strings.forEach(e => {
            orig = src[e['$'].name];
            tran = e['_'];
            checkAgainstRegexes(orig,tran, prefix, e, filename);
          });
          const plurals = (xml.resources.plurals || []);
          plurals.forEach(e => {
            orig = src[e['$'].name];
            tran = e.item[0]['_'];
            checkAgainstRegexes(orig,tran, prefix, e, filename);
          });
        }));
      });
    }));
  })
}

function VerifyTranslations(name) {
  return fs.readFile(`${baseDir}/${name}.xml`, { encoding: 'utf8' }).then(txt => {
    return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
      const strings = (xml.resources.string || []);
      let source_strings = {};
      strings.forEach(e => {
        source_strings[e['$'].name] = e['_'];
      });
      const plurals = (xml.resources.plurals || []);
      plurals.forEach(e => {
        source_strings[e['$'].name] = e.item[0]['_'];
      });
      resolve(source_strings);
    })).then(src => {
      compareDestToSource(src, name)
    });
  });
}

Promise.all(dbs.map(VerifyTranslations));
