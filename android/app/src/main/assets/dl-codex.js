"use strict";
/*
 * Resumable, verified downloader for the Codex native binary tarball.
 * Runs on-device through Node.js. Survives flaky mobile networks by
 * resuming from the locally-downloaded chunk on every attempt and only
 * exiting 0 when the final size exactly matches the expected size.
 */
const https = require("https");
const fs = require("fs");
const path = require("path");

const URL = process.env.CODEX_TGZ_URL || "";
const FILE = path.join(process.cwd(), "codex-bin.tgz");
const EXPECTED = parseInt(process.env.CODEX_TGZ_SIZE || "0", 10);
const INTERNAL_ATTEMPTS = 8;
const CHUNK = 8 * 1024 * 1024;

function log(msg) { console.error("[download] " + msg); }
function fail(msg) { log("FATAL: " + msg); process.exit(1); }

function download() {
  return new Promise((resolve) => {
    const got = fs.existsSync(FILE) ? fs.statSync(FILE).size : 0;
    if (got === EXPECTED) {
      log("already complete (" + got + " bytes)");
      resolve(true);
      return;
    }
    let fd;
    try {
      fd = fs.openSync(FILE, got > 0 ? "a" : "w");
    } catch (e) {
      log("cannot open output: " + e.message);
      resolve(false);
      return;
    }
    const headers = {};
    if (got > 0) headers["Range"] = "bytes=" + got + "-";
    let count = got;
    let nextLog = Math.floor(count / CHUNK) * CHUNK;

    const req = https.get(URL, { headers: headers, timeout: 60000 }, (res) => {
      const code = res.statusCode;
      if (code === 200 && got > 0) {
        try { fs.ftruncateSync(fd, 0); count = 0; } catch (e) {}
      }
      if (code !== 200 && code !== 206) {
        try { fs.closeSync(fd); } catch (e) {}
        log("server responded HTTP " + code);
        resolve(false);
        return;
      }
      res.on("data", (c) => {
        try {
          fs.writeSync(fd, c);
          count += c.length;
          if (count - nextLog >= CHUNK) {
            nextLog = count;
            log(Math.floor(count / CHUNK) * 8 + " MB");
          }
        } catch (e) {
          log("write error: " + e.message);
        }
      });
      res.on("end", () => {
        try { fs.closeSync(fd); } catch (e) {}
        log("downloaded " + count + " of " + EXPECTED + " bytes");
        resolve(count === EXPECTED);
      });
      res.on("error", (e) => {
        try { fs.closeSync(fd); } catch (x) {}
        log("stream error: " + e.message);
        resolve(false);
      });
      res.on("aborted", () => {
        try { fs.closeSync(fd); } catch (x) {}
        log("connection aborted");
        resolve(false);
      });
    });
    req.setTimeout(60000, () => req.destroy(new Error("timeout")));
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.on("error", (e) => {
      log("request error: " + e.message);
      resolve(false);
    });
  });
}

(async () => {
  if (EXPECTED <= 0) fail("CODEX_TGZ_SIZE missing");
  if (!URL) fail("CODEX_TGZ_URL missing");
  for (let i = 1; i <= INTERNAL_ATTEMPTS; i++) {
    const ok = await download();
    if (ok) process.exit(0);
    log("attempt " + i + " of " + INTERNAL_ATTEMPTS + " failed — resuming");
    if (i < INTERNAL_ATTEMPTS) await new Promise((r) => setTimeout(r, 2000));
  }
  fail("could not download after " + INTERNAL_ATTEMPTS + " attempts");
})();