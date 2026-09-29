import Fastify from "fastify";
import cors from "@fastify/cors";
import { WebSocketServer } from "ws";
import { ChessGame } from "./game.js";

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

const games = new Map<string, ChessGame>();

app.get("/health", async () => ({ ok: true, service: "chess-platform" }));

app.post("/api/games", async (_request, reply) => {
  const id = crypto.randomUUID();
  const game = new ChessGame();
  games.set(id, game);
  return reply.code(201).send({ id, fen: game.fen, pgn: game.pgn, status: game.status() });
});

app.get("/api/games/:id", async (request, reply) => {
  const { id } = request.params as { id: string };
  const game = games.get(id);
  if (!game) return reply.code(404).send({ error: "game_not_found" });
  return { id, fen: game.fen, pgn: game.pgn, status: game.status() };
});

app.post("/api/games/:id/moves", async (request, reply) => {
  const { id } = request.params as { id: string };
  const body = request.body as { from: string; to: string; promotion?: "q" | "r" | "b" | "n" };
  const game = games.get(id);
  if (!game) return reply.code(404).send({ error: "game_not_found" });

  try {
    const move = game.move(body.from, body.to, body.promotion);
    return { move, fen: game.fen, pgn: game.pgn, status: game.status() };
  } catch {
    return reply.code(400).send({ error: "illegal_move" });
  }
});

await app.listen({ port: Number(process.env.PORT ?? 8080), host: "0.0.0.0" });

const wss = new WebSocketServer({ port: 8081 });
wss.on("connection", socket => {
  socket.send(JSON.stringify({ type: "connected", service: "chess-platform" }));
});
