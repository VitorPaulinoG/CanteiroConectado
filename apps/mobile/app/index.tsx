import { StyleSheet, Text, View } from 'react-native';

export default function TelaInicial() {
  return (
    <View style={styles.container}>
      <Text style={styles.titulo}>Canteiro Conectado</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  titulo: {
    fontSize: 20,
    fontWeight: 'bold',
  },
});
