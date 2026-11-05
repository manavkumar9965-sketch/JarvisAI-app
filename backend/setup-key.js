// Interactive setup: key screen par nahi dikhti, turant Google par test hoti hai,
// aur sirf valid hone par .env mein save hoti hai.
import readline from 'node:readline';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const envPath = path.join(here, '.env');
const examplePath = path.join(here, '.env.example');

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
let muted = false;
rl._writeToOutput = (s) => { if (!muted) rl.output.write(s); };

function ask(question, secret = false) {
  return new Promise((resolve) => {
    muted = secret;
    rl.question(question, (answer) => {
      muted = false;
      process.stdout.write('\n');
      resolve(answer);
    });
  });
}

async function testKey(key) {
  const r = await fetch('https://generativelanguage.googleapis.com/v1beta/models', {
    headers: { 'x-goog-api-key': key },
  });
  if (r.ok) return { ok: true };
  const data = await r.json().catch(() => ({}));
  return { ok: false, message: data.error?.message ?? `HTTP ${r.status}` };
}

async function main() {
  const raw = await ask('Gemini API key paste karo (screen par nahi dikhegi), phir Enter: ', true);
  const key = raw.replace(/\s+/g, '').replace(/^GEMINI_API_KEY=/, '');

  if (key.length < 20) {
    console.log('Key bahut chhoti lag rahi hai. Dobara run karo: npm run setup');
    return;
  }

  console.log('Key Google par test ho rahi hai...');
  const result = await testKey(key);
  if (!result.ok) {
    console.log(`Key kaam nahi kar rahi: ${result.message}`);
    console.log('Nayi key Google AI Studio (aistudio.google.com/apikey) se banao aur dobara run karo.');
    return;
  }

  let env = fs.existsSync(envPath) ? fs.readFileSync(envPath, 'utf8') : fs.readFileSync(examplePath, 'utf8');
  const line = `GEMINI_API_KEY=${key}`;
  env = /^GEMINI_API_KEY=.*$/m.test(env)
    ? env.replace(/^GEMINI_API_KEY=.*$/m, () => line)
    : `${line}\n${env}`;
  fs.writeFileSync(envPath, env, { mode: 0o600 });
  console.log('Key save ho gayi. Ab "npm start" chalao.');
}

main()
  .catch((e) => console.log(`Error: ${e.message}`))
  .finally(() => rl.close());
