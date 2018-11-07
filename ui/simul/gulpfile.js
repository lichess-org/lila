const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const size = require('gulp-size');

const browserifyOpts = (debug) => ({
  entries: ['./src/main.js'],
  standalone: 'LidraughtsSimul',
  debug: debug
});
const destination = () => gulp.dest('../../public/compiled/');

const prod = () => browserify(browserifyOpts(false)).bundle()
  .pipe(source('lidraughts.simul.min.js'))
  .pipe(buffer())
  .pipe(uglify())
  .pipe(size())
  .pipe(destination());

const dev = () => browserify(browserifyOpts(true)).bundle()
  .pipe(source('lidraughts.simul.js'))
  .pipe(destination());

const watch = () => {

  const bundle = () => bundler.bundle()
    .on('error', error => logger.error(colors.red(error.message)))
    .pipe(source('lidraughts.simul.js'))
    .pipe(destination());

  const bundler = watchify(
    browserify(Object.assign({}, watchify.args, browserifyOpts(true)))
  ).on('update', bundle).on('log', logger.info);

  return bundle();
};

gulp.task('prod', prod);
gulp.task('dev', dev);
gulp.task('default', watch);
