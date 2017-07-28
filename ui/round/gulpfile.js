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

function build(debug, withChat) {
  return browserify('src/' + (withChat ? 'withChat' : 'main') + '.ts', {
    standalone: 'LichessRound',
    debug: debug
  })
    .plugin(tsify);
}

const watchedBrowserify = watchify(build(true, false));

function bundle() {
  return watchedBrowserify
    .bundle()
    .on('error', onError)
    .pipe(source('lichess.round.js'))
    .pipe(buffer())
    .pipe(gulp.dest(destination));
}

watchedBrowserify.on('update', bundle);
watchedBrowserify.on('log', gutil.log);

function dev(withChat) {
  return () => build(true, withChat)
    .bundle()
    .pipe(source('lichess.round' + (withChat ? '.chat' : '') + '.js'))
    .pipe(gulp.dest(destination));
};
gulp.task('dev-alone', dev(false));
gulp.task('dev-chat', dev(true));

function prod(withChat) {
  return () => build(false, withChat)
    .bundle()
    .pipe(source('lichess.round' + (withChat ? '.chat' : '') + '.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest(destination));
}

gulp.task('prod-alone', prod(false));
gulp.task('prod-chat', prod(true));

gulp.task('prod', ['prod-alone', 'prod-chat']);
gulp.task('dev', ['dev-alone', 'dev-chat']);

gulp.task('default', bundle);
