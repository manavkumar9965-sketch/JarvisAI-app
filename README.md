# JARVIS AI

Personal AI companion for Android (Gemini chat, live Google search, voice, app launching, memory).

## Termux par backend setup (sabse important)

    pkg install nodejs unzip
    termux-setup-storage
    cp /sdcard/Download/JarvisAI.zip ~/ && cd ~ && unzip -o JarvisAI.zip
    cd jarvis-ai/backend
    npm install
    npm run setup      # Gemini key paste karo (screen par nahi dikhegi), key apne aap test hogi
    npm start          # "Gemini key OK" aana chahiye

Test (naye Termux session mein):

    curl http://127.0.0.1:3000/health
    curl -X POST http://127.0.0.1:3000/api/chat -H "Content-Type: application/json" -d '{"message":"Hello baby, kaise ho?"}'

## Google Search keys (optional)
`backend/.env` mein ye bharo, phir server restart karo:
- GOOGLE_SEARCH_API_KEY: Google Cloud Console > Credentials > API key (Custom Search API enabled honi chahiye)
- GOOGLE_SEARCH_CX: https://programmablesearchengine.google.com/ par banaya hua Search engine ID

## Android APK

Option A: PC par Android Studio mein `android` folder kholo, Build > Build APK(s).
Option B: GitHub par ye poora folder push karo. `.github/workflows/build-apk.yml` APK build karega, Actions tab se `JarvisAI-debug-apk` download karo.

Backend URL `android/gradle.properties` mein `BACKEND_URL` se set hota hai:
- Backend isi phone par Termux mein: `http://127.0.0.1:3000` (default)
- Emulator se PC par: `http://10.0.2.2:3000`
- Real phone se PC par: `http://<PC-ka-LAN-IP>:3000`

## Chat commands
- Normal chat: Hindi / English / Hinglish mein
- `search latest AI news`: live Google results, links ke saath
- `open youtube | whatsapp | instagram | chrome | settings | camera | gallery`
- `call 9876543210`: dialer khulega
- `remember mera favourite food paneer hai`: memory mein save, Memory button se dekh/delete kar sakte ho

## Abhi shamil nahi hai
Accessibility (screen control), social media comment assistant, Firebase login/Firestore, avatar art, onboarding screens, encrypted memory storage. Ye agle phases mein honge.
