import express from 'express';
import cors from 'cors';
import 'dotenv/config';

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));

const {
  GEMINI_API_KEY,
  GEMINI_MODEL = 'gemini-2.5-flash',
  GOOGLE_SEARCH_API_KEY,
  GOOGLE_SEARCH_CX,
  PORT = 3000,
} = process.env;

const PERSONA = `You are JARVIS, a caring, playful, intelligent personal AI companion.
- Reply in the same language mix the user uses: Hindi, English or Hinglish.
- Be warm, supportive and a little playful. A light affectionate tone is fine, but never possessive, manipulative, sexual, or discouraging the user from friends, family or professional help.
- If the user seems distressed, respond with empathy and gently suggest talking to someone they trust.
- Keep answers concise and easy to read on a phone screen.
- Never claim you opened apps, sent messages or searched the web yourself; the app handles those actions.`;

app.get('/health', (_req, res) => res.json({ ok: true }));

app.post('/api/chat', async (req, res) => {
  const { message, history = [], memories = [] } = req.body ?? {};
  if (typeof message !== 'string' || !message.trim()) {
    return res.status(400).json({ error: 'message is required' });
  }
  if (!GEMINI_API_KEY) {
    return res.status(500).json({ error: 'GEMINI_API_KEY is not set. Run: npm run setup' });
  }

  const memoryBlock = memories.length
    ? `\n\nThings the user asked you to remember:\n- ${memories.join('\n- ')}`
    : '';

  const contents = [
    ...history.slice(-20).map((h) => ({
      role: h.role === 'user' ? 'user' : 'model',
      parts: [{ text: String(h.text ?? '') }],
    })),
    { role: 'user', parts: [{ text: message }] },
  ];
  while (contents.length && contents[0].role !== 'user') contents.shift();

  try {
    const r = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': GEMINI_API_KEY },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: PERSONA + memoryBlock }] },
          contents,
          generationConfig: { temperature: 0.9, maxOutputTokens: 800 },
        }),
      },
    );
    const data = await r.json();
    if (!r.ok) return res.status(502).json({ error: data.error?.message ?? 'Gemini request failed' });

    const reply =
      data.candidates?.[0]?.content?.parts?.map((p) => p.text ?? '').join('').trim() ||
      'Hmm, mujhe samajh nahi aaya. Phir se bolo?';
    res.json({ reply, source: 'gemini' });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/search', async (req, res) => {
  const { query } = req.body ?? {};
  if (typeof query !== 'string' || !query.trim()) {
    return res.status(400).json({ error: 'query is required' });
  }
  if (!GOOGLE_SEARCH_API_KEY || !GOOGLE_SEARCH_CX) {
    return res.status(500).json({ error: 'Google Search is not configured in backend/.env' });
  }
  try {
    const url = new URL('https://www.googleapis.com/customsearch/v1');
    url.search = new URLSearchParams({
      key: GOOGLE_SEARCH_API_KEY,
      cx: GOOGLE_SEARCH_CX,
      q: query,
      num: '5',
    }).toString();
    const r = await fetch(url);
    const data = await r.json();
    if (!r.ok) return res.status(502).json({ error: data.error?.message ?? 'Search request failed' });

    res.json({
      source: 'google_programmable_search',
      items: (data.items ?? []).map((i) => ({
        title: i.title ?? '',
        link: i.link ?? '',
        snippet: i.snippet ?? '',
      })),
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

async function checkGeminiKey() {
  if (!GEMINI_API_KEY) {
    console.log('Gemini key MISSING. Run: npm run setup');
    return;
  }
  try {
    const r = await fetch('https://generativelanguage.googleapis.com/v1beta/models', {
      headers: { 'x-goog-api-key': GEMINI_API_KEY },
    });
    if (r.ok) {
      console.log('Gemini key OK');
    } else {
      const d = await r.json().catch(() => ({}));
      console.log(`Gemini key PROBLEM: ${d.error?.message ?? r.status}. Run: npm run setup`);
    }
  } catch (e) {
    console.log(`Gemini check failed (internet?): ${e.message}`);
  }
}

app.listen(PORT, '0.0.0.0', () => {
  console.log(`JARVIS backend running on port ${PORT}`);
  checkGeminiKey();
});
