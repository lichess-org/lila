import { RoundTour } from '../interfaces';
import Shepherd from 'shepherd.js';

export function initModule(): RoundTour {
  return {
    corresRematchOffline,
  };

  function corresRematchOffline() {
    const tour = new Shepherd.Tour();

    tour.addStep({
      title: 'Challenged to a rematch',
      text: 'Your opponent is offline, but they can accept this challenge later!',
      attachTo: {
        element: 'button.rematch',
        on: 'bottom',
      },
      buttons: [
        {
          action() {
            return this.next();
          },
          text: 'Ok, got it',
        },
      ],
    });

    tour.start();
  }
}
