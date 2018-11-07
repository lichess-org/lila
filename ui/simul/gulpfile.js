const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const size = require('gulp-size');

const browserifyOpts = {
  entries: ['./src/main.js'],
  standalone: 'LidraughtsSimul'
};
const destination = gulp.dest('../../public/compiled/');

function prod() {
  return browserify(browserifyOpts).bundle()
    .pipe(source('lidraughts.simul.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(size())
    .pipe(destination);
}

function dev() {
  return browserify(browserifyOpts).bundle()
    .pipe(source('lidraughts.simul.js'))
    .pipe(destination);
}

function watch() {

  const bundle = () => bundler.bundle()
    .on('error', error => logger.error(colors.red(error.message)))
    .pipe(source('lidraughts.simul.js'))
    .pipe(destination);

  const bundler = watchify(
    browserify(Object.assign({}, watchify.args, browserifyOpts, {
      debug: true
    }))
  ).on('update', bundle).on('log', logger.info);

  return bundle();
}

gulp.task('prod', prod);
gulp.task('dev', dev);
gulp.task('watch', watch);
gulp.task('default', watch);
