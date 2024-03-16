import { randomToken } from 'common/random';

// Unique id for the current document/navigation. Should be different after
// each page load and for each tab. Should be unpredictable and secret while
// in use.
const sri = randomToken();

export default sri;
