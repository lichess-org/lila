import contactEmail from './component/contactEmail';

lichess.load.then(() => {
  location.hash ||= '#help-root';
  contactEmail();
});
