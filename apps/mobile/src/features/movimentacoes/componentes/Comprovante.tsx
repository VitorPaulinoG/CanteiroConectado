import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, List, Text } from 'react-native-paper';

import { FaixaEstadoEnvio } from '@/shared';
import type { Envio } from '@/shared/api';
import { ROTULO_DO_MOTIVO, type MotivoMovimentacao, type Movimentacao } from '@/shared/estoque';
import { formatarDataHora } from '@/shared/utils/format';
import { formatarQuantidade } from '@/shared/utils/quantidade';

import type { PedidoDeMovimentacao } from '../api';

export type ResumoDoPedido = {
  titulo: string;
  itemDescricao: string;
  itemCodigo: string;
  unidadeSimbolo: string;
  motivo: MotivoMovimentacao;
  quantidade: number;
  colaboradorNome?: string;
  frenteObra?: string;
  observacao?: string;
};

type Props = {
  resumo: ResumoDoPedido;
  envio: Envio<PedidoDeMovimentacao, Movimentacao>;
  aoCorrigir: () => void;
  aoNovoRegistro: () => void;
  aoConcluir: () => void;
  rotuloNovoRegistro: string;
};

export function Comprovante({
  resumo,
  envio,
  aoCorrigir,
  aoNovoRegistro,
  aoConcluir,
  rotuloNovoRegistro,
}: Props) {
  const gravada = envio.resposta;
  const linhas: { titulo: string; valor?: string | null }[] = [
    { titulo: 'Item', valor: `${resumo.itemDescricao} (${resumo.itemCodigo})` },
    { titulo: 'Quantidade', valor: formatarQuantidade(resumo.quantidade, resumo.unidadeSimbolo) },
    { titulo: 'Motivo', valor: ROTULO_DO_MOTIVO[resumo.motivo] },
    { titulo: 'Colaborador', valor: gravada?.colaboradorNome ?? resumo.colaboradorNome },
    { titulo: 'Frente de obra', valor: gravada?.frenteObra ?? resumo.frenteObra },
    { titulo: 'Observação', valor: gravada?.observacao ?? resumo.observacao },
    {
      titulo: 'Saldo depois',
      valor: gravada ? formatarQuantidade(gravada.saldoApos, gravada.unidadeSimbolo) : null,
    },
    { titulo: 'Registrado por', valor: gravada?.registradoPorNome },
    { titulo: 'Data e hora', valor: gravada ? formatarDataHora(gravada.criadoEm) : null },
  ];

  return (
    <View style={styles.tela}>
      <ScrollView contentContainerStyle={styles.conteudo}>
        <Text variant="headlineSmall" accessibilityRole="header">
          {resumo.titulo}
        </Text>
        <FaixaEstadoEnvio
          estado={envio.estado}
          erro={envio.erro}
          aoTentarDeNovo={envio.tentarDeNovo}
          aoCorrigir={aoCorrigir}
        />
        <Card mode="outlined">
          {linhas
            .filter((linha) => Boolean(linha.valor))
            .map((linha) => (
              <List.Item key={linha.titulo} title={linha.titulo} description={linha.valor} />
            ))}
        </Card>
        {!envio.salvo ? (
          <Text variant="bodyMedium">
            O registro só vale depois de aparecer “Salvo no servidor”. Se sair agora, o envio
            continua, e reenviar não duplica.
          </Text>
        ) : null}
      </ScrollView>
      {envio.salvo ? (
        <View style={styles.rodape}>
          <Button mode="contained" onPress={aoNovoRegistro} contentStyle={styles.botao}>
            {rotuloNovoRegistro}
          </Button>
          <Button mode="outlined" onPress={aoConcluir} contentStyle={styles.botao}>
            Concluir
          </Button>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1 },
  conteudo: { padding: 16, gap: 16 },
  rodape: { padding: 16, gap: 8 },
  botao: { minHeight: 48 },
});
