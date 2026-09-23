import { fileURLToPath } from 'node:url'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { dirname, extname, join, normalize, sep } from 'node:path'
import type { IncomingMessage, ServerResponse } from 'node:http'
import { createCodexBridgeMiddleware } from './codexAppServerBridge.js'
import { createAuthMiddleware, type AuthMiddleware } from './authMiddleware.js'

const __dirname = dirname(fileURLToPath(import.meta.url))

export type ServerOptions = {
  password?: string
  distDir?: string
}

export type ServerInstance = {
  app: (req: IncomingMessage, res: ServerResponse) => void
  dispose: () => void
}

const MIME: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.txt': 'text/plain; charset=utf-8',
}

function sendNotFound(res: ServerResponse): void {
  res.statusCode = 404
  res.setHeader('Content-Type', 'text/plain; charset=utf-8')
  res.end('Not Found')
}

export function createServer(options: ServerOptions = {}): ServerInstance {
  const bridge = createCodexBridgeMiddleware()
  const auth: AuthMiddleware | null = options.password ? createAuthMiddleware(options.password) : null
  const distDir = options.distDir ?? join(__dirname, '..', 'dist')

  function serveStatic(req: IncomingMessage, res: ServerResponse, pathname: string): void {
    const requested = pathname === '/' ? '/index.html' : pathname
    const safePath = normalize(requested).replace(/^(\.\.[\\/])+/, '').replace(/^([\\/])+/, '')
    let fullPath = join(distDir, safePath)
    if (!fullPath.startsWith(distDir + sep) && fullPath !== distDir) {
      sendNotFound(res)
      return
    }

    if (!existsSync(fullPath) || !statSync(fullPath).isFile()) {
      // SPA fallback
      fullPath = join(distDir, 'index.html')
      if (!existsSync(fullPath)) {
        sendNotFound(res)
        return
      }
    }

    const mime = MIME[extname(fullPath).toLowerCase()] ?? 'application/octet-stream'
    res.statusCode = 200
    res.setHeader('Content-Type', mime)
    createReadStream(fullPath).pipe(res)
  }

  const app = (req: IncomingMessage, res: ServerResponse): void => {
    let pathname: string
    try {
      pathname = decodeURIComponent(new URL(req.url ?? '/', 'http://localhost').pathname)
    } catch {
      sendNotFound(res)
      return
    }

    if (auth) {
      let authed = false
      auth(req, res, () => {
        authed = true
      })
      if (!authed) {
        // Auth middleware already responded (login POST handled asynchronously
        // or login page served). Do not fall through.
        return
      }
    }

    let passedToStatic = false
    const next = () => {
      passedToStatic = true
    }

    bridge(req, res, next)
      .then(() => {
        if (passedToStatic) {
          serveStatic(req, res, pathname)
        }
      })
      .catch((error: unknown) => {
        const message =
          error instanceof Error && error.message.trim().length > 0
            ? error.message.trim()
            : 'Unknown bridge error'
        if (res.writableEnded || res.destroyed) return
        res.statusCode = 502
        res.setHeader('Content-Type', 'application/json; charset=utf-8')
        res.end(JSON.stringify({ error: message }))
      })
  }

  return {
    app,
    dispose: () => {
      try {
        bridge.dispose()
      } catch {
        // ignore disposal errors
      }
    },
  }
}