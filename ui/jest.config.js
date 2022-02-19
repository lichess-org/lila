/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
/* eslint-env node */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  transform: {
    '^.+\\.(ts|tsx)?$': 'ts-jest',
    '^.+\\.(js|jsx)$': 'babel-jest',
  },
  globals: {
    lichess: {},
    'ts-jest': {
      tsconfig: 'tsconfig.base.json',
    },
  },
};
