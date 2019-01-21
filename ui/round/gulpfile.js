const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const size = require('gulp-size');
const tsify = require('tsify');

const lilaGulp = require('../gulp/tsProject.js');

lilaGulp('LichessRound', 'lichess.round', __dirname);

/* The following code adds two dev commands:
 * KeyboardMoves
 * NVUI
 */

const roundProd = gulp.task('prod');

const destination = () => gulp.dest(`../../public/compiled/`);

const plugins = [
  {
    entries: ['src/plugins/keyboardMove.ts'],
    standalone: 'KeyboardMove',
    target: 'lichess.round.keyboardMove.min.js'
  },
  {
    entries: ['src/plugins/nvui.ts'],
    standalone: 'NVUI',
    target: 'lichess.round.nvui.min.js'
  }
];

const pluginsProd = plugins.map(opts => {
  return () => browserify(opts)
    .plugin(tsify)
    .bundle()
    .pipe(source(opts.target))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(size())
    .pipe(destination());
});

gulp.task('prod', gulp.series(pluginsProd.concat(roundProd)));

const pluginWatch = (opts) => () => {

  const bundle = () => bundler
    .bundle()
    .on('error', error => logger.error(colors.red(error.message)))
    .pipe(source(opts.target))
    .pipe(destination());

  const bundler = watchify(
    browserify(Object.assign({}, watchify.args, {...opts, debug: true}))
    .plugin(tsify)
  ).on('update', bundle).on('log', logger.info);

  return bundle();
};

plugins.map(plugin => {
  gulp.task(plugin.standalone, pluginWatch(plugin));
});
