export default {
  multipass: false,
  js2svg: {
    pretty: false,
  },
  plugins: [
    {
      name: 'preset-default',
      params: {
        overrides: {
          convertPathData: false,
        },
      },
    },
    {
      name: 'convertPathData',
      params: {
        floatPrecision: 4,
        straightCurves: false,
        smartArcRounding: false,
        forceAbsolutePath: false,
      },
    },
    'removeTitle',
  ],
};
