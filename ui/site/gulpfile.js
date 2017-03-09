var source = require('vinyl-source-stream');
var gulp = require('gulp');
var gutil = require('gulp-util');
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

gulp.task('jquery-fill', function() {
  return gulp.src('src/jquery.fill.js')
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

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

function makeBundle(filename) {
  return function() {
    return gulp.src([
      '../../public/javascripts/vendor/jquery.min.js',
      './dist/jquery.fill.js',
      '../../public/vendor/moment/min/moment.min.js',
      './dep/misc.min.js',
      './dist/' + filename,
      '../../public/javascripts/ga.js'
    ])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(gulp.dest(destination));
  };
}

gulp.task('dev-bundle', makeBundle('lichess.site.source.js'));
gulp.task('prod-bundle', makeBundle('lichess.site.source.min.js'));

gulp.task('dev', ['jquery-fill', 'dev-source', 'dev-bundle']);
gulp.task('prod', ['jquery-fill', 'prod-source', 'prod-bundle']);

gulp.task('dev-watch', function() {
  return gulp.watch('src/*.js', ['dev']);
});
gulp.task('default', ['dev', 'dev-watch']);
