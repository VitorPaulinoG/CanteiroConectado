import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, Chip, Divider, List, Text } from 'react-native-paper';

import { ErrorState, LoadingState, TelaComCabecalho } from '@/shared';
import { paraAppError } from '@/shared/api';
import {
  ICONE_DO_TIPO_DE_MOVIMENTACAO,
  ROTULO_DO_MOTIVO,
  ROTULO_DO_TIPO_DE_MOVIMENTACAO,
  type Movimentacao,
} from '@/shared/estoque';
import { useUsuario } from '@/shared/sessao';
import { formatarDataHora } from '@/shared/utils/format';
import { formatarQuantidade } from '@/shared/utils/quantidade';
import { useAppTheme } from '@/theme';

import { buscarMovimentacao, chavesMovimentacao } from '../api';
import { EstornoDialog } from '../componentes/EstornoDialog';

type Props = { id: string };

export function DetalheMovimentacaoTela({ id }: Props) {
  const router = useRouter();
  const consulta = useQuery({
    queryKey: chavesMovimentacao.detalhe(id),
    queryFn: () => buscarMovimentacao(id),
  });
  const movimentacao = consulta.data;

  return (
    <TelaComCabecalho
      titulo={movimentacao ? ROTULO_DO_TIPO_DE_MOVIMENTACAO[movimentacao.tipo] : 'Movimentação'}
      subtitulo={movimentacao ? ROTULO_DO_MOTIVO[movimentacao.motivo] : undefined}
      aoVoltar={() => router.back()}
    >
      {consulta.isPending ? (
        <LoadingState message="Carregando o registro..." />
      ) : consulta.isError ? (
        <ErrorState
          message={paraAppError(consulta.error).message}
          onRetry={() => consulta.refetch()}
        />
      ) : (
        <Conteudo
          movimentacao={consulta.data}
          atualizando={consulta.isRefetching}
          aoAtualizar={() => void consulta.refetch()}
        />
      )}
    </TelaComCabecalho>
  );
}

export function podeEstornar(
  movimentacao: Movimentacao,
  usuario: { perfil: string; canteiroId: string | null },
): boolean {
  return (
    usuario.perfil === 'ALMOXARIFE' &&
    usuario.canteiroId === movimentacao.canteiroId &&
    movimentacao.motivo !== 'ESTORNO' &&
    !movimentacao.estornadaPorId
  );
}

type PropsConteudo = { movimentacao: Movimentacao; atualizando: boolean; aoAtualizar: () => void };

function Conteudo({ movimentacao, atualizando, aoAtualizar }: PropsConteudo) {
  const router = useRouter();
  const theme = useAppTheme();
  const usuario = useUsuario();
  const [estornando, setEstornando] = useState(false);
  const destaque = movimentacao.motivo === 'ESTORNO' || movimentacao.motivo === 'AJUSTE';
  const sinal = movimentacao.tipo === 'ENTRADA' ? '+' : '−';

  const campos: [string, string | null][] = [
    ['Item', `${movimentacao.itemDescricao} (${movimentacao.itemCodigo})`],
    [
      'Quantidade',
      `${sinal}${formatarQuantidade(movimentacao.quantidade, movimentacao.unidadeSimbolo)}`,
    ],
    ['Saldo depois', formatarQuantidade(movimentacao.saldoApos, movimentacao.unidadeSimbolo)],
    ['Canteiro', movimentacao.canteiroNome],
    ['Frente de obra', movimentacao.frenteObra],
    ['Colaborador', movimentacao.colaboradorNome],
    ['Registrado por', movimentacao.registradoPorNome],
    ['Data e hora', formatarDataHora(movimentacao.criadoEm)],
    [
      movimentacao.motivo === 'ESTORNO' ? 'Motivo do estorno' : 'Observação',
      movimentacao.observacao,
    ],
  ];

  return (
    <>
      <ScrollView
        contentContainerStyle={styles.conteudo}
        refreshControl={<RefreshControl refreshing={atualizando} onRefresh={aoAtualizar} />}
      >
        <Card mode="contained">
          <Card.Title
            title={`${ROTULO_DO_TIPO_DE_MOVIMENTACAO[movimentacao.tipo]} · ${ROTULO_DO_MOTIVO[movimentacao.motivo]}`}
            titleVariant="titleLarge"
            left={(props) => (
              <List.Icon {...props} icon={ICONE_DO_TIPO_DE_MOVIMENTACAO[movimentacao.tipo]} />
            )}
          />
          <Card.Content style={styles.selos}>
            {destaque ? (
              <Chip
                compact
                icon={movimentacao.motivo === 'ESTORNO' ? 'undo-variant' : 'tune-variant'}
                style={{ backgroundColor: theme.status.belowMinimumContainer }}
                textStyle={{ color: theme.status.onBelowMinimumContainer }}
              >
                {ROTULO_DO_MOTIVO[movimentacao.motivo]}
              </Chip>
            ) : null}
            {movimentacao.estornadaPorId ? (
              <Chip
                compact
                icon="undo"
                style={{ backgroundColor: theme.status.emptyContainer }}
                textStyle={{ color: theme.status.onEmptyContainer }}
              >
                Estornada
              </Chip>
            ) : null}
          </Card.Content>
        </Card>

        <List.Section>
          {campos
            .filter(([, valor]) => Boolean(valor))
            .map(([titulo, valor]) => (
              <List.Item
                key={titulo}
                title={titulo}
                description={valor}
                descriptionNumberOfLines={4}
              />
            ))}
        </List.Section>

        {movimentacao.estornaId || movimentacao.estornadaPorId ? (
          <>
            <Divider />
            <List.Section>
              <List.Subheader>Vínculos</List.Subheader>
              {movimentacao.estornaId ? (
                <List.Item
                  title="Lançamento corrigido por este estorno"
                  left={(props) => <List.Icon {...props} icon="link-variant" />}
                  right={(props) => <List.Icon {...props} icon="chevron-right" />}
                  onPress={() => router.push(`/movimentacao/${movimentacao.estornaId}`)}
                />
              ) : null}
              {movimentacao.estornadaPorId ? (
                <List.Item
                  title="Estorno que corrigiu este lançamento"
                  left={(props) => <List.Icon {...props} icon="link-variant" />}
                  right={(props) => <List.Icon {...props} icon="chevron-right" />}
                  onPress={() => router.push(`/movimentacao/${movimentacao.estornadaPorId}`)}
                />
              ) : null}
            </List.Section>
          </>
        ) : null}

        <Text variant="bodySmall" style={styles.aviso}>
          O livro de movimentações não permite editar nem excluir. Lançamentos errados são
          corrigidos por estorno.
        </Text>
      </ScrollView>

      {podeEstornar(movimentacao, usuario) ? (
        <View style={styles.rodape}>
          <Button
            mode="outlined"
            icon="undo-variant"
            onPress={() => setEstornando(true)}
            contentStyle={styles.botao}
          >
            Estornar
          </Button>
        </View>
      ) : null}
      <EstornoDialog
        movimentacao={movimentacao}
        visivel={estornando}
        aoEstornar={aoAtualizar}
        aoFechar={() => setEstornando(false)}
      />
    </>
  );
}

const styles = StyleSheet.create({
  conteudo: { padding: 16, gap: 8, paddingBottom: 32 },
  selos: { flexDirection: 'row', gap: 8 },
  aviso: { marginTop: 8 },
  rodape: { padding: 16 },
  botao: { minHeight: 48 },
});
