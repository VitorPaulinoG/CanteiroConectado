import type { ExpoConfig } from 'expo/config';

const config: ExpoConfig = {
  name: 'Canteiro Conectado',
  slug: 'canteiroconectado',
  owner: 'vitorpaulinogs-team',
  version: '1.0.0',
  orientation: 'portrait',
  scheme: 'canteiroconectado',
  userInterfaceStyle: 'light',
  icon: './assets/images/icon.png',
  android: {
    package: 'br.com.canteiroconectado',
    adaptiveIcon: {
      foregroundImage: './assets/images/android-icon-foreground.png',
      backgroundImage: './assets/images/android-icon-background.png',
      monochromeImage: './assets/images/android-icon-monochrome.png',
      backgroundColor: '#E6F4FE',
    },
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        image: './assets/images/splash-icon.png',
        imageWidth: 200,
        backgroundColor: '#E6F4FE',
      },
    ],
  ],
  extra: {
    eas: {
      projectId: '9e8ff013-c33f-4add-8837-f58baa2c41d9',
    },
  },
};

export default config;
