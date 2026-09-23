import { createServer } from 'node:http'
import { createServer as createApp } from '../server/httpServer.js'
import { generatePassword } from '../server/password.js'

type ParsedArgs = Record<string, string | true>

function parseArgs(argv: string[]): ParsedArgs {
  const out: ParsedArgs = {}
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i]
    if (!arg.startsWith('--')) continue
    const eq = arg.indexOf('=')
    const key = eq >= 0 ? arg.slice(0, eq) : arg
    if (eq >= 0) {
      out[key] = arg.slice(eq + 1)
      continue
    }
    const next = argv[i + 1]
    if (next && !next.startsWith('--')) {
      out[key] = next
      i++
    } else {
      out[key] = true
    }
  }
  return out
}

const opts = parseArgs(process.argv.slice(2))

const port = parseInt(String(opts['--port'] ?? '3000'), 10) || 3000

let password: string | undefined
if (opts['--no-password'] === true) {
  password = undefined
} else if (typeof opts['--password'] === 'string') {
  password = opts['--password']
} else {
  password = generatePassword()
}

const { app, dispose } = createApp({ password })
const server = createServer(app)

server.listen(port, () => {
  const lines = [
    '',
    'Codex Web Local is running!',
    '',
    `  Local:    http://localhost:${String(port)}`,
  ]

  if (password) {
    lines.push(`  Password: ${password}`)
  }

  lines.push('')
  console.log(lines.join('\n'))
})

function shutdown() {
  console.log('\nShutting down...')
  server.close(() => {
    dispose()
    process.exit(0)
  })
  // Force exit after timeout
  setTimeout(() => {
    dispose()
    process.exit(1)
  }, 5000).unref()
}

process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)