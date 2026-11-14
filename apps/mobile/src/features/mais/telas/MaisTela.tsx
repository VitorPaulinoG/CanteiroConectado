import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Avatar, Button, Dialog, Divider, List, Portal, Text } from 'react-native-paper';

import { TelaComCabecalho } from '@/shared';
import { useCanteiroAtivo } from '@/shared/canteiro';
import { iniciais, rotuloDoPerfil, SomentePerfil, useSessao, useUsuario } from '@/shared/sessao';

export function MaisTela() {
  const router = useRouter();
  const usuario = useUsuario();
  const { sair } = useSessao();
  const { canteiro, podeEscolher } = useCanteiroAtivo();
  const [confirmandoSaida, setConfirmandoSaida] = useState(false);

  return (
    <TelaComCabecalho titulo="Mais">
      <ScrollView contentContainerStyle={styles.conteudo}>
        <View style={styles.perfil}>
          <Avatar.Text label={iniciais(usuario.nome)} size={64} accessibilityLabel="Iniciais" />
          <View style={styles.dados}>
            <Text variant="titleLarge">{usuario.nome}</Text>
            <Text variant="bodyLarge">{rotuloDoPerfil(usuario)}</Text>
            <Text variant="bodyMedium">{usuario.email}</Text>
          </View>
        </View>

        <Divider />

        <List.Section>
          <List.Subheader>Canteiro</List.Subheader>
          <List.Item
            title={canteiro?.nome ?? 'Nenhum canteiro'}
            description={podeEscolher ? 'Toque para trocar' : 'Seu canteiro de trabalho'}
            left={(props) => <List.Icon {...props} icon="office-building-marker-outline" />}
            right={
              podeEscolher ? (props) => <List.Icon {...props} icon="chevron-right" /> : undefined
            }
            onPress={podeEscolher ? () => router.push('/canteiro') : undefined}
          />
        </List.Section>

        <List.Section>
          <List.Subheader>Cadastros</List.Subheader>
          <List.Item
            title="Colaboradores"
            description="Quem retira material no canteiro"
            left={(props) => <List.Icon {...props} icon="account-hard-hat-outline" />}
            right={(props) => <List.Icon {...props} icon="chevron-right" />}
            onPress={() => router.push('/cadastros/colaboradores')}
          />
        </List.Section>

        <SomentePerfil administrador>
          <List.Section>
            <List.Subheader>Administração</List.Subheader>
            <List.Item
              title="Canteiros"
              description="Obras e situação"
              left={(props) => <List.Icon {...props} icon="office-building-outline" />}
              right={(props) => <List.Icon {...props} icon="chevron-right" />}
              onPress={() => router.push('/cadastros/canteiros')}
            />
            <List.Item
              title="Funcionários"
              description="Acessos, senhas temporárias e desativação"
              left={(props) => <List.Icon {...props} icon="account-group-outline" />}
              right={(props) => <List.Icon {...props} icon="chevron-right" />}
              onPress={() => router.push('/cadastros/usuarios')}
            />
          </List.Section>
        </SomentePerfil>

        <List.Section>
          <List.Subheader>Conta</List.Subheader>
          <List.Item
            title="Trocar senha"
            left={(props) => <List.Icon {...props} icon="lock-reset" />}
            right={(props) => <List.Icon {...props} icon="chevron-right" />}
            onPress={() => router.push('/trocar-senha')}
          />
          <List.Item
            title="Sobre"
            description="Versões do app e da API"
            left={(props) => <List.Icon {...props} icon="information-outline" />}
            right={(props) => <List.Icon {...props} icon="chevron-right" />}
            onPress={() => router.push('/sobre')}
          />
        </List.Section>

        <View style={styles.sair}>
          <Button mode="outlined" icon="logout" onPress={() => setConfirmandoSaida(true)}>
            Sair
          </Button>
        </View>
      </ScrollView>

      <Portal>
        <Dialog visible={confirmandoSaida} onDismiss={() => setConfirmandoSaida(false)}>
          <Dialog.Title>Sair do aplicativo?</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyLarge">
              Você vai precisar do e-mail e da senha para entrar de novo.
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setConfirmandoSaida(false)}>Cancelar</Button>
            <Button
              onPress={() => {
                setConfirmandoSaida(false);
                void sair();
              }}
            >
              Sair
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    </TelaComCabecalho>
  );
}

const styles = StyleSheet.create({
  conteudo: { paddingBottom: 24 },
  perfil: { flexDirection: 'row', alignItems: 'center', gap: 16, padding: 16 },
  dados: { flex: 1, gap: 2 },
  sair: { paddingHorizontal: 16, paddingTop: 8 },
});
