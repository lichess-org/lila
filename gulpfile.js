var gulp = require('gulp');
var svgmin = require('gulp-svgmin');
var map = require('map-stream');

var sourceDir = 'public/piece-src/';
var destDir = 'public/stylesheets/piece/';

var pieces = [].concat.apply([], ['w', 'b'].map(function(color) {
  return ['P', 'B', 'N', 'R', 'Q', 'K'].map(function(role) {
    return color + role;
  });
}));

function makeCss(code) {
  console.log(code);
  return code;
}

var log = function(file, cb) {
  console.log(file.path);
  console.log(file.contents);
  console.log(cb);
  // cb(null, file);
};

gulp.task('piece-sprite', function() {
  ['cburnett', 'alpha'].forEach(function(theme) {
    gulp.src(sourceDir + theme + '/*.svg')
      .pipe(svgmin({
        multipass: true
      }))
      .pipe(map(log));
  });
});

gulp.task('piece-sprite-fs', function() {
  var svgo = new SVGOptim({
    multipass: true
  });
  var svgs = {};
  ['cburnett', 'alpha'].forEach(function(theme) {
    svgs[theme] = {};
    pieces.map(function(piece) {
      var file = new File({
        base: sourceDir + theme + '/' + piece + '.svg'
      });
      console.log(file.relative);
      console.log(file.contents);
      svgo.optimize(String(file.contents), function(result) {
        svgs[theme][piece] = result.data;
      });
    });
  });
  console.log(svgs);
});
