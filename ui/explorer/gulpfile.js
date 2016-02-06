var source = require('vinyl-source-stream');
var gulp = require('gulp');
var gutil = require('gulp-util');
var browserify = require('browserify');
var watchify = require('watchify');

var sources = ['./src/main.js'];
var destination = '../../public/compiled/';
var onError = function(error) {
  gutil.log(gutil.colors.red(error.message));
};
var standalone = 'LichessExplorer';

gulp.task('dev', function() {
  return browserify('./src/main.js', {
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.explorer.js'))
    .pipe(gulp.dest(destination));
});

gulp.task('watch', function() {
  var opts = watchify.args;
  opts.debug = true;
  opts.standalone = standalone;

  var bundleStream = watchify(browserify(sources, opts))
    .on('update', rebundle)
    .on('log', gutil.log);

  function rebundle() {
    return bundleStream.bundle()
      .on('error', onError)
      .pipe(source('lichess.explorer.js'))
      .pipe(gulp.dest(destination));
  }

  return rebundle();
});

gulp.task('default', ['watch']);
