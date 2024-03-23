import contactEmail from './pagelets.contactEmail';

site.load.then(() => {
  location.hash ||= '#help-root';
  contactEmail();
});
