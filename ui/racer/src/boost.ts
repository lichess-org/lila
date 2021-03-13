import { getNow } from 'puz/util';
import { PlayerWithMoves } from './interfaces';

type Timestamp = number;

export interface CarBoost {
  score: number;
  time: Timestamp; // of last score update
}

export class Boost {
  cars: CarBoost[] = [];

  setPlayers = (players: PlayerWithMoves[]) => {
    const now = getNow();
    if (players.length != this.cars.length) {
      this.cars = players.map(p => ({ score: p.moves, time: now }));
    } else {
      this.cars = this.cars.map((car, i) => ({
        score: players[i].moves,
        time: players[i].moves > car.score ? now : car.time,
      }));
    }
  };

  isBoosting = (index: number) => this.cars[index]?.time > getNow() - 1000;
}
