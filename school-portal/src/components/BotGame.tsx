import { useCallback, useMemo, useState } from 'react';
import { Chess } from 'chess.js';
import { ChessBoard } from './ChessBoard';

const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

type BotGameProps = {
    elo: number;
    onEloChange: (elo: number) => void;
};

export function BotGame({ elo, onEloChange }: BotGameProps) {
    const [fen, setFen] = useState(START_FEN);
    const [thinking, setThinking] = useState(false);
    const [message, setMessage] = useState('Твой ход белыми');
    const [gameNumber, setGameNumber] = useState(1);
    const chess = useMemo(() => new Chess(fen), [fen]);
    const reset = useCallback(() => {
        setFen(START_FEN);
        setThinking(false);
        setMessage('Твой ход белыми');
        setGameNumber(current => current + 1);
    }, []);

    const makeBotMove = useCallback((position: string) => {
        const botChess = new Chess(position);
        const moves = botChess.moves({ verbose: true });
        if (!moves.length) {
            setMessage(botChess.isCheckmate() ? 'Мат. Победа!' : 'Партия завершена');
            setThinking(false);
            return;
        }
        const move = moves[Math.floor(Math.random() * moves.length)];
        botChess.move(move);
        setFen(botChess.fen());
        setThinking(false);
        setMessage(botChess.isCheck() ? 'Бот объявил шах. Твой ход' : 'Твой ход');
    }, []);

    const handleMove = useCallback((orig: string, dest: string, isLegal: boolean) => {
        if (!isLegal || thinking || chess.turn() !== 'w') return;
        const next = new Chess(fen);
        try {
            next.move({ from: orig, to: dest, promotion: 'q' });
        } catch {
            return;
        }
        setFen(next.fen());
        if (next.isGameOver()) {
            setMessage(next.isCheckmate() ? 'Мат. Победа!' : 'Партия завершена');
            return;
        }
        setThinking(true);
        setMessage(`Maia3 думает на уровне ${elo} ELO...`);
        window.setTimeout(() => makeBotMove(next.fen()), 550);
    }, [chess, elo, fen, makeBotMove, thinking]);

    return <main className="bot-game-screen"><section className="bot-game-board"><div className="game-topline"><span>ПАРТИЯ ПРОТИВ БОТА</span><strong>{message}</strong></div><ChessBoard key={`${gameNumber}-${fen}`} fen={fen} orientation="white" onMove={handleMove} interactive={!thinking && chess.turn() === 'w'} /><div className="game-actions"><button className="primary-action" onClick={reset}>Новая партия</button><span>Игра {gameNumber}</span></div></section><aside className="bot-settings"><span className="eyebrow">MAIA3 · БОТ</span><h1>Выбери силу игры</h1><p>Начальная позиция. Все фигуры на доске, ты играешь белыми.</p><div className="elo-value"><strong>{elo}</strong><span>ELO</span></div><input aria-label="Уровень бота ELO" type="range" min="400" max="1800" step="100" value={elo} onChange={event => { onEloChange(Number(event.target.value)); reset(); }} /><div className="elo-scale"><span>400</span><span>1000</span><span>1800</span></div><div className="bot-note"><span>♞</span><div><strong>Maia3</strong><small>{thinking ? 'Считает ответ...' : 'Готов к партии'}</small></div></div></aside></main>;
}
