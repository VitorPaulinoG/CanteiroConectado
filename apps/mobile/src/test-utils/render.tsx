import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import type { ReactElement, ReactNode } from 'react';
import { PaperProvider } from 'react-native-paper';

import { appTheme } from '@/theme';

export function criarQueryClientDeTeste() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } },
  });
}

export function Provedores({
  children,
  queryClient = criarQueryClientDeTeste(),
}: {
  children: ReactNode;
  queryClient?: QueryClient;
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <PaperProvider theme={appTheme}>{children}</PaperProvider>
    </QueryClientProvider>
  );
}

export function renderizar(ui: ReactElement, queryClient = criarQueryClientDeTeste()) {
  return render(<Provedores queryClient={queryClient}>{ui}</Provedores>);
}
