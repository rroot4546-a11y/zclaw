import { defineConfig } from 'tsup'

export default defineConfig({
  entry: ['src/cli/index.ts'],
  outDir: 'dist-cli',
  format: 'esm',
  target: 'node18',
  sourcemap: true,
  clean: true,
  banner: {
    js: '#!/usr/bin/env node',
  },
  // Bundle EVERY dependency (express, commander AND all of express's
  // transitive deps) into the CLI so it runs completely standalone from
  // the APK assets — no node_modules tree required at runtime.
  noExternal: [/.*/],
})
