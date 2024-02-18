import contactEmail from './contactEmail';

site.load.then(() => {
  location.hash ||= '#help-root';
  contactEmail();
});
