var gulp = require('gulp');
var svgSprite = require('gulp-svg-sprite');

var config = {
  mode: {
    inline: true, // Prepare for inline embedding
    symbol: true, // Create a «symbol» sprite
    example: true
  }
  // mode: {
  //   css: { // Activate the «css» mode
  //     render: {
  //       css: true // Activate CSS output (with default options)
  //     },
  //     example: true
  //   }
  // }
};
var sourceDir = 'public/piece-src/';
var destDir = 'public/piece-sprite/';

gulp.task('piece-sprite', function() {
  ['cburnett', 'alpha'].forEach(function(theme) {
    gulp.src(sourceDir + theme + '/*.svg')
      .pipe(svgSprite(config))
      .pipe(gulp.dest(destDir + theme));
  });
});
