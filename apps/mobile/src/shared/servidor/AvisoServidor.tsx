import { Banner } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useServidor } from './ServidorProvider';

export function AvisoServidor() {
  const { estado, tentarDeNovo } = useServidor();

  if (estado === 'acordando') {
    return (
      <SafeAreaView edges={['top']}>
        <Banner visible icon="weather-night">
          O servidor está acordando. Isso pode levar até um minuto e meio na primeira vez do dia.
        </Banner>
      </SafeAreaView>
    );
  }

  if (estado === 'falhou') {
    return (
      <SafeAreaView edges={['top']}>
        <Banner
          visible
          icon="cloud-off-outline"
          actions={[{ label: 'Tentar de novo', onPress: tentarDeNovo }]}
        >
          Não foi possível falar com o servidor. Confira a internet e tente de novo.
        </Banner>
      </SafeAreaView>
    );
  }

  return null;
}
