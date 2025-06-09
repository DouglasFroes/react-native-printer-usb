import { useState } from 'react';
import { Button, ScrollView, StyleSheet, Text, View } from 'react-native';
import {
  barCode,
  clean,
  getList,
  off,
  printCut,
  printText,
  qrCode,
} from 'react-native-usb-printer';
import { commands } from '../../src/utils/commands';

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
      `${commands.text_format.txt_normal}${commands.text_format.txt_align_lt}Titulo em destaque`,
      selectedProductId
    );
    // await printText(
    //   `${commands.text_format.txt_normal}Linha normal\n${commands.text_format.txt_italic_on}Texto em itálico\n`,
    //   selectedProductId
    // );
    // await printText(
    //   `${commands.horizontal_line.hr_58mm}${commands.text_format.txt_4square}GRANDE\n${commands.text_format.txt_normal}`,
    //   selectedProductId
    // );
    // const result = await printText(
    //   'Final do recibo\n' + commands.paper.paper_cut_a,
    //   selectedProductId
    // );

    if (result.success) {
      setPrintResult(result.message || 'Impressão realizada com sucesso!');
    } else {
      setPrintResult(
        'Erro ao imprimir: ' + (result.message || 'Erro desconhecido')
      );
    }
  };

  const handlePrintCut = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para cortar.');
      return;
    }
    const result = await printCut(true, true, selectedProductId);
    setPrintResult(
      result.success ? 'Corte realizado!' : 'Erro: ' + (result.message || '')
    );
  };

  const handleBarCode = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para código de barras.');
      return;
    }
    const result = await barCode('123456789012', 2, 80, selectedProductId);
    setPrintResult(
      result.success
        ? 'Código de barras impresso!'
        : 'Erro: ' + (result.message || '')
    );
  };

  const handleQrCode = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para QR Code.');
      return;
    }
    const result = await qrCode(
      'https://reactnative.dev',
      6,
      selectedProductId
    );
    setPrintResult(
      result.success ? 'QR Code impresso!' : 'Erro: ' + (result.message || '')
    );
  };

  const handleClean = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para limpar.');
      return;
    }
    const result = await clean(selectedProductId);
    setPrintResult(
      result.success ? 'Limpeza realizada!' : 'Erro: ' + (result.message || '')
    );
  };

  const handleOff = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para desligar.');
      return;
    }
    const result = await off(selectedProductId);
    setPrintResult(
      result.success
        ? 'Comando de desligar enviado!'
        : 'Erro: ' + (result.message || '')
    );
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
        <View style={styles.buttonSpacer} />
        <Button title="Cortar" onPress={handlePrintCut} color="#ff9800" />
        <View style={styles.buttonSpacer} />
        <Button
          title="Código de Barras"
          onPress={handleBarCode}
          color="#6a1b9a"
        />
        <View style={styles.buttonSpacer} />
        <Button title="QR Code" onPress={handleQrCode} color="#0288d1" />
        <View style={styles.buttonSpacer} />
        <Button title="Limpar" onPress={handleClean} color="#607d8b" />
        <View style={styles.buttonSpacer} />
        <Button title="Desligar" onPress={handleOff} color="#b71c1c" />
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
