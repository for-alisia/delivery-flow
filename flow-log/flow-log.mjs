#!/usr/bin/env node

import { runCli } from "./src/cli.mjs";

runCli(process.argv.slice(2), {
  cwd: process.cwd(),
  stdout: process.stdout,
  stderr: process.stderr
});
