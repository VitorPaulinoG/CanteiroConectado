import MaterialCommunityIcons from '@expo/vector-icons/MaterialCommunityIcons';
import { Tabs } from 'expo-router';
import type { BottomTabBarProps } from 'expo-router/tabs';
import type { ComponentProps } from 'react';
import type { ColorValue } from 'react-native';
import { BottomNavigation } from 'react-native-paper';

import { usePermite } from '@/shared/sessao';

type NomeDoIcone = ComponentProps<typeof MaterialCommunityIcons>['name'];

function icone(nome: NomeDoIcone) {
  return function Icone({ color, size }: { color: ColorValue; size: number }) {
    return <MaterialCommunityIcons name={nome} color={String(color)} size={size} />;
  };
}

function BarraDeAbas({ state, descriptors, emitter, navigateToTab, insets }: BottomTabBarProps) {
  return (
    <BottomNavigation.Bar
      navigationState={state}
      safeAreaInsets={insets}
      onTabPress={({ route, preventDefault }) => {
        const evento = emitter.emit({
          type: 'tabPress',
          target: route.key,
          canPreventDefault: true,
        });
        if (evento.defaultPrevented) {
          preventDefault();
          return;
        }
        navigateToTab(route.key);
      }}
      renderIcon={({ route, focused, color }) =>
        descriptors[route.key].options.tabBarIcon?.({ focused, color, size: 24 }) ?? null
      }
      getLabelText={({ route }) => descriptors[route.key].options.title ?? route.name}
      getAccessibilityLabel={({ route }) => descriptors[route.key].options.title ?? route.name}
    />
  );
}

export default function AbasLayout() {
  const almoxarife = usePermite({ perfis: ['ALMOXARIFE'] });
  const gestor = usePermite({ perfis: ['GESTOR'] });

  return (
    <Tabs screenOptions={{ headerShown: false }} tabBar={(props) => <BarraDeAbas {...props} />}>
      <Tabs.Screen name="inicio" options={{ title: 'Início', tabBarIcon: icone('home') }} />
      <Tabs.Screen
        name="estoque"
        options={{ title: 'Estoque', tabBarIcon: icone('package-variant-closed') }}
      />
      <Tabs.Protected guard={almoxarife} redirectTo="/inicio">
        <Tabs.Screen
          name="movimentar"
          options={{ title: 'Movimentar', tabBarIcon: icone('swap-horizontal') }}
        />
      </Tabs.Protected>
      <Tabs.Screen
        name="requisicoes"
        options={{ title: 'Requisições', tabBarIcon: icone('clipboard-list-outline') }}
      />
      <Tabs.Protected guard={gestor} redirectTo="/inicio">
        <Tabs.Screen
          name="historico"
          options={{ title: 'Histórico', tabBarIcon: icone('history') }}
        />
      </Tabs.Protected>
      <Tabs.Screen name="mais" options={{ title: 'Mais', tabBarIcon: icone('dots-horizontal') }} />
    </Tabs>
  );
}
