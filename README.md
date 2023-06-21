# ChatGPT for Android

An Android implementation of OpenAI's official ChatGPT iOS app.
Users can send messages and receive responses from the GPT-3.5 and GPT-4 models in a chat-like
interface.

## Features

- Chat-like interface for easy interaction with ChatGPT
- Pick between GPT-3.5 and GPT-4
- Custom fonts and smooth animations for cool UX;)

## Installation

1. Clone this repository:

```bash
git clone https://github.com/aphexcx/ChatGPT-Android.git
```

2. Open the project in Android Studio
3. Build and run the application on an emulator or a physical device

## Configuration

Make sure to set up your OpenAI API key in the `local.properties` file:

```
openai.api_key=your_api_key_here
```

Replace `your_api_key_here` with your actual API key.

## Dependencies

- [OpenAI SDK](https://github.com/openai/openai)
- [Markwon](https://github.com/noties/Markwon)
- [OkHttp](https://github.com/square/okhttp)
- [Jsoup](https://github.com/jhy/jsoup)
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Material Components for Android](https://github.com/material-components/material-components-android)
