import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import { terser } from 'rollup-plugin-terser';
// const analyze = require('rollup-plugin-analyzer');

export default targets => {
  return args => {
    const prod = args['config-prod'];
    const selected = args['config-all'] ? Object.values(targets) : [targets[args['config-plugin'] || 'main']];
    return selected.map(target => ({
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
    }));
  };
};
