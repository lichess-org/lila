var gulp = require('gulp');
var svgmin = require('gulp-svgmin');

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

gulp.task('piece-sprite', function() {
  ['cburnett', 'alpha'].forEach(function(theme) {
    var code = pieces.map(function(piece) {
      var code = gulp.src(sourceDir + theme + '/' + piece + '.svg')
        .pipe(svgmin({
          multipass: true,
          'datauri': 'enc'
        }))
        // .pipe(makeCss)
        .pipe(gulp.dest(destDir + theme));
    });
  });
});
