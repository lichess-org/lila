const gulp = require('gulp');
const gutil = require('gulp-util');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const tsify = require('tsify');

const destination = '../../public/compiled/';

function onError(error) {
  gutil.log(gutil.colors.red(error.message));
}

function build(debug) {
  return browserify('src/main.ts', {
    standalone: 'LidraughtsRound',
    debug: debug
  })
    .plugin(tsify);
}

const watchedBrowserify = watchify(build(true, false));

function bundle() {
  return watchedBrowserify
    .bundle()
    .on('error', onError)
    .pipe(source('lidraughts.round.js'))
    .pipe(buffer())
    .pipe(gulp.dest(destination));
}

watchedBrowserify.on('update', bundle);
watchedBrowserify.on('log', gutil.log);

function dev() {
  return () => build(true)
    .bundle()
    .pipe(source('lidraughts.round.js'))
    .pipe(gulp.dest(destination));
};
gulp.task('dev', dev(false));

function prod() {
  return () => build(false)
    .bundle()
    .pipe(source('lidraughts.round.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest(destination));
}

gulp.task('prod', prod(false));

gulp.task('default', bundle);
