const gulp = require("gulp");
const source = require("vinyl-source-stream");
const buffer = require("vinyl-buffer");
const colors = require("ansi-colors");
const logger = require("fancy-log");
const watchify = require("watchify");
const browserify = require("browserify");
const terser = require("gulp-terser");
const size = require("gulp-size");
const tsify = require("tsify");
const concat = require("gulp-concat");
const execSync = require("child_process").execSync;
const fs = require("fs");
const path = require("path");

const browserifyOpts = (entries, debug) => ({
  entries: entries,
  standalone: "Lishogi",
  debug: debug,
});
const destinationPath = "../../public/compiled/";
const destination = () => gulp.dest(destinationPath);
const fileBaseName = "lishogi.site";

const abFile = process.env.LILA_AB_FILE;

const jqueryFill = () =>
  gulp
    .src("src/jquery.fill.js")
    .pipe(buffer())
    .pipe(terser({ safari10: true }))
    .pipe(gulp.dest("./dist"));

const ab = () => {
  if (abFile)
    return gulp
      .src(abFile)
      .pipe(buffer())
      .pipe(terser({ safari10: true }))
      .pipe(gulp.dest("./dist"));
  else {
    logger.info(colors.yellow("Building without AB file"));
    return gulp.src(".");
  }
};

const hopscotch = () =>
  gulp
    .src(["dist/js/hopscotch.min.js", "dist/**/*.min.css", "dist/img/*"], {
      cwd: path.dirname(require.resolve("hopscotch/package.json")),
      cwdbase: true,
    })
    .pipe(gulp.dest("../../public/vendor/hopscotch/"));

const jqueryBarRating = () =>
  gulp
    .src(["dist/jquery.barrating.min.js"], {
      cwd: path.dirname(require.resolve("jquery-bar-rating/package.json")),
      cwdbase: true,
    })
    .pipe(gulp.dest("../../public/vendor/bar-rating/"));

const highcharts = () =>
  gulp
    .src(["highcharts.js", "highcharts-more.js", "highstock.js"], {
      cwd: path.dirname(require.resolve("highcharts/package.json")),
      cwdbase: true,
    })
    .pipe(gulp.dest("../../public/vendor/highcharts-4.2.5/"));

// const stockfishJs = () => gulp.src([
//   require.resolve('stockfish.js/stockfish.wasm.js'),
//   require.resolve('stockfish.js/stockfish.wasm'),
//   require.resolve('stockfish.js/stockfish.js')
// ]).pipe(gulp.dest('../../public/vendor/stockfish.js'));
//
// const stockfishWasm = () => gulp.src([
//   require.resolve('stockfish.wasm/stockfish.js'),
//   require.resolve('stockfish.wasm/stockfish.wasm'),
//   require.resolve('stockfish.wasm/stockfish.worker.js')
// ]).pipe(gulp.dest('../../public/vendor/stockfish.wasm/'));
//
// const stockfishMvWasm = () => gulp.src([
//   require.resolve('stockfish-mv.wasm/stockfish.js'),
//   require.resolve('stockfish-mv.wasm/stockfish.js.mem'),
//   require.resolve('stockfish-mv.wasm/stockfish.wasm'),
//   require.resolve('stockfish-mv.wasm/pthread-main.js')
// ]).pipe(gulp.dest('../../public/vendor/stockfish-mv.wasm/'));

const prodSource = () =>
  browserify(browserifyOpts("src/index.ts", false))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.source.min.js`))
    .pipe(buffer())
    .pipe(terser({ safari10: true }))
    .pipe(gulp.dest("./dist"));

const devSource = () =>
  browserify(browserifyOpts("src/index.ts", true))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.js`))
    .pipe(destination());

function makeDependencies(filename) {
  return function bundleDeps() {
    return gulp
      .src([
        "../../public/javascripts/vendor/jquery.min.js",
        "./dist/jquery.fill.js",
        "./dep/powertip.min.js",
        "./dep/howler.min.js",
        "./dep/mousetrap.min.js",
        "./dist/consolemsg.js",
        ...(abFile ? ["./dist/ab.js"] : []),
      ])
      .pipe(concat(filename))
      .pipe(destination());
  };
}

function makeBundle(filename) {
  return function bundleItAll() {
    return gulp
      .src([destinationPath + "lishogi.deps.js", "./dist/" + filename])
      .pipe(concat(filename.replace("source.", "")))
      .pipe(destination());
  };
}

const gitSha = (cb) => {
  const info = JSON.stringify({
    date:
      new Date(new Date().toUTCString()).toISOString().split(".")[0] + "+00:00",
    commit: execSync("git rev-parse -q --short HEAD", {
      encoding: "utf-8",
    }).trim(),
    message: execSync("git log -1 --pretty=%s", { encoding: "utf-8" }).trim(),
  });
  if (!fs.existsSync("./dist")) fs.mkdirSync("./dist");
  fs.writeFileSync(
    "./dist/consolemsg.js",
    `window.lishogi=window.lishogi||{};console.info("Lishogi is open source! https://github.com/WandererXII/lila");lishogi.info=${info};`
  );
  cb();
};

const standalonesJs = () =>
  gulp
    .src(
      [
        "util.js",
        "trans.js",
        "tv.js",
        "puzzle.js",
        "user.js",
        "coordinate.js",
        "captcha.js",
        "embed-analyse.js",
      ].map((f) => `src/standalones/${f}`)
    )
    .pipe(buffer())
    .pipe(terser({ safari10: true }))
    .pipe(destination());

function singlePackage(file, dest) {
  return () =>
    browserify(browserifyOpts(file, false))
      .bundle()
      .pipe(source(dest))
      .pipe(buffer())
      .pipe(terser({ safari10: false }))
      .pipe(destination());
}

const userMod = singlePackage("./src/user-mod.js", "user-mod.js");
const clas = singlePackage("./src/clas.js", "clas.js");

const deps = makeDependencies("lishogi.deps.js");

const tasks = [
  gitSha,
  jqueryFill,
  ab,
  standalonesJs,
  userMod,
  clas,
  deps,
  hopscotch,
  jqueryBarRating,
  highcharts,
];

const dev = gulp.series(tasks.concat([devSource]));

gulp.task(
  "prod",
  gulp.series(tasks, prodSource, makeBundle(`${fileBaseName}.source.min.js`))
);
gulp.task("dev", gulp.series(tasks, dev));
gulp.task(
  "default",
  gulp.series(tasks, dev, () => gulp.watch("src/**/*.js", dev))
);
