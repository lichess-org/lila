var gulp = require("gulp");
var browserify = require("browserify");
var source = require('vinyl-source-stream');
var tsify = require("tsify");
var watchify = require("watchify");
var gutil = require("gulp-util");
var uglify = require('gulp-uglify');
var sourcemaps = require('gulp-sourcemaps');
var buffer = require('vinyl-buffer');

var destination = '../../public/compiled/';

var build = browserify({
    standalone: 'LichessChat',
    basedir: '.',
    debug: true,
    entries: ['src/main.ts'],
    cache: {},
    packageCache: {}
  })
  .plugin(tsify);
  // .transform('babelify', {
  //   presets: ['es2015'],
  //   extensions: ['.ts']
  // });

var watchedBrowserify = watchify(build);

function bundle() {
  return watchedBrowserify
    .bundle()
    .pipe(source('lichess.chat2.js'))
    .pipe(buffer())
    .pipe(sourcemaps.init({
      loadMaps: true
    }))
    .pipe(sourcemaps.write('./'))
    .pipe(gulp.dest(destination));
}

gulp.task("default", [], bundle);
watchedBrowserify.on("update", bundle);
watchedBrowserify.on("log", gutil.log);

gulp.task("prod", [], function() {
  return build
    .bundle()
    .pipe(source('lichess.chat2.min.js'))
    .pipe(buffer())
    // .pipe(sourcemaps.init({loadMaps: false}))
    .pipe(uglify())
    .pipe(sourcemaps.write('./'))
    .pipe(gulp.dest(destination));
});
