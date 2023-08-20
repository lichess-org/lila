/* eslint-env node */
module.exports = {
  testMatch: ['**/dist/**/*.test.js'],
  testEnvironment: 'jsdom',
  transform: {},
  globals: {
    lichess: {},
  },
};
