const fs = require('fs-extra');
const parseString = require('xml2js').parseString;

const baseDir = 'translation/source';
const destDir = 'translation/dest';
const dbs = ['site', 'arena', 'emails', 'learn', 'activity', 'coordinates', 'study'];

function printError(level, file, error) {
    console.log(`${level.toUpperCase()} ${file} ${error}`);
}

function printTransError(level, r, orig, tran, db, e, filename) {
  if (r.test(orig) && !r.test(tran))
    printError(level, `${db}/${filename}`, `${e['$'].name} lacks <${orig.match(r)[0]}>: ${e['_']}`);
}

function checkAgainstRegexes(orig, tran, db, e, filename) {
  const warnings = [/lidraughts/i, /lidraughts\.org/i, /SAN/, /FEN/, /PDN/];
  const errors = [/%s/, /%\d\$s/];
  warnings.forEach(r => printTransError('warning', r, orig, tran, db, e, filename));
  errors.forEach(r => printTransError('error', r, orig, tran, db, e, filename));
}

function checkPlaceholders(str, db, e, filename, severity = 'error') {
  if (/%\d\$s/.test(str)) {
    placeholderNums = str.match(/%\d\$s/g).map(x => parseInt(x[1])).sort();
    for (i = 1; i <= placeholderNums.length; i++) {
      if (placeholderNums[i-1] !== i) {
        printError(severity, `${db}/${filename}`, `${e['$'].name} bad placeholder: ${str}`);
      }
    }
  }
}

function compareDestToSource(src, db) {
  fs.readdir(`${destDir}/${db}`, { encoding: 'utf8' }).then(filenames => {
    Promise.all(filenames.map((filename, index, resolve, reject) =>  {
      fs.readFile(`${destDir}/${db}/${filename}`, { encoding: 'utf8' }).then(txt => {
        return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
          const strings = (xml.resources.string || []);
          strings.forEach(e => {
            const orig = src[e['$'].name];
            const tran = e['_'];
            checkAgainstRegexes(orig, tran, db, e, filename);
            checkPlaceholders(tran, db, e, filename);
          });
          const plurals = (xml.resources.plurals || []);
          plurals.forEach(e => {
            const orig = src[e['$'].name];
            const quantity = e.item[0].$.quantity;
            const tran = e.item[0]['_'];
            checkAgainstRegexes(orig, tran, db, e, filename);
            checkPlaceholders(tran, db, e, filename, quantity == 'one' ? 'warning' : 'error');
          });
        }));
      });
    }));
  })
}

function VerifyTranslations(db) {
  return fs.readFile(`${baseDir}/${db}.xml`, { encoding: 'utf8' }).then(txt => {
    return new Promise((resolve, reject) => parseString(txt, (_, xml) => {
      const prefix = db === 'site' ? '' : `${db}:`
      const strings = (xml.resources.string || []);
      let source_strings = {};
      strings.forEach(e => {
        source_strings[e['$'].name] = e['_'];
        checkPlaceholders(e['_'], db, e, 'src');
      });
      const plurals = (xml.resources.plurals || []);
      plurals.forEach(e => {
        const quantity = e.item[0].$.quantity;
        source_strings[e['$'].name] = e.item[0]['_'];
        checkPlaceholders(e.item[0]['_'], db, e, 'src', quantity == 'one' ? 'warning' : 'error');
      });
      resolve(source_strings);
    })).then(src => {
      compareDestToSource(src, db)
    });
  });
}

Promise.all(dbs.map(VerifyTranslations));
