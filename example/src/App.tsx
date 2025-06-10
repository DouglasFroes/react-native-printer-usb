import { useState } from 'react';
import { Button, ScrollView, StyleSheet, Text, View } from 'react-native';
import {
  barCode,
  clean,
  getList,
  off,
  printCut,
  printHtml,
  printImageBase64,
  printImageUri,
  printText,
  qrCode,
  sendRawData,
} from 'react-native-usb-printer';
import { img64 } from './img64';

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
    // Exemplo de impressão de texto formatado
    const result = await printText({
      text: 'Título em destaque\nLinha normal\nTexto em negrito',
      size: 2,
      align: 'center',
      bold: true,
      productId: selectedProductId,
      cut: true,
      beep: true,
    });
    setPrintResult(
      result.success
        ? 'Texto impresso com sucesso!'
        : 'Erro: ' + (result.message || '')
    );
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
    const result = await barCode({
      text: '123456789012',
      width: 2,
      height: 80,
      productId: selectedProductId,
    });
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
    const result = await qrCode({
      text: 'https://reactnative.dev',
      size: 6,
      productId: selectedProductId,
    });
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

  const handlePrintImageBase64 = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para imagem base64.');
      return;
    }
    // Exemplo base64 PNG (imagem preta 1x1)
    const result = await printImageBase64({
      base64Image: img64,
      align: 'center',
      productId: selectedProductId,
    });
    setPrintResult(
      result.success
        ? 'Imagem (base64) impressa!'
        : 'Erro: ' + (result.message || '')
    );
  };

  const handlePrintImageUri = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para imagem URI.');
      return;
    }
    // Exemplo de URI local (ajuste conforme necessário)
    const imageUri = 'https://avatars.githubusercontent.com/u/0';
    const result = await printImageUri({
      imageUri,
      align: 'center',
      productId: selectedProductId,
    });
    setPrintResult(
      result.success
        ? 'Imagem (URI) impressa!'
        : 'Erro: ' + (result.message || '')
    );
  };

  const handlePrintHtml = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para HTML.');
      return;
    }
    const html =
      '<div style="font-size:18px;text-align:center;">Impressão <b>HTML</b>!</div>';
    const result = await printHtml({
      html,
      align: 'center',
      productId: selectedProductId,
    });
    setPrintResult(
      result.success ? 'HTML impresso!' : 'Erro: ' + (result.message || '')
    );
  };

  const handleSendRawData = async () => {
    if (selectedProductId == null) {
      setPrintResult('Selecione um dispositivo para enviar RAW.');
      return;
    }
    // Exemplo: comando ESC/POS para bipar e cortar papel
    // Comando: <BEEP>\x1B\x42\x03\x02  <CUT>\x1D\x56\x00
    // Aqui usamos base64 para enviar bytes: BEEP+CUT = 'G0IDAg1WAA=='
    const base64Data = 'G0IDAg1WAA==';
    const result = await sendRawData(base64Data, selectedProductId);
    setPrintResult(
      result.success
        ? 'RAW enviado com sucesso!'
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
        <View style={styles.buttonSpacer} />
        <Button
          title="Imagem Base64"
          onPress={handlePrintImageBase64}
          color="#009688"
        />
        <View style={styles.buttonSpacer} />
        <Button
          title="Imagem URI"
          onPress={handlePrintImageUri}
          color="#8bc34a"
        />
        <View style={styles.buttonSpacer} />
        <Button title="HTML" onPress={handlePrintHtml} color="#f44336" />
        <View style={styles.buttonSpacer} />
        <Button title="RAW" onPress={handleSendRawData} color="#607d8b" />
        <View style={styles.buttonSpacer} />
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
        horizontal
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
