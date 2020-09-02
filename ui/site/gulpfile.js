const gulp = require('gulp');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const terser = require('gulp-terser');
const concat = require('gulp-concat');
const path = require('path');

const destinationPath = '../../public/compiled/';
const destination = () => gulp.dest(destinationPath);
const fileBaseName = 'lichess.site';

function makeDependencies(filename) {
  return function bundleDeps() {
    return gulp.src([
  '../../public/javascripts/vendor/jquery.min.js',
  './dep/powertip.min.js',
  './dep/howler.min.js',
  './dep/mousetrap.min.js',
])
      .pipe(concat(filename))
      .pipe(destination());
  };
}

function makeBundle(filename) {
  return function bundleItAll() {
    return gulp.src([
      destinationPath + 'lichess.deps.js',
      './dist/' + filename,
    ])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(destination());
  };
}


const deps = makeDependencies('lichess.deps.js');

const tasks = [
  standalonesJs,
  deps
];

const dev = gulp.series(tasks);

gulp.task('prod', gulp.series(tasks, makeBundle(`${fileBaseName}.source.min.js`)));
gulp.task('dev', gulp.series(tasks, dev));
gulp.task('default', gulp.series(tasks, dev, () => gulp.watch('src/**/*.js', dev)));
