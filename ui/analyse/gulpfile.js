const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const size = require('gulp-size');
const tsify = require('tsify');

const browserifyOpts = (debug) => ({
  entries: ['./src/main.ts'],
  standalone: 'LidraughtsAnalyse',
  debug: debug
});
const destination = () => gulp.dest('../../public/compiled/');

const prod = () => browserify(browserifyOpts(false))
    .plugin(tsify)
    .bundle()
    .pipe(source('lidraughts.analyse.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(size())
    .pipe(destination());

const dev = () => browserify(browserifyOpts(true))
    .plugin(tsify)
    .bundle()
    .pipe(source('lidraughts.analyse.js'))
    .pipe(destination());

const watch = () => {

  const bundle = () => bundler.bundle()
    .on('error', error => logger.error(colors.red(error.message)))
    .pipe(source('lidraughts.analyse.js'))
    .pipe(destination());

  const bundler = watchify(
    browserify(browserifyOpts(true)).plugin(tsify)
  ).on('update', bundle).on('log', logger.info);

  return bundle();
};

gulp.task('prod', prod);
gulp.task('dev', dev);
gulp.task('default', watch);
