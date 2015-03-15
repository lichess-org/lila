var fs = require('fs');
var async = require('async');
var SVGOptim = require('svgo');

var sourceDir = 'public/piece-src/';
var destDir = 'public/stylesheets/piece/';

var pieces = [].concat.apply([], ['w', 'b'].map(function(color) {
  return ['P', 'B', 'N', 'R', 'Q', 'K'].map(function(role) {
    return color + role;
  });
}));

var themes = ['cburnett', 'alpha'];

var svgo = new SVGOptim({
  multipass: true
});

themes.forEach(function(theme) {
  var files = pieces.map(function(piece) {
    return sourceDir + theme + '/' + piece + '.svg';
  });
  async.map(files, fs.readFile, function(err, svgs) {
    console.log(svgs);
  })
});
