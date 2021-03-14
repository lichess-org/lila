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
    if (players.length != this.cars.length) {
      this.cars = players.map(p => ({ score: p.moves, time: -9999999 }));
    } else {
      this.cars = this.cars.map((car, i) => ({
        score: players[i].moves,
        time: players[i].moves > car.score ? getNow() : car.time,
      }));
    }
  };

  isBoosting = (index: number) => this.cars[index]?.score && this.cars[index]?.time > getNow() - 1000;
}
