import express from "express";
import { createServer } from "http";
import { WebSocketServer } from "ws";

const app = express();
const server = createServer(app);
const wss = new WebSocketServer({ server, path: "/ws" });

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "backend-node" });
});

wss.on("connection", (socket) => {
  socket.send(JSON.stringify({ type: "connected", message: "backend-node realtime channel" }));
  socket.on("message", (data) => {
    // Placeholder relay: Checkpoint 8 wires this to real dispatch board events.
    socket.send(data);
  });
});

const PORT = process.env.PORT || 4000;
server.listen(PORT, () => {
  console.log(`backend-node listening on http://localhost:${PORT} (WebSocket at /ws)`);
});
