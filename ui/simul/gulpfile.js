const source = require('vinyl-source-stream');
const gulp = require('gulp');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const streamify = require('gulp-streamify');

const sources = ['./src/main.js'];
const destination = '../../public/compiled/';
const onError = function(error) {
  logger.error(colors.red(error.message));
};
const standalone = 'LidraughtsSimul';

function prod() {
  return browserify(sources, {
    standalone: standalone
  }).bundle()
    .pipe(source('lidraughts.simul.min.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(destination));
}

function dev() {
  return browserify(sources, {
    standalone: standalone
  }).bundle()
    .pipe(source('lidraughts.simul.js'))
    .pipe(gulp.dest(destination));
}

function watch() {
  const opts = watchify.args;
  opts.debug = true;
  opts.standalone = standalone;

  function rebundle() {
    return bundleStream.bundle()
      .on('error', onError)
      .pipe(source('lidraughts.simul.js'))
      .pipe(gulp.dest(destination));
  }

  const bundleStream = watchify(browserify(sources, opts))
    .on('update', rebundle)
    .on('log', logger.info);

  return rebundle();
}

gulp.task('prod', prod);
gulp.task('dev', dev);
gulp.task('watch', watch);

gulp.task('default', watch);
