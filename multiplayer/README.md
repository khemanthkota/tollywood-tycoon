# Cinema Tycoon Bid multiplayer preview

This is an **early prototype** and is not yet integrated with the single-device game engine, celebrity catalog, scoring, or winner logic. No accounts or persistent room storage.

## Run on local Wi-Fi
1. Install Node.js 20+ on a PC.
2. In the repository's `multiplayer` directory run `npm install` and `npm start`.
3. Find the PC's LAN IPv4 address (e.g. `192.168.1.20`). Allow TCP port 3000 through the firewall.
4. Open `app/src/main/assets/multiplayer.html` in a browser on the TV/host and phones (serve the HTML over HTTP, e.g. `python -m http.server 8080` from `app/src/main/assets`).
5. Enter `ws://192.168.1.20:3000/ws` on all devices. Host creates a room; phones enter the six-digit code and their names.

## Internet play
Deploy the same Node server to a provider supporting long-running WebSockets. Set `PORT` if needed, enable HTTPS/TLS at the proxy, and enter its `wss://your-domain/ws` URL on each device. **No internet deployment is included in this commit.**

## Known limitations
- The server keeps rooms in RAM; a restart loses games. Host disconnect closes the room.
- No reconnect authentication, anti-cheat, room expiration, rate limiting, or production security controls. Do not publicly deploy without hardening.
- Movie submissions require owned director/actor/actress and >=30% cast-cost production budget, but results intentionally show **profit pending** instead of inventing scoring.
- The Android WebView may block insecure `ws://` from file pages; browser-based HTTP serving is the recommended local test.
- Timed auctions extend by five seconds when a last-second bid arrives.
- This is a separate prototype UI; existing offline single-device mode remains unchanged.
