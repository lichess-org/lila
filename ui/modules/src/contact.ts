import contactEmail from './contactEmail';

lichess.load.then(() => {
  location.hash ||= '#help-root';
  contactEmail();
});
