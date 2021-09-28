import contactEmail from './component/contactEmail';

lichess.load.then(() => {
  window.location.href = location.hash || '#help-root';
  contactEmail();
});
