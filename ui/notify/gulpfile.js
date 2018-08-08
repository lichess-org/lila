const gulp = require("gulp");
const browserify = require("browserify");
const source = require('vinyl-source-stream');
const tsify = require("tsify");
const watchify = require("watchify");
const gutil = require("gulp-util");
const uglify = require('gulp-uglify');
const buffer = require('vinyl-buffer');

const destination = '../../public/compiled/';

function onError(error) {
  return gutil.log(gutil.colors.red(error.message));
}

function build(debug) {
  return browserify('src/main.ts', {
      standalone: 'LidraughtsNotify',
      debug: debug
    })
    .plugin(tsify);
}

const watchedBrowserify = watchify(build(true));

function bundle() {
  return watchedBrowserify
    .bundle()
    .on('error', onError)
    .pipe(source('lidraughts.notify.js'))
    .pipe(buffer())
    .pipe(gulp.dest(destination));
}

gulp.task("default", [], bundle);
watchedBrowserify.on("update", bundle);
watchedBrowserify.on("log", gutil.log);

gulp.task('dev', function() {
  return build(true)
    .bundle()
    .pipe(source('lidraughts.notify.js'))
    .pipe(gulp.dest(destination));
});

gulp.task("prod", [], function() {
  return build(false)
    .bundle()
    .pipe(source('lidraughts.notify.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest(destination));
});
