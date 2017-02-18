var gulp = require("gulp");
var browserify = require("browserify");
var source = require('vinyl-source-stream');
var tsify = require("tsify");
var watchify = require("watchify");
var gutil = require("gulp-util");
var uglify = require('gulp-uglify');
var buffer = require('vinyl-buffer');

var destination = '../../public/compiled/';

function build(debug) {
  return browserify('src/main.ts', {
      standalone: 'LichessChat',
      debug: debug
    })
    .plugin(tsify);
}

var watchedBrowserify = watchify(build(true));

function bundle() {
  return watchedBrowserify
    .bundle()
    .on('error', function(error) {
      gutil.log(gutil.colors.red(error.message));
    })
    .pipe(source('lichess.chat2.js'))
    .pipe(buffer())
    .pipe(gulp.dest(destination));
}

gulp.task("default", [], bundle);
watchedBrowserify.on("update", bundle);
watchedBrowserify.on("log", gutil.log);

gulp.task("prod", [], function() {
  return build(false)
    .bundle()
    .pipe(source('lichess.chat2.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest(destination));
});
