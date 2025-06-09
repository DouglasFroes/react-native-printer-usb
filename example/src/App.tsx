import { useState } from 'react';
import { Button, ScrollView, StyleSheet, Text, View } from 'react-native';
import { getList, printText } from 'react-native-usb-printer';

export default function App() {
  const [devices, setDevices] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [printResult, setPrintResult] = useState<string | null>(null);
  const [selectedProductId, setSelectedProductId] = useState<number | null>(
    null
  );

  const refreshDevices = async () => {
    setLoading(true);
    try {
      const list = getList();
      setDevices(list);
    } catch (e) {
      setDevices([]);
    }
    setLoading(false);
  };

  const handlePrint = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para imprimir.');
      return;
    }
    const result = await printText(
      'Teste de impressão USB!',
      selectedProductId
    );
    if (result.success) {
      setPrintResult('Impressão realizada com sucesso!');
    } else {
      setPrintResult(
        'Erro ao imprimir: ' + (result.message || 'Erro desconhecido')
      );
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Impressora USB</Text>
      <View style={styles.buttonRow}>
        <Button
          title={loading ? 'Buscando...' : 'Buscar USB'}
          onPress={refreshDevices}
          disabled={loading}
          color="#1976d2"
        />
        <View style={styles.buttonSpacer} />
        <Button title="Imprimir texto" onPress={handlePrint} color="#388e3c" />
      </View>
      {printResult && (
        <Text
          style={[
            styles.result,
            printResult.startsWith('Erro') ? styles.error : styles.success,
          ]}
        >
          {printResult}
        </Text>
      )}
      <ScrollView
        style={styles.deviceList}
        contentContainerStyle={styles.deviceListContent}
      >
        {devices.length === 0 && (
          <Text style={styles.noDevice}>Nenhum dispositivo encontrado</Text>
        )}
        {devices.map((d, i) => (
          <View
            key={i}
            style={[
              styles.deviceCard,
              selectedProductId === d.productId && styles.deviceCardSelected,
            ]}
          >
            <Text style={styles.deviceTitle}>
              {d.productName || d.deviceName}
            </Text>
            <Text style={styles.deviceInfo}>
              Vendor ID: <Text style={styles.deviceValue}>{d.vendorId}</Text>
            </Text>
            <Text style={styles.deviceInfo}>
              Product ID: <Text style={styles.deviceValue}>{d.productId}</Text>
            </Text>
            <Text style={styles.deviceInfo}>
              Device ID: <Text style={styles.deviceValue}>{d.deviceId}</Text>
            </Text>
            {d.manufacturerName && (
              <Text style={styles.deviceInfo}>
                Fabricante:{' '}
                <Text style={styles.deviceValue}>{d.manufacturerName}</Text>
              </Text>
            )}
            {d.productName && (
              <Text style={styles.deviceInfo}>
                Produto: <Text style={styles.deviceValue}>{d.productName}</Text>
              </Text>
            )}
            {d.serialNumber && (
              <Text style={styles.deviceInfo}>
                Serial: <Text style={styles.deviceValue}>{d.serialNumber}</Text>
              </Text>
            )}
            <View style={styles.selectButtonWrapper}>
              <Button
                title={
                  selectedProductId === d.productId
                    ? 'Selecionado'
                    : 'Selecionar'
                }
                onPress={() => setSelectedProductId(d.productId)}
                color={selectedProductId === d.productId ? '#1976d2' : '#888'}
              />
            </View>
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f5f6fa',
    width: '100%',
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    color: '#222',
    marginTop: 24,
    marginBottom: 16,
    letterSpacing: 1,
  },
  buttonRow: {
    flexDirection: 'row',
    marginBottom: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonSpacer: {
    width: 16,
  },
  result: {
    marginTop: 10,
    fontSize: 16,
    fontWeight: 'bold',
    padding: 8,
    borderRadius: 6,
    textAlign: 'center',
  },
  error: {
    color: '#c62828',
    backgroundColor: '#ffebee',
    borderColor: '#c62828',
    borderWidth: 1,
  },
  success: {
    color: '#388e3c',
    backgroundColor: '#e8f5e9',
    borderColor: '#388e3c',
    borderWidth: 1,
  },
  deviceList: {
    maxHeight: 340,
    width: '100%',
    marginTop: 10,
  },
  deviceListContent: {
    paddingBottom: 30,
    alignItems: 'center',
  },
  noDevice: {
    marginTop: 20,
    color: '#888',
    fontSize: 16,
    textAlign: 'center',
  },
  deviceCard: {
    marginTop: 12,
    padding: 14,
    borderWidth: 1,
    borderRadius: 10,
    borderColor: '#b0bec5',
    backgroundColor: '#fff',
    width: 320,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  deviceCardSelected: {
    borderColor: '#1976d2',
    backgroundColor: '#e3f2fd',
  },
  deviceTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1976d2',
    marginBottom: 4,
  },
  deviceInfo: {
    fontSize: 15,
    color: '#333',
    marginBottom: 2,
  },
  deviceValue: {
    fontWeight: 'bold',
    color: '#222',
  },
  selectButtonWrapper: {
    marginTop: 10,
    alignItems: 'flex-end',
  },
});
