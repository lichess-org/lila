const resolve = require('@rollup/plugin-node-resolve').default;
const commonjs = require('@rollup/plugin-commonjs');
const typescript = require('@rollup/plugin-typescript');
const terser = require('rollup-plugin-terser').terser;
// const analyze = require('rollup-plugin-analyzer');

module.exports = targets => {
  return args => {
    const prod = args['config-prod'];
    const target = targets[args['config-plugin'] || 'main'];
    return {
      input: target.input,
      external: target.external,
      output: [
        {
          format: 'iife',
          name: target.name,
          globals: target.globals,
          generatedCode: {
            preset: 'es2015',
            constBindings: false, // for window[target.name]
          },
          ...(prod
            ? {
                file: `../../public/compiled/${target.output}.min.js`,
                plugins: [
                  terser({
                    safari10: true,
                    output: {
                      comments: false,
                    },
                    numWorkers: 1,
                  }),
                ],
              }
            : {
                file: `../../public/compiled/${target.output}.js`,
              }),
        },
      ],
      plugins: [
        ...(target.plugins || []),
        resolve(),
        ...(target.js ? [] : [typescript()]),
        commonjs({
          extensions: ['.js'],
        }),
        // analyze({
        //   summaryOnly: true,
        // }),
      ],
    };
  };
};
