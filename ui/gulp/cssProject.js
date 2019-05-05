const gulp = require('gulp');
const sass = require('gulp-sass');
const sourcemaps = require('gulp-sourcemaps');
const autoprefixer = require('gulp-autoprefixer');
const rename = require('gulp-rename');
const fs = require('fs');

const themes = ['light', 'dark', 'transp'];

const sassOptions = {
  errLogToConsole: true,
  outputStyle: 'expanded'
};
const autoprefixerOptions = {
  // https://browserl.ist/?q=last+5+versions%2C+Firefox+ESR%2C+not+IE+<+12%2C+not+<+0.1%25%2C+not+IE_Mob+<+12
  browsers: 'last 5 versions, Firefox ESR, not IE < 12, not < 0.1%, not IE_Mob < 12'.split(', ')
};
const destination = () => gulp.dest('../../public/css/');

module.exports = (dir) => {

  const sourceDir = `${dir}/css`;
  const buildDir = `${sourceDir}/build`;
  const sourcesGlob = sourceDir + '/**/*.scss';
  const buildsGlob = sourceDir + '/build/*.scss';
  const commonGlob = '../common/css/**/*.scss';

  createThemedBuilds(buildDir);

  const build = () => gulp
    .src(buildsGlob)
    .pipe(sourcemaps.init())
    .pipe(sass(sassOptions).on('error', sass.logError))
    .pipe(sourcemaps.write())
  // .pipe(autoprefixer(autoprefixerOptions))
  // .pipe(source(`${fileBaseName}.min.js`))
    .pipe(renameAs('dev'))
    .pipe(destination());

  gulp.task('css', gulp.series([
    build,
    () => {
      gulp.watch(sourcesGlob, build);
      gulp.watch(commonGlob, build);
    }
  ]));

  gulp.task('css-dev', build);

  gulp.task('css-prod', () => gulp
    .src(buildsGlob)
    .pipe(sass({
      ...sassOptions,
      ...{ outputStyle: 'compressed' }
    }).on('error', sass.logError))
    .pipe(autoprefixer(autoprefixerOptions))
    .pipe(renameAs('min'))
    .pipe(destination())
  );
}

function renameAs(ext) {
  return rename(path => {
    path.basename = `${path.basename}.${ext}`;
    return path;
  });
}

function createThemedBuilds(buildDir) {
  const builds = fs.readdirSync(buildDir);
  builds
    .filter(fileName => fileName[0] === '_')
    .forEach(fileName => {
      themes.forEach(theme => {
        const themedName = fileName.replace(/^_(.+)\.scss$/, `$1.${theme}.scss`);
        const themedPath = `${buildDir}/${themedName}`;
        if (!fs.existsSync(themedPath)) {
          const buildName = fileName.replace(/^_(.+)\.scss$/, '$1');
          const code = `@import '../../../common/css/theme/${theme}';\n@import '${buildName}';\n`;
          console.log(`Create missing SCSS themed build: ${themedPath}`);
          fs.writeFileSync(themedPath, code);
        }
      });
    });
}
