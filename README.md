# ChatGPT pour Android

Une implémentation Android de l'application officielle ChatGPT iOS d'OpenAI.
Les utilisateurs peuvent envoyer des messages et recevoir des réponses des modèles GPT-3.5 et GPT-4 dans une interface de type chat.

## Caractéristiques

- Interface de type chat pour une interaction facile avec ChatGPT
- Choisissez entre GPT-3.5 et GPT-4
- Polices personnalisées et animations fluides pour une UX cool ;)

## Installation

1. Clonez ce dépôt:

```bash
git clone https://github.com/aphexcx/ChatGPT-Android.git
```

2. Ouvrez le projet dans Android Studio
3. Construisez et exécutez l'application sur un émulateur ou un appareil physique

## Configuration

Assurez-vous de configurer votre clé API OpenAI dans le fichier `local.properties`:

```
openai.api_key=votre_clé_api_ici
```

Remplacez `votre_clé_api_ici` par votre véritable clé API.

## Dépendances

- [SDK OpenAI](https://github.com/openai/openai)
- [Markwon](https://github.com/noties/Markwon)
- [OkHttp](https://github.com/square/okhttp)
- [Jsoup](https://github.com/jhy/jsoup)
- [Coroutines Kotlin](https://github.com/Kotlin/kotlinx.coroutines)
- [Composants d'architecture Android](https://developer.android.com/topic/libraries/architecture)
- [Composants matériels pour Android](https://github.com/material-components/material-components-android)
