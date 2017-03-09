var source = require('vinyl-source-stream');
var gulp = require('gulp');
var gutil = require('gulp-util');
var watchify = require('watchify');
var browserify = require('browserify');
var uglify = require('gulp-uglify');
var streamify = require('gulp-streamify');
var concat = require('gulp-concat');

var sources = ['./src/index.js'];
var destination = '../../public/compiled/';
var onError = function(error) {
  gutil.log(gutil.colors.red(error.message));
};
var standalone = 'Lichess';

gulp.task('prod-source', function() {
  return browserify('./src/index.js', {
    standalone: standalone
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.site.source.min.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

gulp.task('dev-source', function() {
  return browserify('./src/index.js', {
    standalone: standalone
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.site.source.js'))
    .pipe(gulp.dest('./dist'));
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
      .pipe(source('lichess.site.js'))
      .pipe(gulp.dest(destination))
      .pipe(addDepsTo('lichess.site.js'));
  }

  return rebundle();
});

function addDepsTo(filename) {
  return function() {
    return gulp.src([
      './dep/jquery.min.js',
      './src/jquery.fill.js',
      './dep/moment.min.js',
      './dep/misc.min.js',
      './dist/' + filename
    ])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(gulp.dest(destination));
  };
}

gulp.task('dev-bundle', addDepsTo('lichess.site.source.js'));
gulp.task('prod-bundle', addDepsTo('lichess.site.source.min.js'));

gulp.task('default', ['watch']);
gulp.task('dev', ['dev-source', 'dev-bundle']);
gulp.task('prod', ['prod-source', 'prod-bundle']);
