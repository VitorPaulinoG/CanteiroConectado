import { StyleSheet, View } from 'react-native';
import { Button, Modal, Portal, SegmentedButtons, Text } from 'react-native-paper';

import { CampoData } from '@/shared';
import { SeletorColaborador } from '@/shared/colaborador';
import type { TipoMovimentacao } from '@/shared/estoque';
import { SeletorItem } from '@/shared/item';
import { hoje } from '@/shared/utils/data';
import { useAppTheme } from '@/theme';

import type { FiltrosDoHistorico } from '../filtros';

type Props = {
  visivel: boolean;
  canteiroId: string;
  filtros: FiltrosDoHistorico;
  aoMudar: (mudanca: Partial<FiltrosDoHistorico>) => void;
  aoLimpar: () => void;
  aoFechar: () => void;
};

export function FiltrosModal({ visivel, canteiroId, filtros, aoMudar, aoLimpar, aoFechar }: Props) {
  const theme = useAppTheme();
  return (
    <Portal>
      <Modal
        visible={visivel}
        onDismiss={aoFechar}
        style={styles.modal}
        contentContainerStyle={[styles.folha, { backgroundColor: theme.colors.surface }]}
      >
        <View style={styles.conteudo}>
          <Text variant="titleLarge">Filtros</Text>
          <Text variant="labelLarge">Tipo</Text>
          <SegmentedButtons
            value={filtros.tipo ?? 'TODOS'}
            onValueChange={(valor) =>
              aoMudar({ tipo: valor === 'TODOS' ? null : (valor as TipoMovimentacao) })
            }
            buttons={[
              { value: 'TODOS', label: 'Todos' },
              { value: 'ENTRADA', label: 'Entradas' },
              { value: 'SAIDA', label: 'Saídas' },
            ]}
          />
          <SeletorItem
            modo="catalogo"
            incluirInativos
            rotulo="Item"
            valor={
              filtros.item
                ? {
                    itemId: filtros.item.itemId,
                    descricao: filtros.item.descricao,
                    codigo: '',
                    unidadeSimbolo: '',
                    tipo: 'MATERIAL',
                  }
                : null
            }
            aoMudar={(item) =>
              aoMudar({ item: { itemId: item.itemId, descricao: item.descricao } })
            }
          />
          {filtros.item ? (
            <Button compact onPress={() => aoMudar({ item: null })} style={styles.limparCampo}>
              Qualquer item
            </Button>
          ) : null}
          <SeletorColaborador
            canteiroId={canteiroId}
            rotulo="Colaborador"
            permitirCriar={false}
            incluirInativos
            valor={filtros.colaborador ? { ...filtros.colaborador, canteiroId, ativo: true } : null}
            aoMudar={(colaborador) =>
              aoMudar({
                colaborador: colaborador ? { id: colaborador.id, nome: colaborador.nome } : null,
              })
            }
          />
          {filtros.periodo === 'personalizado' ? (
            <View style={styles.datas}>
              <View style={styles.data}>
                <CampoData
                  rotulo="De"
                  valor={filtros.de}
                  aoMudar={(de) => aoMudar({ de })}
                  opcional
                />
              </View>
              <View style={styles.data}>
                <CampoData
                  rotulo="Até"
                  valor={filtros.ate}
                  aoMudar={(ate) => aoMudar({ ate })}
                  minimo={filtros.de ?? undefined}
                  dica={filtros.ate === hoje() ? 'Inclui o dia de hoje' : undefined}
                  opcional
                />
              </View>
            </View>
          ) : null}
          <View style={styles.botoes}>
            <Button onPress={aoLimpar}>Limpar filtros</Button>
            <Button mode="contained" onPress={aoFechar} contentStyle={styles.botao}>
              Pronto
            </Button>
          </View>
        </View>
      </Modal>
    </Portal>
  );
}

const styles = StyleSheet.create({
  modal: { justifyContent: 'flex-end', margin: 0 },
  folha: { borderTopLeftRadius: 24, borderTopRightRadius: 24 },
  conteudo: { padding: 24, gap: 8 },
  limparCampo: { alignSelf: 'flex-start', marginTop: -12 },
  datas: { flexDirection: 'row', gap: 12 },
  data: { flex: 1 },
  botoes: { flexDirection: 'row', justifyContent: 'flex-end', gap: 8, marginTop: 8 },
  botao: { minHeight: 48 },
});
