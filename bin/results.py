#!/usr/bin/env python3

import json
import sys

# type Info = {
#   name: string;
#   id: string;
#   movetime: number;
#   threads: number;
#   hash: number;
# };

# type GameResult = {
#   result: string;
#   reason: string;
#   white: Info;
#   black: Info;
#   moves: number;
#   //moves: string[];
# };

def main():
    draws = {}
    byEngine = {}
    results = []
    errors = []
    template = {'W': 0, 'D': 0, 'L': 0}

    for x in range(1, len(sys.argv)):

        with open(sys.argv[x], 'r') as f:
            results.extend(json.load(f))
    
    for result in results:
        outcome = result['result']
        reason = result['reason']
        if outcome == 'draw':
            if reason.startswith('Stockfish'):
                errors.append(reason)
            else:
                byEngine.setdefault(get_name_compat(result, 'white'), template.copy())['D'] += 1
                byEngine.setdefault(get_name_compat(result, 'black'), template.copy())['D'] += 1
                draws[reason] = draws.get(reason, 0) + 1
        elif outcome == 'error':
            errors.append(reason)
        else:
            try:
                winner = result[outcome]['name']
                loser = result['black' if outcome == 'white' else 'white']['name']
            except TypeError as e:
                winner = result[outcome]
                loser = result['black' if outcome == 'white' else 'white']
            byEngine.setdefault(loser, template.copy())['L'] += 1
            byEngine.setdefault(winner, template.copy())['W'] += 1

    print('Total games:', len(results))
    print('Draws by reason:')
    for reason, count in draws.items():
        print(f'  {reason}: {count}')
    print('  error: ', len(errors))
    print('Records by engine (W/D/L):')
    for engine, record in byEngine.items():
        print(f'  {engine}: {record["W"]}/{record["D"]}/{record["L"]}')

def get_name_compat(result, color):
    try:
        return result[color]['name']
    except TypeError as e:
        return result[color]
    
if __name__ == '__main__':
    main()